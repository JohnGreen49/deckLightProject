package com.euchreflow.platform.scheduling;

/**
 * Input to a scheduler.
 *
 * @param playerCount     number of enrolled players (seat indices run 0..playerCount-1)
 * @param requestedRounds desired round count, or {@code null} to let the scheduler pick its natural maximum
 */
public record ScheduleRequest(int playerCount, Integer requestedRounds) {}
