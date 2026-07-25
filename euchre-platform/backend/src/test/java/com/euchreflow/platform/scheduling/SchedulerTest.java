package com.euchreflow.platform.scheduling;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic verification of the schedulers (no Spring context, no database).
 * A schedule is "valid" when, in every round, each player is seated exactly once, and every
 * unordered partnership across a full run occurs exactly once.
 */
class SchedulerTest {

    @Test
    void standardCardEightPlayersIsValid() {
        Schedule s = new StandardCardScheduler().build(new ScheduleRequest(8, null));
        assertEquals(7, s.rounds().size(), "8 players should yield 7 rounds");
        assertEachRoundSeatsEveryoneOnce(s, 8);
        assertPartnerEveryoneExactlyOnce(s, 8);
    }

    @Test
    void standardCardFourPlayersIsValid() {
        Schedule s = new StandardCardScheduler().build(new ScheduleRequest(4, null));
        assertEquals(3, s.rounds().size());
        assertEachRoundSeatsEveryoneOnce(s, 4);
        assertPartnerEveryoneExactlyOnce(s, 4);
    }

    @Test
    void generatedSchedulerIsValidForVariousSizes() {
        for (int n : new int[] {4, 8, 12, 16, 20}) {
            Schedule s = new GeneratedScheduler().build(new ScheduleRequest(n, null));
            assertEquals(n - 1, s.rounds().size(), "n players should yield n-1 rounds for n=" + n);
            assertEachRoundSeatsEveryoneOnce(s, n);
            assertPartnerEveryoneExactlyOnce(s, n);
        }
    }

    @Test
    void generatedSchedulerRespectsRequestedRounds() {
        Schedule s = new GeneratedScheduler().build(new ScheduleRequest(8, 3));
        assertEquals(3, s.rounds().size());
        assertEachRoundSeatsEveryoneOnce(s, 8);
    }

    @Test
    void generatedSchedulerRejectsNonMultipleOfFour() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedScheduler().build(new ScheduleRequest(6, null)));
    }

    // --- assertions -----------------------------------------------------------

    private void assertEachRoundSeatsEveryoneOnce(Schedule s, int n) {
        for (ScheduledRound round : s.rounds()) {
            Set<Integer> seen = new HashSet<>();
            for (TableAssignment t : round.tables()) {
                for (int seat : t.seats()) {
                    assertTrue(seat >= 0 && seat < n, "seat index out of range: " + seat);
                    assertTrue(seen.add(seat),
                            "seat " + seat + " appears twice in round " + round.roundNumber());
                }
            }
            assertEquals(n, seen.size(),
                    "round " + round.roundNumber() + " should seat all " + n + " players");
        }
    }

    private void assertPartnerEveryoneExactlyOnce(Schedule s, int n) {
        Set<String> partnerships = new HashSet<>();
        for (ScheduledRound round : s.rounds()) {
            for (TableAssignment t : round.tables()) {
                assertTrue(partnerships.add(pairKey(t.t1a(), t.t1b())),
                        "duplicate partnership " + t.t1a() + "&" + t.t1b());
                assertTrue(partnerships.add(pairKey(t.t2a(), t.t2b())),
                        "duplicate partnership " + t.t2a() + "&" + t.t2b());
            }
        }
        // A full run partners everyone once: n*(n-1)/2 distinct pairs.
        assertEquals(n * (n - 1) / 2, partnerships.size(),
                "every player should partner every other exactly once");
    }

    private String pairKey(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }
}
