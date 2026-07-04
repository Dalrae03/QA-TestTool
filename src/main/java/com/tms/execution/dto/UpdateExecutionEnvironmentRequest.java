package com.tms.execution.dto;

/** 테스트런의 실행환경(테스트 컨피그) 재배정 요청. null이면 환경 없음으로 비운다. */
public record UpdateExecutionEnvironmentRequest(
        Long testConfigurationId
) {
}
