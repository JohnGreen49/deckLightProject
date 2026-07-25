package com.euchreflow.platform.scheduling;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The default scheduler: curated, printed-card style rotations of the kind widely used for
 * progressive euchre parties. Each curated chart lets every player partner every other player
 * exactly once. Charts are stored as flat {@code {t1a, t1b, t2a, t2b}} rows in seat-index form.
 *
 * <p>Currently ships charts for 4- and 8-player events (the most common home-party sizes).
 * For other multiples of four, prefer {@link GeneratedScheduler}, which the registry selects
 * automatically as a fallback.
 */
@Component
public class StandardCardScheduler implements TournamentScheduler {

    /** playerCount -> rounds; each round is a list of tables; each table is {t1a,t1b,t2a,t2b}. */
    private static final Map<Integer, List<List<int[]>>> CHARTS = Map.of(
            4, List.of(
                    List.of(new int[]{0, 3, 1, 2}),
                    List.of(new int[]{0, 2, 3, 1}),
                    List.of(new int[]{0, 1, 2, 3})
            ),
            8, List.of(
                    List.of(new int[]{0, 7, 1, 6}, new int[]{2, 5, 3, 4}),
                    List.of(new int[]{0, 6, 7, 5}, new int[]{1, 4, 2, 3}),
                    List.of(new int[]{0, 5, 6, 4}, new int[]{7, 3, 1, 2}),
                    List.of(new int[]{0, 4, 5, 3}, new int[]{6, 2, 7, 1}),
                    List.of(new int[]{0, 3, 4, 2}, new int[]{5, 1, 6, 7}),
                    List.of(new int[]{0, 2, 3, 1}, new int[]{4, 7, 5, 6}),
                    List.of(new int[]{0, 1, 2, 7}, new int[]{3, 6, 4, 5})
            )
    );

    @Override
    public String id() {
        return "standard-card";
    }

    @Override
    public String displayName() {
        return "Standard tally cards";
    }

    @Override
    public boolean supports(int playerCount) {
        return CHARTS.containsKey(playerCount);
    }

    @Override
    public Schedule build(ScheduleRequest request) {
        List<List<int[]>> chart = CHARTS.get(request.playerCount());
        if (chart == null) {
            throw new IllegalArgumentException(
                    "No standard card chart for " + request.playerCount() + " players");
        }

        int roundCount = request.requestedRounds() == null
                ? chart.size()
                : Math.min(request.requestedRounds(), chart.size());

        List<ScheduledRound> rounds = new ArrayList<>();
        for (int r = 0; r < roundCount; r++) {
            List<int[]> chartRound = chart.get(r);
            List<TableAssignment> tables = new ArrayList<>();
            for (int t = 0; t < chartRound.size(); t++) {
                int[] row = chartRound.get(t);
                tables.add(new TableAssignment(t + 1, row[0], row[1], row[2], row[3]));
            }
            rounds.add(new ScheduledRound(r + 1, tables));
        }
        return new Schedule(rounds);
    }
}
