package com.tms.execution.service;

import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.TestCaseExecutionHistoryResponse;
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
    private final AttachmentService attachmentService;
    private final AuditLogService auditLogService;

    public ExecutionService(ExecutionRepository executionRepository, ExecutionItemRepository executionItemRepository,
                             TestSuiteRepository testSuiteRepository,
                             TestPlanRepository testPlanRepository, TestCaseRepository testCaseRepository,
                             TestCaseVersionRepository testCaseVersionRepository,
                             AttachmentService attachmentService,
                             AuditLogService auditLogService) {
        this.executionRepository = executionRepository;
        this.executionItemRepository = executionItemRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testPlanRepository = testPlanRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseVersionRepository = testCaseVersionRepository;
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

    public List<ExecutionResponse> getExecutions(Long projectId) {
        List<Execution> executions = projectId != null
                ? executionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                : executionRepository.findAllByOrderByCreatedAtDesc();
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
                normalizeOptional(request.assignee())
        );
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
                normalizeOptional(request.assignee())
        );
        uniqueIds.forEach(id -> addItemWithCurrentVersion(execution, casesById.get(id)));
        return saveAndLog(execution);
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

    private Execution findById(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + id));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
