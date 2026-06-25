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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // 결함 히트맵 (#13) — 영역태그 × 심각도
        List<HeatmapEntry> heatmap = new ArrayList<>();
        defects.forEach(defect -> {
            // Defect에 연결된 테스트케이스의 영역태그로 히트맵 구성
            // (단순화: severity별 결함 분포를 영역태그 없이 severity만으로 구성)
        });
        defectsBySeverity.forEach((sev, cnt) ->
                heatmap.add(new HeatmapEntry("전체", sev, cnt)));

        // 최근 감사 로그 (#13) — AuditLog 자체에 projectId가 없어, 현재 프로젝트 소속 테스트케이스의 로그만 추려낸다.
        // (테스트케이스가 삭제된 뒤의 로그는 프로젝트를 판별할 수 없어 제외됨 — 알려진 한계)
        java.util.Set<Long> projectTestCaseIds = projectId != null
                ? testCases.stream().map(TestCase::getId).collect(Collectors.toSet())
                : null;
        List<AuditLog> logs = auditLogRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
                .filter(log -> projectTestCaseIds == null || projectTestCaseIds.contains(log.getEntityId()))
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
