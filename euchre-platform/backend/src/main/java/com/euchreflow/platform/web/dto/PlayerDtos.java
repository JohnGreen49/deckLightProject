package com.euchreflow.platform.web.dto;

import com.euchreflow.platform.domain.Player;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PlayerDtos {
    private PlayerDtos() {}

    public record CreatePlayer(
            @NotBlank @Size(max = 200) String name,
            @Email @Size(max = 320) String email
    ) {}

    /** Bulk import: one player per line, "Name, email" (email optional). */
    public record ImportPlayers(@NotBlank String csv) {}

    public record PlayerView(Long id, String name, String email, String joinCode) {
        public static PlayerView of(Player p) {
            return new PlayerView(p.getId(), p.getName(), p.getEmail(), p.getJoinCode());
        }
    }
}
