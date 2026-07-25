package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findBySlug(String slug);
}
