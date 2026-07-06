package com.tms.environment.entity;

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
@Table(name = "server_environments")
public class ServerEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerEnvironmentType type;

    @Column(nullable = false, length = 500)
    private String baseUrl;

    @Lob
    @Column(length = Length.LONG32)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected ServerEnvironment() {
    }

    public ServerEnvironment(
            String name,
            ServerEnvironmentType type,
            String baseUrl,
            String description,
            boolean active
    ) {
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
        this.description = description;
        this.active = active;
    }

    public void update(
            String name,
            ServerEnvironmentType type,
            String baseUrl,
            String description,
            boolean active
    ) {
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
        this.description = description;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ServerEnvironmentType getType() { return type; }
    public String getBaseUrl() { return baseUrl; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
