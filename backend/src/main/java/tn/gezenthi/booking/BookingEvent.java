package tn.gezenthi.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** History line of a booking: who did what and when (no personal data in details). */
@Entity
@Table(name = "booking_event")
public class BookingEvent {

    @Id @UuidGenerator UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") BookingRequest booking;
    @Column(nullable = false) Instant at;
    @Column(nullable = false, length = 160) String actor;
    @Column(nullable = false, length = 40) String action;
    @Column(length = 1000) String details;
}
