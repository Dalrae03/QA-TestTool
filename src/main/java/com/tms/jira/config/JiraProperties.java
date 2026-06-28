package com.tms.jira.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jira")
public record JiraProperties(
        String baseUrl,
        String email,
        String apiToken,
        String projectKey,
        String webBaseUrl
) {
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && email != null && !email.isBlank()
                && apiToken != null && !apiToken.isBlank()
                && projectKey != null && !projectKey.isBlank();
    }

    /** TMS 웹 주소가 설정되어 Jira에 역링크를 남길 수 있는지 여부 */
    public boolean hasWebBaseUrl() {
        return webBaseUrl != null && !webBaseUrl.isBlank();
    }
}
