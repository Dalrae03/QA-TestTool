package com.tms.defect.entity;

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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.Length;

@Entity
@Table(name = "defects")
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(length = Length.LONG32)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefectSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefectStatus status;

    @Column(length = 500)
    private String externalUrl;

    @Column(length = 50)
    private String jiraKey;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected Defect() {
    }

    public Defect(String title, String description, DefectSeverity severity, DefectStatus status, String externalUrl) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = status != null ? status : DefectStatus.OPEN;
        this.externalUrl = externalUrl;
    }

    public void update(String title, String description, DefectSeverity severity, DefectStatus status, String externalUrl) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = status;
        this.externalUrl = externalUrl;
    }

    public void linkJira(String jiraKey) { this.jiraKey = jiraKey; }
    public void updateStatus(DefectStatus status) { this.status = status; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public DefectSeverity getSeverity() { return severity; }
    public DefectStatus getStatus() { return status; }
    public String getExternalUrl() { return externalUrl; }
    public String getJiraKey() { return jiraKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
