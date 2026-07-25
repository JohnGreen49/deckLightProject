package com.euchreflow.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * The recorded result of a seating. Exactly one per seating; re-entering a score
 * updates the existing row (see {@code seating_id} unique constraint).
 */
@Entity
@Table(name = "game_scores")
@Getter
@Setter
public class GameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seating_id", nullable = false, unique = true)
    private Long seatingId;

    @Column(name = "team1_score", nullable = false)
    private int team1Score;

    @Column(name = "team2_score", nullable = false)
    private int team2Score;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
