package com.tms.jira.dto;

/**
 * Jira 이슈의 핵심 정보 스냅샷.
 * 추적성(traceability) 화면에서 연결된 요구사항/이슈의 현재 상태를 보여줄 때 사용한다.
 */
public record JiraIssueInfo(
        String key,
        String summary,
        String statusName,
        String statusCategory,
        String url
) {
}
