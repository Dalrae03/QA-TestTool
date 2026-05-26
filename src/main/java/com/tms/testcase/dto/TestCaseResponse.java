package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
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
        String expected,
        String notes,
        TestCaseOs os,
        TestCaseBrowser browser,
        TestCaseDevice device,
        List<AreaTagResponse> areaTags
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
                testCase.getExpected(),
                testCase.getNotes(),
                testCase.getOs(),
                testCase.getBrowser(),
                testCase.getDevice(),
                testCase.getAreaTags().stream().map(AreaTagResponse::from).toList()
        );
    }
}
