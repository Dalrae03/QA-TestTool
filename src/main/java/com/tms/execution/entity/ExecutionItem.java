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

    // 생성 시점 스냅샷 — 케이스가 삭제/변경돼도 런 이력은 보존된다 (FK 미사용).
    @Column(nullable = false)
    private Long testCaseId;

    @Column(nullable = false, length = 500)
    private String caseTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    @Lob
    private String comment;

    private LocalDateTime executedAt;

    protected ExecutionItem() {
    }

    public ExecutionItem(Execution execution, Long testCaseId, String caseTitle) {
        this.execution = execution;
        this.testCaseId = testCaseId;
        this.caseTitle = caseTitle;
        this.status = ResultStatus.UNTESTED;
    }

    public void record(ResultStatus status, String comment) {
        this.status = status;
        this.comment = comment;
        this.executedAt = status == ResultStatus.UNTESTED ? null : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Execution getExecution() { return execution; }
    public Long getTestCaseId() { return testCaseId; }
    public String getCaseTitle() { return caseTitle; }
    public ResultStatus getStatus() { return status; }
    public String getComment() { return comment; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
