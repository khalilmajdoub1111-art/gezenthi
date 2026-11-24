package tn.gezenthi.catalog.flight;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightDealRepository extends JpaRepository<FlightDeal, UUID> {
}
