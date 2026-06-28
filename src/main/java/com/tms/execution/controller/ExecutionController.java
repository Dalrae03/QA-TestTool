package com.tms.execution.controller;

import com.tms.execution.dto.BulkRecordResultRequest;
import com.tms.execution.dto.CreateExecutionRequest;
import com.tms.execution.dto.ExecutionResponse;
import com.tms.execution.dto.RecordResultRequest;
import com.tms.execution.dto.TestCaseExecutionHistoryResponse;
import com.tms.execution.dto.UpdateExecutionEnvironmentRequest;
import com.tms.execution.dto.UpdateExecutionPlanRequest;
import com.tms.execution.dto.UpdateExecutionRequest;
import com.tms.execution.service.ExecutionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-runs")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public List<ExecutionResponse> getExecutions(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String assignee) {
        return executionService.getExecutions(projectId, assignee);
    }

    @GetMapping("/{id}")
    public ExecutionResponse getExecution(@PathVariable Long id) {
        return executionService.getExecution(id);
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> createExecution(@Valid @RequestBody CreateExecutionRequest request) {
        ExecutionResponse response = executionService.createExecution(request);
        return ResponseEntity.created(URI.create("/api/test-runs/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ExecutionResponse updateExecution(@PathVariable Long id, @Valid @RequestBody UpdateExecutionRequest request) {
        return executionService.updateExecution(id, request);
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ExecutionResponse> cloneExecution(@PathVariable Long id) {
        ExecutionResponse response = executionService.cloneExecution(id);
        return ResponseEntity.created(URI.create("/api/test-runs/" + response.id())).body(response);
    }

    @GetMapping("/items/by-test-case/{testCaseId}")
    public List<TestCaseExecutionHistoryResponse> getExecutionHistory(@PathVariable Long testCaseId) {
        return executionService.getExecutionHistory(testCaseId);
    }

    @PatchMapping("/{id}/plan")
    public ExecutionResponse updateExecutionPlan(@PathVariable Long id, @RequestBody UpdateExecutionPlanRequest request) {
        return executionService.updateExecutionPlan(id, request);
    }

    @PatchMapping("/{id}/environment")
    public ExecutionResponse updateExecutionEnvironment(@PathVariable Long id, @RequestBody UpdateExecutionEnvironmentRequest request) {
        return executionService.updateExecutionEnvironment(id, request);
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ExecutionResponse recordResult(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody RecordResultRequest request
    ) {
        return executionService.recordResult(id, itemId, request);
    }

    @PatchMapping("/{id}/bulk-results")
    public ExecutionResponse bulkRecordResult(
            @PathVariable Long id,
            @Valid @RequestBody BulkRecordResultRequest request
    ) {
        return executionService.bulkRecordResult(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExecution(@PathVariable Long id) {
        executionService.deleteExecution(id);
        return ResponseEntity.noContent().build();
    }
}
