package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateExecutionRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull ExecutionStatus status,
        @Size(max = 100) String assignee
) {
}
