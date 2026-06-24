package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;

public record ExecutionItemResponse(
        Long id,
        Long testCaseId,
        String caseTitle,
        ResultStatus status,
        String comment,
        LocalDateTime executedAt
) {
    public static ExecutionItemResponse from(ExecutionItem item) {
        return new ExecutionItemResponse(
                item.getId(),
                item.getTestCaseId(),
                item.getCaseTitle(),
                item.getStatus(),
                item.getComment(),
                item.getExecutedAt()
        );
    }
}
