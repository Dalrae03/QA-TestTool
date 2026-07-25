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

        // 결함 히트맵 데이터 (#13) — 영역태그별 결함(실패) 빈도
        List<HeatmapEntry> defectHeatmap,

        // 잔존 이슈 = 미해결 결함(OPEN·IN_PROGRESS) 목록
        List<OpenDefectEntry> openDefects,

        // 최근 감사 로그 (#13)
        List<AuditLogEntry> recentAuditLogs
) {
    public record OpenDefectEntry(
            Long id,
            String title,
            String severity,
            String status
    ) {}

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

    // 결함 히트맵 한 행 — 영역태그별로 심각도(CRITICAL/MAJOR/MINOR/TRIVIAL)마다 서로 다른 결함 수.
    public record HeatmapEntry(
            String areaTag,
            Map<String, Long> bySeverity,
            long total
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
