package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItemHistory;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;

/** 테스트런 항목의 재시도 이력 한 줄 — 시도 시점의 결과 스냅샷. */
public record ExecutionItemHistoryResponse(
        Long id,
        ResultStatus status,
        String comment,
        String failureReason,
        LocalDateTime recordedAt
) {
    public static ExecutionItemHistoryResponse from(ExecutionItemHistory history) {
        return new ExecutionItemHistoryResponse(
                history.getId(),
                history.getStatus(),
                history.getComment(),
                history.getFailureReason(),
                history.getRecordedAt()
        );
    }
}
