package tn.gezenthi.booking;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** A booking request made on the website. Contains personal data — admin access only, never logged. */
@Entity
@Table(name = "booking_request")
public class BookingRequest {

    public enum Status { NEW, IN_PROGRESS, PRICE_CHECKED, CONFIRMED, CANCELLED, REJECTED }

    @Id @UuidGenerator UUID id;
    @Column(nullable = false, length = 20, updatable = false) String reference;
    @Column(nullable = false, length = 20) @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING) Status status = Status.NEW;
    @Column(nullable = false, length = 12) String origin;

    @Column(name = "hotel_ref", length = 60) String hotelRef;
    @Column(name = "hotel_name", nullable = false, length = 200) String hotelName;
    @Column(length = 120) String city;
    Integer stars;
    @Column(name = "supplier_code", length = 40) String supplierCode;
    @Column(name = "supplier_hotel_id", length = 40) String supplierHotelId;
    @Column(name = "supplier_city_id", length = 40) String supplierCityId;

    @Column(name = "check_in", nullable = false) LocalDate checkIn;
    @Column(name = "check_out", nullable = false) LocalDate checkOut;
    @Column(nullable = false) int adults;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "child_ages", nullable = false, columnDefinition = "integer[]") List<Integer> childAges = new ArrayList<>();
    @Column(name = "board_id", length = 40) String boardId;
    @Column(name = "board_name", length = 120) String boardName;
    @Column(name = "room_id", length = 40) String roomId;
    @Column(name = "room_name", length = 200) String roomName;
    @Column(name = "total_price", nullable = false, precision = 12, scale = 3) BigDecimal totalPrice;
    @Column(nullable = false, length = 3) String currency;
    @Column(nullable = false) boolean refundable;
    @Column(name = "cancellation_deadline", length = 40) String cancellationDeadline;

    @Column(name = "customer_civility", nullable = false, length = 5) String customerCivility;
    @Column(name = "customer_first_name", nullable = false, length = 80) String customerFirstName;
    @Column(name = "customer_last_name", nullable = false, length = 80) String customerLastName;
    @Column(name = "customer_email", nullable = false, length = 160) String customerEmail;
    @Column(name = "customer_phone", nullable = false, length = 30) String customerPhone;
    @Column(length = 1000) String comment;
    @Column(name = "consent_at", nullable = false) Instant consentAt;

    @Column(name = "supplier_booking_id", length = 40) String supplierBookingId;
    @Column(name = "supplier_state", length = 20) String supplierState;
    @Column(name = "supplier_price", precision = 12, scale = 3) BigDecimal supplierPrice;
    @Column(name = "voucher_url", columnDefinition = "text") String voucherUrl;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position")
    List<BookingPassenger> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("at")
    List<BookingEvent> events = new ArrayList<>();

    void addPassenger(BookingPassenger p) {
        p.booking = this;
        p.position = passengers.size() + 1;
        passengers.add(p);
    }

    void log(String actor, String action, String details) {
        BookingEvent e = new BookingEvent();
        e.booking = this;
        e.at = Instant.now();
        e.actor = actor;
        e.action = action;
        e.details = details == null ? null : details.substring(0, Math.min(1000, details.length()));
        events.add(e);
    }
}
