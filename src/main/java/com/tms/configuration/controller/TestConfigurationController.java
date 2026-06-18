package com.tms.configuration.controller;

import com.tms.configuration.dto.TestConfigurationRequest;
import com.tms.configuration.dto.TestConfigurationResponse;
import com.tms.configuration.service.TestConfigurationService;
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
@RequestMapping("/api/test-configurations")
public class TestConfigurationController {

    private final TestConfigurationService configurationService;

    public TestConfigurationController(TestConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public List<TestConfigurationResponse> getAll() { return configurationService.getAll(); }

    @GetMapping("/{id}")
    public TestConfigurationResponse get(@PathVariable Long id) { return configurationService.get(id); }

    @PostMapping
    public ResponseEntity<TestConfigurationResponse> create(@Valid @RequestBody TestConfigurationRequest request) {
        TestConfigurationResponse response = configurationService.create(request);
        return ResponseEntity.created(URI.create("/api/test-configurations/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public TestConfigurationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TestConfigurationRequest request
    ) {
        return configurationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configurationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
