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
        String expectedResult,
        String notes,
        TestCaseOs os,
        TestCaseBrowser browser,
        TestCaseDevice device,
        String assignee,
        String version,
        String currentVersionLabel,
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
    /** 하위 호환 — 버전 이력 폴백 없이 현재 저장된 version 필드 그대로를 라벨로 사용한다. */
    public static TestCaseResponse from(TestCase testCase) {
        return from(testCase, testCase.getVersion());
    }

    /**
     * currentVersionLabel: 목록 등에서 "현재 버전"으로 보여줄 라벨.
     * 사용자가 버전 필드를 직접 입력하지 않았을 때는 호출부에서 최신 TestCaseVersion의 라벨로 폴백해 넘겨준다.
     */
    public static TestCaseResponse from(TestCase testCase, String currentVersionLabel) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getType(),
                testCase.getPriority(),
                testCase.getStatus(),
                testCase.getTitle(),
                testCase.getDescription(),
                testCase.getPrecondition(),
                testCase.getSteps(),
                testCase.getExpectedResult(),
                testCase.getNotes(),
                testCase.getOs(),
                testCase.getBrowser(),
                testCase.getDevice(),
                testCase.getAssignee(),
                testCase.getVersion(),
                currentVersionLabel,
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
