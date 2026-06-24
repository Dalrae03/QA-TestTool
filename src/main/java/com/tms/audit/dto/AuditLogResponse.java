package com.tms.audit.dto;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.entity.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        AuditAction action,
        String fieldName,
        String oldValue,
        String newValue,
        String summary,
        String actor,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getFieldName(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getSummary(),
                auditLog.getActor(),
                auditLog.getCreatedAt()
        );
    }
}
