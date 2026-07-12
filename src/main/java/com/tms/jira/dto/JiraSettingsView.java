package com.tms.jira.dto;

import com.tms.jira.config.JiraConfig;

/**
 * 설정 화면에 내려주는 현재 Jira 설정 상태. 보안을 위해 apiToken 값 자체는 내려주지 않고
 * 저장 여부({@code hasToken})만 표시한다.
 */
public record JiraSettingsView(
        String baseUrl,
        String email,
        String projectKey,
        String webBaseUrl,
        boolean hasToken,
        boolean configured,
        boolean enabled
) {
    public static JiraSettingsView of(JiraConfig cfg, boolean enabled) {
        return new JiraSettingsView(
                cfg.baseUrl(),
                cfg.email(),
                cfg.projectKey(),
                cfg.webBaseUrl(),
                cfg.apiToken() != null && !cfg.apiToken().isBlank(),
                cfg.isConfigured() && enabled,
                enabled
        );
    }
}
