package com.tms.testcase.controller;

import com.tms.testcase.dto.TestFolderRequest;
import com.tms.testcase.dto.TestFolderResponse;
import com.tms.testcase.service.TestFolderService;
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

@RestController
@RequestMapping("/api/folders")
public class TestFolderController {

    private final TestFolderService testFolderService;

    public TestFolderController(TestFolderService testFolderService) {
        this.testFolderService = testFolderService;
    }

    @GetMapping
    public ResponseEntity<List<TestFolderResponse>> getRootFolders(@RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(testFolderService.getRootFolders(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestFolderResponse> getFolder(@PathVariable Long id) {
        return ResponseEntity.ok(testFolderService.getFolder(id));
    }

    @PostMapping
    public ResponseEntity<TestFolderResponse> createFolder(@Valid @RequestBody TestFolderRequest request) {
        TestFolderResponse response = testFolderService.createFolder(request);
        return ResponseEntity.created(URI.create("/api/folders/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestFolderResponse> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody TestFolderRequest request
    ) {
        return ResponseEntity.ok(testFolderService.updateFolder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        testFolderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
