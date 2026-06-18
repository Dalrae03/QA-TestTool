package com.tms.defect.controller;

import com.tms.defect.dto.DefectRequest;
import com.tms.defect.dto.DefectResponse;
import com.tms.defect.service.DefectService;
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
@RequestMapping("/api/defects")
public class DefectController {

    private final DefectService defectService;

    public DefectController(DefectService defectService) {
        this.defectService = defectService;
    }

    @GetMapping
    public List<DefectResponse> getAll() {
        return defectService.getAll();
    }

    @GetMapping("/{id}")
    public DefectResponse get(@PathVariable Long id) {
        return defectService.get(id);
    }

    @PostMapping
    public ResponseEntity<DefectResponse> create(@Valid @RequestBody DefectRequest request) {
        DefectResponse response = defectService.create(request);
        return ResponseEntity.created(URI.create("/api/defects/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public DefectResponse update(@PathVariable Long id, @Valid @RequestBody DefectRequest request) {
        return defectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        defectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
