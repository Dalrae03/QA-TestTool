package com.tms.jira.dto;

import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseStatus;
import java.util.List;

/**
 * 요구사항→테스트 역방향 추적 결과.
 * 하나의 Jira 요구사항(이슈)에 연결된 TMS 테스트케이스 목록과, 가능하면 Jira 이슈의 현재 상태를 함께 담는다.
 */
public record RequirementCoverageResponse(
        String jiraKey,
        JiraIssueInfo issue,
        int testCaseCount,
        List<TestCaseRef> testCases
) {
    /** 추적 화면에서 보여줄 테스트케이스 경량 참조. */
    public record TestCaseRef(Long id, String title, TestCaseStatus status) {
        public static TestCaseRef from(TestCase testCase) {
            return new TestCaseRef(testCase.getId(), testCase.getTitle(), testCase.getStatus());
        }
    }

    public static RequirementCoverageResponse of(String jiraKey, JiraIssueInfo issue, List<TestCase> testCases) {
        List<TestCaseRef> refs = testCases.stream().map(TestCaseRef::from).toList();
        return new RequirementCoverageResponse(jiraKey, issue, refs.size(), refs);
    }
}
