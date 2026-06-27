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
        long completedRunCount,
        // #5 확장 필드
        String riskItems,
        String scope,
        Integer teamSize,
        String teamMembers,
        String qualityCriteria,
        String budget,
        String planNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestPlanResponse from(TestPlan plan, long suiteCount, long testCaseCount, long completedRunCount) {
        return new TestPlanResponse(
                plan.getId(), plan.getName(), plan.getDescription(), plan.getStatus(),
                plan.getAssignee(), plan.getStartDate(), plan.getEndDate(),
                suiteCount, testCaseCount, completedRunCount,
                plan.getRiskItems(), plan.getScope(), plan.getTeamSize(), plan.getTeamMembers(),
                plan.getQualityCriteria(), plan.getBudget(), plan.getPlanNotes(),
                plan.getCreatedAt(), plan.getUpdatedAt()
        );
    }
}
