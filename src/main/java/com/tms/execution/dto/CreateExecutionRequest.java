package com.tms.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExecutionRequest(
        @NotNull Long suiteId,
        @Size(max = 200) String name,
        String description,
        @Size(max = 100) String assignee
) {
}
