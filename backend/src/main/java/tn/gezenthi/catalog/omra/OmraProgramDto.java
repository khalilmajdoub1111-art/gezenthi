package tn.gezenthi.catalog.omra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tn.gezenthi.catalog.Validation;

public record OmraProgramDto(
        UUID id,
        boolean published,
        @NotBlank @Size(max = 160) String title,
        @NotNull @Min(1) Integer durationDays,
        @NotBlank @Size(max = 60) String departure,
        @PositiveOrZero Integer hotelDistance,
        @NotNull @PositiveOrZero Integer priceFrom,
        /* optional: empty string allowed */
        @Size(max = Validation.IMAGE_MAX) @Pattern(regexp = "^$|" + Validation.IMAGE_URL, message = Validation.IMAGE_MSG) String image,
        @Size(max = 12) List<@NotBlank @Size(max = 60) String> includes,
        Instant updatedAt) {

    static OmraProgramDto of(OmraProgram o) {
        return new OmraProgramDto(o.getId(), o.isPublished(), o.title, o.durationDays, o.departure, o.hotelDistance,
                o.priceFrom, o.image, List.copyOf(o.includes), o.getUpdatedAt());
    }
}
