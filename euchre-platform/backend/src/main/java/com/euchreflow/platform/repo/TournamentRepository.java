package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
