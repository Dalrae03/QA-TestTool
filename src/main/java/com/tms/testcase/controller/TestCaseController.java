package com.tms.testcase.controller;

import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.MoveToFolderRequest;
import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseStatusRequest;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.service.TestCaseService;
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
@RequestMapping("/api/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public ResponseEntity<List<TestCaseResponse>> getAllTestCases(
            @RequestParam(required = false) TestCaseType type,
            @RequestParam(required = false) TestCasePriority priority,
            @RequestParam(required = false) TestCaseStatus status,
            @RequestParam(required = false) TestCaseOs os,
            @RequestParam(required = false) TestCaseBrowser browser,
            @RequestParam(required = false) TestCaseDevice device,
            @RequestParam(required = false) Long areaTagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long folderId
    ) {
        return ResponseEntity.ok(
                testCaseService.getAllTestCases(type, priority, status, os, browser, device, areaTagId, keyword, folderId)
        );
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<TestCaseResponse> updateTestCaseStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestCaseStatusRequest request
    ) {
        return ResponseEntity.ok(testCaseService.updateTestCaseStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/folder")
    public ResponseEntity<TestCaseResponse> moveToFolder(
            @PathVariable Long id,
            @RequestBody MoveToFolderRequest request
    ) {
        return ResponseEntity.ok(testCaseService.moveToFolder(id, request.folderId()));
    }

    @PostMapping("/{id}/defects/{defectId}")
    public ResponseEntity<TestCaseResponse> linkDefect(
            @PathVariable Long id,
            @PathVariable Long defectId
    ) {
        return ResponseEntity.ok(testCaseService.linkDefect(id, defectId));
    }

    @DeleteMapping("/{id}/defects/{defectId}")
    public ResponseEntity<TestCaseResponse> unlinkDefect(
            @PathVariable Long id,
            @PathVariable Long defectId
    ) {
        return ResponseEntity.ok(testCaseService.unlinkDefect(id, defectId));
    }
}
