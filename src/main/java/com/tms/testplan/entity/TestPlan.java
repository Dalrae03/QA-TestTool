package com.tms.testplan.entity;

import com.tms.testcase.entity.TestCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPlanStatus status;

    @Column(length = 100)
    private String assignee;

    private LocalDate startDate;
    private LocalDate endDate;

    // ── 1. 기본 정보 ──────────────────────────────────────────────────

    @Column(length = 200)
    private String targetSystem;

    @Column(length = 200)
    private String targetVersion;

    // ── 2. 프로젝트 개요 ──────────────────────────────────────────────

    @Lob
    private String testGoal;

    @Lob
    private String testTarget;

    // ── 3. 테스트 범위 ────────────────────────────────────────────────

    /** 3.1 핵심 테스트 대상 — TestSuite와는 별개로 플랜에 직접 연결하는 테스트케이스 목록 */
    @ManyToMany
    @JoinTable(
            name = "test_plan_core_cases",
            joinColumns = @JoinColumn(name = "test_plan_id"),
            inverseJoinColumns = @JoinColumn(name = "test_case_id")
    )
    private List<TestCase> coreTestCases = new ArrayList<>();

    @Lob
    private String impactScope;

    @Lob
    private String commonScope;

    // ── 5. 테스트 우선순위 및 리스크 ──────────────────────────────────

    @Lob
    private String priorityTargets;

    @Lob
    private String riskAnalysis;

    // ── 6. 테스트 전략 ────────────────────────────────────────────────

    @Lob
    private String testApproach;

    @Lob
    private String testPerspective;

    // ── 7. 진입 조건, 종료 기준 ───────────────────────────────────────

    @Lob
    private String entryCriteria;

    @Lob
    private String exitCriteria;

    // ── 9. 테스트 환경 ────────────────────────────────────────────────

    @Lob
    private String serverEnvironmentNote;

    /** 9.2 테스트 디바이스 — [{platform, device}] 형태의 JSON 배열 문자열. 백엔드는 파싱하지 않고 그대로 저장/반환한다. */
    @Lob
    private String deviceMatrix;

    @Lob
    private String testData;

    // ── 10. 테스트 일정 및 절차 (소분류 없음) ────────────────────────

    /** [{period, phase, task}] 형태의 JSON 배열 문자열. 백엔드는 파싱하지 않는다. */
    @Lob
    private String schedule;

    // ── 11. 산출물 (소분류 없음) ──────────────────────────────────────

    @Lob
    private String deliverables;

    @Column(name = "project_id")
    private Long projectId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestPlan() {}

    public TestPlan(String name, TestPlanStatus status, String assignee, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.status = status != null ? status : TestPlanStatus.DRAFT;
        this.assignee = assignee;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(
            String name, TestPlanStatus status, String assignee, LocalDate startDate, LocalDate endDate,
            String targetSystem, String targetVersion,
            String testGoal, String testTarget,
            String impactScope, String commonScope,
            String priorityTargets, String riskAnalysis,
            String testApproach, String testPerspective,
            String entryCriteria, String exitCriteria,
            String serverEnvironmentNote, String deviceMatrix, String testData,
            String schedule, String deliverables
    ) {
        this.name = name;
        this.status = status;
        this.assignee = assignee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.targetSystem = targetSystem;
        this.targetVersion = targetVersion;
        this.testGoal = testGoal;
        this.testTarget = testTarget;
        this.impactScope = impactScope;
        this.commonScope = commonScope;
        this.priorityTargets = priorityTargets;
        this.riskAnalysis = riskAnalysis;
        this.testApproach = testApproach;
        this.testPerspective = testPerspective;
        this.entryCriteria = entryCriteria;
        this.exitCriteria = exitCriteria;
        this.serverEnvironmentNote = serverEnvironmentNote;
        this.deviceMatrix = deviceMatrix;
        this.testData = testData;
        this.schedule = schedule;
        this.deliverables = deliverables;
    }

    /** 3.1 핵심 테스트 대상 갱신 — delta 방식(Hibernate INSERT-before-DELETE 회피), TestSuite.replaceTestCases와 동일 패턴. */
    public void replaceCoreTestCases(List<TestCase> newTestCases) {
        List<TestCase> newList = newTestCases != null ? newTestCases : List.of();
        Set<Long> newIds = newList.stream().map(TestCase::getId).collect(java.util.stream.Collectors.toSet());
        this.coreTestCases.removeIf(tc -> !newIds.contains(tc.getId()));
        Set<Long> existingIds = this.coreTestCases.stream().map(TestCase::getId).collect(java.util.stream.Collectors.toSet());
        for (TestCase tc : newList) {
            if (!existingIds.contains(tc.getId())) {
                this.coreTestCases.add(tc);
            }
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public TestPlanStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getTargetSystem() { return targetSystem; }
    public String getTargetVersion() { return targetVersion; }
    public String getTestGoal() { return testGoal; }
    public String getTestTarget() { return testTarget; }
    public List<TestCase> getCoreTestCases() { return coreTestCases; }
    public String getImpactScope() { return impactScope; }
    public String getCommonScope() { return commonScope; }
    public String getPriorityTargets() { return priorityTargets; }
    public String getRiskAnalysis() { return riskAnalysis; }
    public String getTestApproach() { return testApproach; }
    public String getTestPerspective() { return testPerspective; }
    public String getEntryCriteria() { return entryCriteria; }
    public String getExitCriteria() { return exitCriteria; }
    public String getServerEnvironmentNote() { return serverEnvironmentNote; }
    public String getDeviceMatrix() { return deviceMatrix; }
    public String getTestData() { return testData; }
    public String getSchedule() { return schedule; }
    public String getDeliverables() { return deliverables; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
