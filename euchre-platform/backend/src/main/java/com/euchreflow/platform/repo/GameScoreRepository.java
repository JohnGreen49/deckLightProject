package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.GameScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {
    Optional<GameScore> findBySeatingId(Long seatingId);
    List<GameScore> findBySeatingIdIn(List<Long> seatingIds);
}
