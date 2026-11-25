package tn.gezenthi.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gezenthi.notifications.* — everything is OFF until configured. Secrets (SMTP password,
 * WhatsApp token) come only from the private env file / environment variables.
 */
@ConfigurationProperties(prefix = "gezenthi.notifications")
public class NotificationProperties {

    /** Public site URL, used for links in messages. */
    private String siteUrl = "http://localhost:4200";
    /** Agency inbox that receives "new booking" alerts. */
    private String agencyEmail = "";
    /** Agency phone shown in customer messages. */
    private String agencyPhone = "+216 56 762 281";
    private final Email email = new Email();
    private final WhatsApp whatsapp = new WhatsApp();

    public static class Email {
        private boolean enabled = false;
        private String host = "";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "";
        private boolean startTls = true;
        private boolean ssl = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public boolean isStartTls() { return startTls; }
        public void setStartTls(boolean startTls) { this.startTls = startTls; }
        public boolean isSsl() { return ssl; }
        public void setSsl(boolean ssl) { this.ssl = ssl; }

        boolean ready() {
            return enabled && !host.isBlank() && !from.isBlank();
        }

        @Override
        public String toString() { // never print the password
            return "Email[enabled=" + enabled + ", host=" + host + ", port=" + port + ", from=" + from + "]";
        }
    }

    /** WhatsApp Business Cloud API (Meta). Needs a verified business, a number and an APPROVED template. */
    public static class WhatsApp {
        private boolean enabled = false;
        private String token = "";
        private String phoneNumberId = "";
        private String apiVersion = "v21.0";
        /** Approved template name; body params: {{1}} first name, {{2}} reference, {{3}} hotel, {{4}} dates. */
        private String template = "booking_received";
        private String language = "fr";
        /** Country code added to local numbers (Tunisia). */
        private String defaultCountryCode = "216";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getDefaultCountryCode() { return defaultCountryCode; }
        public void setDefaultCountryCode(String defaultCountryCode) { this.defaultCountryCode = defaultCountryCode; }

        boolean ready() {
            return enabled && !token.isBlank() && !phoneNumberId.isBlank();
        }

        @Override
        public String toString() { // never print the token
            return "WhatsApp[enabled=" + enabled + ", phoneNumberId=" + phoneNumberId + ", template=" + template + "]";
        }
    }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getAgencyEmail() { return agencyEmail; }
    public void setAgencyEmail(String agencyEmail) { this.agencyEmail = agencyEmail; }
    public String getAgencyPhone() { return agencyPhone; }
    public void setAgencyPhone(String agencyPhone) { this.agencyPhone = agencyPhone; }
    public Email getEmail() { return email; }
    public WhatsApp getWhatsapp() { return whatsapp; }
}
