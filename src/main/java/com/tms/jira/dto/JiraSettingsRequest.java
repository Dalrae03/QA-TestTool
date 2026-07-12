package com.tms.jira.dto;

import jakarta.validation.constraints.Size;

/**
 * Jira 연동 설정 저장 요청.
 * apiToken은 비워 두면(공백/null) 기존에 저장된 토큰을 그대로 유지한다 — 마스킹된 값을 다시 저장하지 않기 위함.
 */
public record JiraSettingsRequest(
        @Size(max = 500) String baseUrl,
        @Size(max = 200) String email,
        @Size(max = 500) String apiToken,
        @Size(max = 50) String projectKey,
        @Size(max = 500) String webBaseUrl,
        Boolean enabled
) {
}
