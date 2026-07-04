package com.tms.testcase.dto;

import com.tms.configuration.dto.TestConfigurationResponse;
import com.tms.defect.dto.DefectResponse;
import com.tms.environment.dto.ServerEnvironmentResponse;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import java.time.LocalDateTime;
import java.util.List;

public record TestCaseResponse(
        Long id,
        TestCaseType type,
        TestCasePriority priority,
        TestCaseStatus status,
        String title,
        String description,
        String precondition,
        String steps,
        String notes,
        TestCaseOs os,
        TestCaseBrowser browser,
        TestCaseDevice device,
        String assignee,
        String version,
        Long folderId,
        String folderName,
        ServerEnvironmentResponse serverEnvironment,
        TestConfigurationResponse testConfiguration,
        List<AreaTagResponse> areaTags,
        List<DefectResponse> defects,
        List<String> jiraRequirementKeys,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestCaseResponse from(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getType(),
                testCase.getPriority(),
                testCase.getStatus(),
                testCase.getTitle(),
                testCase.getDescription(),
                testCase.getPrecondition(),
                testCase.getSteps(),
                testCase.getNotes(),
                testCase.getOs(),
                testCase.getBrowser(),
                testCase.getDevice(),
                testCase.getAssignee(),
                testCase.getVersion(),
                testCase.getFolder() == null ? null : testCase.getFolder().getId(),
                testCase.getFolder() == null ? null : testCase.getFolder().getName(),
                testCase.getServerEnvironment() == null ? null : ServerEnvironmentResponse.from(testCase.getServerEnvironment()),
                testCase.getTestConfiguration() == null ? null : TestConfigurationResponse.from(testCase.getTestConfiguration()),
                testCase.getAreaTags().stream().map(AreaTagResponse::from).toList(),
                testCase.getDefects().stream().map(DefectResponse::from).toList(),
                List.copyOf(testCase.getJiraRequirementKeys()),
                testCase.getCreatedAt(),
                testCase.getUpdatedAt()
        );
    }
}
