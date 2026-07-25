package com.euchreflow.platform.scheduling;

/**
 * Pluggable strategy that assigns players to partners, tables, and rounds.
 *
 * <p>Implementations are discovered as Spring beans and registered by {@link SchedulerRegistry}
 * under their {@link #id()}. To add a new algorithm, implement this interface and annotate the
 * class with {@code @Component} &mdash; no other wiring is required.
 *
 * <p>Schedulers operate on abstract seat indices (0..playerCount-1); the caller maps those to
 * concrete players. This keeps algorithms pure and unit-testable without a database.
 */
public interface TournamentScheduler {

    /** Stable identifier persisted on the tournament (e.g. {@code "standard-card"}). */
    String id();

    /** Human-friendly name for organizer UIs. */
    String displayName();

    /** @return true if this scheduler can build a schedule for the given number of players. */
    boolean supports(int playerCount);

    /**
     * Build the schedule.
     *
     * @throws IllegalArgumentException if {@code request.playerCount()} is unsupported
     */
    Schedule build(ScheduleRequest request);
}
