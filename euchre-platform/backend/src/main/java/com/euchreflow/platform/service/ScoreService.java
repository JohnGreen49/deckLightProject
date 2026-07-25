package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.GameScore;
import com.euchreflow.platform.domain.Round;
import com.euchreflow.platform.domain.Seating;
import com.euchreflow.platform.events.EventBroker;
import com.euchreflow.platform.events.TournamentEvent;
import com.euchreflow.platform.repo.GameScoreRepository;
import com.euchreflow.platform.repo.RoundRepository;
import com.euchreflow.platform.repo.SeatingRepository;
import com.euchreflow.platform.web.NotFoundException;
import com.euchreflow.platform.web.dto.ScoreDtos.SubmitScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;

/**
 * Records a game result and pushes live updates. When one player enters a score, their partner's
 * page and the leaderboard update immediately via {@link EventBroker}. Events are published only
 * after the transaction commits, so subscribers never see a score that later rolls back.
 */
@Service
public class ScoreService {

    private final SeatingRepository seatings;
    private final RoundRepository rounds;
    private final GameScoreRepository scores;
    private final EventBroker broker;
    private final LeaderboardService leaderboardService;

    public ScoreService(SeatingRepository seatings, RoundRepository rounds, GameScoreRepository scores,
                        EventBroker broker, LeaderboardService leaderboardService) {
        this.seatings = seatings;
        this.rounds = rounds;
        this.scores = scores;
        this.broker = broker;
        this.leaderboardService = leaderboardService;
    }

    @Transactional
    public GameScore submit(Long seatingId, SubmitScore req) {
        Seating seating = seatings.findById(seatingId)
                .orElseThrow(() -> new NotFoundException("seating not found: " + seatingId));
        Round round = rounds.findById(seating.getRoundId())
                .orElseThrow(() -> new NotFoundException("round not found: " + seating.getRoundId()));
        Long tournamentId = round.getTournamentId();

        if (req.submittedByPlayerId() != null && !seating.seats(req.submittedByPlayerId())) {
            throw new IllegalArgumentException("player " + req.submittedByPlayerId()
                    + " does not play at this table");
        }

        GameScore score = scores.findBySeatingId(seatingId).orElseGet(GameScore::new);
        score.setSeatingId(seatingId);
        score.setTeam1Score(req.team1Score());
        score.setTeam2Score(req.team2Score());
        score.setSubmittedBy(req.submittedByPlayerId());
        score.setSubmittedAt(Instant.now());
        GameScore saved = scores.save(score);

        publishAfterCommit(tournamentId, seatingId, round.getRoundNumber(), saved);
        return saved;
    }

    private void publishAfterCommit(Long tournamentId, Long seatingId, int roundNumber, GameScore saved) {
        Runnable publish = () -> {
            broker.publish(tournamentId, new TournamentEvent(TournamentEvent.SCORE_UPDATED, Map.of(
                    "seatingId", seatingId,
                    "roundNumber", roundNumber,
                    "team1Score", saved.getTeam1Score(),
                    "team2Score", saved.getTeam2Score()
            )));
            broker.publish(tournamentId, new TournamentEvent(
                    TournamentEvent.LEADERBOARD_UPDATED, leaderboardService.compute(tournamentId)));
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
