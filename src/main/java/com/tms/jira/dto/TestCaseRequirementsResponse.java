package com.tms.jira.dto;

import java.util.List;

/**
 * 테스트→요구사항 정방향 추적 결과.
 * 한 테스트케이스에 연결된 Jira 요구사항(이슈)들의 현재 정보를 담는다.
 */
public record TestCaseRequirementsResponse(
        Long testCaseId,
        List<JiraIssueInfo> requirements
) {
}
