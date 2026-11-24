package tn.gezenthi.catalog.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tn.gezenthi.catalog.CatalogEntity;

/** On-request service: visa file, CTN ferry crossing, TRE pack. */
@Entity
@Table(name = "service_offer")
public class ServiceOffer extends CatalogEntity {

    @Column(nullable = false, length = 20) String kind;
    @Column(nullable = false, length = 160) String title;
    @Column(name = "price_from") Integer priceFrom;
    @Column(nullable = false, length = 500) String description;

    void apply(ServiceOfferDto d) {
        setPublished(d.published());
        kind = d.kind();
        title = d.title().trim();
        priceFrom = d.priceFrom();
        description = d.description().trim();
    }
}
