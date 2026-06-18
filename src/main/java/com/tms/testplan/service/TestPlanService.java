package com.tms.testplan.service;

import com.tms.global.exception.InvalidRequestException;
import com.tms.testplan.dto.TestPlanRequest;
import com.tms.testplan.dto.TestPlanResponse;
import com.tms.testplan.entity.TestPlan;
import com.tms.testplan.repository.TestPlanRepository;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestPlanService {

    private final TestPlanRepository testPlanRepository;
    private final TestSuiteRepository testSuiteRepository;

    public TestPlanService(TestPlanRepository testPlanRepository, TestSuiteRepository testSuiteRepository) {
        this.testPlanRepository = testPlanRepository;
        this.testSuiteRepository = testSuiteRepository;
    }

    public List<TestPlanResponse> getPlans() {
        return testPlanRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    public TestPlanResponse getPlan(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TestPlanResponse createPlan(TestPlanRequest request) {
        validateDates(request.startDate(), request.endDate());
        TestPlan plan = new TestPlan(
                request.name(), request.description(), request.status(), request.assignee(), request.startDate(), request.endDate()
        );
        return toResponse(testPlanRepository.save(plan));
    }

    @Transactional
    public TestPlanResponse updatePlan(Long id, TestPlanRequest request) {
        validateDates(request.startDate(), request.endDate());
        TestPlan plan = findById(id);
        plan.update(request.name(), request.description(), request.status(), request.assignee(), request.startDate(), request.endDate());
        return toResponse(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        TestPlan plan = findById(id);
        testSuiteRepository.deleteByTestPlanId(id);
        testPlanRepository.delete(plan);
    }

    private TestPlanResponse toResponse(TestPlan plan) {
        return TestPlanResponse.from(
                plan,
                testSuiteRepository.countByTestPlanId(plan.getId()),
                testSuiteRepository.countDistinctTestCasesByTestPlanId(plan.getId())
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
}
