package tn.gezenthi.notification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Snapshot of a booking for rendering messages (decoupled from the JPA entity). */
public record BookingMessage(UUID bookingId, String reference, String status, String hotelName, String city,
                             LocalDate checkIn, LocalDate checkOut, int adults, int children, String roomName,
                             String boardName, BigDecimal totalPrice, String currency, String customerCivility,
                             String customerFirstName, String customerLastName, String customerEmail,
                             String customerPhone, boolean whatsappOptIn) {

    public long nights() {
        return checkIn.until(checkOut).getDays();
    }
}
