package com.tms.execution.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Length;

@Entity
@Table(name = "test_execution_items")
public class ExecutionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    @Column(nullable = false)
    private Long testCaseId;

    @Column(nullable = false, length = 500)
    private String caseTitle;

    // 런 생성 시점의 테스트케이스 버전 스냅샷 — 케이스가 나중에 수정돼도 이 런의 기록은 그대로 보존된다.
    private Integer versionNumber;

    @Column(length = 100)
    private String versionLabel;

    // 이 항목이 생성될 당시 속해 있던 출처 스위트 스냅샷 — 여러 스위트를 병합해 만든 런에서
    // 항목별로 원래 어느 스위트에서 왔는지 구분해 보여주기 위함 (FK 미사용, 스위트가 삭제돼도 보존).
    private Long sourceSuiteId;

    @Column(length = 200)
    private String sourceSuiteName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    @Lob
    @Column(length = Length.LONG32)
    private String comment;

    /** 실패 사유 — Jira 티켓 URL 등 (#9) */
    @Lob
    @Column(length = Length.LONG32)
    private String failureReason;

    private LocalDateTime executedAt;

    // 재시도 이력 — 결과를 기록할 때마다 한 줄씩 쌓인다(시간 순). 가장 최근 시도가 위 status 필드와 일치한다.
    @OneToMany(mappedBy = "executionItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExecutionItemHistory> history = new ArrayList<>();

    protected ExecutionItem() {}

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle) {
        this(execution, testCaseId, caseTitle, null, null);
    }

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel) {
        this(execution, testCaseId, caseTitle, versionNumber, versionLabel, null, null);
    }

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel,
                          Long sourceSuiteId, String sourceSuiteName) {
        this.execution = execution;
        this.testCaseId = testCaseId;
        this.caseTitle = caseTitle;
        this.versionNumber = versionNumber;
        this.versionLabel = versionLabel;
        this.sourceSuiteId = sourceSuiteId;
        this.sourceSuiteName = sourceSuiteName;
        this.status = ResultStatus.UNTESTED;
    }

    public void record(ResultStatus status, String comment, String failureReason) {
        this.status = status;
        this.comment = comment;
        this.failureReason = failureReason;
        if (status == ResultStatus.UNTESTED) {
            // '미실행'으로 되돌리는 것은 오클릭 복구다 — 직전 시도 기록을 이력에서 함께 되돌린다.
            this.executedAt = null;
            if (!history.isEmpty()) {
                history.remove(history.size() - 1);
            }
        } else {
            this.executedAt = LocalDateTime.now();
            history.add(new ExecutionItemHistory(this, status, comment, failureReason, this.executedAt));
        }
    }

    /** 하위 호환 */
    public void record(ResultStatus status, String comment) {
        record(status, comment, null);
    }

    public Long getId() { return id; }
    public Execution getExecution() { return execution; }
    public Long getTestCaseId() { return testCaseId; }
    public String getCaseTitle() { return caseTitle; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getVersionLabel() { return versionLabel; }
    public Long getSourceSuiteId() { return sourceSuiteId; }
    public String getSourceSuiteName() { return sourceSuiteName; }
    public ResultStatus getStatus() { return status; }
    public String getComment() { return comment; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public List<ExecutionItemHistory> getHistory() { return history; }
}
