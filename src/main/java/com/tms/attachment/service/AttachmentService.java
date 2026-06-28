package com.tms.attachment.service;

import com.tms.attachment.dto.AttachmentResponse;
import com.tms.attachment.entity.Attachment;
import com.tms.attachment.entity.AttachmentEntityType;
import com.tms.attachment.repository.AttachmentRepository;
import com.tms.global.exception.InvalidRequestException;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[A-Za-z0-9]{1,16}");

    /** 첨부로 허용하는 확장자 화이트리스트(소문자, 점 포함). 실행 가능한 형식은 의도적으로 제외한다. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp",
            ".pdf", ".txt", ".log", ".csv", ".json", ".xml", ".md",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".mp4", ".mov", ".webm"
    );

    private final AttachmentRepository attachmentRepository;
    private final com.tms.audit.service.AuditLogService auditLogService;
    private final Path uploadRoot;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            com.tms.audit.service.AuditLogService auditLogService,
            @Value("${tms.upload.dir}") String uploadDir
    ) throws IOException {
        this.attachmentRepository = attachmentRepository;
        this.auditLogService = auditLogService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    public AttachmentResponse get(Long id) {
        return AttachmentResponse.from(findById(id));
    }

    public List<AttachmentResponse> getList(AttachmentEntityType entityType, Long entityId) {
        return attachmentRepository
                .findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId)
                .stream().map(AttachmentResponse::from).toList();
    }

    @Transactional
    public AttachmentResponse upload(AttachmentEntityType entityType, Long entityId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("업로드할 파일이 비어있습니다.");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String extension = sanitizeExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidRequestException("허용되지 않는 파일 형식입니다: " + originalFilename);
        }
        String storedFilename = UUID.randomUUID() + extension;

        Path target = resolveWithinRoot(storedFilename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new InvalidRequestException("파일 저장에 실패했습니다: " + e.getMessage());
        }

        Attachment attachment = new Attachment(
                entityType, entityId,
                originalFilename, storedFilename,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize()
        );
        Attachment saved = attachmentRepository.save(attachment);
        auditLogService.log(com.tms.audit.service.AuditLogService.ATTACHMENT, saved.getId(),
                com.tms.audit.entity.AuditAction.ATTACHMENT_ADDED,
                "첨부파일 '" + originalFilename + "'이(가) 업로드되었습니다. (" + entityType + " #" + entityId + ")");
        return AttachmentResponse.from(saved);
    }

    public Resource download(Long id) {
        Attachment attachment = findById(id);
        Path filePath = resolveWithinRoot(attachment.getStoredFilename());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("파일을 찾을 수 없습니다. id=" + id);
            }
            return resource;
        } catch (IOException e) {
            throw new InvalidRequestException("파일 읽기에 실패했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public void delete(Long id) {
        Attachment attachment = findById(id);
        Path filePath = resolveWithinRoot(attachment.getStoredFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new InvalidRequestException("파일 삭제에 실패했습니다: " + e.getMessage());
        }
        attachmentRepository.delete(attachment);
        auditLogService.log(com.tms.audit.service.AuditLogService.ATTACHMENT, id,
                com.tms.audit.entity.AuditAction.ATTACHMENT_DELETED,
                "첨부파일 '" + attachment.getOriginalFilename() + "'이(가) 삭제되었습니다.");
    }

    @Transactional
    public void deleteAllByEntity(AttachmentEntityType entityType, Long entityId) {
        List<Attachment> attachments = attachmentRepository
                .findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        for (Attachment attachment : attachments) {
            Path filePath = resolveWithinRoot(attachment.getStoredFilename());
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
        }
        attachmentRepository.deleteAllByEntityTypeAndEntityId(entityType, entityId);
    }

    private Attachment findById(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found. id=" + id));
    }

    /**
     * 원본 파일명에서 안전한 확장자만 추출한다.
     * 경로 구분자나 ".."가 섞인 확장자로 인한 경로 조작을 막기 위해
     * "."+영숫자(최대 16자) 형태만 허용하고, 그 외에는 확장자를 버린다.
     */
    private String sanitizeExtension(String originalFilename) {
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String ext = originalFilename.substring(dot);
        return SAFE_EXTENSION.matcher(ext).matches() ? ext.toLowerCase() : "";
    }

    /**
     * 저장 파일명을 업로드 루트 기준으로 해석하되, 결과 경로가 루트를 벗어나면 거부한다(심층 방어).
     */
    private Path resolveWithinRoot(String storedFilename) {
        Path target = uploadRoot.resolve(storedFilename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new InvalidRequestException("잘못된 파일 경로입니다.");
        }
        return target;
    }
}
