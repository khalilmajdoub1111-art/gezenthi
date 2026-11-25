package tn.gezenthi.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API shapes for bookings (public request, offer summary, admin views). */
public final class BookingDtos {

    private BookingDtos() {
    }

    // ------------------------------------------------------------------ public

    /** Only the offer + chosen room are referenced: price/hotel/dates come from the server. */
    public record CreateBooking(
            @NotBlank @Size(max = 60) String offerId,
            @Min(0) int board,
            @Min(0) int room,
            @Valid @NotNull Customer customer,
            @NotNull @Size(min = 1, max = 10) List<@Valid PassengerIn> passengers,
            @Size(max = 1000) String comment,
            @AssertTrue(message = "Vous devez accepter le traitement de vos données pour cette réservation.") boolean consent,
            /* optional, explicit opt-in (Meta policy); absent = no WhatsApp */
            Boolean whatsappOptIn) {
    }

    public record Customer(
            @NotNull @Pattern(regexp = "Mr|Mrs|Ms", message = "civilité invalide") String civility,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = "^[+0-9 ()./-]{6,30}$", message = "numéro de téléphone invalide") String phone) {
    }

    public record PassengerIn(
            @NotNull @Pattern(regexp = "ADULT|CHILD") String type,
            @Pattern(regexp = "Mr|Mrs|Ms", message = "civilité invalide") String civility,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName) {
    }

    public record Created(String reference, String status) {
    }

    /** What the booking page shows before the customer fills the form. */
    public record OfferSummary(String hotelName, String city, Integer stars, String image, LocalDate checkIn,
                               LocalDate checkOut, long nights, int adults, List<Integer> childAges,
                               String boardName, String roomName, BigDecimal totalPrice, String currency,
                               boolean refundable, String cancellationDeadline, boolean available) {
    }

    // ------------------------------------------------------------------ admin

    public record BookingSummary(UUID id, String reference, String status, String origin, String hotelName,
                                 String city, LocalDate checkIn, LocalDate checkOut, String customerName,
                                 BigDecimal totalPrice, String currency, Instant createdAt) {
    }

    public record PassengerOut(int position, String type, String civility, String firstName, String lastName, Integer age) {
    }

    public record EventOut(Instant at, String actor, String action, String details) {
    }

    public record BookingDetail(UUID id, String reference, String status, String origin, String hotelRef,
                                String hotelName, String city, Integer stars, LocalDate checkIn, LocalDate checkOut,
                                int adults, List<Integer> childAges, String boardName, String roomName,
                                BigDecimal totalPrice, String currency, boolean refundable, String cancellationDeadline,
                                String customerCivility, String customerFirstName, String customerLastName,
                                String customerEmail, String customerPhone, String comment, Instant consentAt,
                                String supplierName, String supplierBookingId, String supplierState,
                                BigDecimal supplierPrice, String voucherUrl, boolean supplierBookingEnabled,
                                List<PassengerOut> passengers, List<EventOut> events, Instant createdAt,
                                boolean whatsappOptIn,
                                List<tn.gezenthi.notification.NotificationService.LogEntry> notifications,
                                tn.gezenthi.notification.NotificationService.ChannelStatus channels) {
    }

    public record StatusChange(@NotNull BookingRequest.Status status, @Size(max = 1000) String note) {
    }

    public record Note(@NotBlank @Size(max = 1000) String note) {
    }

    /** Result of a supplier pre-booking (verification only). */
    public record SupplierCheckResult(boolean available, BigDecimal supplierPrice, String currency, boolean onRequest,
                                      BigDecimal sellPrice, String message) {
    }
}
