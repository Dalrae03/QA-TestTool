package com.tms.execution.dto;

import com.tms.execution.entity.ExecutionItem;
import com.tms.execution.entity.ExecutionStatus;
import com.tms.execution.entity.ResultStatus;
import com.tms.testcase.entity.TestCaseVersion;
import java.time.LocalDateTime;

/** 테스트케이스 상세의 "실행 기록" 탭에서 보여주는 — 테스트런(Execution)을 통한 실행 이력 한 줄. */
public record TestCaseExecutionHistoryResponse(
        Long executionItemId,
        Long executionId,
        String executionName,
        ExecutionStatus executionStatus,
        Integer versionNumber,
        String versionLabel,
        ResultStatus status,
        String comment,
        String failureReason,
        LocalDateTime executedAt
) {
    /**
     * @param fallbackVersion item 자체에 저장된 버전 스냅샷이 없을 때(런 생성이 버전 스냅샷 기능보다 더 오래된 경우)
     *                        대신 보여줄 역추적된 버전. item에 스냅샷이 있으면 무시된다.
     */
    public static TestCaseExecutionHistoryResponse from(ExecutionItem item, TestCaseVersion fallbackVersion) {
        Integer versionNumber = item.getVersionNumber() != null ? item.getVersionNumber()
                : fallbackVersion != null ? fallbackVersion.getVersionNumber() : null;
        String versionLabel = item.getVersionLabel() != null ? item.getVersionLabel()
                : fallbackVersion != null ? fallbackVersion.getLabel() : null;
        return new TestCaseExecutionHistoryResponse(
                item.getId(),
                item.getExecution().getId(),
                item.getExecution().getName(),
                item.getExecution().getStatus(),
                versionNumber,
                versionLabel,
                item.getStatus(),
                item.getComment(),
                item.getFailureReason(),
                item.getExecutedAt()
        );
    }
}
