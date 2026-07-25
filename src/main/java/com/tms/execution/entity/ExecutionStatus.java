package com.tms.execution.entity;

public enum ExecutionStatus {
    READY,        // 케이스는 올라갔으나 아직 첫 결과가 기록되지 않음 (준비됨)
    IN_PROGRESS,  // 첫 결과가 기록되어 진행 중
    COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
