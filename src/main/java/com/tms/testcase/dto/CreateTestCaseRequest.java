package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTestCaseRequest(
        @NotNull
        TestCaseType type,

        @NotNull
        TestCasePriority priority,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String description,

        @NotBlank
        String precondition,

        @NotBlank
        String steps,

        @NotBlank
        String expected,

        String notes
) {
}
