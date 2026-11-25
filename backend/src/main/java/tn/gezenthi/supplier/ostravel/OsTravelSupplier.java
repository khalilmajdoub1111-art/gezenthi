package tn.gezenthi.supplier.ostravel;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.SupplierException;
import tn.gezenthi.supplier.SupplierProperties;
import tn.gezenthi.supplier.SupplierRateLimitException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Adapter for suppliers running the OS-TRAVEL Hotel API v2 (TunisiaBeds and others).
 * Docs: https://api-edocs.os-travel.com — every call is POST [base]/api/hotel/{Method}
 * with a JSON body containing the Credential object.
 *
 * READ-ONLY on purpose: only static data, connection test and availability search are
 * implemented. BookingCreation / BookingCancellation are NOT called anywhere.
 */
public class OsTravelSupplier implements HotelSupplier {

    private static final Logger log = LoggerFactory.getLogger(OsTravelSupplier.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    static final int MAX_HOTELS_PER_SEARCH = 200;
    static final int MAX_RATE_LIMIT_RETRIES = 4;
    /** Known supplier limits (TunisiaBeds: "ListHotel is limited to 5 calls per minute"). Kept 1 below to be safe. */
    private static final Map<String, Integer> CALLS_PER_MINUTE = Map.of("ListHotel", 4);

    private final Map<String, Deque<Instant>> recentCalls = new HashMap<>();

    private final String code;
    private final SupplierProperties.Supplier config;
    private final JsonMapper json;
    private final RestClient http;

    public OsTravelSupplier(String code, SupplierProperties.Supplier config, JsonMapper json) {
        this.code = code;
        this.config = config;
        this.json = json;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(25));
        String base = config.getBaseUrl() == null ? "" : config.getBaseUrl().replaceAll("/+$", "");
        this.http = RestClient.builder().requestFactory(factory).baseUrl(base + "/api/hotel/").build();
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String displayName() {
        return config.getName() == null ? code : config.getName();
    }

    @Override
    public boolean configured() {
        return notBlank(config.getBaseUrl()) && notBlank(config.getLogin()) && notBlank(config.getPassword());
    }

    @Override
    public String testConnection() {
        List<JsonNode> countries = OsTravelParser.list(call("ListCountry", body()), "ListCountry");
        return "Connexion OK — " + countries.size() + " pays disponibles.";
    }

    @Override
    public void fetchHotels(List<String> countryCodes, CityBatchListener listener) {
        List<String> wanted = countryCodes.stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
        List<JsonNode> cities = new ArrayList<>();
        for (JsonNode country : OsTravelParser.list(callPatiently("ListCountry", body()), "ListCountry")) {
            if (!wanted.contains(OsTravelParser.text(country, "Code").toLowerCase(Locale.ROOT))) continue;
            ObjectNode cityReq = body();
            cityReq.put("Country", country.path("Id").asInt());
            cities.addAll(OsTravelParser.list(callPatiently("ListCity", cityReq), "ListCity"));
        }
        for (int i = 0; i < cities.size(); i++) {
            JsonNode city = cities.get(i);
            String cityId = OsTravelParser.text(city, "Id");
            String cityName = OsTravelParser.text(city, "Name");
            ObjectNode hotelReq = body();
            hotelReq.put("City", city.path("Id").asInt());
            List<StaticHotel> hotels = new ArrayList<>();
            for (JsonNode h : OsTravelParser.list(callPatiently("ListHotel", hotelReq), "ListHotel")) {
                hotels.add(OsTravelParser.staticHotel(h, cityId, cityName));
            }
            listener.onCity(cityName, i + 1, cities.size(), hotels);
        }
    }

    @Override
    public List<Offer> search(SearchQuery query, List<String> hotelIds) {
        List<Offer> all = new ArrayList<>();
        for (int i = 0; i < hotelIds.size(); i += MAX_HOTELS_PER_SEARCH) {
            List<String> chunk = hotelIds.subList(i, Math.min(i + MAX_HOTELS_PER_SEARCH, hotelIds.size()));
            ObjectNode req = body();
            ObjectNode details = req.putObject("SearchDetails");
            ObjectNode booking = details.putObject("BookingDetails");
            booking.put("CheckIn", DATE.format(query.checkIn()));
            booking.put("CheckOut", DATE.format(query.checkOut()));
            ArrayNode hotels = booking.putArray("Hotels");
            chunk.forEach(id -> hotels.add(Integer.parseInt(id)));
            // false: also get "on request" / stop-sale rates; unbookable rooms are flagged by the parser
            // (StopReservation) and excluded from the "from" price, instead of hiding the hotel.
            details.putObject("Filters").put("OnlyAvailable", false);
            ArrayNode rooms = details.putArray("Rooms");
            for (Room r : query.rooms()) {
                ObjectNode room = rooms.addObject();
                room.put("Adult", r.adults());
                ArrayNode child = room.putArray("Child");
                r.childAges().forEach(child::add);
            }
            all.addAll(OsTravelParser.offers(call("HotelSearch", req)));
        }
        return all;
    }

    @Override
    public HotelDetails hotelDetail(String externalHotelId) {
        ObjectNode req = body();
        req.put("Hotel", Integer.parseInt(externalHotelId));
        return OsTravelParser.hotelDetails(call("HotelDetail", req));
    }

    // ------------------------------------------------------------------ HTTP

    /** Request body pre-filled with the Credential object. Never logged. */
    private ObjectNode body() {
        ObjectNode root = json.createObjectNode();
        ObjectNode cred = root.putObject("Credential");
        cred.put("Login", config.getLogin());
        cred.put("Password", config.getPassword());
        return root;
    }

    /**
     * For background jobs (sync): respects the per-method rate limit and, if the supplier still
     * answers 429, waits until its "Retry after" time and tries again (bounded).
     * Never used for customer searches, which must answer fast.
     */
    private JsonNode callPatiently(String method, ObjectNode body) {
        for (int attempt = 1; ; attempt++) {
            throttle(method);
            try {
                return call(method, body);
            } catch (SupplierRateLimitException e) {
                if (attempt >= MAX_RATE_LIMIT_RETRIES) throw e;
                Duration wait = e.retryAfter() == null ? Duration.ofSeconds(60)
                        : Duration.between(Instant.now(), e.retryAfter()).plusSeconds(2);
                if (wait.isNegative() || wait.isZero()) wait = Duration.ofSeconds(2);
                if (wait.compareTo(Duration.ofMinutes(5)) > 0) throw e; // don't block a job for too long
                log.info("supplier={} method={} rate-limited, waiting {}s (attempt {})", code, method, wait.toSeconds(), attempt);
                sleep(wait);
            }
        }
    }

    /** Keeps at most N calls per rolling minute for methods the supplier limits. */
    private void throttle(String method) {
        Integer perMinute = CALLS_PER_MINUTE.get(method);
        if (perMinute == null) return;
        synchronized (recentCalls) {
            Deque<Instant> calls = recentCalls.computeIfAbsent(method, m -> new ArrayDeque<>());
            Instant windowStart = Instant.now().minusSeconds(60);
            while (!calls.isEmpty() && calls.peekFirst().isBefore(windowStart)) calls.pollFirst();
            if (calls.size() >= perMinute) {
                Duration wait = Duration.between(Instant.now(), calls.peekFirst().plusSeconds(61));
                if (!wait.isNegative()) sleep(wait);
                calls.pollFirst();
            }
            calls.addLast(Instant.now());
        }
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new SupplierException("Synchronisation interrompue.");
        }
    }

