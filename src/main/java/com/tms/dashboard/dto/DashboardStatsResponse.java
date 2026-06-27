package com.tms.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsResponse(
        // 테스트케이스 통계
        long totalTestCases,
        Map<String, Long> testCasesByStatus,
        Map<String, Long> testCasesByPriority,
        Map<String, Long> testCasesByAreaTag,

        // 테스트런 현황 (#12)
        long totalExecutions,
        long activeExecutions,
        long completedExecutions,
        double passRate,
        List<ExecutionSummary> recentExecutions,

        // 결함 현황
        long totalDefects,
        Map<String, Long> defectsBySeverity,
        Map<String, Long> defectsByStatus,

        // 결함 히트맵 데이터 (#13) — 영역태그 x 결함심각도 매트릭스
        List<HeatmapEntry> defectHeatmap,

        // 최근 감사 로그 (#13)
        List<AuditLogEntry> recentAuditLogs
) {
    public record ExecutionSummary(
            Long id,
            String name,
            String status,
            int total,
            int passed,
            int failed,
            int blocked,
            int untested,
            String createdAt
    ) {}

    public record HeatmapEntry(
            String areaTag,
            String severity,
            long count
    ) {}

    public record AuditLogEntry(
            Long id,
            String action,
            String entityType,
            Long entityId,
            String summary,
            String actor,
            String createdAt
    ) {}
}
