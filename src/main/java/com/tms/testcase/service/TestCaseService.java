package com.tms.testcase.service;

import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    public TestCaseService(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    public List<TestCaseResponse> getAllTestCases() {
        return testCaseRepository.findAll()
                .stream()
                .map(TestCaseResponse::from)
                .toList();
    }

    public TestCaseResponse getTestCase(Long id) {
        return TestCaseResponse.from(findById(id));
    }

    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest request) {
        TestCase testCase = new TestCase(
                request.type(),
                request.priority(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.expected(),
                request.notes()
        );

        return TestCaseResponse.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest request) {
        TestCase testCase = findById(id);
        testCase.update(
                request.type(),
                request.priority(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.expected(),
                request.notes()
        );
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public void deleteTestCase(Long id) {
        TestCase testCase = findById(id);
        testCaseRepository.delete(testCase);
    }

    private TestCase findById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found. id=" + id));
    }
}
