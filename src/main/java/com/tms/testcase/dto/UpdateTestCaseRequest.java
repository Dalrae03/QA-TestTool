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

public record UpdateTestCaseRequest(
        @NotNull
        TestCaseType type,

        @NotNull
        TestCasePriority priority,

        @NotNull
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

        String notes,

        TestCaseOs os,

        TestCaseBrowser browser,

        TestCaseDevice device,

        List<Long> areaTagIds,

        Long serverEnvironmentId,

        Long testConfigurationId,

        @Size(max = 100) String assignee,

        @Size(max = 50) String version,

        Long folderId,

        Long projectId,

        String expectedResult
) {
    public UpdateTestCaseRequest(
            TestCaseType type, TestCasePriority priority, TestCaseStatus status, String title,
            String description, String precondition, String steps, String notes, TestCaseOs os,
            TestCaseBrowser browser, TestCaseDevice device, List<Long> areaTagIds
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTagIds, null, null, null, null, null, null, null);
    }

    public UpdateTestCaseRequest(
            TestCaseType type, TestCasePriority priority, TestCaseStatus status, String title,
            String description, String precondition, String steps, String notes, TestCaseOs os,
            TestCaseBrowser browser, TestCaseDevice device, List<Long> areaTagIds,
            Long serverEnvironmentId
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTagIds, serverEnvironmentId, null, null, null, null, null, null);
    }

    public UpdateTestCaseRequest(
            TestCaseType type, TestCasePriority priority, TestCaseStatus status, String title,
            String description, String precondition, String steps, String notes, TestCaseOs os,
            TestCaseBrowser browser, TestCaseDevice device, List<Long> areaTagIds,
            Long serverEnvironmentId, Long testConfigurationId
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTagIds, serverEnvironmentId, testConfigurationId, null, null, null, null, null);
    }
}
