package tn.gezenthi.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationUnitTests {

    @Test
    void phoneNumbersAreNormalisedForWhatsApp() {
        assertThat(WhatsAppSender.toE164Digits("+216 20 123 456", "216")).isEqualTo("21620123456");
        assertThat(WhatsAppSender.toE164Digits("20 123 456", "216")).isEqualTo("21620123456");
        assertThat(WhatsAppSender.toE164Digits("0033 6 12 34 56 78", "216")).isEqualTo("33612345678");
        assertThat(WhatsAppSender.toE164Digits("123", "216")).isNull();
    }

    @Test
    void recipientsAreMaskedInTheLog() {
        assertThat(NotificationService.maskEmail("amira@example.tn")).isEqualTo("a***@example.tn");
        assertThat(NotificationService.maskPhone("21620123456")).isEqualTo("********456");
    }

    @Test
    void emailTemplatesEscapeHtml() {
        BookingMessage b = new BookingMessage(UUID.randomUUID(), "GZ-2027-000001", "NEW", "<script>x</script> Hotel",
                "Hammamet", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 4), 2, 1, "Double", "All inclusive",
                new BigDecimal("1100.000"), "TND", "Mrs", "Amira", "Ben Ali", "a@b.tn", "+216 20 000 000", false);
        MessageTemplates.Mail m = MessageTemplates.customerReceived(b, "+216 56 762 281");
        assertThat(m.subject()).contains("GZ-2027-000001");
        assertThat(m.html()).doesNotContain("<script>").contains("&lt;script&gt;");
        assertThat(m.text()).contains("Bonjour Mme Ben Ali").contains("3 nuit(s)").contains("1 enfant(s)");
        assertThat(MessageTemplates.customerStatus(b, "x")).as("NEW status sends nothing").isNull();
    }
}
