package be.ucll.controller.dto;

import jakarta.validation.constraints.NotNull;

public record EndMatchRequest(@NotNull Long winnerPlayerId) {
}
