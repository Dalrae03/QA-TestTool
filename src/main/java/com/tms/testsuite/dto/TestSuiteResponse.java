package com.tms.testsuite.dto;

import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testsuite.entity.TestSuite;
import java.time.LocalDateTime;
import java.util.List;

public record TestSuiteResponse(
        Long id,
        Long testPlanId,   // null 가능 (독립 스위트)
        String testPlanName,
        String name,
        String description,
        List<TestCaseResponse> testCases,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestSuiteResponse from(TestSuite suite) {
        return new TestSuiteResponse(
                suite.getId(),
                suite.getTestPlan() != null ? suite.getTestPlan().getId() : null,
                suite.getTestPlan() != null ? suite.getTestPlan().getName() : null,
                suite.getName(),
                suite.getDescription(),
                suite.getTestCases().stream().map(TestCaseResponse::from).toList(),
                suite.getCreatedAt(),
                suite.getUpdatedAt()
        );
    }
}
