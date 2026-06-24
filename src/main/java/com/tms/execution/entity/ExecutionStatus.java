package com.tms.execution.entity;

public enum ExecutionStatus {
    IN_PROGRESS,
    COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
