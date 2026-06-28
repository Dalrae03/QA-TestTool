package com.tms.audit.service;

import com.tms.audit.dto.AuditLogResponse;
import com.tms.audit.entity.AuditAction;
import com.tms.audit.entity.AuditLog;
import com.tms.audit.repository.AuditLogRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    // 엔티티 타입 상수
    public static final String TEST_CASE = "TEST_CASE";
    public static final String TEST_RUN = "TEST_RUN";
    public static final String DEFECT = "DEFECT";
    public static final String ATTACHMENT = "ATTACHMENT";
    public static final String CONFIGURATION = "CONFIGURATION";
    public static final String SERVER_ENVIRONMENT = "SERVER_ENVIRONMENT";
    public static final String USER = "USER";
    public static final String AREA_TAG = "AREA_TAG";
    public static final String SETTING = "SETTING";
    public static final String BACKUP = "BACKUP";
    public static final String IMPORT = "IMPORT";

    /** 엔티티 타입 → 사람이 읽는 라벨. 요약 문구 생성에 사용한다. */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(TEST_CASE, "테스트케이스"),
            Map.entry(TEST_RUN, "테스트런"),
            Map.entry(DEFECT, "결함"),
            Map.entry(ATTACHMENT, "첨부파일"),
            Map.entry(CONFIGURATION, "Configuration"),
            Map.entry(SERVER_ENVIRONMENT, "서버 환경"),
            Map.entry(USER, "사용자"),
            Map.entry(AREA_TAG, "영역 태그"),
            Map.entry(SETTING, "설정"),
            Map.entry(BACKUP, "백업"),
            Map.entry(IMPORT, "가져오기")
    );

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
                summarize(entityType, action, fieldName),
                SYSTEM_ACTOR
        ));
    }

    @Transactional
    public void recordIfChanged(String entityType, Long entityId, AuditAction action, String fieldName, Object oldValue, Object newValue) {
        if (!Objects.equals(stringify(oldValue), stringify(newValue))) {
            record(entityType, entityId, action, fieldName, oldValue, newValue);
        }
    }

    /** 직접 작성한 요약 문구로 로그를 남긴다(이름·파일명 등 구체 정보를 포함할 때 사용). */
    @Transactional
    public void log(String entityType, Long entityId, AuditAction action, String summary) {
        auditLogRepository.save(new AuditLog(
                entityType,
                entityId != null ? entityId : 0L,
                action,
                action.name(),
                null,
                null,
                summary,
                SYSTEM_ACTOR
        ));
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private String summarize(String entityType, AuditAction action, String fieldName) {
        String label = LABELS.getOrDefault(entityType, "항목");
        return switch (action) {
            case CREATED -> label + "이(가) 생성되었습니다.";
            case UPDATED -> fieldName + " 항목이 변경되었습니다.";
            case STATUS_CHANGED -> label + " 상태가 변경되었습니다.";
            case MOVED -> "폴더가 변경되었습니다.";
            case DEFECT_LINKED -> "결함이 연결되었습니다.";
            case DEFECT_UNLINKED -> "결함 연결이 해제되었습니다.";
            case VERSION_CREATED -> label + " 버전이 생성되었습니다.";
            case VERSION_RESTORED -> label + " 버전이 복원되었습니다.";
            case DELETED -> label + "이(가) 삭제되었습니다.";
            case RESULT_RECORDED -> "실행 결과가 기록되었습니다.";
            case COMPLETED -> label + "이(가) 완료 처리되었습니다.";
            case REOPENED -> label + "이(가) 다시 열렸습니다.";
            case ATTACHMENT_ADDED -> "첨부파일이 추가되었습니다.";
            case ATTACHMENT_DELETED -> "첨부파일이 삭제되었습니다.";
            case SETTING_CHANGED -> "설정이 변경되었습니다.";
            case IMPORTED -> "데이터를 가져왔습니다.";
            case BACKUP_CREATED -> "백업이 생성되었습니다.";
            case BACKUP_RESTORED -> "백업에서 복구되었습니다.";
        };
    }
}
