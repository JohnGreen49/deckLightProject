package com.euchreflow.platform.web.dto;

import com.euchreflow.platform.domain.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class GroupDtos {
    private GroupDtos() {}

    public record CreateGroup(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "[a-z0-9-]+", message = "slug may contain only lowercase letters, digits and hyphens")
            String slug,
            @NotBlank @Size(max = 200) String name,
            String theme
    ) {}

    public record GroupView(Long id, String slug, String name, String theme) {
        public static GroupView of(Group g) {
            return new GroupView(g.getId(), g.getSlug(), g.getName(), g.getTheme());
        }
    }
}
