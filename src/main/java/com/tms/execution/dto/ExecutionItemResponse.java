package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;

public record ExecutionItemResponse(
        Long id,
        Long testCaseId,
        String caseTitle,
        Integer versionNumber,
        String versionLabel,
        ResultStatus status,
        String comment,
        String failureReason,
        LocalDateTime executedAt
) {
    public static ExecutionItemResponse from(ExecutionItem item) {
        return new ExecutionItemResponse(
                item.getId(),
                item.getTestCaseId(),
                item.getCaseTitle(),
                item.getVersionNumber(),
                item.getVersionLabel(),
                item.getStatus(),
                item.getComment(),
                item.getFailureReason(),
                item.getExecutedAt()
        );
    }
}
