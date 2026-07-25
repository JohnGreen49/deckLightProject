package com.euchreflow.platform.web;

import com.euchreflow.platform.events.EventBroker;
import com.euchreflow.platform.service.LeaderboardService;
import com.euchreflow.platform.service.ScheduleService;
import com.euchreflow.platform.service.TournamentService;
import com.euchreflow.platform.web.dto.LeaderboardDtos.Leaderboard;
import com.euchreflow.platform.web.dto.ScheduleDtos.FullSchedule;
import com.euchreflow.platform.web.dto.ScheduleDtos.PlayerSchedule;
import com.euchreflow.platform.web.dto.TournamentDtos.GenerateSchedule;
import com.euchreflow.platform.web.dto.TournamentDtos.TournamentView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Tournament-scoped operations: read, schedule generation, live views, and the SSE stream. */
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournaments;
    private final ScheduleService schedule;
    private final LeaderboardService leaderboard;
    private final EventBroker broker;

    public TournamentController(TournamentService tournaments, ScheduleService schedule,
                                LeaderboardService leaderboard, EventBroker broker) {
        this.tournaments = tournaments;
        this.schedule = schedule;
        this.leaderboard = leaderboard;
        this.broker = broker;
    }

    @GetMapping("/{id}")
    public TournamentView get(@PathVariable Long id) {
        return TournamentView.of(tournaments.get(id));
    }

    /** Enroll players (in the given order) and generate the full schedule. */
    @PostMapping("/{id}/schedule")
    public FullSchedule generate(@PathVariable Long id, @Valid @RequestBody GenerateSchedule req) {
        tournaments.generateSchedule(id, req.playerIds(), req.requestedRounds());
        return schedule.getFullSchedule(id);
    }

    @GetMapping("/{id}/schedule")
    public FullSchedule fullSchedule(@PathVariable Long id) {
        return schedule.getFullSchedule(id);
    }

    @GetMapping("/{id}/players/{playerId}/schedule")
    public PlayerSchedule playerSchedule(@PathVariable Long id, @PathVariable Long playerId) {
        return schedule.getPlayerSchedule(id, playerId);
    }

    @GetMapping("/{id}/leaderboard")
    public Leaderboard leaderboard(@PathVariable Long id) {
        return leaderboard.compute(id);
    }

    /** Live event stream (SSE). Player pages and the leaderboard subscribe here. */
    @GetMapping("/{id}/events")
    public SseEmitter events(@PathVariable Long id) {
        return broker.subscribe(id);
    }
}
