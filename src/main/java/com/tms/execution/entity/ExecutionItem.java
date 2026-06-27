package com.tms.execution.entity;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    @Lob
    private String comment;

    /** 실패 사유 — Jira 티켓 URL 등 (#9) */
    @Lob
    private String failureReason;

    private LocalDateTime executedAt;

    protected ExecutionItem() {}

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle) {
        this(execution, testCaseId, caseTitle, null, null);
    }

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle, Integer versionNumber, String versionLabel) {
        this.execution = execution;
        this.testCaseId = testCaseId;
        this.caseTitle = caseTitle;
        this.versionNumber = versionNumber;
        this.versionLabel = versionLabel;
        this.status = ResultStatus.UNTESTED;
    }

    public void record(ResultStatus status, String comment, String failureReason) {
        this.status = status;
        this.comment = comment;
        this.failureReason = failureReason;
        this.executedAt = status == ResultStatus.UNTESTED ? null : LocalDateTime.now();
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
    public ResultStatus getStatus() { return status; }
    public String getComment() { return comment; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
