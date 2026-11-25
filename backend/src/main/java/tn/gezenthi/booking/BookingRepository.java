package tn.gezenthi.booking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingRequest, UUID> {

    List<BookingRequest> findAllByOrderByCreatedAtDesc();

    List<BookingRequest> findByStatusOrderByCreatedAtDesc(BookingRequest.Status status);

    long countByStatus(BookingRequest.Status status);
}
