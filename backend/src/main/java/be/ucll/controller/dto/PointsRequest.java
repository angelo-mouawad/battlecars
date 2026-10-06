package be.ucll.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointsRequest(@NotNull @Min(-1000) @Max(1000) Integer points) {
}
