package com.euchreflow.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single game at one table in one round: team 1 (p1 &amp; p2) versus team 2 (p1 &amp; p2).
 */
@Entity
@Table(name = "seatings")
@Getter
@Setter
public class Seating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "table_number", nullable = false)
    private int tableNumber;

    @Column(name = "team1_p1", nullable = false)
    private Long team1p1;

    @Column(name = "team1_p2", nullable = false)
    private Long team1p2;

    @Column(name = "team2_p1", nullable = false)
    private Long team2p1;

    @Column(name = "team2_p2", nullable = false)
    private Long team2p2;

    /** @return true if the given player id sits at this table (on either team). */
    public boolean seats(Long playerId) {
        return playerId.equals(team1p1) || playerId.equals(team1p2)
                || playerId.equals(team2p1) || playerId.equals(team2p2);
    }
}
