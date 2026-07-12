package com.tms.jira.client;

import com.tms.global.exception.InvalidRequestException;
import com.tms.jira.config.JiraConfig;
import com.tms.jira.dto.JiraConnectionTestResult;
import com.tms.jira.dto.JiraIssueInfo;
import com.tms.jira.service.JiraSettingsService;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class JiraClient {

    private final JiraSettingsService settingsService;

    public JiraClient(JiraSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** Jira 이슈 생성. key(e.g. "TMS-1") 반환 */
    @SuppressWarnings("unchecked")
    public String createIssue(String summary, String description, String issueType, String priority) {
        JiraConfig cfg = requireConfigured();
        Map<String, Object> body = Map.of(
                "fields", Map.of(
                        "project", Map.of("key", cfg.projectKey()),
                        "summary", summary,
                        "description", toAdf(description),
                        "issuetype", Map.of("name", issueType),
                        "priority", Map.of("name", priority)
                )
        );
        try {
            Map<String, Object> response = client(cfg).post()
                    .uri("/rest/api/3/issue")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return (String) response.get("key");
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 이슈 생성 실패: " + e.getResponseBodyAsString());
        }
    }

    /** Jira 이슈 상태 카테고리 조회 ("new" | "indeterminate" | "done") */
    @SuppressWarnings("unchecked")
    public String getStatusCategory(String issueKey) {
        JiraConfig cfg = requireConfigured();
        try {
            Map<String, Object> issue = client(cfg).get()
                    .uri("/rest/api/3/issue/{key}?fields=status", issueKey)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            Map<String, Object> status = (Map<String, Object>) fields.get("status");
            Map<String, Object> category = (Map<String, Object>) status.get("statusCategory");
            return (String) category.get("key");
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 이슈 조회 실패: " + e.getResponseBodyAsString());
        }
    }

    /** Jira 이슈 핵심 정보(요약/상태/링크) 조회. 추적성 화면 표시에 사용 */
    @SuppressWarnings("unchecked")
    public JiraIssueInfo getIssue(String issueKey) {
        JiraConfig cfg = requireConfigured();
        try {
            Map<String, Object> issue = client(cfg).get()
                    .uri("/rest/api/3/issue/{key}?fields=summary,status", issueKey)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            String summary = (String) fields.get("summary");
            Map<String, Object> status = (Map<String, Object>) fields.get("status");
            String statusName = (String) status.get("name");
            Map<String, Object> category = (Map<String, Object>) status.get("statusCategory");
            String categoryKey = (String) category.get("key");
            return new JiraIssueInfo(issueKey, summary, statusName, categoryKey, browseUrl(cfg, issueKey));
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 이슈 조회 실패: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Jira 이슈에 TMS 항목을 가리키는 원격 링크(remote link)를 추가한다.
     * TMS→Jira 방향의 추적 연결로, Jira 화면에서도 연결된 TMS 항목을 볼 수 있게 한다.
     * webBaseUrl이 설정되지 않았으면 아무 동작도 하지 않는다(best-effort).
     */
    public void addRemoteLink(String issueKey, String url, String title) {
        JiraConfig cfg = requireConfigured();
        if (url == null || url.isBlank()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "object", Map.of(
                        "url", url,
                        "title", title != null ? title : url
                )
        );
        try {
            client(cfg).post()
                    .uri("/rest/api/3/issue/{key}/remotelink", issueKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 원격 링크 추가 실패: " + e.getResponseBodyAsString());
        }
    }

    /** 이슈를 targetCategory("new"|"indeterminate"|"done")에 해당하는 상태로 전환 */
    @SuppressWarnings("unchecked")
    public void transitionToCategory(String issueKey, String targetCategory) {
        JiraConfig cfg = requireConfigured();
        try {
            Map<String, Object> transitionsResp = client(cfg).get()
                    .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> transitions = (List<Map<String, Object>>) transitionsResp.get("transitions");
            String transitionId = transitions.stream()
                    .filter(t -> {
                        Map<String, Object> to = (Map<String, Object>) t.get("to");
                        Map<String, Object> cat = (Map<String, Object>) to.get("statusCategory");
                        return targetCategory.equals(cat.get("key"));
                    })
                    .map(t -> (String) t.get("id"))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException(
                            "Jira에서 '" + targetCategory + "' 카테고리로의 전환을 찾을 수 없습니다. key=" + issueKey));

            client(cfg).post()
                    .uri("/rest/api/3/issue/{key}/transitions", issueKey)
                    .body(Map.of("transition", Map.of("id", transitionId)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 상태 전환 실패: " + e.getResponseBodyAsString());
        }
    }

    /** 이슈 요약/설명 업데이트 */
    public void updateIssue(String issueKey, String summary, String description) {
        JiraConfig cfg = requireConfigured();
        Map<String, Object> body = Map.of(
                "fields", Map.of(
                        "summary", summary,
                        "description", toAdf(description)
                )
        );
        try {
            client(cfg).put()
                    .uri("/rest/api/3/issue/{key}", issueKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 이슈 수정 실패: " + e.getResponseBodyAsString());
        }
    }

    /**
     * 주어진 설정으로 Jira에 실제 연결을 시도해 자격 증명과 프로젝트 접근을 검증한다.
     * 인증은 {@code /myself}, 프로젝트 존재/권한은 {@code /project/{key}}로 확인한다.
     */
    @SuppressWarnings("unchecked")
    public JiraConnectionTestResult testConnection(JiraConfig cfg) {
        if (!cfg.isConfigured()) {
            throw new InvalidRequestException(
                    "Jira 연동 필수 항목(Base URL, 이메일, API 토큰, 프로젝트 키)을 모두 입력하세요.");
        }
        RestClient rest = client(cfg);
        String displayName;
        String accountEmail;
        try {
            Map<String, Object> me = rest.get()
                    .uri("/rest/api/3/myself")
                    .retrieve()
                    .body(Map.class);
            displayName = me != null ? (String) me.get("displayName") : null;
            accountEmail = me != null ? (String) me.get("emailAddress") : null;
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException(authFailureMessage(e));
        } catch (ResourceAccessException e) {
            throw new InvalidRequestException("Jira 서버에 연결할 수 없습니다. Base URL을 확인하세요: " + rootMessage(e));
        }

        String projectName;
        try {
            Map<String, Object> project = rest.get()
                    .uri("/rest/api/3/project/{key}", cfg.projectKey())
                    .retrieve()
                    .body(Map.class);
            projectName = project != null ? (String) project.get("name") : cfg.projectKey();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new InvalidRequestException(
                        "인증은 성공했지만 프로젝트 키 '" + cfg.projectKey() + "'를 찾을 수 없습니다. 프로젝트 키를 확인하세요.");
            }
            throw new InvalidRequestException("Jira 프로젝트 확인 실패: " + e.getResponseBodyAsString());
        }

        return new JiraConnectionTestResult(true, displayName, accountEmail, projectName,
                "Jira 연결에 성공했습니다.");
    }

    private JiraConfig requireConfigured() {
        JiraConfig cfg = settingsService.current();
        if (!cfg.isConfigured()) {
            throw new InvalidRequestException(
                    "Jira 연동이 설정되지 않았습니다. 설정 화면에서 Base URL, 이메일, API 토큰, 프로젝트 키를 입력하세요.");
        }
        return cfg;
    }

    /** 주어진 설정으로 인증 헤더가 실린 RestClient를 만든다(호출 시점의 설정 반영). */
    private RestClient client(JiraConfig cfg) {
        return RestClient.builder()
                .baseUrl(cfg.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth(cfg))
                .build();
    }

    private static String authFailureMessage(HttpClientErrorException e) {
        int code = e.getStatusCode().value();
        if (code == 401) {
            return "Jira 인증에 실패했습니다(401). 이메일과 API 토큰을 확인하세요.";
        }
        if (code == 403) {
            return "Jira 접근이 거부되었습니다(403). 계정 권한을 확인하세요.";
        }
        return "Jira 연결 실패(HTTP " + code + "): " + e.getResponseBodyAsString();
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    /** 이슈 key로 Jira 웹 브라우즈 URL 생성 (baseUrl 미설정 시 null) */
    private static String browseUrl(JiraConfig cfg, String issueKey) {
        String base = cfg.baseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.replaceAll("/+$", "") + "/browse/" + issueKey;
    }

    private static String buildBasicAuth(JiraConfig cfg) {
        if (cfg.email() == null || cfg.apiToken() == null) return "";
        String credentials = cfg.email() + ":" + cfg.apiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /** Jira ADF(Atlassian Document Format) plain text 변환 */
    private static Map<String, Object> toAdf(String text) {
        String content = text != null ? text : "";
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(
                        Map.of("type", "paragraph",
                                "content", List.of(
                                        Map.of("type", "text", "text", content)
                                ))
                )
        );
    }
}
