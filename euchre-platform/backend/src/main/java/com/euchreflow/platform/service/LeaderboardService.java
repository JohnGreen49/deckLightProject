package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.*;
import com.euchreflow.platform.repo.*;
import com.euchreflow.platform.web.dto.LeaderboardDtos.Leaderboard;
import com.euchreflow.platform.web.dto.LeaderboardDtos.LeaderboardEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Computes tournament standings. Each player earns their team's game score as points for every
 * scored game they played; ties break on games won, then name. Ranking is standard competition
 * ranking (equal totals share a rank).
 */
@Service
public class LeaderboardService {

    private final RoundRepository rounds;
    private final SeatingRepository seatings;
    private final GameScoreRepository scores;
    private final PlayerRepository players;

    public LeaderboardService(RoundRepository rounds, SeatingRepository seatings,
                              GameScoreRepository scores, PlayerRepository players) {
        this.rounds = rounds;
        this.seatings = seatings;
        this.scores = scores;
        this.players = players;
    }

    @Transactional(readOnly = true)
    public Leaderboard compute(Long tournamentId) {
        List<Long> roundIds = rounds.findByTournamentIdOrderByRoundNumber(tournamentId)
                .stream().map(Round::getId).toList();
        List<Seating> allSeatings = roundIds.isEmpty() ? List.of() : seatings.findByRoundIdIn(roundIds);

        List<Long> seatingIds = new ArrayList<>();
        for (Seating s : allSeatings) {
            seatingIds.add(s.getId());
        }
        Map<Long, GameScore> scoreBySeating = new HashMap<>();
        if (!seatingIds.isEmpty()) {
            scores.findBySeatingIdIn(seatingIds).forEach(gs -> scoreBySeating.put(gs.getSeatingId(), gs));
        }

        Map<Long, Acc> accByPlayer = new HashMap<>();
        for (Seating s : allSeatings) {
            GameScore gs = scoreBySeating.get(s.getId());
            if (gs == null) {
                continue;
            }
            boolean team1Won = gs.getTeam1Score() > gs.getTeam2Score();
            boolean team2Won = gs.getTeam2Score() > gs.getTeam1Score();
            award(accByPlayer, s.getTeam1p1(), gs.getTeam1Score(), team1Won);
            award(accByPlayer, s.getTeam1p2(), gs.getTeam1Score(), team1Won);
            award(accByPlayer, s.getTeam2p1(), gs.getTeam2Score(), team2Won);
            award(accByPlayer, s.getTeam2p2(), gs.getTeam2Score(), team2Won);
        }

        Map<Long, String> names = new HashMap<>();
        players.findAllById(accByPlayer.keySet()).forEach(p -> names.put(p.getId(), p.getName()));

        List<Acc> ordered = new ArrayList<>(accByPlayer.values());
        ordered.sort(Comparator
                .comparingInt((Acc a) -> a.points).reversed()
                .thenComparing(Comparator.comparingInt((Acc a) -> a.won).reversed())
                .thenComparing(a -> names.getOrDefault(a.playerId, "")));

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 0;
        int index = 0;
        Acc previous = null;
        for (Acc a : ordered) {
            index++;
            if (previous == null || a.points != previous.points || a.won != previous.won) {
                rank = index; // standard competition ranking
            }
            entries.add(new LeaderboardEntry(
                    rank, a.playerId, names.getOrDefault(a.playerId, "Player " + a.playerId),
                    a.points, a.played, a.won));
            previous = a;
        }
        return new Leaderboard(tournamentId, entries);
    }

    private void award(Map<Long, Acc> accs, Long playerId, int points, boolean won) {
        Acc acc = accs.computeIfAbsent(playerId, Acc::new);
        acc.points += points;
        acc.played += 1;
        if (won) {
            acc.won += 1;
        }
    }

    private static final class Acc {
        final Long playerId;
        int points;
        int played;
        int won;

        Acc(Long playerId) {
            this.playerId = playerId;
        }
    }
}
