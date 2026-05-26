package com.tms.testrun.dto;

import com.tms.testrun.entity.TestRun;
import com.tms.testrun.entity.TestRunStatus;
import java.time.LocalDateTime;

public record TestRunResponse(
        Long id,
        Long testCaseId,
        TestRunStatus status,
        String actualResult,
        String notes,
        LocalDateTime executedAt
) {
    public static TestRunResponse from(TestRun testRun) {
        return new TestRunResponse(
                testRun.getId(),
                testRun.getTestCase().getId(),
                testRun.getStatus(),
                testRun.getActualResult(),
                testRun.getNotes(),
                testRun.getExecutedAt()
        );
    }
}
