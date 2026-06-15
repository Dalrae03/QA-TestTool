package com.tms.defect.service;

import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.service.AttachmentService;
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

    public DefectService(DefectRepository defectRepository, @Lazy AttachmentService attachmentService) {
        this.defectRepository = defectRepository;
        this.attachmentService = attachmentService;
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
        return DefectResponse.from(defectRepository.save(defect));
    }

    @Transactional
    public DefectResponse update(Long id, DefectRequest request) {
        Defect defect = findById(id);
        defect.update(request.title(), request.description(),
                request.severity(), request.status(), request.externalUrl());
        return DefectResponse.from(defect);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        attachmentService.deleteAllByEntity(AttachmentEntityType.DEFECT, id);
        defectRepository.deleteById(id);
    }

    public Defect findById(Long id) {
        return defectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Defect not found. id=" + id));
    }
}
