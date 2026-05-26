package com.tms.testrun.service;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testrun.dto.CreateTestRunRequest;
import com.tms.testrun.dto.TestRunResponse;
import com.tms.testrun.entity.TestRun;
import com.tms.testrun.repository.TestRunRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;

    public TestRunService(TestRunRepository testRunRepository, TestCaseRepository testCaseRepository) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<TestRunResponse> getTestRuns(Long testCaseId) {
        ensureTestCaseExists(testCaseId);
        return testRunRepository.findByTestCaseIdOrderByExecutedAtDesc(testCaseId)
                .stream()
                .map(TestRunResponse::from)
                .toList();
    }

    @Transactional
    public TestRunResponse createTestRun(Long testCaseId, CreateTestRunRequest request) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found. id=" + testCaseId));

        TestRun testRun = new TestRun(
                testCase,
                request.status(),
                request.actualResult(),
                request.notes(),
                LocalDateTime.now()
        );

        return TestRunResponse.from(testRunRepository.save(testRun));
    }

    @Transactional
    public void deleteTestRun(Long testCaseId, Long runId) {
        ensureTestCaseExists(testCaseId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + runId));

        if (!testRun.getTestCase().getId().equals(testCaseId)) {
            throw new EntityNotFoundException("TestRun not found. id=" + runId);
        }

        testRunRepository.delete(testRun);
    }

    private void ensureTestCaseExists(Long testCaseId) {
        if (!testCaseRepository.existsById(testCaseId)) {
            throw new EntityNotFoundException("TestCase not found. id=" + testCaseId);
        }
    }
}
