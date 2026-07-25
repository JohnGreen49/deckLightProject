package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByTournamentIdOrderByRoundNumber(Long tournamentId);
}
