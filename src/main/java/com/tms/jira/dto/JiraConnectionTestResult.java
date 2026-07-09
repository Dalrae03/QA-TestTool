package com.tms.jira.dto;

/** Jira 연결 테스트 결과 — 인증된 사용자 정보와 프로젝트 확인 결과. */
public record JiraConnectionTestResult(
        boolean success,
        String accountDisplayName,
        String accountEmail,
        String projectName,
        String message
) {
}
