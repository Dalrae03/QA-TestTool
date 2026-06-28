package com.tms.defect.service;

import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.defect.dto.DefectRequest;
import com.tms.defect.dto.DefectResponse;
import com.tms.defect.entity.Defect;
import com.tms.defect.repository.DefectRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefectService {

    private final DefectRepository defectRepository;
    private final AttachmentService attachmentService;
    private final AuditLogService auditLogService;

    public DefectService(DefectRepository defectRepository, @Lazy AttachmentService attachmentService,
                         AuditLogService auditLogService) {
        this.defectRepository = defectRepository;
        this.attachmentService = attachmentService;
        this.auditLogService = auditLogService;
    }

    public List<DefectResponse> getAll() {
        return defectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DefectResponse::from).toList();
    }

    public DefectResponse get(Long id) {
        return DefectResponse.from(findById(id));
    }

    @Transactional
    public DefectResponse create(DefectRequest request) {
        Defect defect = new Defect(
                request.title(), request.description(),
                request.severity(), request.status(), request.externalUrl()
        );
        Defect saved = defectRepository.save(defect);
        auditLogService.log(AuditLogService.DEFECT, saved.getId(), AuditAction.CREATED,
                "결함 '" + saved.getTitle() + "'이(가) 생성되었습니다. (심각도 " + saved.getSeverity() + ")");
        return DefectResponse.from(saved);
    }

    @Transactional
    public DefectResponse update(Long id, DefectRequest request) {
        Defect defect = findById(id);
        var beforeStatus = defect.getStatus();
        defect.update(request.title(), request.description(),
                request.severity(), request.status(), request.externalUrl());
        // 상태 변경을 별도 로그로 남긴다(요청: 결함 상태변경).
        if (beforeStatus != defect.getStatus()) {
            auditLogService.log(AuditLogService.DEFECT, id, AuditAction.STATUS_CHANGED,
                    "결함 '" + defect.getTitle() + "' 상태: " + beforeStatus + " → " + defect.getStatus());
        } else {
            auditLogService.log(AuditLogService.DEFECT, id, AuditAction.UPDATED,
                    "결함 '" + defect.getTitle() + "'이(가) 수정되었습니다.");
        }
        return DefectResponse.from(defect);
    }

    @Transactional
    public void delete(Long id) {
        Defect defect = findById(id);
        attachmentService.deleteAllByEntity(AttachmentEntityType.DEFECT, id);
        defectRepository.deleteById(id);
        auditLogService.log(AuditLogService.DEFECT, id, AuditAction.DELETED,
                "결함 '" + defect.getTitle() + "'이(가) 삭제되었습니다.");
    }

    public Defect findById(Long id) {
        return defectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Defect not found. id=" + id));
    }
}
