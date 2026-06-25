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

    // ── #5 테스트 플랜 확장 필드 ──────────────────────────────────────

    /** 위험 요소 / 리스크 목록 (줄바꿈 구분) */
    @Lob
    private String riskItems;

    /** 테스트 범위 / 스코프 */
    @Lob
    private String scope;

    /** 투입 인력 수 */
    private Integer teamSize;

    /** 투입 인원 목록 (줄바꿈 구분) */
    @Lob
    private String teamMembers;

    /** 목표 품질 기준 */
    @Lob
    private String qualityCriteria;

    /** 예산 */
    @Column(length = 200)
    private String budget;

    /** 기타 메모 */
    @Lob
    private String planNotes;

    @Column(name = "project_id")
    private Long projectId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestPlan() {}

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

    public void update(String name, String description, TestPlanStatus status, String assignee,
                       LocalDate startDate, LocalDate endDate,
                       String riskItems, String scope, Integer teamSize, String teamMembers,
                       String qualityCriteria, String budget, String planNotes) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.riskItems = riskItems;
        this.scope = scope;
        this.teamSize = teamSize;
        this.teamMembers = teamMembers;
        this.qualityCriteria = qualityCriteria;
        this.budget = budget;
        this.planNotes = planNotes;
    }

    /** 하위 호환 — 확장 필드 없이 기본 업데이트 */
    public void update(String name, String description, TestPlanStatus status, String assignee,
                       LocalDate startDate, LocalDate endDate) {
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
    public String getRiskItems() { return riskItems; }
    public String getScope() { return scope; }
    public Integer getTeamSize() { return teamSize; }
    public String getTeamMembers() { return teamMembers; }
    public String getQualityCriteria() { return qualityCriteria; }
    public String getBudget() { return budget; }
    public String getPlanNotes() { return planNotes; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
