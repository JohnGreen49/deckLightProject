package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.*;
import com.euchreflow.platform.repo.*;
import com.euchreflow.platform.web.NotFoundException;
import com.euchreflow.platform.web.dto.ScheduleDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Builds read-only views of a tournament's schedule for organizers and individual players. */
@Service
public class ScheduleService {

    private final TournamentRepository tournaments;
    private final RoundRepository rounds;
    private final SeatingRepository seatings;
    private final GameScoreRepository scores;
    private final PlayerRepository players;

    public ScheduleService(TournamentRepository tournaments, RoundRepository rounds,
                           SeatingRepository seatings, GameScoreRepository scores,
                           PlayerRepository players) {
        this.tournaments = tournaments;
        this.rounds = rounds;
        this.seatings = seatings;
        this.scores = scores;
        this.players = players;
    }

    @Transactional(readOnly = true)
    public FullSchedule getFullSchedule(Long tournamentId) {
        Tournament t = tournaments.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("tournament not found: " + tournamentId));

        List<Round> roundList = rounds.findByTournamentIdOrderByRoundNumber(tournamentId);
        Context ctx = loadContext(roundList);

        List<RoundView> roundViews = new ArrayList<>();
        for (Round round : roundList) {
            List<SeatingView> tableViews = new ArrayList<>();
            for (Seating s : ctx.byRound.getOrDefault(round.getId(), List.of())) {
                tableViews.add(toSeatingView(s, ctx));
            }
            roundViews.add(new RoundView(round.getId(), round.getRoundNumber(), round.getStatus(), tableViews));
        }
        return new FullSchedule(tournamentId, t.getName(), roundViews);
    }

    @Transactional(readOnly = true)
    public PlayerSchedule getPlayerSchedule(Long tournamentId, Long playerId) {
        Tournament t = tournaments.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("tournament not found: " + tournamentId));
        Player me = players.findById(playerId)
                .orElseThrow(() -> new NotFoundException("player not found: " + playerId));

        List<Round> roundList = rounds.findByTournamentIdOrderByRoundNumber(tournamentId);
        Context ctx = loadContext(roundList);

        List<PlayerRoundCard> cards = new ArrayList<>();
        for (Round round : roundList) {
            for (Seating s : ctx.byRound.getOrDefault(round.getId(), List.of())) {
                if (!s.seats(playerId)) {
                    continue;
                }
                cards.add(toPlayerCard(round.getRoundNumber(), s, playerId, ctx));
                break;
            }
        }
        return new PlayerSchedule(tournamentId, t.getName(),
                new PlayerRef(me.getId(), me.getName()), cards);
    }

    // --- helpers ---------------------------------------------------------------

    private record Context(Map<Long, List<Seating>> byRound,
                           Map<Long, String> names,
                           Map<Long, GameScore> scoreBySeating) {}

    private Context loadContext(List<Round> roundList) {
        List<Long> roundIds = roundList.stream().map(Round::getId).toList();
        List<Seating> allSeatings = roundIds.isEmpty() ? List.of() : seatings.findByRoundIdIn(roundIds);

        Map<Long, List<Seating>> byRound = new HashMap<>();
        Set<Long> playerIds = new HashSet<>();
        List<Long> seatingIds = new ArrayList<>();
        for (Seating s : allSeatings) {
            byRound.computeIfAbsent(s.getRoundId(), k -> new ArrayList<>()).add(s);
            seatingIds.add(s.getId());
            playerIds.add(s.getTeam1p1());
            playerIds.add(s.getTeam1p2());
            playerIds.add(s.getTeam2p1());
            playerIds.add(s.getTeam2p2());
        }
        byRound.values().forEach(list -> list.sort(Comparator.comparingInt(Seating::getTableNumber)));

        Map<Long, String> names = new HashMap<>();
        players.findAllById(playerIds).forEach(p -> names.put(p.getId(), p.getName()));

        Map<Long, GameScore> scoreBySeating = new HashMap<>();
        if (!seatingIds.isEmpty()) {
            scores.findBySeatingIdIn(seatingIds)
                    .forEach(gs -> scoreBySeating.put(gs.getSeatingId(), gs));
        }
        return new Context(byRound, names, scoreBySeating);
    }

    private PlayerRef ref(Long playerId, Context ctx) {
        return new PlayerRef(playerId, ctx.names.getOrDefault(playerId, "Player " + playerId));
    }

    private SeatingView toSeatingView(Seating s, Context ctx) {
        GameScore gs = ctx.scoreBySeating.get(s.getId());
        return new SeatingView(
                s.getId(),
                s.getTableNumber(),
                List.of(ref(s.getTeam1p1(), ctx), ref(s.getTeam1p2(), ctx)),
                List.of(ref(s.getTeam2p1(), ctx), ref(s.getTeam2p2(), ctx)),
                gs == null ? null : gs.getTeam1Score(),
                gs == null ? null : gs.getTeam2Score(),
                gs != null);
    }

    private PlayerRoundCard toPlayerCard(int roundNumber, Seating s, Long playerId, Context ctx) {
        boolean onTeam1 = playerId.equals(s.getTeam1p1()) || playerId.equals(s.getTeam1p2());
        Long partnerId = onTeam1
                ? (playerId.equals(s.getTeam1p1()) ? s.getTeam1p2() : s.getTeam1p1())
                : (playerId.equals(s.getTeam2p1()) ? s.getTeam2p2() : s.getTeam2p1());
        List<PlayerRef> opponents = onTeam1
                ? List.of(ref(s.getTeam2p1(), ctx), ref(s.getTeam2p2(), ctx))
                : List.of(ref(s.getTeam1p1(), ctx), ref(s.getTeam1p2(), ctx));

        GameScore gs = ctx.scoreBySeating.get(s.getId());
        Integer yourScore = null;
        Integer oppScore = null;
        if (gs != null) {
            yourScore = onTeam1 ? gs.getTeam1Score() : gs.getTeam2Score();
            oppScore = onTeam1 ? gs.getTeam2Score() : gs.getTeam1Score();
        }
        return new PlayerRoundCard(
                roundNumber, s.getId(), s.getTableNumber(),
                ref(partnerId, ctx), opponents, onTeam1, yourScore, oppScore, gs != null);
    }
}
