package com.tms.environment.dto;

import com.tms.environment.entity.ServerEnvironment;
import com.tms.environment.entity.ServerEnvironmentType;
import java.time.LocalDateTime;

public record ServerEnvironmentResponse(
        Long id,
        String name,
        ServerEnvironmentType type,
        String baseUrl,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServerEnvironmentResponse from(ServerEnvironment environment) {
        return new ServerEnvironmentResponse(
                environment.getId(), environment.getName(), environment.getType(), environment.getBaseUrl(),
                environment.getDescription(), environment.isActive(), environment.getCreatedAt(), environment.getUpdatedAt()
        );
    }
}
