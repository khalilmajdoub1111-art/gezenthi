package tn.gezenthi.inquiry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import tn.gezenthi.booking.BookingDtos;
import tn.gezenthi.notification.NotificationService;

/** API shapes for quote requests ("demandes de devis"). */
public final class InquiryDtos {

    private InquiryDtos() {
    }

    public static final String TYPES = "HOTEL|TRIP|OMRA|VISA|FLIGHT|FERRY|OTHER";

    public record CreateInquiry(
            @NotNull @Pattern(regexp = TYPES, message = "type de demande inconnu") String type,
            @NotBlank @Size(max = 200) String subject,
            @Size(max = 80) String offerRef,
            LocalDate dateFrom,
            LocalDate dateTo,
            @NotNull @Min(1) @Max(20) Integer adults,
            @Min(0) @Max(10) Integer children,
            @Size(max = 1500) String message,
            @Valid @NotNull BookingDtos.Customer customer,
            Boolean whatsappOptIn,
            @NotNull(message = "Vous devez accepter le traitement de vos données.")
            @AssertTrue(message = "Vous devez accepter le traitement de vos données.") Boolean consent) {
    }

    public record Created(String reference) {
    }

    public record InquirySummary(UUID id, String reference, String status, String type, String subject,
                                 LocalDate dateFrom, LocalDate dateTo, int adults, int children,
                                 String customerName, Instant createdAt) {
    }

    public record EventOut(Instant at, String actor, String action, String details) {
    }

    public record InquiryDetail(UUID id, String reference, String status, String type, String subject, String offerRef,
                                LocalDate dateFrom, LocalDate dateTo, int adults, int children, String message,
                                String customerCivility, String customerFirstName, String customerLastName,
                                String customerEmail, String customerPhone, boolean whatsappOptIn, Instant consentAt,
                                Instant createdAt, List<EventOut> events,
                                List<NotificationService.LogEntry> notifications,
                                NotificationService.ChannelStatus channels) {
    }

    public enum Status { NEW, IN_PROGRESS, DONE, CANCELLED }

    public record StatusChange(@NotNull Status status, @Size(max = 1000) String note) {
    }

    public record Note(@NotBlank @Size(max = 1000) String note) {
    }
}
