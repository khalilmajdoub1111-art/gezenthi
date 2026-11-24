package tn.gezenthi.catalog.flight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import tn.gezenthi.catalog.CatalogEntity;

/** "From" price for a return flight departing Tunis. */
@Entity
@Table(name = "flight_deal")
public class FlightDeal extends CatalogEntity {

    @Column(nullable = false, length = 80) String city;
    @Column(name = "airport_code", nullable = false, length = 4) String airportCode;
    @Column(name = "price_from", nullable = false) Integer priceFrom;

    void apply(FlightDealDto d) {
        setPublished(d.published());
        city = d.city().trim();
        airportCode = d.airportCode().trim().toUpperCase(Locale.ROOT);
        priceFrom = d.priceFrom();
    }
}
