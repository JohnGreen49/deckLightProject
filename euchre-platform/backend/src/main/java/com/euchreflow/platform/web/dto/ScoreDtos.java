package com.euchreflow.platform.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class ScoreDtos {
    private ScoreDtos() {}

    /**
     * Submit (or correct) the result of a seating. Scores are absolute team scores.
     * {@code submittedByPlayerId} is the player entering it (their partner sees it live).
     */
    public record SubmitScore(
            @NotNull @Min(0) Integer team1Score,
            @NotNull @Min(0) Integer team2Score,
            Long submittedByPlayerId
    ) {}

    public record ScoreView(Long seatingId, int team1Score, int team2Score) {}
}
