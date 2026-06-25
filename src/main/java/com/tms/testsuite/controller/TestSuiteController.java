package com.tms.testsuite.controller;

import com.tms.testsuite.dto.TestSuiteRequest;
import com.tms.testsuite.dto.TestSuiteResponse;
import com.tms.testsuite.service.TestSuiteService;
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

/** 플랜 소속 스위트 — 기존 API 유지 */
@RestController
@RequestMapping("/api/test-plans/{planId}/suites")
public class TestSuiteController {

    private final TestSuiteService testSuiteService;

    public TestSuiteController(TestSuiteService testSuiteService) {
        this.testSuiteService = testSuiteService;
    }

    @GetMapping
    public List<TestSuiteResponse> getSuites(@PathVariable Long planId) {
        return testSuiteService.getSuites(planId);
    }

    @GetMapping("/{suiteId}")
    public TestSuiteResponse getSuite(@PathVariable Long planId, @PathVariable Long suiteId) {
        return testSuiteService.getSuite(planId, suiteId);
    }

    @PostMapping
    public ResponseEntity<TestSuiteResponse> createSuite(@PathVariable Long planId,
                                                          @Valid @RequestBody TestSuiteRequest request) {
        TestSuiteResponse response = testSuiteService.createSuite(planId, request);
        return ResponseEntity.created(URI.create("/api/test-plans/" + planId + "/suites/" + response.id()))
                .body(response);
    }

    @PutMapping("/{suiteId}")
    public TestSuiteResponse updateSuite(@PathVariable Long planId, @PathVariable Long suiteId,
                                          @Valid @RequestBody TestSuiteRequest request) {
        return testSuiteService.updateSuite(planId, suiteId, request);
    }

    @DeleteMapping("/{suiteId}")
    public ResponseEntity<Void> deleteSuite(@PathVariable Long planId, @PathVariable Long suiteId) {
        testSuiteService.deleteSuite(planId, suiteId);
        return ResponseEntity.noContent().build();
    }
}
