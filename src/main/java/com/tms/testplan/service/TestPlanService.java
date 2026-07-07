package com.tms.testplan.service;

import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.global.util.ProjectScope;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testplan.dto.TestPlanRequest;
import com.tms.testplan.dto.TestPlanResponse;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
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
public class TestPlanService {

    private final TestPlanRepository testPlanRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final ExecutionRepository executionRepository;
    private final TestCaseRepository testCaseRepository;

    public TestPlanService(TestPlanRepository testPlanRepository, TestSuiteRepository testSuiteRepository,
                           ExecutionRepository executionRepository, TestCaseRepository testCaseRepository) {
        this.testPlanRepository = testPlanRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.executionRepository = executionRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<TestPlanResponse> getPlans() {
        return testPlanRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<TestPlanResponse> getPlansByProject(Long projectId) {
        return testPlanRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(this::toResponse).toList();
    }

    public TestPlanResponse getPlan(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TestPlanResponse createPlan(TestPlanRequest request) {
        validateDates(request.startDate(), request.endDate());
        TestPlan plan = new TestPlan(
                request.name(), request.status(), request.assignee(), request.startDate(), request.endDate()
        );
        applyFields(plan, request, request.projectId());
        plan.setProjectId(request.projectId());
        return toResponse(testPlanRepository.save(plan));
    }

    @Transactional
    public TestPlanResponse updatePlan(Long id, TestPlanRequest request) {
        validateDates(request.startDate(), request.endDate());
        TestPlan plan = findById(id);
        Long effectiveProjectId = request.projectId() != null ? request.projectId() : plan.getProjectId();
        applyFields(plan, request, effectiveProjectId);
        if (request.projectId() != null) plan.setProjectId(request.projectId());
        return toResponse(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        TestPlan plan = findById(id);
        testSuiteRepository.detachFromPlan(id);
        testPlanRepository.delete(plan);
    }

    private void applyFields(TestPlan plan, TestPlanRequest request, Long effectiveProjectId) {
        plan.update(
                request.name(), request.status(), request.assignee(), request.startDate(), request.endDate(),
                request.targetSystem(), request.targetVersion(),
                request.testGoal(), request.testTarget(),
                request.impactScope(), request.commonScope(),
                request.priorityTargets(), request.riskAnalysis(),
                request.testApproach(), request.testPerspective(),
                request.entryCriteria(), request.exitCriteria(),
                request.serverEnvironmentNote(), request.deviceMatrix(), request.testData(),
                request.schedule(), request.deliverables()
        );
        plan.replaceCoreTestCases(loadTestCases(request.coreTestCaseIds(), effectiveProjectId));
    }

    private TestPlanResponse toResponse(TestPlan plan) {
        return TestPlanResponse.from(
                plan,
                testSuiteRepository.countByTestPlanId(plan.getId()),
                testSuiteRepository.countDistinctTestCasesByTestPlanId(plan.getId()),
                executionRepository.countByTestPlanIdAndStatus(plan.getId(), ExecutionStatus.COMPLETED)
        );
    }

    private TestPlan findById(Long id) {
        return testPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + id));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidRequestException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    /** 3.1 핵심 테스트 대상 — TestSuiteService.loadTestCases와 동일한 검증 로직. */
    private List<TestCase> loadTestCases(List<Long> ids, Long projectId) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> uniqueIds = new LinkedHashSet<>(ids).stream().toList();
        Map<Long, TestCase> casesById = testCaseRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));
        List<Long> missingIds = uniqueIds.stream().filter(id -> !casesById.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new InvalidRequestException("존재하지 않는 테스트케이스 ID가 포함되어 있습니다: " + missingIds);
        }
        List<TestCase> cases = uniqueIds.stream().map(casesById::get).toList();
        List<Long> mismatched = cases.stream()
                .filter(tc -> !ProjectScope.compatible(projectId, tc.getProjectId()))
                .map(TestCase::getId).toList();
        if (!mismatched.isEmpty()) {
            throw new InvalidRequestException("다른 프로젝트의 테스트케이스는 플랜에 포함할 수 없습니다: " + mismatched);
        }
        return cases;
    }
}
