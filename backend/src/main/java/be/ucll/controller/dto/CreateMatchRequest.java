package be.ucll.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMatchRequest(
        @NotNull Long player1Id,
        @NotNull @Min(1) @Max(2) Integer car1Id,
        @NotNull Long player2Id,
        @NotNull @Min(1) @Max(2) Integer car2Id
) {
}
