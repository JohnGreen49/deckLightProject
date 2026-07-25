package com.euchreflow.platform.web.dto;

import java.util.List;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    /** A named player at a table (id + display name). */
    public record PlayerRef(Long id, String name) {}

    /** One table in one round, with resolved player names and the recorded score (if any). */
    public record SeatingView(
            Long seatingId,
            int tableNumber,
            List<PlayerRef> team1,
            List<PlayerRef> team2,
            Integer team1Score,
            Integer team2Score,
            boolean scored
    ) {}

    /** The organizer's full-tournament view. */
    public record RoundView(Long roundId, int roundNumber, String status, List<SeatingView> tables) {}

    public record FullSchedule(Long tournamentId, String name, List<RoundView> rounds) {}

    /** A single player's per-round card: where they sit, with whom, against whom, and the score. */
    public record PlayerRoundCard(
            int roundNumber,
            Long seatingId,
            int tableNumber,
            PlayerRef partner,
            List<PlayerRef> opponents,
            boolean onTeam1,
            Integer yourScore,
            Integer opponentScore,
            boolean scored
    ) {}

    /** Everything a player's personal page needs. */
    public record PlayerSchedule(
            Long tournamentId,
            String tournamentName,
            PlayerRef you,
            List<PlayerRoundCard> rounds
    ) {}
}
