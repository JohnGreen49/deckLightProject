package com.euchreflow.platform.web.dto;

import com.euchreflow.platform.domain.Tournament;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class TournamentDtos {
    private TournamentDtos() {}

    public record CreateTournament(
            @NotBlank @Size(max = 200) String name,
            String schedulerId
    ) {}

    /** Enroll players (by id) and generate the schedule in one step. */
    public record GenerateSchedule(
            @NotEmpty List<Long> playerIds,
            Integer requestedRounds
    ) {}

    public record TournamentView(
            Long id, String name, String status, String schedulerId, Integer numRounds) {
        public static TournamentView of(Tournament t) {
            return new TournamentView(
                    t.getId(), t.getName(), t.getStatus().name(), t.getSchedulerId(), t.getNumRounds());
        }
    }

    public record SchedulerView(String id, String displayName) {}
}
