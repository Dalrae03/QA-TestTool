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
import org.hibernate.Length;

/**
 * 테스트런 항목(ExecutionItem)에 결과를 기록할 때마다 남기는 "재시도 이력" 한 줄 —
 * 같은 케이스를 실패→재테스트→통과처럼 여러 번 실행한 흐름을 시간 순으로 보존한다.
 * ExecutionItem이 보관하는 것은 "가장 최근 결과"뿐이라, 그 이전 시도들은 여기에 쌓인다.
 */
@Entity
@Table(name = "test_execution_item_history")
public class ExecutionItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_item_id", nullable = false)
    private ExecutionItem executionItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    @Lob
    @Column(length = Length.LONG32)
    private String comment;

    @Lob
    @Column(length = Length.LONG32)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    protected ExecutionItemHistory() {}

    public ExecutionItemHistory(ExecutionItem executionItem, ResultStatus status, String comment, String failureReason, LocalDateTime recordedAt) {
        this.executionItem = executionItem;
        this.status = status;
        this.comment = comment;
        this.failureReason = failureReason;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public ExecutionItem getExecutionItem() { return executionItem; }
    public ResultStatus getStatus() { return status; }
    public String getComment() { return comment; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
