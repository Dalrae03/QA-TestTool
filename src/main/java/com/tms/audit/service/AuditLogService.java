package com.tms.audit.service;

import com.tms.audit.dto.AuditLogResponse;
import com.tms.audit.entity.AuditAction;
import com.tms.audit.entity.AuditLog;
import com.tms.audit.repository.AuditLogRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    public static final String TEST_CASE = "TEST_CASE";

    private static final String SYSTEM_ACTOR = "system";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogResponse> getLogs(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(entityType, entityId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional
    public void record(String entityType, Long entityId, AuditAction action, String fieldName, Object oldValue, Object newValue) {
        auditLogRepository.save(new AuditLog(
                entityType,
                entityId,
                action,
                fieldName,
                stringify(oldValue),
                stringify(newValue),
                summarize(action, fieldName),
                SYSTEM_ACTOR
        ));
    }

    @Transactional
    public void recordIfChanged(String entityType, Long entityId, AuditAction action, String fieldName, Object oldValue, Object newValue) {
        if (!Objects.equals(stringify(oldValue), stringify(newValue))) {
            record(entityType, entityId, action, fieldName, oldValue, newValue);
        }
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private String summarize(AuditAction action, String fieldName) {
        return switch (action) {
            case CREATED -> "테스트케이스가 생성되었습니다.";
            case UPDATED -> fieldName + " 항목이 변경되었습니다.";
            case STATUS_CHANGED -> "상태가 변경되었습니다.";
            case MOVED -> "폴더가 변경되었습니다.";
            case DEFECT_LINKED -> "결함이 연결되었습니다.";
            case DEFECT_UNLINKED -> "결함 연결이 해제되었습니다.";
            case VERSION_CREATED -> "테스트케이스 버전이 생성되었습니다.";
            case VERSION_RESTORED -> "테스트케이스 버전이 복원되었습니다.";
            case DELETED -> "테스트케이스가 삭제되었습니다.";
        };
    }
}
