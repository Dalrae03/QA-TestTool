package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ExecutionItemResponse(
        Long id,
        Long testCaseId,
        String caseTitle,
        Integer versionNumber,
        String versionLabel,
        Long sourceSuiteId,
        String sourceSuiteName,
        ResultStatus status,
        String comment,
        String failureReason,
        LocalDateTime executedAt,
        List<ExecutionItemHistoryResponse> history
) {
    public static ExecutionItemResponse from(ExecutionItem item) {
        return new ExecutionItemResponse(
                item.getId(),
                item.getTestCaseId(),
                item.getCaseTitle(),
                item.getVersionNumber(),
                item.getVersionLabel(),
                item.getSourceSuiteId(),
                item.getSourceSuiteName(),
                item.getStatus(),
                item.getComment(),
                item.getFailureReason(),
                item.getExecutedAt(),
                item.getHistory().stream().map(ExecutionItemHistoryResponse::from).toList()
        );
    }
}
