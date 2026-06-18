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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final Path uploadRoot;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            @Value("${tms.upload.dir}") String uploadDir
    ) throws IOException {
        this.attachmentRepository = attachmentRepository;
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
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String storedFilename = UUID.randomUUID() + ext;

        Path target = uploadRoot.resolve(storedFilename);
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
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public Resource download(Long id) {
        Attachment attachment = findById(id);
        Path filePath = uploadRoot.resolve(attachment.getStoredFilename()).normalize();
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
        Path filePath = uploadRoot.resolve(attachment.getStoredFilename()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new InvalidRequestException("파일 삭제에 실패했습니다: " + e.getMessage());
        }
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAllByEntity(AttachmentEntityType entityType, Long entityId) {
        List<Attachment> attachments = attachmentRepository
                .findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        for (Attachment attachment : attachments) {
            Path filePath = uploadRoot.resolve(attachment.getStoredFilename()).normalize();
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
}
