package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByTournamentIdOrderBySeatIndex(Long tournamentId);
}
