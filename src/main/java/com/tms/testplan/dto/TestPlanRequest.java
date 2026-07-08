package com.tms.testplan.dto;

import com.tms.testplan.entity.TestPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TestPlanRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull TestPlanStatus status,
        @Size(max = 100) String assignee,
        LocalDate startDate,
        LocalDate endDate,
        // ── 1. 기본 정보 ──
        @Size(max = 200) String targetSystem,
        @Size(max = 200) String targetVersion,
        // ── 2. 프로젝트 개요 ──
        String testGoal,
        String testTarget,
        // ── 3. 테스트 범위 ──
        List<Long> coreTestCaseIds,
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
        Long projectId
) {}
