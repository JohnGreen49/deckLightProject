package com.euchreflow.platform.scheduling;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers all {@link TournamentScheduler} beans and resolves the right one for a tournament.
 * Adding a scheduler is zero-config: define a {@code @Component} implementing the interface.
 */
@Component
public class SchedulerRegistry {

    private static final String FALLBACK_ID = "generated";

    private final Map<String, TournamentScheduler> byId = new LinkedHashMap<>();

    public SchedulerRegistry(List<TournamentScheduler> schedulers) {
        for (TournamentScheduler s : schedulers) {
            byId.put(s.id(), s);
        }
    }

    /** All registered schedulers, for organizer selection UIs. */
    public List<TournamentScheduler> all() {
        return List.copyOf(byId.values());
    }

    /**
     * Resolve the scheduler to actually run for the requested id and player count. If the chosen
     * scheduler does not support the count, transparently fall back to the generated scheduler so
     * the organizer still gets a valid schedule.
     *
     * @throws IllegalStateException if no scheduler (not even the fallback) supports the count
     */
    public TournamentScheduler resolve(String schedulerId, int playerCount) {
        TournamentScheduler chosen = byId.get(schedulerId);
        if (chosen != null && chosen.supports(playerCount)) {
            return chosen;
        }
        TournamentScheduler fallback = byId.get(FALLBACK_ID);
        if (fallback != null && fallback.supports(playerCount)) {
            return fallback;
        }
        throw new IllegalStateException(
                "No scheduler supports " + playerCount + " players (requested '" + schedulerId + "')");
    }
}
