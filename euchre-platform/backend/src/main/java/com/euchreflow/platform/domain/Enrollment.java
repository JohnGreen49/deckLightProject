package com.euchreflow.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Links a player into a tournament with a stable {@code seatIndex} (0..N-1).
 * Schedulers work purely in terms of seat indices; the seat index maps back to a player.
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "seat_index", nullable = false)
    private int seatIndex;
}
