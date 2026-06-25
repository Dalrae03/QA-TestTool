package com.tms.execution.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    private String description;

    @Column(name = "project_id")
    private Long projectId;

    // 출처 스냅샷 — 플랜/스위트가 삭제돼도 런은 남는다 (FK 미사용).
    private Long testPlanId;

    @Column(length = 200)
    private String planName;

    private Long testSuiteId;

    @Column(length = 200)
    private String suiteName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(length = 100)
    private String assignee;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "item_order")
    private List<ExecutionItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    protected Execution() {
    }

    public Execution(String name, String description, Long projectId, Long testPlanId, String planName, Long testSuiteId, String suiteName, String assignee) {
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.testPlanId = testPlanId;
        this.planName = planName;
        this.testSuiteId = testSuiteId;
        this.suiteName = suiteName;
        this.assignee = assignee;
        this.status = ExecutionStatus.IN_PROGRESS;
    }

    public void addItem(Long testCaseId, String caseTitle) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle));
    }

    public void addItem(Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle, versionNumber, versionLabel));
    }

    public void updatePlan(Long testPlanId, String planName) {
        this.testPlanId = testPlanId;
        this.planName = planName;
    }

    public void update(String name, String description, ExecutionStatus status, String assignee) {
        this.name = name;
        this.description = description;
        this.assignee = assignee;
        setStatus(status);
    }

    public void setStatus(ExecutionStatus status) {
        if (status == ExecutionStatus.COMPLETED && this.status != ExecutionStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        } else if (status == ExecutionStatus.IN_PROGRESS) {
            this.completedAt = null;
        }
        this.status = status;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getProjectId() { return projectId; }
    public Long getTestPlanId() { return testPlanId; }
    public String getPlanName() { return planName; }
    public Long getTestSuiteId() { return testSuiteId; }
    public String getSuiteName() { return suiteName; }
    public ExecutionStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public List<ExecutionItem> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
