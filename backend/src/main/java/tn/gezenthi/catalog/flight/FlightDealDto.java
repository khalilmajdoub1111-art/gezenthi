package tn.gezenthi.catalog.flight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record FlightDealDto(
        UUID id,
        boolean published,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Pattern(regexp = "^\\s*[A-Za-z]{3,4}\\s*$", message = "code aéroport de 3 ou 4 lettres (ex. ORY)") String airportCode,
        @NotNull @PositiveOrZero Integer priceFrom,
        Instant updatedAt) {

    static FlightDealDto of(FlightDeal f) {
        return new FlightDealDto(f.getId(), f.isPublished(), f.city, f.airportCode, f.priceFrom, f.getUpdatedAt());
    }
}
