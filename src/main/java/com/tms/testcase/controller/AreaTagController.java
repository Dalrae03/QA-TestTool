package com.tms.testcase.controller;

import com.tms.testcase.dto.AreaTagResponse;
import com.tms.testcase.dto.CreateAreaTagRequest;
import com.tms.testcase.service.AreaTagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/area-tags")
public class AreaTagController {

    private final AreaTagService areaTagService;

    public AreaTagController(AreaTagService areaTagService) {
        this.areaTagService = areaTagService;
    }

    @GetMapping
    public ResponseEntity<List<AreaTagResponse>> getAllAreaTags() {
        return ResponseEntity.ok(areaTagService.getAllAreaTags());
    }

    @PostMapping
    public ResponseEntity<AreaTagResponse> createAreaTag(@Valid @RequestBody CreateAreaTagRequest request) {
        AreaTagResponse response = areaTagService.createAreaTag(request);
        return ResponseEntity.created(URI.create("/api/area-tags/" + response.id())).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAreaTag(@PathVariable Long id) {
        areaTagService.deleteAreaTag(id);
        return ResponseEntity.noContent().build();
    }
}
