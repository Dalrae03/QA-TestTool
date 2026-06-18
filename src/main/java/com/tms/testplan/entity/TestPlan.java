package com.tms.testplan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_plans")
public class TestPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPlanStatus status;

    @Column(length = 100)
    private String assignee;

    private LocalDate startDate;

    private LocalDate endDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestPlan() {
    }

    public TestPlan(String name, String description, TestPlanStatus status, LocalDate startDate, LocalDate endDate) {
        this(name, description, status, null, startDate, endDate);
    }

    public TestPlan(String name, String description, TestPlanStatus status, String assignee, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.status = status != null ? status : TestPlanStatus.DRAFT;
        this.assignee = assignee;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(String name, String description, TestPlanStatus status, LocalDate startDate, LocalDate endDate) {
        update(name, description, status, null, startDate, endDate);
    }

    public void update(String name, String description, TestPlanStatus status, String assignee, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TestPlanStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
