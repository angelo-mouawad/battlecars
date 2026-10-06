package be.ucll.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank @Size(max = 30) String name,
        @NotBlank @Size(min = 4, max = 72) String password
) {
}
