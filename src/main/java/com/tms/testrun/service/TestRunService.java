package com.tms.testrun.service;

import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
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
    private final AttachmentService attachmentService;

    public TestRunService(TestRunRepository testRunRepository, TestCaseRepository testCaseRepository,
                          AttachmentService attachmentService) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.attachmentService = attachmentService;
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
                request.assignee(),
                LocalDateTime.now()
        );

        return TestRunResponse.from(testRunRepository.save(testRun));
    }

    @Transactional
    public TestRunResponse updateTestRun(Long testCaseId, Long runId, CreateTestRunRequest request) {
        TestRun testRun = findTestRunInTestCase(testCaseId, runId);

        testRun.setStatus(request.status());
        testRun.setActualResult(request.actualResult());
        testRun.setNotes(request.notes());
        testRun.setAssignee(request.assignee());

        return TestRunResponse.from(testRun);
    }

    @Transactional
    public void deleteTestRun(Long testCaseId, Long runId) {
        TestRun testRun = findTestRunInTestCase(testCaseId, runId);
        attachmentService.deleteAllByEntity(AttachmentEntityType.TEST_RUN, runId);
        testRunRepository.delete(testRun);
    }

    private TestRun findTestRunInTestCase(Long testCaseId, Long runId) {
        ensureTestCaseExists(testCaseId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + runId));

        if (!testRun.getTestCase().getId().equals(testCaseId)) {
            throw new EntityNotFoundException("TestRun not found. id=" + runId);
        }

        return testRun;
    }

    private void ensureTestCaseExists(Long testCaseId) {
        if (!testCaseRepository.existsById(testCaseId)) {
            throw new EntityNotFoundException("TestCase not found. id=" + testCaseId);
        }
    }
}
