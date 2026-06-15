package com.tms.testplan.dto;

import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.entity.TestPlanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TestPlanResponse(
        Long id,
        String name,
        String description,
        TestPlanStatus status,
        String assignee,
        LocalDate startDate,
        LocalDate endDate,
        long suiteCount,
        long testCaseCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestPlanResponse from(TestPlan plan, long suiteCount, long testCaseCount) {
        return new TestPlanResponse(
                plan.getId(), plan.getName(), plan.getDescription(), plan.getStatus(),
                plan.getAssignee(), plan.getStartDate(), plan.getEndDate(), suiteCount, testCaseCount,
                plan.getCreatedAt(), plan.getUpdatedAt()
        );
    }
}
