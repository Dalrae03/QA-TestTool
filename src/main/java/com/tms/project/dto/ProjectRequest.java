package com.tms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @Size(max = 100) String owner
) {}
