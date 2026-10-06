package be.ucll.controller.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SensorReadingRequest(
        @NotNull @Min(1) @Max(2) Integer carId,
        @Size(max = 50) String event,
        Map<String, Object> data
) {
}
