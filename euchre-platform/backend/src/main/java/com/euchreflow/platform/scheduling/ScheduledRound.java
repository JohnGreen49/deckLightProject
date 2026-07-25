package com.euchreflow.platform.scheduling;

import java.util.List;

/** A round produced by a scheduler: an ordered list of table assignments. */
public record ScheduledRound(int roundNumber, List<TableAssignment> tables) {}
