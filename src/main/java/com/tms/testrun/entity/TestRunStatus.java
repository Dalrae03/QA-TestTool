package com.tms.testrun.entity;

public enum TestRunStatus {
    PASSED,
    FAILED,
    BLOCKED,

    // 기존 데이터 역직렬화 호환용. 신규 생성과 수정 요청에서는 허용하지 않는다.
    SKIPPED,
    IN_PROGRESS,
    RETEST,
    NOT_EXECUTED
}
