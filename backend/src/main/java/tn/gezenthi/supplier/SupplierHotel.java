package tn.gezenthi.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** Local copy of a supplier's static hotel data (needed because searches are by supplier hotel id). */
@Entity
@Table(name = "supplier_hotel")
public class SupplierHotel {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "supplier_code", nullable = false, length = 40) private String supplierCode;
    @Column(name = "external_id", nullable = false, length = 40) private String externalId;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "city_external_id", length = 40) private String cityExternalId;
    @Column(name = "city_name", nullable = false, length = 120) private String cityName;
    private Integer stars;
    @Column(columnDefinition = "text") private String image;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "synced_at", nullable = false) private Instant syncedAt;

    protected SupplierHotel() {
    }

    SupplierHotel(String supplierCode, String externalId) {
        this.supplierCode = supplierCode;
        this.externalId = externalId;
    }

    void refresh(HotelSupplier.StaticHotel h, Instant now) {
        name = h.name().isBlank() ? "Hôtel " + h.externalId() : h.name();
        cityExternalId = h.cityExternalId();
        cityName = h.cityName().isBlank() ? "—" : h.cityName();
        stars = h.stars();
        image = h.image();
        active = true;
        syncedAt = now;
    }

    void deactivate() {
        active = false;
    }

    public String getSupplierCode() { return supplierCode; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getCityExternalId() { return cityExternalId; }
    public String getCityName() { return cityName; }
    public Integer getStars() { return stars; }
    public String getImage() { return image; }
    public boolean isActive() { return active; }
}
