package com.tms.testcase.controller;

import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.service.TestCaseService;
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
@RequestMapping("/api/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public ResponseEntity<List<TestCaseResponse>> getAllTestCases() {
        return ResponseEntity.ok(testCaseService.getAllTestCases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCaseResponse> getTestCase(@PathVariable Long id) {
        return ResponseEntity.ok(testCaseService.getTestCase(id));
    }

    @PostMapping
    public ResponseEntity<TestCaseResponse> createTestCase(@Valid @RequestBody CreateTestCaseRequest request) {
        TestCaseResponse response = testCaseService.createTestCase(request);
        return ResponseEntity.created(URI.create("/api/testcases/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestCaseRequest request
    ) {
        return ResponseEntity.ok(testCaseService.updateTestCase(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.noContent().build();
    }
}
