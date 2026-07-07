package com.tms.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.Length;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(nullable = false, length = 100)
    private String fieldName;

    @Lob
    @Column(length = Length.LONG32)
    private String oldValue;

    @Lob
    @Column(length = Length.LONG32)
    private String newValue;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(length = 100)
    private String actor;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(
            String entityType,
            Long entityId,
            AuditAction action,
            String fieldName,
            String oldValue,
            String newValue,
            String summary,
            String actor
    ) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.summary = summary;
        this.actor = actor;
    }

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getSummary() {
        return summary;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
