package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.Player;
import com.euchreflow.platform.repo.PlayerRepository;
import com.euchreflow.platform.web.NotFoundException;
import com.euchreflow.platform.web.dto.PlayerDtos.CreatePlayer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    /** Unambiguous alphabet for join codes (no O/0, I/1). */
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final PlayerRepository players;
    private final GroupService groups;

    public PlayerService(PlayerRepository players, GroupService groups) {
        this.players = players;
        this.groups = groups;
    }

    @Transactional
    public Player create(Long groupId, String name, String email) {
        groups.get(groupId); // validates existence
        Player p = new Player();
        p.setGroupId(groupId);
        p.setName(name.trim());
        p.setEmail(StringUtils.hasText(email) ? email.trim() : null);
        p.setJoinCode(uniqueJoinCode());
        return players.save(p);
    }

    /**
     * Import many players from CSV text: one per line, {@code Name} or {@code Name, email}.
     * Blank lines are skipped. Returns the created players.
     */
    @Transactional
    public List<Player> importCsv(Long groupId, String csv) {
        groups.get(groupId);
        List<Player> created = new ArrayList<>();
        for (String rawLine : csv.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", 2);
            String name = parts[0].strip();
            if (name.isEmpty()) {
                continue;
            }
            String email = parts.length > 1 ? parts[1].strip() : null;
            created.add(create(groupId, name, email));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<Player> listByGroup(Long groupId) {
        return players.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public Player get(Long id) {
        return players.findById(id).orElseThrow(() -> new NotFoundException("player not found: " + id));
    }

    @Transactional(readOnly = true)
    public Player getByJoinCode(String joinCode) {
        return players.findByJoinCode(joinCode.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("no player for join code"));
    }

    private String uniqueJoinCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (!players.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not allocate a unique join code");
    }
}
