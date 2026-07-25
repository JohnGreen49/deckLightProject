package com.euchreflow.platform.events;

/**
 * An event broadcast to everyone watching a tournament's live stream.
 *
 * @param type    event name the frontend switches on (e.g. {@code "score-updated"})
 * @param payload arbitrary JSON-serializable body
 */
public record TournamentEvent(String type, Object payload) {

    public static final String SCORE_UPDATED = "score-updated";
    public static final String LEADERBOARD_UPDATED = "leaderboard-updated";
    public static final String SCHEDULE_UPDATED = "schedule-updated";
}
