package be.ucll.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HitRequest(
        @NotNull @Min(1) @Max(2) Integer carId,
        @Min(1) @Max(50) Integer damage
) {
    public int damageOrDefault() {
        return damage == null ? 10 : damage;
    }
}
