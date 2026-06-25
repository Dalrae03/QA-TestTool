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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 독립 스위트 — 테스트런에서 직접 생성/관리 (#6) */
@RestController
@RequestMapping("/api/suites")
public class StandaloneSuiteController {

    private final TestSuiteService testSuiteService;

    public StandaloneSuiteController(TestSuiteService testSuiteService) {
        this.testSuiteService = testSuiteService;
    }

    @GetMapping
    public List<TestSuiteResponse> getAllSuites(@RequestParam(required = false) Long projectId) {
        return testSuiteService.getAllSuites(projectId);
    }

    @GetMapping("/standalone")
    public List<TestSuiteResponse> getStandaloneSuites(@RequestParam(required = false) Long projectId) {
        return testSuiteService.getStandaloneSuites(projectId);
    }

    @GetMapping("/{suiteId}")
    public TestSuiteResponse getSuite(@PathVariable Long suiteId) {
        return testSuiteService.getAnySuite(suiteId);
    }

    @PostMapping
    public ResponseEntity<TestSuiteResponse> createSuite(@Valid @RequestBody TestSuiteRequest request) {
        TestSuiteResponse response = testSuiteService.createStandaloneSuite(request);
        return ResponseEntity.created(URI.create("/api/suites/" + response.id())).body(response);
    }

    @PutMapping("/{suiteId}")
    public TestSuiteResponse updateSuite(@PathVariable Long suiteId, @Valid @RequestBody TestSuiteRequest request) {
        return testSuiteService.updateAnySuite(suiteId, request);
    }

    @DeleteMapping("/{suiteId}")
    public ResponseEntity<Void> deleteSuite(@PathVariable Long suiteId) {
        testSuiteService.deleteAnySuite(suiteId);
        return ResponseEntity.noContent().build();
    }
}
