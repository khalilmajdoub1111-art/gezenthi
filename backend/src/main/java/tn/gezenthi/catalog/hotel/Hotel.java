package tn.gezenthi.catalog.hotel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tn.gezenthi.catalog.CatalogEntity;

@Entity
@Table(name = "hotel")
public class Hotel extends CatalogEntity {

    @Column(nullable = false, length = 160) String name;
    @Column(nullable = false, length = 10) String region;
    @Column(nullable = false, length = 80) String city;
    @Column(nullable = false) Integer stars;
    @Column(nullable = false, precision = 3, scale = 1) BigDecimal rating;
    @Column(nullable = false) Integer reviews;
    @Column(nullable = false, length = 40) String board;
    @Column(name = "price_per_night", nullable = false) Integer pricePerNight;
    @Column(name = "old_price") Integer oldPrice;
    @Column(nullable = false, columnDefinition = "text") String image;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    List<String> tags = new ArrayList<>();

    void apply(HotelDto d) {
        setPublished(d.published());
        name = d.name().trim();
        region = d.region();
        city = d.city().trim();
        stars = d.stars();
        rating = d.rating() == null ? BigDecimal.ZERO : d.rating();
        reviews = d.reviews() == null ? 0 : d.reviews();
        board = d.board();
        pricePerNight = d.pricePerNight();
        oldPrice = d.oldPrice();
        image = d.image();
        tags = d.tags() == null ? new ArrayList<>() : new ArrayList<>(d.tags());
    }
}
