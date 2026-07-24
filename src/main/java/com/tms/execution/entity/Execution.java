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
import org.hibernate.Length;

@Entity
@Table(name = "test_executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    // 제품/릴리스 버전 라벨 (예: 2.21, 2.22) — 같은 케이스를 버전별로 생성·수행·종료해 사이클을 구분한다.
    @Column(length = 50)
    private String version;

    @Lob
    @Column(length = Length.LONG32)
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

    // 실행환경(테스트 컨피그) 스냅샷 — 컨피그가 삭제·수정돼도 런이 "어떤 환경/버전에서 실행됐는지"는 그대로 보존된다 (FK 미사용).
    private Long testConfigurationId;

    @Column(length = 100)
    private String configurationName;

    // OS/브라우저/기기/Java·DB 버전까지 한 줄로 굳혀둔 스냅샷 — 컨피그가 나중에 바뀌어도 당시 환경이 흔들리지 않게.
    @Column(length = 500)
    private String environmentDetail;

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

    public Execution(String name, String description, Long projectId, Long testPlanId, String planName, Long testSuiteId, String suiteName, String assignee, String version) {
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.testPlanId = testPlanId;
        this.planName = planName;
        this.testSuiteId = testSuiteId;
        this.suiteName = suiteName;
        this.assignee = assignee;
        this.version = version;
        this.status = ExecutionStatus.READY;
    }

    public void addItem(Long testCaseId, String caseTitle) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle));
    }

    public void addItem(Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle, versionNumber, versionLabel));
    }

    public void addItem(Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel,
                         Long sourceSuiteId, String sourceSuiteName) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle, versionNumber, versionLabel, sourceSuiteId, sourceSuiteName));
    }

    public void addItem(Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel,
                         Long sourceSuiteId, String sourceSuiteName, Long sourceFolderId, String sourceFolderName, String sourceFolderCode) {
        this.items.add(new ExecutionItem(this, testCaseId, caseTitle, versionNumber, versionLabel,
                sourceSuiteId, sourceSuiteName, sourceFolderId, sourceFolderName, sourceFolderCode));
    }

    public void updatePlan(Long testPlanId, String planName) {
        this.testPlanId = testPlanId;
        this.planName = planName;
    }

    /** 스위트를 추가·제거한 뒤 출처 스위트 스냅샷(단일 id/이름 또는 병합 이름)을 다시 굳힌다. */
    public void updateSuiteSnapshot(Long testSuiteId, String suiteName) {
        this.testSuiteId = testSuiteId;
        this.suiteName = suiteName;
    }

    public void updateEnvironment(Long testConfigurationId, String configurationName, String environmentDetail) {
        this.testConfigurationId = testConfigurationId;
        this.configurationName = configurationName;
        this.environmentDetail = environmentDetail;
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
        } else if (status == ExecutionStatus.IN_PROGRESS || status == ExecutionStatus.READY) {
            this.completedAt = null;
        }
        this.status = status;
    }

    /** 첫 실제 결과가 기록되면 '준비됨' 런을 '진행 중'으로 자동 전환한다. (이미 진행 중·완료면 그대로) */
    public void startIfReady() {
        if (this.status == ExecutionStatus.READY) {
            setStatus(ExecutionStatus.IN_PROGRESS);
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public Long getProjectId() { return projectId; }
    public Long getTestPlanId() { return testPlanId; }
    public String getPlanName() { return planName; }
    public Long getTestSuiteId() { return testSuiteId; }
    public String getSuiteName() { return suiteName; }
    public Long getTestConfigurationId() { return testConfigurationId; }
    public String getConfigurationName() { return configurationName; }
    public String getEnvironmentDetail() { return environmentDetail; }
    public ExecutionStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public List<ExecutionItem> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
