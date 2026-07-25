package com.euchreflow.platform.scheduling;

import java.util.List;

/** The full set of rounds a scheduler produces for a tournament. */
public record Schedule(List<ScheduledRound> rounds) {}
