package com.euchreflow.platform.web.dto;

import java.util.List;

public final class LeaderboardDtos {
    private LeaderboardDtos() {}

    public record LeaderboardEntry(
            int rank,
            Long playerId,
            String name,
            int totalPoints,
            int gamesPlayed,
            int gamesWon
    ) {}

    public record Leaderboard(Long tournamentId, List<LeaderboardEntry> entries) {}
}
