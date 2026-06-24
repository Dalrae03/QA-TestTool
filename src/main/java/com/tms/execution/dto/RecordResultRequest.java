package com.tms.execution.dto;

import com.tms.execution.entity.ResultStatus;
import jakarta.validation.constraints.NotNull;

public record RecordResultRequest(
        @NotNull ResultStatus status,
        String comment
) {
}
