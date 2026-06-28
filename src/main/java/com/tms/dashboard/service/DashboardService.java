package com.tms.dashboard.service;

import com.tms.audit.entity.AuditLog;
import com.tms.audit.repository.AuditLogRepository;
import com.tms.dashboard.dto.DashboardStatsResponse;
import com.tms.dashboard.dto.DashboardStatsResponse.AuditLogEntry;
import com.tms.dashboard.dto.DashboardStatsResponse.ExecutionSummary;
import com.tms.dashboard.dto.DashboardStatsResponse.HeatmapEntry;
import com.tms.defect.entity.Defect;
import com.tms.defect.repository.DefectRepository;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.entity.ResultStatus;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TestCaseRepository testCaseRepository;
    private final ExecutionRepository executionRepository;
    private final DefectRepository defectRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(TestCaseRepository testCaseRepository,
                            ExecutionRepository executionRepository,
                            DefectRepository defectRepository,
                            AuditLogRepository auditLogRepository) {
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.defectRepository = defectRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public DashboardStatsResponse getStats(Long projectId) {
        List<TestCase> testCases = projectId != null
                ? testCaseRepository.findAllByProjectId(projectId)
                : testCaseRepository.findAll();

        List<Execution> executions = projectId != null
                ? executionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                : executionRepository.findAllByOrderByCreatedAtDesc();
        List<Defect> defects = defectRepository.findAllByOrderByCreatedAtDesc();

        // TC 통계
        Map<String, Long> byStatus = testCases.stream()
                .collect(Collectors.groupingBy(tc -> tc.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPriority = testCases.stream()
                .collect(Collectors.groupingBy(tc -> tc.getPriority().name(), Collectors.counting()));
        Map<String, Long> byAreaTag = new HashMap<>();
        testCases.forEach(tc -> tc.getAreaTags().forEach(tag ->
                byAreaTag.merge(tag.getName(), 1L, Long::sum)));

        // 실행 통계 (#12)
        long active = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.IN_PROGRESS).count();
        long completed = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.COMPLETED).count();

        long totalItems = executions.stream().mapToLong(e -> e.getItems().size()).sum();
        long passedItems = executions.stream().flatMap(e -> e.getItems().stream())
                .filter(i -> i.getStatus() == ResultStatus.PASSED).count();
        double passRate = totalItems > 0 ? (double) passedItems / totalItems * 100 : 0;

        List<ExecutionSummary> recentExecs = executions.stream().limit(10).map(e -> {
            List<ExecutionItem> items = e.getItems();
            int total = items.size();
            int passed = (int) items.stream().filter(i -> i.getStatus() == ResultStatus.PASSED).count();
            int failed = (int) items.stream().filter(i -> i.getStatus() == ResultStatus.FAILED).count();
            int blocked = (int) items.stream().filter(i -> i.getStatus() == ResultStatus.BLOCKED).count();
            int untested = total - passed - failed - blocked;
            return new ExecutionSummary(e.getId(), e.getName(), e.getStatus().name(),
                    total, passed, failed, blocked, untested,
                    e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        }).toList();

        // 결함 통계
        Map<String, Long> defectsBySeverity = defects.stream()
                .collect(Collectors.groupingBy(d -> d.getSeverity().name(), Collectors.counting()));
        Map<String, Long> defectsByStatus = defects.stream()
                .collect(Collectors.groupingBy(d -> d.getStatus().name(), Collectors.counting()));

        // 결함 히트맵 (#13) — 영역태그별 결함(실패) 빈도.
        // 각 테스트케이스에 연결된 결함을 그 케이스의 영역태그별로 누적하되, 영역 안에서 결함은 중복 없이(distinct) 센다.
        // testCases 가 이미 프로젝트 범위로 필터링돼 있어 히트맵도 자동으로 프로젝트 스코프를 따른다.
        Map<String, Set<Long>> defectIdsByArea = new HashMap<>();
        testCases.forEach(tc -> {
            if (tc.getDefects().isEmpty() || tc.getAreaTags().isEmpty()) return;
            List<Long> defectIds = tc.getDefects().stream().map(Defect::getId).toList();
            tc.getAreaTags().forEach(tag ->
                    defectIdsByArea.computeIfAbsent(tag.getName(), k -> new HashSet<>()).addAll(defectIds));
        });
        List<HeatmapEntry> heatmap = defectIdsByArea.entrySet().stream()
                .map(entry -> new HeatmapEntry(entry.getKey(), (long) entry.getValue().size()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();

        // 최근 감사 로그 (#13) — TEST_CASE 로그는 현재 프로젝트 소속 케이스만 추리고,
        // 그 외 타입(테스트런·결함·첨부·설정·백업·가져오기 등)은 프로젝트 매핑이 없어 항상 포함한다.
        java.util.Set<Long> projectTestCaseIds = projectId != null
                ? testCases.stream().map(TestCase::getId).collect(Collectors.toSet())
                : null;
        List<AuditLog> logs = auditLogRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
                .filter(log -> projectTestCaseIds == null
                        || !"TEST_CASE".equals(log.getEntityType())
                        || projectTestCaseIds.contains(log.getEntityId()))
                .limit(20)
                .toList();
        List<AuditLogEntry> auditEntries = logs.stream().map(log ->
                new AuditLogEntry(log.getId(), log.getAction().name(), log.getEntityType(),
                        log.getEntityId(), log.getSummary(), log.getActor(),
                        log.getCreatedAt() != null ? log.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
        ).toList();

        return new DashboardStatsResponse(
                testCases.size(), byStatus, byPriority, byAreaTag,
                executions.size(), active, completed, passRate, recentExecs,
                defects.size(), defectsBySeverity, defectsByStatus,
                heatmap, auditEntries
        );
    }
}
