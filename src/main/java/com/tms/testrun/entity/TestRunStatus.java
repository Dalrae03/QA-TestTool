package com.tms.testrun.entity;

public enum TestRunStatus {
    PASSED,
    FAILED,
    BLOCKED,        // 선행 조건 미충족으로 테스트 진행 불가
    SKIPPED,        // 의도적으로 건너뜀
    IN_PROGRESS,    // 장시간 테스트 등 진행 중
    RETEST,         // 결함 수정 후 재확인 필요
    NOT_EXECUTED    // 아직 실행되지 않음
}
