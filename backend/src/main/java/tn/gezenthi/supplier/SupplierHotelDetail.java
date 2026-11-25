package tn.gezenthi.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cached HotelDetail content for one supplier hotel (HTML already sanitised). */
@Entity
@Table(name = "supplier_hotel_detail")
public class SupplierHotelDetail {

    @Id
    @Column(name = "supplier_hotel_id")
    private UUID supplierHotelId;

    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String notes;
    @Column(length = 300) private String address;
    @Column(length = 30) private String latitude;
    @Column(length = 30) private String longitude;
    @Column(name = "check_in", length = 30) private String checkIn;
    @Column(name = "check_out", length = 30) private String checkOut;
    @Column(length = 60) private String type;

    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "text[]") private List<String> images = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "text[]") private List<String> facilities = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "text[]") private List<String> tags = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "text[]") private List<String> themes = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false, columnDefinition = "text[]") private List<String> boards = new ArrayList<>();

    @Column(name = "fetched_at", nullable = false) private Instant fetchedAt;

    protected SupplierHotelDetail() {
    }

    SupplierHotelDetail(UUID supplierHotelId) {
        this.supplierHotelId = supplierHotelId;
    }

    void fill(HotelSupplier.HotelDetails d, String safeDescription, String safeNotes, Instant now) {
        description = safeDescription;
        notes = safeNotes;
        address = cut(d.address(), 300);
        latitude = cut(d.latitude(), 30);
        longitude = cut(d.longitude(), 30);
        checkIn = cut(d.checkIn(), 30);
        checkOut = cut(d.checkOut(), 30);
        type = cut(d.type(), 60);
        images = d.images().stream().filter(u -> u.startsWith("https://") || u.startsWith("http://")).limit(40).toList();
        facilities = limit(d.facilities());
        tags = limit(d.tags());
        themes = limit(d.themes());
        boards = limit(d.boards());
        fetchedAt = now;
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.substring(0, Math.min(max, s.length()));
    }

    private static List<String> limit(List<String> l) {
        return l.stream().map(s -> cut(s, 120)).limit(60).toList();
    }

    public String getDescription() { return description; }
    public String getNotes() { return notes; }
    public String getAddress() { return address; }
    public String getLatitude() { return latitude; }
    public String getLongitude() { return longitude; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public String getType() { return type; }
    public List<String> getImages() { return images; }
    public List<String> getFacilities() { return facilities; }
    public List<String> getTags() { return tags; }
    public List<String> getThemes() { return themes; }
    public List<String> getBoards() { return boards; }
    public Instant getFetchedAt() { return fetchedAt; }
}
