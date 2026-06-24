package com.tms.execution.service;

import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.entity.Execution;
import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.repository.ExecutionRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.testsuite.entity.TestSuite;
import com.tms.testsuite.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final TestSuiteRepository testSuiteRepository;

    public ExecutionService(ExecutionRepository executionRepository, TestSuiteRepository testSuiteRepository) {
        this.executionRepository = executionRepository;
        this.testSuiteRepository = testSuiteRepository;
    }

    public List<ExecutionResponse> getExecutions() {
        return executionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ExecutionResponse::summary).toList();
    }

    public ExecutionResponse getExecution(Long id) {
        return ExecutionResponse.detail(findById(id));
    }

    @Transactional
    public ExecutionResponse createExecution(CreateExecutionRequest request) {
        TestSuite suite = testSuiteRepository.findById(request.suiteId())
                .orElseThrow(() -> new EntityNotFoundException("TestSuite not found. id=" + request.suiteId()));
        if (suite.getTestCases().isEmpty()) {
            throw new InvalidRequestException("테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다.");
        }

        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : suite.getName() + " — " + LocalDate.now();

        Execution execution = new Execution(
                name,
                normalizeOptional(request.description()),
                suite.getTestPlan().getId(),
                suite.getId(),
                suite.getName(),
                normalizeOptional(request.assignee())
        );
        suite.getTestCases().forEach(testCase -> execution.addItem(testCase.getId(), testCase.getTitle()));
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
        item.record(request.status(), normalizeOptional(request.comment()));
        return ExecutionResponse.detail(execution);
    }

    private Execution findById(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestRun not found. id=" + id));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
