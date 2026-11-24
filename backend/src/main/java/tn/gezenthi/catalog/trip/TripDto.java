package tn.gezenthi.catalog.trip;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import tn.gezenthi.catalog.Validation;

public record TripDto(
        UUID id,
        boolean published,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String country,
        @NotNull @Min(1) Integer durationDays,
        @NotNull @PositiveOrZero Integer priceFrom,
        @NotBlank @Size(max = Validation.IMAGE_MAX) @Pattern(regexp = Validation.IMAGE_URL, message = Validation.IMAGE_MSG) String image,
        boolean noVisa,
        @Size(max = 40) String badge,
        Instant updatedAt) {

    static TripDto of(Trip t) {
        return new TripDto(t.getId(), t.isPublished(), t.name, t.country, t.durationDays, t.priceFrom, t.image,
                t.noVisa, t.badge, t.getUpdatedAt());
    }
}
