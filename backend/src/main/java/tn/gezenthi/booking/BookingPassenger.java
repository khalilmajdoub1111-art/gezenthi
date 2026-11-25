package tn.gezenthi.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "booking_passenger")
public class BookingPassenger {

    @Id @UuidGenerator UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") BookingRequest booking;
    @Column(nullable = false) int position;
    @Column(nullable = false, length = 5) String type; // ADULT | CHILD
    @Column(length = 5) String civility;
    @Column(name = "first_name", nullable = false, length = 80) String firstName;
    @Column(name = "last_name", nullable = false, length = 80) String lastName;
    Integer age;
}
