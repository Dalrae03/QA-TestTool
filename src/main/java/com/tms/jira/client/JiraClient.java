package com.tms.jira.client;

import com.tms.global.exception.InvalidRequestException;
import com.tms.jira.config.JiraProperties;
import com.tms.jira.dto.JiraIssueInfo;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class JiraClient {

    private final JiraProperties props;
    private final RestClient restClient;

    public JiraClient(JiraProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl() != null ? props.baseUrl() : "https://placeholder.atlassian.net")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth(props))
                .build();
    }

    /** Jira 이슈 생성. key(e.g. "TMS-1") 반환 */
    @SuppressWarnings("unchecked")
    public String createIssue(String summary, String description, String issueType, String priority) {
        requireConfigured();
        Map<String, Object> body = Map.of(
                "fields", Map.of(
                        "project", Map.of("key", props.projectKey()),
                        "summary", summary,
                        "description", toAdf(description),
                        "issuetype", Map.of("name", issueType),
                        "priority", Map.of("name", priority)
                )
        );
        try {
            Map<String, Object> response = restClient.post()
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
        requireConfigured();
        try {
            Map<String, Object> issue = restClient.get()
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
        requireConfigured();
        try {
            Map<String, Object> issue = restClient.get()
                    .uri("/rest/api/3/issue/{key}?fields=summary,status", issueKey)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            String summary = (String) fields.get("summary");
            Map<String, Object> status = (Map<String, Object>) fields.get("status");
            String statusName = (String) status.get("name");
            Map<String, Object> category = (Map<String, Object>) status.get("statusCategory");
            String categoryKey = (String) category.get("key");
            return new JiraIssueInfo(issueKey, summary, statusName, categoryKey, browseUrl(issueKey));
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
        requireConfigured();
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
            restClient.post()
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
        requireConfigured();
        try {
            Map<String, Object> transitionsResp = restClient.get()
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

            restClient.post()
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
        requireConfigured();
        Map<String, Object> body = Map.of(
                "fields", Map.of(
                        "summary", summary,
                        "description", toAdf(description)
                )
        );
        try {
            restClient.put()
                    .uri("/rest/api/3/issue/{key}", issueKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new InvalidRequestException("Jira 이슈 수정 실패: " + e.getResponseBodyAsString());
        }
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new InvalidRequestException(
                    "Jira 연동이 설정되지 않았습니다. JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_PROJECT_KEY 환경변수를 확인하세요.");
        }
    }

    /** 이슈 key로 Jira 웹 브라우즈 URL 생성 (baseUrl 미설정 시 null) */
    private String browseUrl(String issueKey) {
        String base = props.baseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.replaceAll("/+$", "") + "/browse/" + issueKey;
    }

    private static String buildBasicAuth(JiraProperties props) {
        if (props.email() == null || props.apiToken() == null) return "";
        String credentials = props.email() + ":" + props.apiToken();
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