    private JsonNode call(String method, ObjectNode body) {
        if (!configured()) {
            throw new SupplierException(displayName() + " : non configuré (URL, login ou mot de passe manquant).");
        }
        long start = System.nanoTime();
        try {
            String payload = json.writeValueAsString(body);
            String response = http.post().uri(method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((req, res) -> new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            JsonNode root = json.readTree(response == null || response.isBlank() ? "{}" : response);
            log.info("supplier={} method={} durationMs={} shape={}", code, method, (System.nanoTime() - start) / 1_000_000, shape(root));
            OsTravelParser.checkError(displayName(), root);
            return root;
        } catch (SupplierException e) {
            log.warn("supplier={} method={} error={}", code, method, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Only the exception type/message is logged — never the request body (it contains the password).
            log.warn("supplier={} method={} failed: {}", code, method, e.getClass().getSimpleName());
            throw new SupplierException(displayName() + " : appel " + method + " impossible (" + e.getClass().getSimpleName() + ").", e);
        }
    }

    /**
     * Structure only, for diagnostics: top-level field names with array sizes / value kinds.
     * Never logs values (no prices, tokens or personal data).
     */
    static String shape(JsonNode root) {
        if (root.isArray()) return "array[" + root.size() + "]";
        StringBuilder sb = new StringBuilder("{");
        root.propertyNames().forEach(name -> {
            JsonNode v = root.get(name);
            String kind = v.isArray() ? "array[" + v.size() + "]" : v.isObject() ? "object" : v.isNull() ? "null" : v.getNodeType().name().toLowerCase(Locale.ROOT);
            sb.append(name).append(':').append(kind).append(' ');
        });
        return sb.toString().trim() + "}";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
