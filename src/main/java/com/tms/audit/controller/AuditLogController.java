package com.tms.audit.controller;

import com.tms.audit.dto.AuditLogResponse;
import com.tms.audit.service.AuditLogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/testcases/{testCaseId}/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getTestCaseAuditLogs(@PathVariable Long testCaseId) {
        return ResponseEntity.ok(auditLogService.getLogs(AuditLogService.TEST_CASE, testCaseId));
    }
}
