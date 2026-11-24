package tn.gezenthi.catalog.omra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tn.gezenthi.catalog.CatalogEntity;

@Entity
@Table(name = "omra_program")
public class OmraProgram extends CatalogEntity {

    @Column(nullable = false, length = 160) String title;
    @Column(name = "duration_days", nullable = false) Integer durationDays;
    @Column(nullable = false, length = 60) String departure;
    @Column(name = "hotel_distance") Integer hotelDistance;
    @Column(name = "price_from", nullable = false) Integer priceFrom;
    @Column(nullable = false, columnDefinition = "text") String image = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    List<String> includes = new ArrayList<>();

    void apply(OmraProgramDto d) {
        setPublished(d.published());
        title = d.title().trim();
        durationDays = d.durationDays();
        departure = d.departure().trim();
        hotelDistance = d.hotelDistance();
        priceFrom = d.priceFrom();
        image = d.image() == null ? "" : d.image();
        includes = d.includes() == null ? new ArrayList<>() : new ArrayList<>(d.includes());
    }
}
