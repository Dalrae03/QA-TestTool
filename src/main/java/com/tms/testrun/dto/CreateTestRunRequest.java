package com.tms.testrun.dto;

import com.tms.testrun.entity.TestRunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTestRunRequest(
        @NotNull
        TestRunStatus status,

        @NotBlank
        String actualResult,

        String notes
) {
}
