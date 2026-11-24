package tn.gezenthi.catalog.hotel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tn.gezenthi.catalog.Validation;

/** API shape of a hotel. id / updatedAt are set by the server and ignored on input. */
public record HotelDto(
        UUID id,
        boolean published,
        @NotBlank @Size(max = 160) String name,
        @NotNull @Pattern(regexp = "tunisie|monde", message = "doit être 'tunisie' ou 'monde'") String region,
        @NotBlank @Size(max = 80) String city,
        @NotNull @Min(3) @Max(5) Integer stars,
        @DecimalMin("0") @DecimalMax("10") BigDecimal rating,
        @PositiveOrZero Integer reviews,
        @NotNull @Pattern(regexp = Validation.BOARD_TYPES, message = "type de pension inconnu") String board,
        @NotNull @PositiveOrZero Integer pricePerNight,
        @PositiveOrZero Integer oldPrice,
        @NotBlank @Size(max = Validation.IMAGE_MAX) @Pattern(regexp = Validation.IMAGE_URL, message = Validation.IMAGE_MSG) String image,
        @Size(max = 12) List<@NotBlank @Size(max = 40) String> tags,
        Instant updatedAt) {

    static HotelDto of(Hotel h) {
        return new HotelDto(h.getId(), h.isPublished(), h.name, h.region, h.city, h.stars, h.rating, h.reviews,
                h.board, h.pricePerNight, h.oldPrice, h.image, List.copyOf(h.tags), h.getUpdatedAt());
    }
}
