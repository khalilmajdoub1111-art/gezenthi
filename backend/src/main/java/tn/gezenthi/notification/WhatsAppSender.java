package tn.gezenthi.notification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * WhatsApp Business Cloud API (Meta): sends an APPROVED template message.
 * Business-initiated WhatsApp messages are only allowed with templates approved by Meta and
 * with the customer's opt-in — free-form text is not possible here.
 * Docs: https://developers.facebook.com/docs/whatsapp/cloud-api/guides/send-message-templates
 */
@Component
class WhatsAppSender {

    private final NotificationProperties.WhatsApp config;
    private final JsonMapper json;
    private final RestClient http;

    WhatsAppSender(NotificationProperties props, JsonMapper json) {
        this.config = props.getWhatsapp();
        this.json = json;
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
        f.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().requestFactory(f).baseUrl("https://graph.facebook.com").build();
    }

    boolean ready() {
        return config.ready();
    }

    /** to = E.164 digits without "+"; params fill {{1}}..{{n}} of the template body. */
    void sendTemplate(String to, List<String> params) {
        ObjectNode body = json.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("to", to);
        body.put("type", "template");
        ObjectNode template = body.putObject("template");
        template.put("name", config.getTemplate());
        template.putObject("language").put("code", config.getLanguage());
        ArrayNode parameters = template.putArray("components").addObject().put("type", "body").putArray("parameters");
        params.forEach(p -> parameters.addObject().put("type", "text").put("text", p));

        http.post().uri("/{v}/{id}/messages", config.getApiVersion(), config.getPhoneNumberId())
                .header("Authorization", "Bearer " + config.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.writeValueAsString(body))
                .retrieve()
                .toBodilessEntity();
    }

    /** "+216 20 000 000" / "20000000" / "00216…" → "21620000000". Null if not plausible. */
    static String toE164Digits(String phone, String defaultCountryCode) {
        if (phone == null) return null;
        String d = phone.replaceAll("[^0-9+]", "");
        if (d.startsWith("+")) d = d.substring(1);
        else if (d.startsWith("00")) d = d.substring(2);
        else if (d.length() <= 8) d = defaultCountryCode + d;
        d = d.replaceAll("\\D", "");
        return d.length() >= 10 && d.length() <= 15 ? d : null;
    }
}
