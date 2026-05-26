package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTestCaseStatusRequest(
        @NotNull
        TestCaseStatus status
) {
}
