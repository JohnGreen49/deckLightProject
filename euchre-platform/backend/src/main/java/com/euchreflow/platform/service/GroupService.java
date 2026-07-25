package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.Group;
import com.euchreflow.platform.repo.GroupRepository;
import com.euchreflow.platform.web.NotFoundException;
import com.euchreflow.platform.web.dto.GroupDtos.CreateGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groups;

    public GroupService(GroupRepository groups) {
        this.groups = groups;
    }

    @Transactional
    public Group create(CreateGroup req) {
        groups.findBySlug(req.slug()).ifPresent(g -> {
            throw new IllegalArgumentException("slug already in use: " + req.slug());
        });
        Group g = new Group();
        g.setSlug(req.slug());
        g.setName(req.name());
        g.setTheme(StringUtils.hasText(req.theme()) ? req.theme() : "{}");
        return groups.save(g);
    }

    @Transactional(readOnly = true)
    public List<Group> list() {
        return groups.findAll();
    }

    @Transactional(readOnly = true)
    public Group get(Long id) {
        return groups.findById(id).orElseThrow(() -> new NotFoundException("group not found: " + id));
    }

    @Transactional(readOnly = true)
    public Group getBySlug(String slug) {
        return groups.findBySlug(slug).orElseThrow(() -> new NotFoundException("group not found: " + slug));
    }
}
