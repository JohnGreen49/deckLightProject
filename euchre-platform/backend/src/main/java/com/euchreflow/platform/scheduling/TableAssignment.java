package com.euchreflow.platform.scheduling;

/**
 * One table in one round, expressed purely in <em>seat indices</em> (0-based) rather than
 * player ids, so schedulers stay independent of the roster. Team 1 is ({@code t1a}, {@code t1b}),
 * team 2 is ({@code t2a}, {@code t2b}).
 */
public record TableAssignment(int tableNumber, int t1a, int t1b, int t2a, int t2b) {

    /** All four seat indices at this table. */
    public int[] seats() {
        return new int[] { t1a, t1b, t2a, t2b };
    }
}
