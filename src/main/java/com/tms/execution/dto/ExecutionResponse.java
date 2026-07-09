package com.tms.execution.dto;

import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ExecutionResponse(
        Long id,
        String name,
        String version,
        String description,
        Long testPlanId,
        String planName,
        Long testSuiteId,
        String suiteName,
        Long testConfigurationId,
        String configurationName,
        String environmentDetail,
        ExecutionStatus status,
        String assignee,
        int total,
        int untested,
        int passed,
        int failed,
        int blocked,
        int retest,
        int progressPct,
        List<ExecutionItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    /** 목록용 — items 제외, 집계만 포함. */
    public static ExecutionResponse summary(Execution execution) {
        return build(execution, null);
    }

    /** 상세용 — items 포함. defectCounts: testCaseId → 연결된 결함 수 (없으면 0으로 처리). */
    public static ExecutionResponse detail(Execution execution, Map<Long, Integer> defectCounts) {
        return build(execution, execution.getItems().stream()
                .map(item -> ExecutionItemResponse.from(item, defectCounts))
                .toList());
    }

    private static ExecutionResponse build(Execution execution, List<ExecutionItemResponse> items) {
        List<ExecutionItem> all = execution.getItems();
        int total = all.size();
        int passed = count(all, ResultStatus.PASSED);
        int failed = count(all, ResultStatus.FAILED);
        int blocked = count(all, ResultStatus.BLOCKED);
        int retest = count(all, ResultStatus.RETEST);
        int untested = count(all, ResultStatus.UNTESTED);
        int executed = total - untested;
        int progressPct = total == 0 ? 0 : Math.round((executed * 100f) / total);

        return new ExecutionResponse(
                execution.getId(),
                execution.getName(),
                execution.getVersion(),
                execution.getDescription(),
                execution.getTestPlanId(),
                execution.getPlanName(),
                execution.getTestSuiteId(),
                execution.getSuiteName(),
                execution.getTestConfigurationId(),
                execution.getConfigurationName(),
                execution.getEnvironmentDetail(),
                execution.getStatus(),
                execution.getAssignee(),
                total, untested, passed, failed, blocked, retest, progressPct,
                items,
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                execution.getCompletedAt()
        );
    }

    private static int count(List<ExecutionItem> items, ResultStatus status) {
        return (int) items.stream().filter(item -> item.getStatus() == status).count();
    }
}
