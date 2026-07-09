package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ResultStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        int defectCount,
        LocalDateTime executedAt,
        List<ExecutionItemHistoryResponse> history
) {
    public static ExecutionItemResponse from(ExecutionItem item, Map<Long, Integer> defectCounts) {
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
                defectCounts.getOrDefault(item.getTestCaseId(), 0),
                item.getExecutedAt(),
                item.getHistory().stream().map(ExecutionItemHistoryResponse::from).toList()
        );
    }
}
