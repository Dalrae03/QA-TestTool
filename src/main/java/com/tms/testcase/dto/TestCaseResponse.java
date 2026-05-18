package com.tms.testcase.dto;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseType;

public record TestCaseResponse(
        Long id,
        TestCaseType type,
        String title,
        String description,
        String precondition,
        String steps,
        String expected,
        String notes
) {
    public static TestCaseResponse from(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getType(),
                testCase.getTitle(),
                testCase.getDescription(),
                testCase.getPrecondition(),
                testCase.getSteps(),
                testCase.getExpected(),
                testCase.getNotes()
        );
    }
}
