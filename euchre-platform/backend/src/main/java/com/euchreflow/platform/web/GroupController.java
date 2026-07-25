package com.euchreflow.platform.web;

import com.euchreflow.platform.service.GroupService;
import com.euchreflow.platform.service.PlayerService;
import com.euchreflow.platform.service.TournamentService;
import com.euchreflow.platform.web.dto.GroupDtos.CreateGroup;
import com.euchreflow.platform.web.dto.GroupDtos.GroupView;
import com.euchreflow.platform.web.dto.PlayerDtos.CreatePlayer;
import com.euchreflow.platform.web.dto.PlayerDtos.ImportPlayers;
import com.euchreflow.platform.web.dto.PlayerDtos.PlayerView;
import com.euchreflow.platform.web.dto.TournamentDtos.CreateTournament;
import com.euchreflow.platform.web.dto.TournamentDtos.TournamentView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Group (tenant) management plus its nested roster and tournament collections. */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groups;
    private final PlayerService playersService;
    private final TournamentService tournaments;

    public GroupController(GroupService groups, PlayerService playersService, TournamentService tournaments) {
        this.groups = groups;
        this.playersService = playersService;
        this.tournaments = tournaments;
    }

    @PostMapping
    public GroupView create(@Valid @RequestBody CreateGroup req) {
        return GroupView.of(groups.create(req));
    }

    @GetMapping
    public List<GroupView> list() {
        return groups.list().stream().map(GroupView::of).toList();
    }

    @GetMapping("/{id}")
    public GroupView get(@PathVariable Long id) {
        return GroupView.of(groups.get(id));
    }

    @GetMapping("/slug/{slug}")
    public GroupView getBySlug(@PathVariable String slug) {
        return GroupView.of(groups.getBySlug(slug));
    }

    // --- roster ---------------------------------------------------------------

    @PostMapping("/{groupId}/players")
    public PlayerView addPlayer(@PathVariable Long groupId, @Valid @RequestBody CreatePlayer req) {
        return PlayerView.of(playersService.create(groupId, req.name(), req.email()));
    }

    @PostMapping("/{groupId}/players/import")
    public List<PlayerView> importPlayers(@PathVariable Long groupId, @Valid @RequestBody ImportPlayers req) {
        return playersService.importCsv(groupId, req.csv()).stream().map(PlayerView::of).toList();
    }

    @GetMapping("/{groupId}/players")
    public List<PlayerView> listPlayers(@PathVariable Long groupId) {
        return playersService.listByGroup(groupId).stream().map(PlayerView::of).toList();
    }

    // --- tournaments ----------------------------------------------------------

    @PostMapping("/{groupId}/tournaments")
    public TournamentView createTournament(@PathVariable Long groupId, @Valid @RequestBody CreateTournament req) {
        return TournamentView.of(tournaments.create(groupId, req));
    }

    @GetMapping("/{groupId}/tournaments")
    public List<TournamentView> listTournaments(@PathVariable Long groupId) {
        return tournaments.listByGroup(groupId).stream().map(TournamentView::of).toList();
    }
}
