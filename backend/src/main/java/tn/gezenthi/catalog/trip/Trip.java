package tn.gezenthi.catalog.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tn.gezenthi.catalog.CatalogEntity;

/** Organised trip abroad ("Voyages à l'étranger"). */
@Entity
@Table(name = "trip")
public class Trip extends CatalogEntity {

    @Column(nullable = false, length = 160) String name;
    @Column(nullable = false, length = 80) String country;
    @Column(name = "duration_days", nullable = false) Integer durationDays;
    @Column(name = "price_from", nullable = false) Integer priceFrom;
    @Column(nullable = false, columnDefinition = "text") String image;
    @Column(name = "no_visa", nullable = false) boolean noVisa;
    @Column(length = 40) String badge;

    void apply(TripDto d) {
        setPublished(d.published());
        name = d.name().trim();
        country = d.country().trim();
        durationDays = d.durationDays();
        priceFrom = d.priceFrom();
        image = d.image();
        noVisa = d.noVisa();
        badge = d.badge() == null || d.badge().isBlank() ? null : d.badge().trim();
    }
}
