package com.euchreflow.platform.scheduling;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithmic fallback that works for any player count that is a positive multiple of four.
 *
 * <p>It uses the classic circle method (a 1-factorization of the complete graph): player 0 stays
 * put while the rest rotate, yielding {@code N-1} rounds in which every player partners every other
 * player exactly once. Each round's {@code N/2} partnerships are then paired off into four-player
 * tables. Because partnerships change every round, opponents naturally vary too.
 */
@Component
public class GeneratedScheduler implements TournamentScheduler {

    @Override
    public String id() {
        return "generated";
    }

    @Override
    public String displayName() {
        return "Generated rotation (partner everyone once)";
    }

    @Override
    public boolean supports(int playerCount) {
        return playerCount >= 4 && playerCount % 4 == 0;
    }

    @Override
    public Schedule build(ScheduleRequest request) {
        int n = request.playerCount();
        if (!supports(n)) {
            throw new IllegalArgumentException(
                    "GeneratedScheduler requires a player count that is a positive multiple of 4, got " + n);
        }

        List<List<int[]>> partnershipRounds = partnerEveryoneOnce(n);

        int naturalMax = partnershipRounds.size();
        int roundCount = request.requestedRounds() == null
                ? naturalMax
                : Math.min(request.requestedRounds(), naturalMax);

        List<ScheduledRound> rounds = new ArrayList<>();
        for (int r = 0; r < roundCount; r++) {
            List<int[]> pairs = partnershipRounds.get(r);
            List<TableAssignment> tables = new ArrayList<>();
            // Pair consecutive partnerships into tables: (pair 0 vs pair 1), (pair 2 vs pair 3), ...
            for (int t = 0; t < pairs.size() / 2; t++) {
                int[] teamA = pairs.get(2 * t);
                int[] teamB = pairs.get(2 * t + 1);
                tables.add(new TableAssignment(t + 1, teamA[0], teamA[1], teamB[0], teamB[1]));
            }
            rounds.add(new ScheduledRound(r + 1, tables));
        }
        return new Schedule(rounds);
    }

    /**
     * Circle-method round robin. Returns {@code n-1} rounds, each a list of {@code n/2} partnerships.
     */
    private List<List<int[]>> partnerEveryoneOnce(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }

        List<List<int[]>> rounds = new ArrayList<>();
        for (int round = 0; round < n - 1; round++) {
            List<int[]> pairs = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                pairs.add(new int[] { arr[i], arr[n - 1 - i] });
            }
            rounds.add(pairs);

            // Rotate all but arr[0]: last element moves into position 1, others shift right.
            int last = arr[n - 1];
            for (int j = n - 1; j >= 2; j--) {
                arr[j] = arr[j - 1];
            }
            arr[1] = last;
        }
        return rounds;
    }
}
