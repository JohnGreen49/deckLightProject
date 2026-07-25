package com.euchreflow.platform.web;

import com.euchreflow.platform.domain.Player;
import com.euchreflow.platform.scheduling.SchedulerRegistry;
import com.euchreflow.platform.service.PlayerService;
import com.euchreflow.platform.service.TournamentService;
import com.euchreflow.platform.web.dto.PlayerDtos.PlayerView;
import com.euchreflow.platform.web.dto.TournamentDtos.SchedulerView;
import com.euchreflow.platform.web.dto.TournamentDtos.TournamentView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cross-cutting endpoints: available schedulers and the player join-code lookup. */
@RestController
@RequestMapping("/api")
public class MetaController {

    private final SchedulerRegistry schedulers;
    private final PlayerService players;
    private final TournamentService tournaments;

    public MetaController(SchedulerRegistry schedulers, PlayerService players, TournamentService tournaments) {
        this.schedulers = schedulers;
        this.players = players;
        this.tournaments = tournaments;
    }

    @GetMapping("/schedulers")
    public List<SchedulerView> schedulers() {
        return schedulers.all().stream()
                .map(s -> new SchedulerView(s.id(), s.displayName()))
                .toList();
    }

    /** Resolve a player from their join code and list the tournaments they can join. */
    @GetMapping("/join/{joinCode}")
    public JoinResult join(@PathVariable String joinCode) {
        Player p = players.getByJoinCode(joinCode);
        List<TournamentView> ts = tournaments.listByGroup(p.getGroupId()).stream()
                .map(TournamentView::of)
                .toList();
        return new JoinResult(PlayerView.of(p), p.getGroupId(), ts);
    }

    public record JoinResult(PlayerView player, Long groupId, List<TournamentView> tournaments) {}
}
