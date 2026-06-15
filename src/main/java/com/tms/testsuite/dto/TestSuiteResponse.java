package com.tms.testsuite.dto;

import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testsuite.entity.TestSuite;
import java.time.LocalDateTime;
import java.util.List;

public record TestSuiteResponse(
        Long id,
        Long testPlanId,
        String name,
        String description,
        List<TestCaseResponse> testCases,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestSuiteResponse from(TestSuite suite) {
        return new TestSuiteResponse(
                suite.getId(), suite.getTestPlan().getId(), suite.getName(), suite.getDescription(),
                suite.getTestCases().stream().map(TestCaseResponse::from).toList(),
                suite.getCreatedAt(), suite.getUpdatedAt()
        );
    }
}
