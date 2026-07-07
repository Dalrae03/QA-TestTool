package com.tms.testplan.dto;

import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.entity.TestPlanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TestPlanResponse(
        Long id,
        String name,
        TestPlanStatus status,
        String assignee,
        LocalDate startDate,
        LocalDate endDate,
        long suiteCount,
        long testCaseCount,
        long completedRunCount,
        // ── 1. 기본 정보 ──
        String targetSystem,
        String targetVersion,
        // ── 2. 프로젝트 개요 ──
        String testGoal,
        String testTarget,
        // ── 3. 테스트 범위 ──
        List<TestCaseResponse> coreTestCases,
        String impactScope,
        String commonScope,
        // ── 5. 테스트 우선순위 및 리스크 ──
        String priorityTargets,
        String riskAnalysis,
        // ── 6. 테스트 전략 ──
        String testApproach,
        String testPerspective,
        // ── 7. 진입 조건, 종료 기준 ──
        String entryCriteria,
        String exitCriteria,
        // ── 9. 테스트 환경 ──
        String serverEnvironmentNote,
        String deviceMatrix,
        String testData,
        // ── 10. 테스트 일정 및 절차 ──
        String schedule,
        // ── 11. 산출물 ──
        String deliverables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestPlanResponse from(TestPlan plan, long suiteCount, long testCaseCount, long completedRunCount) {
        return new TestPlanResponse(
                plan.getId(), plan.getName(), plan.getStatus(), plan.getAssignee(),
                plan.getStartDate(), plan.getEndDate(),
                suiteCount, testCaseCount, completedRunCount,
                plan.getTargetSystem(), plan.getTargetVersion(),
                plan.getTestGoal(), plan.getTestTarget(),
                plan.getCoreTestCases().stream().map(TestCaseResponse::from).toList(),
                plan.getImpactScope(), plan.getCommonScope(),
                plan.getPriorityTargets(), plan.getRiskAnalysis(),
                plan.getTestApproach(), plan.getTestPerspective(),
                plan.getEntryCriteria(), plan.getExitCriteria(),
                plan.getServerEnvironmentNote(), plan.getDeviceMatrix(), plan.getTestData(),
                plan.getSchedule(),
                plan.getDeliverables(),
                plan.getCreatedAt(), plan.getUpdatedAt()
        );
    }
}
