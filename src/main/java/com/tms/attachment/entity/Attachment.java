package com.tms.attachment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttachmentEntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected Attachment() {
    }

    public Attachment(AttachmentEntityType entityType, Long entityId,
                      String originalFilename, String storedFilename,
                      String contentType, long fileSize) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public Long getId() { return id; }
    public AttachmentEntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
