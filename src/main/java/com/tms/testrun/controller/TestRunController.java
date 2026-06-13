package com.tms.testrun.controller;

import com.tms.testrun.dto.CreateTestRunRequest;
import com.tms.testrun.dto.TestRunResponse;
import com.tms.testrun.service.TestRunService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/testcases/{testCaseId}/runs")
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @GetMapping
    public ResponseEntity<List<TestRunResponse>> getTestRuns(@PathVariable Long testCaseId) {
        return ResponseEntity.ok(testRunService.getTestRuns(testCaseId));
    }

    @PostMapping
    public ResponseEntity<TestRunResponse> createTestRun(
            @PathVariable Long testCaseId,
            @Valid @RequestBody CreateTestRunRequest request
    ) {
        TestRunResponse response = testRunService.createTestRun(testCaseId, request);
        return ResponseEntity.created(URI.create("/api/testcases/" + testCaseId + "/runs/" + response.id()))
                .body(response);
    }

    @PutMapping("/{runId}")
    public ResponseEntity<TestRunResponse> updateTestRun(
            @PathVariable Long testCaseId,
            @PathVariable Long runId,
            @Valid @RequestBody CreateTestRunRequest request
    ) {
        return ResponseEntity.ok(testRunService.updateTestRun(testCaseId, runId, request));
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<Void> deleteTestRun(@PathVariable Long testCaseId, @PathVariable Long runId) {
        testRunService.deleteTestRun(testCaseId, runId);
        return ResponseEntity.noContent().build();
    }
}
