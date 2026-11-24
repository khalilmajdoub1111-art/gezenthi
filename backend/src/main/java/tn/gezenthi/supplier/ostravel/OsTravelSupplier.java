package tn.gezenthi.supplier.ostravel;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.SupplierException;
import tn.gezenthi.supplier.SupplierProperties;
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
    public List<StaticHotel> fetchHotels(List<String> countryCodes) {
        List<String> wanted = countryCodes.stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
        List<StaticHotel> hotels = new ArrayList<>();
        for (JsonNode country : OsTravelParser.list(call("ListCountry", body()), "ListCountry")) {
            if (!wanted.contains(OsTravelParser.text(country, "Code").toLowerCase(Locale.ROOT))) continue;
            ObjectNode cityReq = body();
            cityReq.put("Country", country.path("Id").asInt());
            for (JsonNode city : OsTravelParser.list(call("ListCity", cityReq), "ListCity")) {
                String cityId = OsTravelParser.text(city, "Id");
                String cityName = OsTravelParser.text(city, "Name");
                ObjectNode hotelReq = body();
                hotelReq.put("City", city.path("Id").asInt());
                for (JsonNode h : OsTravelParser.list(call("ListHotel", hotelReq), "ListHotel")) {
                    hotels.add(OsTravelParser.staticHotel(h, cityId, cityName));
                }
            }
        }
        return hotels;
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
            details.putObject("Filters").put("OnlyAvailable", true);
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

    // ------------------------------------------------------------------ HTTP

    /** Request body pre-filled with the Credential object. Never logged. */
    private ObjectNode body() {
        ObjectNode root = json.createObjectNode();
        ObjectNode cred = root.putObject("Credential");
        cred.put("Login", config.getLogin());
        cred.put("Password", config.getPassword());
        return root;
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
            log.info("supplier={} method={} durationMs={}", code, method, (System.nanoTime() - start) / 1_000_000);
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

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
