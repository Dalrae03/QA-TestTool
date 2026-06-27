package com.tms.testrun.dto;

import com.tms.execution.entity.ResultStatus;
import com.tms.testrun.entity.TestRun;
import java.time.LocalDateTime;

public record TestRunResponse(
        Long id,
        Long testCaseId,
        ResultStatus status,
        String actualResult,
        String notes,
        String assignee,
        String failureReason,
        LocalDateTime executedAt
) {
    public static TestRunResponse from(TestRun testRun) {
        return new TestRunResponse(
                testRun.getId(),
                testRun.getTestCase().getId(),
                testRun.getStatus(),
                testRun.getActualResult(),
                testRun.getNotes(),
                testRun.getAssignee(),
                testRun.getFailureReason(),
                testRun.getExecutedAt()
        );
    }
}
