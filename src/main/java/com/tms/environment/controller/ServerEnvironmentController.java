package com.tms.environment.controller;

import com.tms.environment.dto.ServerEnvironmentRequest;
import com.tms.environment.dto.ServerEnvironmentResponse;
import com.tms.environment.service.ServerEnvironmentService;
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
@RequestMapping("/api/server-environments")
public class ServerEnvironmentController {

    private final ServerEnvironmentService serverEnvironmentService;

    public ServerEnvironmentController(ServerEnvironmentService serverEnvironmentService) {
        this.serverEnvironmentService = serverEnvironmentService;
    }

    @GetMapping
    public List<ServerEnvironmentResponse> getAll() { return serverEnvironmentService.getAll(); }

    @GetMapping("/{id}")
    public ServerEnvironmentResponse get(@PathVariable Long id) { return serverEnvironmentService.get(id); }

    @PostMapping
    public ResponseEntity<ServerEnvironmentResponse> create(@Valid @RequestBody ServerEnvironmentRequest request) {
        ServerEnvironmentResponse response = serverEnvironmentService.create(request);
        return ResponseEntity.created(URI.create("/api/server-environments/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ServerEnvironmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ServerEnvironmentRequest request
    ) {
        return serverEnvironmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serverEnvironmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
