package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    List<Organizer> findByGroupId(Long groupId);
}
