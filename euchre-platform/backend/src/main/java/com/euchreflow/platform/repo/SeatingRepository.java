package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Seating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatingRepository extends JpaRepository<Seating, Long> {
    List<Seating> findByRoundIdOrderByTableNumber(Long roundId);
    List<Seating> findByRoundIdIn(List<Long> roundIds);
}
