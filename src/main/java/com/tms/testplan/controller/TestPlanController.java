package com.tms.testplan.controller;

import com.tms.testplan.dto.TestPlanRequest;
import com.tms.testplan.dto.TestPlanResponse;
import com.tms.testplan.service.TestPlanService;
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
@RequestMapping("/api/test-plans")
public class TestPlanController {

    private final TestPlanService testPlanService;

    public TestPlanController(TestPlanService testPlanService) {
        this.testPlanService = testPlanService;
    }

    @GetMapping
    public List<TestPlanResponse> getPlans(@RequestParam(required = false) Long projectId) {
        return projectId != null ? testPlanService.getPlansByProject(projectId) : testPlanService.getPlans();
    }

    @GetMapping("/{id}")
    public TestPlanResponse getPlan(@PathVariable Long id) { return testPlanService.getPlan(id); }

    @PostMapping
    public ResponseEntity<TestPlanResponse> createPlan(@Valid @RequestBody TestPlanRequest request) {
        TestPlanResponse response = testPlanService.createPlan(request);
        return ResponseEntity.created(URI.create("/api/test-plans/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public TestPlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody TestPlanRequest request) {
        return testPlanService.updatePlan(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        testPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
