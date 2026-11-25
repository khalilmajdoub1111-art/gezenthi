package tn.gezenthi.supplier;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supplier configuration, one entry per supplier code:
 *
 * <pre>
 * gezenthi.suppliers.tunisiabeds.type=os-travel
 * gezenthi.suppliers.tunisiabeds.base-url=https://...
 * gezenthi.suppliers.tunisiabeds.login=${TUNISIABEDS_LOGIN:}
 * gezenthi.suppliers.tunisiabeds.password=${TUNISIABEDS_PASSWORD:}
 * </pre>
 *
 * Credentials always come from the private env file / environment variables, never from Git.
 */
@ConfigurationProperties(prefix = "gezenthi")
public class SupplierProperties {

    private Map<String, Supplier> suppliers = new LinkedHashMap<>();

    public Map<String, Supplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(Map<String, Supplier> suppliers) {
        this.suppliers = suppliers;
    }

    public static class Supplier {
        /** Adapter to use. Currently: os-travel. */
        private String type = "os-travel";
        private String name;
        private String baseUrl = "";
        private String login = "";
        private String password = "";
        /** Included in public search results. */
        private boolean enabled = false;
        /** Agency margin added on top of the supplier price, in percent. */
        private BigDecimal markupPercent = BigDecimal.TEN;
        /** Countries to synchronise (ISO codes, e.g. tn). */
        private List<String> countries = List.of("tn");
        /** Safety switch for the future booking workflow: real reservations are impossible while false. */
        private boolean bookingEnabled = false;
        /**
         * If not empty, only these supplier hotel ids are searched. Needed while the supplier
         * account is in test mode: TunisiaBeds rejects a whole search ("Hôtel(s) non affecter")
         * as soon as ONE requested hotel is not assigned to the account.
         */
        private List<String> onlyHotels = List.of();

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public BigDecimal getMarkupPercent() { return markupPercent; }
        public void setMarkupPercent(BigDecimal markupPercent) { this.markupPercent = markupPercent; }
        public List<String> getCountries() { return countries; }
        public void setCountries(List<String> countries) { this.countries = countries; }
        public boolean isBookingEnabled() { return bookingEnabled; }
        public void setBookingEnabled(boolean bookingEnabled) { this.bookingEnabled = bookingEnabled; }
        public List<String> getOnlyHotels() { return onlyHotels; }
        public void setOnlyHotels(List<String> onlyHotels) { this.onlyHotels = onlyHotels == null ? List.of() : onlyHotels; }

        @Override
        public String toString() { // never print credentials
            return "Supplier[type=" + type + ", baseUrl=" + baseUrl + ", enabled=" + enabled + "]";
        }
    }
}
