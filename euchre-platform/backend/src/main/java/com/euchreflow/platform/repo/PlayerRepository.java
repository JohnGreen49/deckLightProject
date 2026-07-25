package com.euchreflow.platform.repo;

import com.euchreflow.platform.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByGroupId(Long groupId);
    Optional<Player> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);
}
