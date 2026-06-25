package com.tms.execution.entity;

/** 통합 테스트 결과 상태. TestRun, ExecutionItem이 공통으로 사용한다. */
public enum ResultStatus {
    UNTESTED,      // 아직 실행하지 않음 (ExecutionItem 기본값)
    NOT_EXECUTED,  // 실행되지 않음 (TestRun 하위 호환)
    IN_PROGRESS,   // 현재 실행 중
    PASSED,
    FAILED,
    BLOCKED,
    RETEST,
    SKIPPED        // 레거시 하위 호환
}
