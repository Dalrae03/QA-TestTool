package com.tms.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tms.defect.dto.DefectResponse;
import com.tms.defect.entity.Defect;
import com.tms.defect.entity.DefectSeverity;
import com.tms.defect.entity.DefectStatus;
import com.tms.defect.repository.DefectRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.jira.client.JiraClient;
import com.tms.jira.config.JiraProperties;
import com.tms.jira.dto.JiraLinkRequest;
import com.tms.jira.dto.JiraSyncResult;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JiraServiceTest {

    @Mock DefectRepository defectRepository;
    @Mock JiraClient jiraClient;
    @Mock JiraProperties jiraProperties;
    @InjectMocks JiraService jiraService;

    @Test
    void push_새결함은_Jira이슈를_생성하고_key를_저장한다() {
        Defect defect = new Defect("로그인 버그", "재현 방법", DefectSeverity.CRITICAL, DefectStatus.OPEN, null);
        given(defectRepository.findById(1L)).willReturn(Optional.of(defect));
        given(jiraClient.createIssue(any(), any(), any(), any())).willReturn("TMS-42");

        DefectResponse response = jiraService.push(1L);

        assertThat(response.jiraKey()).isEqualTo("TMS-42");
        then(jiraClient).should().createIssue("로그인 버그", "재현 방법", "Bug", "Highest");
    }

    @Test
    void push_이미연결된결함은_Jira이슈를_업데이트한다() {
        Defect defect = new Defect("로그인 버그", "재현 방법", DefectSeverity.MAJOR, DefectStatus.IN_PROGRESS, null);
        defect.linkJira("TMS-10");
        given(defectRepository.findById(1L)).willReturn(Optional.of(defect));

        jiraService.push(1L);

        then(jiraClient).should().updateIssue("TMS-10", "로그인 버그", "재현 방법");
        then(jiraClient).should().transitionToCategory("TMS-10", "indeterminate");
        then(jiraClient).should(never()).createIssue(any(), any(), any(), any());
    }

    @Test
    void pull_Jira상태카테고리를_TMS상태로_동기화한다() {
        Defect defect = new Defect("버그", null, DefectSeverity.MINOR, DefectStatus.OPEN, null);
        defect.linkJira("TMS-5");
        given(defectRepository.findById(1L)).willReturn(Optional.of(defect));
        given(jiraClient.getStatusCategory("TMS-5")).willReturn("done");

        DefectResponse response = jiraService.pull(1L);

        assertThat(response.status()).isEqualTo(DefectStatus.RESOLVED);
    }

    @Test
    void pull_jiraKey없으면_예외를_던진다() {
        Defect defect = new Defect("버그", null, DefectSeverity.MINOR, DefectStatus.OPEN, null);
        given(defectRepository.findById(1L)).willReturn(Optional.of(defect));

        assertThatThrownBy(() -> jiraService.pull(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("연결된 Jira 이슈가 없습니다");
    }

    @Test
    void link_기존Jira키를_결함에_연결한다() {
        Defect defect = new Defect("버그", null, DefectSeverity.MINOR, DefectStatus.OPEN, null);
        given(defectRepository.findById(1L)).willReturn(Optional.of(defect));

        DefectResponse response = jiraService.link(1L, new JiraLinkRequest("PROJ-99"));

        assertThat(response.jiraKey()).isEqualTo("PROJ-99");
        then(jiraClient).shouldHaveNoInteractions();
    }

    @Test
    void findByJiraKey_연결된결함을_역방향으로_조회한다() {
        Defect defect = new Defect("버그", null, DefectSeverity.MINOR, DefectStatus.OPEN, null);
        defect.linkJira("TMS-7");
        given(defectRepository.findByJiraKey("TMS-7")).willReturn(Optional.of(defect));

        DefectResponse response = jiraService.findByJiraKey("TMS-7");

        assertThat(response.jiraKey()).isEqualTo("TMS-7");
    }

    @Test
    void findByJiraKey_연결된결함이없으면_예외를_던진다() {
        given(defectRepository.findByJiraKey("TMS-404")).willReturn(Optional.empty());

        assertThatThrownBy(() -> jiraService.findByJiraKey("TMS-404"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("연결된 결함이 없습니다");
    }

    @Test
    void syncAll_연결된결함을_모두_동기화한다() {
        Defect d1 = new Defect("버그1", null, DefectSeverity.MAJOR, DefectStatus.OPEN, null);
        d1.linkJira("TMS-1");
        Defect d2 = new Defect("버그2", null, DefectSeverity.MINOR, DefectStatus.IN_PROGRESS, null);
        d2.linkJira("TMS-2");
        given(defectRepository.findAllByJiraKeyIsNotNull()).willReturn(List.of(d1, d2));
        given(jiraClient.getStatusCategory("TMS-1")).willReturn("indeterminate");
        given(jiraClient.getStatusCategory("TMS-2")).willReturn("done");

        JiraSyncResult result = jiraService.syncAll();

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(0);
        assertThat(d1.getStatus()).isEqualTo(DefectStatus.IN_PROGRESS);
        assertThat(d2.getStatus()).isEqualTo(DefectStatus.RESOLVED);
    }

    @Test
    void syncAll_일부실패해도_나머지는_계속_진행한다() {
        Defect d1 = new Defect("버그1", null, DefectSeverity.MAJOR, DefectStatus.OPEN, null);
        d1.linkJira("TMS-1");
        Defect d2 = new Defect("버그2", null, DefectSeverity.MINOR, DefectStatus.OPEN, null);
        d2.linkJira("TMS-2");
        given(defectRepository.findAllByJiraKeyIsNotNull()).willReturn(List.of(d1, d2));
        given(jiraClient.getStatusCategory("TMS-1")).willThrow(new InvalidRequestException("Jira 오류"));
        given(jiraClient.getStatusCategory("TMS-2")).willReturn("new");

        JiraSyncResult result = jiraService.syncAll();

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.success()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    void statusMapping_양방향_매핑이_올바르다() {
        assertThat(JiraService.toJiraCategory(DefectStatus.OPEN)).isEqualTo("new");
        assertThat(JiraService.toJiraCategory(DefectStatus.IN_PROGRESS)).isEqualTo("indeterminate");
        assertThat(JiraService.toJiraCategory(DefectStatus.RESOLVED)).isEqualTo("done");
        assertThat(JiraService.toJiraCategory(DefectStatus.CLOSED)).isEqualTo("done");

        assertThat(JiraService.fromJiraCategory("new")).isEqualTo(DefectStatus.OPEN);
        assertThat(JiraService.fromJiraCategory("indeterminate")).isEqualTo(DefectStatus.IN_PROGRESS);
        assertThat(JiraService.fromJiraCategory("done")).isEqualTo(DefectStatus.RESOLVED);
    }
}
