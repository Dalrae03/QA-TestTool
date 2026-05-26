package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateTestCaseRequest(
        @NotNull
        TestCaseType type,

        @NotNull
        TestCasePriority priority,

        TestCaseStatus status,

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

        String notes,

        TestCaseOs os,

        TestCaseBrowser browser,

        TestCaseDevice device,

        List<Long> areaTagIds
) {
}
