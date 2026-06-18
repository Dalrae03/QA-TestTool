package com.tms.attachment.dto;

import com.tms.attachment.entity.Attachment;
import com.tms.attachment.entity.AttachmentEntityType;
import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        AttachmentEntityType entityType,
        Long entityId,
        String originalFilename,
        String contentType,
        long fileSize,
        LocalDateTime createdAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getEntityType(),
                attachment.getEntityId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedAt()
        );
    }
}
