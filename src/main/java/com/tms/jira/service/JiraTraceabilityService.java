package com.tms.jira.service;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.jira.client.JiraClient;
import com.tms.jira.config.JiraProperties;
import com.tms.jira.dto.JiraIssueInfo;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.RequirementCoverageResponse;
import com.tms.jira.dto.TestCaseRequirementsResponse;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트케이스 ↔ Jira 요구사항(이슈) 사이의 양방향 추적성(traceability)을 담당한다.
 *
 * <ul>
 *   <li>정방향: 테스트케이스 → 연결된 요구사항 목록 조회</li>
 *   <li>역방향: 요구사항 → 해당 요구사항을 검증하는 테스트케이스 목록 조회(커버리지)</li>
 *   <li>쓰기 동기화: 연결 시 Jira 이슈에 TMS 항목을 가리키는 원격 링크를 남겨 Jira에서도 추적 가능</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class JiraTraceabilityService {

    private final TestCaseRepository testCaseRepository;
    private final JiraClient jiraClient;
    private final JiraProperties jiraProperties;
    private final AuditLogService auditLogService;

    public JiraTraceabilityService(
            TestCaseRepository testCaseRepository,
            JiraClient jiraClient,
            JiraProperties jiraProperties,
            AuditLogService auditLogService
    ) {
        this.testCaseRepository = testCaseRepository;
        this.jiraClient = jiraClient;
        this.jiraProperties = jiraProperties;
        this.auditLogService = auditLogService;
    }

    /** 테스트케이스에 Jira 요구사항을 연결한다. 존재하는 이슈인지 검증한 뒤 Jira에도 역링크를 남긴다. */
    @Transactional
    public TestCaseRequirementsResponse linkRequirement(Long testCaseId, JiraLinkRequest request) {
        TestCase testCase = findTestCase(testCaseId);
        String jiraKey = request.jiraKey();
        // 실재하는 Jira 이슈인지 검증(읽기 동기화). 잘못된 key 연결을 막는다.
        jiraClient.getIssue(jiraKey);
        boolean added = testCase.linkRequirement(jiraKey);
        if (added) {
            createRemoteLink(testCase, jiraKey);
            auditLogService.log(AuditLogService.TEST_CASE, testCaseId, AuditAction.REQUIREMENT_LINKED,
                    "테스트케이스 '" + testCase.getTitle() + "'에 Jira 요구사항 " + jiraKey + "이(가) 연결되었습니다.");
        }
        return buildRequirements(testCase);
    }

    /** 테스트케이스의 Jira 요구사항 연결을 해제한다. */
    @Transactional
    public TestCaseRequirementsResponse unlinkRequirement(Long testCaseId, String jiraKey) {
        TestCase testCase = findTestCase(testCaseId);
        boolean removed = testCase.unlinkRequirement(jiraKey);
        if (removed) {
            auditLogService.log(AuditLogService.TEST_CASE, testCaseId, AuditAction.REQUIREMENT_UNLINKED,
                    "테스트케이스 '" + testCase.getTitle() + "'의 Jira 요구사항 " + jiraKey + " 연결이 해제되었습니다.");
        }
        return buildRequirements(testCase);
    }

    /** 정방향: 테스트케이스에 연결된 요구사항들의 현재 정보를 조회한다. */
    public TestCaseRequirementsResponse getRequirements(Long testCaseId) {
        return buildRequirements(findTestCase(testCaseId));
    }

    /** 역방향: 특정 Jira 요구사항을 검증하는 테스트케이스 목록(커버리지)을 조회한다. */
    public RequirementCoverageResponse getCoverage(String jiraKey) {
        List<TestCase> testCases = testCaseRepository.findAllByJiraRequirementKey(jiraKey);
        JiraIssueInfo issue = issueInfoOrPlaceholder(jiraKey);
        return RequirementCoverageResponse.of(jiraKey, issue, testCases);
    }

    private TestCaseRequirementsResponse buildRequirements(TestCase testCase) {
        List<JiraIssueInfo> requirements = testCase.getJiraRequirementKeys().stream()
                .map(this::issueInfoOrPlaceholder)
                .toList();
        return new TestCaseRequirementsResponse(testCase.getId(), requirements);
    }

    /** Jira 이슈 정보를 조회하되, 조회 실패(미설정·삭제·권한)해도 연결 자체는 보존되도록 최소 정보를 반환한다. */
    private JiraIssueInfo issueInfoOrPlaceholder(String jiraKey) {
        try {
            return jiraClient.getIssue(jiraKey);
        } catch (RuntimeException e) {
            return new JiraIssueInfo(jiraKey, null, null, null, null);
        }
    }

    /** TMS 웹 주소가 설정돼 있으면 Jira 이슈에 TMS 테스트케이스를 가리키는 원격 링크를 남긴다(best-effort). */
    private void createRemoteLink(TestCase testCase, String jiraKey) {
        if (!jiraProperties.hasWebBaseUrl()) {
            return;
        }
        String url = jiraProperties.webBaseUrl().replaceAll("/+$", "") + "/testcases/" + testCase.getId();
        String title = "TMS TC-" + testCase.getId() + ": " + testCase.getTitle();
        try {
            jiraClient.addRemoteLink(jiraKey, url, title);
        } catch (RuntimeException e) {
            // 역링크 실패가 TMS 연결을 막지 않도록 무시한다(추적성은 TMS 측에 이미 저장됨).
        }
    }

    private TestCase findTestCase(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found. id=" + id));
    }
}
