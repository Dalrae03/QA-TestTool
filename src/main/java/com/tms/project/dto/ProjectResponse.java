package com.tms.project.dto;

import com.tms.project.entity.Project;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String owner,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getOwner(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
