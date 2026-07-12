package com.tms.jira.config;

/**
 * 실제 호출에 사용되는 '유효 Jira 설정' 스냅샷.
 * DB에 저장된 런타임 설정({@code jira_settings})이 있으면 그 값을, 없으면 환경변수({@link JiraProperties})를 담는다.
 */
public record JiraConfig(
        String baseUrl,
        String email,
        String apiToken,
        String projectKey,
        String webBaseUrl
) {
    public boolean isConfigured() {
        return notBlank(baseUrl) && notBlank(email) && notBlank(apiToken) && notBlank(projectKey);
    }

    /** TMS 웹 주소가 설정되어 Jira에 역링크를 남길 수 있는지 여부 */
    public boolean hasWebBaseUrl() {
        return notBlank(webBaseUrl);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 아무것도 설정되지 않은 빈 설정 (연동 비활성화 상태 표현용) */
    public static JiraConfig empty() {
        return new JiraConfig(null, null, null, null, null);
    }
}
