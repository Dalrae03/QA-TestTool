package com.tms.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.jira.client.JiraClient;
import com.tms.jira.config.JiraConfig;
import com.tms.jira.dto.JiraIssueInfo;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.RequirementCoverageResponse;
import com.tms.jira.dto.TestCaseRequirementsResponse;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraTraceabilityServiceTest {

    @Mock TestCaseRepository testCaseRepository;
    @Mock JiraClient jiraClient;
    @Mock JiraSettingsService settingsService;
    @Mock AuditLogService auditLogService;
    @InjectMocks JiraTraceabilityService service;

    private TestCase newTestCase(String title) {
        return new TestCase(
                TestCaseType.FUNCTIONAL,
                TestCasePriority.HIGH,
                TestCaseStatus.READY,
                title,
                "설명", "사전조건", "절차", "비고",
                null, null, null, List.of()
        );
    }

    private JiraIssueInfo issue(String key) {
        return new JiraIssueInfo(key, "요구사항 " + key, "In Progress", "indeterminate",
                "https://x.atlassian.net/browse/" + key);
    }

    @Test
    void linkRequirement_존재하는Jira이슈를_테스트케이스에_연결한다() {
        TestCase testCase = newTestCase("로그인 테스트");
        given(testCaseRepository.findById(1L)).willReturn(Optional.of(testCase));
        given(jiraClient.getIssue("PROJ-1")).willReturn(issue("PROJ-1"));
        given(settingsService.current()).willReturn(JiraConfig.empty());

        TestCaseRequirementsResponse response = service.linkRequirement(1L, new JiraLinkRequest("PROJ-1"));

        assertThat(testCase.getJiraRequirementKeys()).containsExactly("PROJ-1");
        assertThat(response.requirements()).extracting(JiraIssueInfo::key).containsExactly("PROJ-1");
        then(auditLogService).should().log(eq(AuditLogService.TEST_CASE), eq(1L),
                eq(AuditAction.REQUIREMENT_LINKED), any());
    }

    @Test
    void linkRequirement_이미연결된키는_중복연결하지_않는다() {
        TestCase testCase = newTestCase("로그인 테스트");
        testCase.linkRequirement("PROJ-1");
        given(testCaseRepository.findById(1L)).willReturn(Optional.of(testCase));
        given(jiraClient.getIssue("PROJ-1")).willReturn(issue("PROJ-1"));

        service.linkRequirement(1L, new JiraLinkRequest("PROJ-1"));

        assertThat(testCase.getJiraRequirementKeys()).containsExactly("PROJ-1");
        then(auditLogService).should(never()).log(any(), any(), any(), any());
    }

    @Test
    void linkRequirement_웹주소가설정되면_Jira에_역링크를_남긴다() {
        TestCase testCase = newTestCase("로그인 테스트");
        given(testCaseRepository.findById(1L)).willReturn(Optional.of(testCase));
        given(jiraClient.getIssue("PROJ-1")).willReturn(issue("PROJ-1"));
        given(settingsService.current()).willReturn(
                new JiraConfig(null, null, null, null, "https://tms.example.com/"));

        service.linkRequirement(1L, new JiraLinkRequest("PROJ-1"));

        then(jiraClient).should().addRemoteLink(eq("PROJ-1"),
                eq("https://tms.example.com/testcases/" + testCase.getId()), any());
    }

    @Test
    void unlinkRequirement_연결을_해제한다() {
        TestCase testCase = newTestCase("로그인 테스트");
        testCase.linkRequirement("PROJ-1");
        testCase.linkRequirement("PROJ-2");
        given(testCaseRepository.findById(1L)).willReturn(Optional.of(testCase));
        given(jiraClient.getIssue("PROJ-2")).willReturn(issue("PROJ-2"));

        TestCaseRequirementsResponse response = service.unlinkRequirement(1L, "PROJ-1");

        assertThat(testCase.getJiraRequirementKeys()).containsExactly("PROJ-2");
        assertThat(response.requirements()).extracting(JiraIssueInfo::key).containsExactly("PROJ-2");
        then(auditLogService).should().log(eq(AuditLogService.TEST_CASE), eq(1L),
                eq(AuditAction.REQUIREMENT_UNLINKED), any());
    }

    @Test
    void getCoverage_요구사항을_검증하는_테스트케이스를_역방향으로_조회한다() {
        TestCase tc1 = newTestCase("케이스1");
        TestCase tc2 = newTestCase("케이스2");
        given(testCaseRepository.findAllByJiraRequirementKey("PROJ-1")).willReturn(List.of(tc1, tc2));
        given(jiraClient.getIssue("PROJ-1")).willReturn(issue("PROJ-1"));

        RequirementCoverageResponse response = service.getCoverage("PROJ-1");

        assertThat(response.jiraKey()).isEqualTo("PROJ-1");
        assertThat(response.testCaseCount()).isEqualTo(2);
        assertThat(response.testCases()).extracting(RequirementCoverageResponse.TestCaseRef::title)
                .containsExactly("케이스1", "케이스2");
        assertThat(response.issue().summary()).isEqualTo("요구사항 PROJ-1");
    }

    @Test
    void getRequirements_Jira조회실패해도_연결key는_보존한다() {
        TestCase testCase = newTestCase("로그인 테스트");
        testCase.linkRequirement("PROJ-1");
        given(testCaseRepository.findById(1L)).willReturn(Optional.of(testCase));
        given(jiraClient.getIssue("PROJ-1")).willThrow(new RuntimeException("Jira 미설정"));

        TestCaseRequirementsResponse response = service.getRequirements(1L);

        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).key()).isEqualTo("PROJ-1");
        assertThat(response.requirements().get(0).summary()).isNull();
    }
}
