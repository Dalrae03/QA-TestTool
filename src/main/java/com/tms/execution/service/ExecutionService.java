package com.tms.execution.service;

import com.tms.execution.dto.BulkRecordResultRequest;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.TestCaseExecutionHistoryResponse;
import com.tms.execution.dto.UpdateExecutionEnvironmentRequest;
import com.tms.execution.dto.UpdateExecutionPlanRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.repository.ExecutionItemRepository;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.configuration.entity.TestConfiguration;
import com.tms.configuration.repository.TestConfigurationRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.global.util.ProjectScope;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseVersion;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestCaseVersionRepository;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final ExecutionItemRepository executionItemRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseVersionRepository testCaseVersionRepository;
    private final TestConfigurationRepository testConfigurationRepository;
    private final AttachmentService attachmentService;
    private final AuditLogService auditLogService;

    public ExecutionService(ExecutionRepository executionRepository, ExecutionItemRepository executionItemRepository,
                             TestSuiteRepository testSuiteRepository,
                             TestPlanRepository testPlanRepository, TestCaseRepository testCaseRepository,
                             TestCaseVersionRepository testCaseVersionRepository,
                             TestConfigurationRepository testConfigurationRepository,
                             AttachmentService attachmentService,
                             AuditLogService auditLogService) {
        this.executionRepository = executionRepository;
        this.executionItemRepository = executionItemRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testPlanRepository = testPlanRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseVersionRepository = testCaseVersionRepository;
        this.testConfigurationRepository = testConfigurationRepository;
        this.auditLogService = auditLogService;
        this.attachmentService = attachmentService;
    }

    /** 테스트케이스 상세 "실행 기록" 탭 — 이 케이스가 테스트런을 통해 실행된 모든 이력(버전 스냅샷 포함). */
    public List<TestCaseExecutionHistoryResponse> getExecutionHistory(Long testCaseId) {
        return executionItemRepository.findAllByTestCaseIdOrderByIdDesc(testCaseId).stream()
                .map(item -> TestCaseExecutionHistoryResponse.from(item, resolveVersion(item)))
                .toList();
    }

    /**
     * 버전 스냅샷 기능 도입 전에 만들어진 런은 ExecutionItem에 버전이 비어 있다 —
     * 그 런이 생성된 시각 기준으로 "당시 현재 버전"이었던 스냅샷을 역추적해 보여준다.
     */
    private TestCaseVersion resolveVersion(ExecutionItem item) {
        if (item.getVersionNumber() != null) return null; // 이미 스냅샷이 있으면 그대로 사용 (item 필드 우선)
        return testCaseVersionRepository
                .findFirstByTestCaseIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        item.getTestCaseId(), item.getExecution().getCreatedAt())
                .orElse(null);
    }

    /** 런 생성 시점 기준 테스트케이스의 최신 버전 스냅샷 — 없으면(버전 이력이 없는 구버전 케이스) null. */
    private void addItemWithCurrentVersion(Execution execution, TestCase testCase) {
        TestCaseVersion latest = testCaseVersionRepository
                .findTopByTestCaseIdOrderByVersionNumberDesc(testCase.getId())
                .orElse(null);
        Integer versionNumber = latest != null ? latest.getVersionNumber() : null;
        String versionLabel = latest != null ? latest.getLabel() : null;
        execution.addItem(testCase.getId(), testCase.getTitle(), versionNumber, versionLabel);
    }

    public List<ExecutionResponse> getExecutions(Long projectId, String assignee) {
        String assigneeFilter = normalizeOptional(assignee);
        List<Execution> executions;
        if (projectId != null && assigneeFilter != null) {
            executions = executionRepository.findAllByProjectIdAndAssigneeOrderByCreatedAtDesc(projectId, assigneeFilter);
        } else if (projectId != null) {
            executions = executionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        } else if (assigneeFilter != null) {
            executions = executionRepository.findAllByAssigneeOrderByCreatedAtDesc(assigneeFilter);
        } else {
            executions = executionRepository.findAllByOrderByCreatedAtDesc();
        }
        return executions.stream().map(ExecutionResponse::summary).toList();
    }

    public ExecutionResponse getExecution(Long id) {
        return ExecutionResponse.detail(findById(id));
    }

    @Transactional
    public ExecutionResponse createExecution(CreateExecutionRequest request) {
        if (request.suiteId() != null) {
            return createFromSuite(request);
        }
        return createFromTestCases(request);
    }

    private ExecutionResponse createFromSuite(CreateExecutionRequest request) {
        TestSuite suite = testSuiteRepository.findById(request.suiteId())
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + request.suiteId()));
        if (suite.getTestCases().isEmpty()) {
            throw new InvalidRequestException("테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다.");
        }

        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : suite.getName() + " — " + LocalDate.now();

        // B안: testPlan이 null일 수 있음
        Long planId = suite.getTestPlan() != null ? suite.getTestPlan().getId() : null;
        String planName = suite.getTestPlan() != null ? suite.getTestPlan().getName() : null;

        Execution execution = new Execution(
                name,
                normalizeOptional(request.description()),
                suite.getProjectId(),
                planId,
                planName,
                suite.getId(),
                suite.getName(),
                normalizeOptional(request.assignee()),
                normalizeOptional(request.version())
        );
        applyConfiguration(execution, request.testConfigurationId());
        suite.getTestCases().forEach(testCase -> addItemWithCurrentVersion(execution, testCase));
        return saveAndLog(execution);
    }

    /** 스위트 없이 테스트케이스를 직접 선택해 만드는 테스트런 — 임시 스위트를 만들지 않는다. */
    private ExecutionResponse createFromTestCases(CreateExecutionRequest request) {
        List<Long> ids = request.testCaseIds();
        if (ids == null || ids.isEmpty()) {
            throw new InvalidRequestException("스위트 또는 테스트케이스를 선택해야 합니다.");
        }
        List<Long> uniqueIds = new LinkedHashSet<>(ids).stream().toList();
        Map<Long, TestCase> casesById = testCaseRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        List<Long> missingIds = uniqueIds.stream().filter(id -> !casesById.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new InvalidRequestException("존재하지 않는 테스트케이스 ID가 포함되어 있습니다: " + missingIds);
        }
        List<Long> mismatched = uniqueIds.stream()
                .map(casesById::get)
                .filter(tc -> !ProjectScope.compatible(request.projectId(), tc.getProjectId()))
                .map(TestCase::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 테스트케이스로는 테스트런을 만들 수 없습니다: " + mismatched);
        }

        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : "직접 선택 테스트런 — " + LocalDate.now();

        Long planId = null;
        String planName = null;
        if (request.testPlanId() != null) {
            TestPlan plan = testPlanRepository.findById(request.testPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + request.testPlanId()));
            planId = plan.getId();
            planName = plan.getName();
        }

        Execution execution = new Execution(
                name,
                normalizeOptional(request.description()),
                request.projectId(),
                planId,
                planName,
                null,
                null,
                normalizeOptional(request.assignee()),
                normalizeOptional(request.version())
        );
        applyConfiguration(execution, request.testConfigurationId());
        uniqueIds.forEach(id -> addItemWithCurrentVersion(execution, casesById.get(id)));
        return saveAndLog(execution);
    }

    /** 요청의 컨피그 id로 실행환경 스냅샷(id + 이름 + 상세 한 줄)을 채운다. id가 null이면 환경 없음. */
    private void applyConfiguration(Execution execution, Long testConfigurationId) {
        if (testConfigurationId == null) {
            execution.updateEnvironment(null, null, null);
            return;
        }
        TestConfiguration config = testConfigurationRepository.findById(testConfigurationId)
                .orElseThrow(() -> new EntityNotFoundException("TestConfiguration not found. id=" + testConfigurationId));
        execution.updateEnvironment(config.getId(), config.getName(), buildEnvironmentDetail(config));
    }

    /** 컨피그의 서버환경·OS·브라우저·기기·런타임(Java)·DB 버전을 한 줄 요약으로 굳힌다. */
    private String buildEnvironmentDetail(TestConfiguration config) {
        List<String> parts = new java.util.ArrayList<>();
        if (config.getServerEnvironment() != null) parts.add(config.getServerEnvironment().getName());
        if (config.getOs() != null) parts.add(config.getOs() + (config.getOsVersion() != null ? " " + config.getOsVersion() : ""));
        if (config.getBrowser() != null) parts.add(config.getBrowser() + (config.getBrowserVersion() != null ? " " + config.getBrowserVersion() : ""));
        if (config.getDevice() != null) parts.add(config.getDevice().toString());
        if (config.getRuntimeVersion() != null) parts.add(config.getRuntimeVersion());
        if (config.getDbVersion() != null) parts.add(config.getDbVersion());
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /**
     * 기존 테스트런을 복제해 회귀 테스트 사이클을 만든다 — 같은 테스트케이스 목록을 가져오되,
     * 각 케이스는 "현재 최신 버전"으로 다시 스냅샷하고 결과는 모두 미실행으로 초기화한다(결과·코멘트·첨부는 복사하지 않음).
     * 회귀는 그동안 수정된 최신 케이스에 대해 다시 도는 것이므로 원본 런의 옛 버전 스냅샷은 따르지 않는다.
     */
    @Transactional
    public ExecutionResponse cloneExecution(Long id) {
        Execution source = findById(id);

        String name = source.getName() + " (복제)";
        if (name.length() > 200) {
            name = name.substring(0, 200);
        }

        Execution clone = new Execution(
                name,
                source.getDescription(),
                source.getProjectId(),
                source.getTestPlanId(),
                source.getPlanName(),
                source.getTestSuiteId(),
                source.getSuiteName(),
                source.getAssignee(),
                source.getVersion()
        );
        clone.updateEnvironment(source.getTestConfigurationId(), source.getConfigurationName(), source.getEnvironmentDetail());
        // 원본 런의 케이스를 현재 최신 버전으로 재스냅샷 — 그 사이 삭제된 케이스는 건너뛴다.
        List<Long> caseIds = source.getItems().stream().map(ExecutionItem::getTestCaseId).toList();
        Map<Long, TestCase> casesById = testCaseRepository.findAllById(caseIds).stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        for (ExecutionItem item : source.getItems()) {
            TestCase testCase = casesById.get(item.getTestCaseId());
            if (testCase != null) {
                addItemWithCurrentVersion(clone, testCase);
            }
        }
        return saveAndLog(clone);
    }

    /** 새 테스트런 저장 + 생성 로그. */
    private ExecutionResponse saveAndLog(Execution execution) {
        Execution saved = executionRepository.save(execution);
        auditLogService.log(AuditLogService.TEST_RUN, saved.getId(), AuditAction.CREATED,
                "테스트런 '" + saved.getName() + "'이(가) 생성되었습니다. (" + saved.getItems().size() + "건)");
        return ExecutionResponse.detail(saved);
    }

    @Transactional
    public ExecutionResponse updateExecution(Long id, UpdateExecutionRequest request) {
        Execution execution = findById(id);
        boolean wasCompleted = execution.getStatus().isCompleted();
        execution.update(
                request.name().trim(),
                normalizeOptional(request.description()),
                request.status(),
                normalizeOptional(request.assignee())
        );
        boolean nowCompleted = execution.getStatus().isCompleted();
        // 완료 ↔ 재오픈 전환을 별도 로그로 남긴다.
        if (!wasCompleted && nowCompleted) {
            auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.COMPLETED,
                    "테스트런 '" + execution.getName() + "'이(가) 완료 처리되었습니다.");
        } else if (wasCompleted && !nowCompleted) {
            auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.REOPENED,
                    "테스트런 '" + execution.getName() + "'이(가) 다시 열렸습니다.");
        }
        return ExecutionResponse.detail(execution);
    }

    @Transactional
    public ExecutionResponse updateExecutionPlan(Long id, UpdateExecutionPlanRequest request) {
        Execution execution = findById(id);
        if (request.testPlanId() == null) {
            execution.updatePlan(null, null);
        } else {
            TestPlan plan = testPlanRepository.findById(request.testPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + request.testPlanId()));
            execution.updatePlan(plan.getId(), plan.getName());
        }
        return ExecutionResponse.detail(execution);
    }

    @Transactional
    public ExecutionResponse updateExecutionEnvironment(Long id, UpdateExecutionEnvironmentRequest request) {
        Execution execution = findById(id);
        applyConfiguration(execution, request.testConfigurationId());
        return ExecutionResponse.detail(execution);
    }

    @Transactional
    public void deleteExecution(Long id) {
        Execution execution = findById(id);
        // 각 실행 아이템에 달린 첨부파일(실패 증거 등)을 먼저 정리한다 — 고아 파일 방지.
        for (ExecutionItem item : execution.getItems()) {
            attachmentService.deleteAllByEntity(AttachmentEntityType.EXECUTION_ITEM, item.getId());
        }
        executionRepository.delete(execution);
        auditLogService.log(AuditLogService.TEST_RUN, id, AuditAction.DELETED,
                "테스트런 '" + execution.getName() + "'이(가) 삭제되었습니다.");
    }

    @Transactional
    public ExecutionResponse recordResult(Long executionId, Long itemId, RecordResultRequest request) {
        Execution execution = findById(executionId);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다.");
        }
        ExecutionItem item = execution.getItems().stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("ExecutionItem not found. id=" + itemId));
        item.record(request.status(), normalizeOptional(request.comment()), normalizeOptional(request.failureReason()));
        auditLogService.log(AuditLogService.TEST_RUN, executionId, AuditAction.RESULT_RECORDED,
                "'" + item.getCaseTitle() + "' 결과: " + request.status()
                        + " (런 '" + execution.getName() + "')");
        return ExecutionResponse.detail(execution);
    }

    /** 선택한 여러 실행 아이템에 같은 결과를 한 번에 기록한다. */
    @Transactional
    public ExecutionResponse bulkRecordResult(Long executionId, BulkRecordResultRequest request) {
        Execution execution = findById(executionId);
        if (execution.getStatus().isCompleted()) {
            throw new InvalidRequestException("완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다.");
        }
        Set<Long> targetIds = new LinkedHashSet<>(request.itemIds());
        Map<Long, ExecutionItem> itemsById = execution.getItems().stream()
                .collect(Collectors.toMap(ExecutionItem::getId, Function.identity()));
        List<Long> missing = targetIds.stream().filter(id -> !itemsById.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("이 테스트런에 속하지 않은 항목이 포함되어 있습니다: " + missing);
        }

        String comment = normalizeOptional(request.comment());
        String failureReason = normalizeOptional(request.failureReason());
        for (Long id : targetIds) {
            itemsById.get(id).record(request.status(), comment, failureReason);
        }
        auditLogService.log(AuditLogService.TEST_RUN, executionId, AuditAction.RESULT_RECORDED,
                "일괄 결과 기록: " + targetIds.size() + "건 → " + request.status()
                        + " (런 '" + execution.getName() + "')");
        return ExecutionResponse.detail(execution);
    }

    private Execution findById(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + id));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
