package tn.gezenthi.catalog.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import tn.gezenthi.catalog.Validation;

public record ServiceOfferDto(
        UUID id,
        boolean published,
        @NotNull @Pattern(regexp = Validation.SERVICE_KINDS, message = "type inconnu (Visa, Ferry CTN ou TRE)") String kind,
        @NotBlank @Size(max = 160) String title,
        @PositiveOrZero Integer priceFrom,
        @NotBlank @Size(max = 500) String description,
        Instant updatedAt) {

    static ServiceOfferDto of(ServiceOffer s) {
        return new ServiceOfferDto(s.getId(), s.isPublished(), s.kind, s.title, s.priceFrom, s.description, s.getUpdatedAt());
    }
}
