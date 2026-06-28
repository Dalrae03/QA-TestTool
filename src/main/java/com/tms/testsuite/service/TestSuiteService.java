package com.tms.testsuite.service;

import com.tms.global.exception.InvalidRequestException;
import com.tms.global.util.ProjectScope;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.dto.TestSuiteRequest;
import com.tms.testsuite.dto.TestSuiteResponse;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestSuiteService {

    private final TestSuiteRepository testSuiteRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestCaseRepository testCaseRepository;

    public TestSuiteService(TestSuiteRepository testSuiteRepository,
                            TestPlanRepository testPlanRepository,
                            TestCaseRepository testCaseRepository) {
        this.testSuiteRepository = testSuiteRepository;
        this.testPlanRepository = testPlanRepository;
        this.testCaseRepository = testCaseRepository;
    }

    // ── 플랜 소속 스위트 ─────────────────────────────────────────────

    public List<TestSuiteResponse> getSuites(Long planId) {
        ensurePlanExists(planId);
        return testSuiteRepository.findByTestPlanIdOrderByCreatedAtAsc(planId)
                .stream().map(TestSuiteResponse::from).toList();
    }

    public TestSuiteResponse getSuite(Long planId, Long suiteId) {
        return TestSuiteResponse.from(findInPlan(planId, suiteId));
    }

    @Transactional
    public TestSuiteResponse createSuite(Long planId, TestSuiteRequest request) {
        TestPlan plan = testPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("TestPlan not found. id=" + planId));
        TestSuite suite = new TestSuite(plan, request.name(), request.description(),
                loadTestCases(request.testCaseIds(), plan.getProjectId()));
        suite.setProjectId(plan.getProjectId());
        return TestSuiteResponse.from(testSuiteRepository.save(suite));
    }

    @Transactional
    public TestSuiteResponse updateSuite(Long planId, Long suiteId, TestSuiteRequest request) {
        TestSuite suite = findInPlan(planId, suiteId);
        suite.update(request.name(), request.description(),
                loadTestCases(request.testCaseIds(), suite.getProjectId()));
        return TestSuiteResponse.from(suite);
    }

    @Transactional
    public void deleteSuite(Long planId, Long suiteId) {
        testSuiteRepository.delete(findInPlan(planId, suiteId));
    }

    // ── 독립 스위트 (플랜 없음, 테스트런에서 직접 생성) ─────────────

    public List<TestSuiteResponse> getStandaloneSuites(Long projectId) {
        List<TestSuite> suites = projectId != null
                ? testSuiteRepository.findByProjectIdAndTestPlanIdIsNullOrderByCreatedAtAsc(projectId)
                : testSuiteRepository.findByTestPlanIdIsNullOrderByCreatedAtAsc();
        return suites.stream().map(TestSuiteResponse::from).toList();
    }

    public List<TestSuiteResponse> getAllSuites(Long projectId) {
        List<TestSuite> suites = projectId != null
                ? testSuiteRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)
                : testSuiteRepository.findAllByOrderByCreatedAtAsc();
        return suites.stream().map(TestSuiteResponse::from).toList();
    }

    public TestSuiteResponse getAnySuite(Long suiteId) {
        TestSuite suite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + suiteId));
        suite.getTestCases().size();
        return TestSuiteResponse.from(suite);
    }

    @Transactional
    public TestSuiteResponse createStandaloneSuite(TestSuiteRequest request) {
        TestSuite suite = new TestSuite(request.name(), request.description(),
                loadTestCases(request.testCaseIds(), request.projectId()));
        suite.setProjectId(request.projectId());
        return TestSuiteResponse.from(testSuiteRepository.save(suite));
    }

    @Transactional
    public TestSuiteResponse updateAnySuite(Long suiteId, TestSuiteRequest request) {
        TestSuite suite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + suiteId));
        Long effectiveProjectId = request.projectId() != null ? request.projectId() : suite.getProjectId();
        suite.update(request.name(), request.description(),
                loadTestCases(request.testCaseIds(), effectiveProjectId));
        if (request.projectId() != null) {
            suite.setProjectId(request.projectId());
        }
        return TestSuiteResponse.from(suite);
    }

    @Transactional
    public void deleteAnySuite(Long suiteId) {
        testSuiteRepository.delete(testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + suiteId)));
    }

    // ── 공통 ─────────────────────────────────────────────────────────

    @Transactional
    public void removeTestCaseFromAllSuites(Long testCaseId) {
        testSuiteRepository.findAllByTestCases_Id(testCaseId)
                .forEach(suite -> suite.removeTestCase(testCaseId));
    }

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
            throw new InvalidRequestException("다른 프로젝트의 테스트케이스는 스위트에 포함할 수 없습니다: " + mismatched);
        }
        return cases;
    }

    private TestSuite findInPlan(Long planId, Long suiteId) {
        ensurePlanExists(planId);
        TestSuite suite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + suiteId));
        if (suite.getTestPlan() == null || !suite.getTestPlan().getId().equals(planId)) {
            throw new EntityNotFoundException("TestSuite not found in plan. id=" + suiteId);
        }
        suite.getTestCases().size();
        return suite;
    }

    private void ensurePlanExists(Long planId) {
        if (!testPlanRepository.existsById(planId)) {
            throw new EntityNotFoundException("TestPlan not found. id=" + planId);
        }
    }
}
