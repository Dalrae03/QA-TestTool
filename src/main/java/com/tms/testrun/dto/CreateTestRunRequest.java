package com.tms.testrun.dto;

import com.tms.execution.entity.ResultStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTestRunRequest(
        @NotNull
        ResultStatus status,

        @NotBlank
        String actualResult,

        String notes,

        @Size(max = 100) String assignee,

        String failureReason
) {
    @AssertTrue(message = "실행 결과는 PASSED, FAILED, BLOCKED 중 하나여야 합니다.")
    public boolean isSupportedStatus() {
        return status == null
                || status == ResultStatus.PASSED
                || status == ResultStatus.FAILED
                || status == ResultStatus.BLOCKED;
    }
}
