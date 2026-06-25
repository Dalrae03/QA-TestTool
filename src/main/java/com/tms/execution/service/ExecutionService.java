package com.tms.execution.service;

import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.TestCaseExecutionHistoryResponse;
import com.tms.execution.dto.UpdateExecutionPlanRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.repository.ExecutionItemRepository;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.global.exception.InvalidRequestException;
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

    public ExecutionService(ExecutionRepository executionRepository, ExecutionItemRepository executionItemRepository,
                             TestSuiteRepository testSuiteRepository,
                             TestPlanRepository testPlanRepository, TestCaseRepository testCaseRepository,
                             TestCaseVersionRepository testCaseVersionRepository) {
        this.executionRepository = executionRepository;
        this.executionItemRepository = executionItemRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testPlanRepository = testPlanRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseVersionRepository = testCaseVersionRepository;
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
        return ExecutionResponse.detail(executionRepository.save(execution));
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
            throw new InvalidRequestException("존재하지 않는 테스트케이스 ID: " + missingIds);
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
        return ExecutionResponse.detail(executionRepository.save(execution));
    }

    @Transactional
    public ExecutionResponse updateExecution(Long id, UpdateExecutionRequest request) {
        Execution execution = findById(id);
        execution.update(
                request.name().trim(),
                normalizeOptional(request.description()),
                request.status(),
                normalizeOptional(request.assignee())
        );
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
        executionRepository.delete(findById(id));
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
