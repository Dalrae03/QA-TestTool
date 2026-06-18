package com.tms.environment.dto;

import com.tms.environment.entity.ServerEnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServerEnvironmentRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ServerEnvironmentType type,
        @NotBlank @Size(max = 500) String baseUrl,
        String description,
        boolean active
) {
}
