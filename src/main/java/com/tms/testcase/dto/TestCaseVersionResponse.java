package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.entity.TestCaseVersion;
import java.time.LocalDateTime;

public record TestCaseVersionResponse(
        Long id,
        Long testCaseId,
        Integer versionNumber,
        String label,
        String changeSummary,
        TestCaseType type,
        TestCasePriority priority,
        TestCaseStatus status,
        String title,
        String version,
        String description,
        String precondition,
        String steps,
        String notes,
        TestCaseOs os,
        TestCaseBrowser browser,
        TestCaseDevice device,
        String assignee,
        Long folderId,
        String folderName,
        Long serverEnvironmentId,
        String serverEnvironmentName,
        Long testConfigurationId,
        String testConfigurationName,
        String areaTagNames,
        LocalDateTime createdAt
) {
    public static TestCaseVersionResponse from(TestCaseVersion version) {
        return new TestCaseVersionResponse(
                version.getId(),
                version.getTestCaseId(),
                version.getVersionNumber(),
                version.getLabel(),
                version.getChangeSummary(),
                version.getType(),
                version.getPriority(),
                version.getStatus(),
                version.getTitle(),
                version.getVersion(),
                version.getDescription(),
                version.getPrecondition(),
                version.getSteps(),
                version.getNotes(),
                version.getOs(),
                version.getBrowser(),
                version.getDevice(),
                version.getAssignee(),
                version.getFolderId(),
                version.getFolderName(),
                version.getServerEnvironmentId(),
                version.getServerEnvironmentName(),
                version.getTestConfigurationId(),
                version.getTestConfigurationName(),
                version.getAreaTagNames(),
                version.getCreatedAt()
        );
    }
}
