package tn.gezenthi.catalog.service;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, UUID> {
}
