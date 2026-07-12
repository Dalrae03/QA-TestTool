package com.tms.jira.service;

import com.tms.jira.config.JiraConfig;
import com.tms.jira.config.JiraProperties;
import com.tms.jira.dto.JiraSettingsRequest;
import com.tms.jira.dto.JiraSettingsView;
import com.tms.jira.entity.JiraSetting;
import com.tms.jira.repository.JiraSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jira 연동 설정의 단일 진입점.
 * 저장된 런타임 설정(DB)이 있으면 그 값을, 없으면 환경변수({@link JiraProperties})를 유효 설정으로 제공한다.
 */
@Service
@Transactional(readOnly = true)
public class JiraSettingsService {

    private final JiraSettingRepository repository;
    private final JiraProperties envProperties;

    public JiraSettingsService(JiraSettingRepository repository, JiraProperties envProperties) {
        this.repository = repository;
        this.envProperties = envProperties;
    }

    /** 실제 호출에 사용할 유효 설정. 저장 행이 없으면 환경변수, 행이 비활성이면 빈 설정을 반환한다. */
    public JiraConfig current() {
        JiraSetting saved = repository.findFirstByOrderByIdAsc().orElse(null);
        if (saved == null) {
            return new JiraConfig(
                    envProperties.baseUrl(), envProperties.email(),
                    envProperties.apiToken(), envProperties.projectKey(), envProperties.webBaseUrl());
        }
        if (!saved.isEnabled()) {
            return JiraConfig.empty();
        }
        return new JiraConfig(
                saved.getBaseUrl(), saved.getEmail(),
                saved.getApiToken(), saved.getProjectKey(), saved.getWebBaseUrl());
    }

    /** 설정 화면 표시용 — 토큰은 마스킹(존재 여부만). */
    public JiraSettingsView view() {
        JiraSetting saved = repository.findFirstByOrderByIdAsc().orElse(null);
        if (saved == null) {
            // 저장 전: 환경변수 기반 설정을 그대로 보여준다(있다면).
            return JiraSettingsView.of(current(), true);
        }
        JiraConfig cfg = new JiraConfig(
                saved.getBaseUrl(), saved.getEmail(),
                saved.getApiToken(), saved.getProjectKey(), saved.getWebBaseUrl());
        return JiraSettingsView.of(cfg, saved.isEnabled());
    }

    /** 설정 저장(업서트). apiToken이 비어 있으면 기존 토큰을 유지한다. */
    @Transactional
    public JiraSettingsView save(JiraSettingsRequest request) {
        JiraSetting saved = repository.findFirstByOrderByIdAsc().orElseGet(JiraSetting::new);
        String token = (request.apiToken() != null && !request.apiToken().isBlank())
                ? request.apiToken().trim()
                : saved.getApiToken();
        boolean enabled = request.enabled() == null || request.enabled();
        saved.update(
                trimToNull(request.baseUrl()),
                trimToNull(request.email()),
                token,
                trimToNull(request.projectKey()),
                trimToNull(request.webBaseUrl()),
                enabled);
        repository.save(saved);
        return view();
    }

    /**
     * 저장된 값 위에 요청으로 들어온 값을 덧씌운 '테스트용' 설정을 만든다.
     * 사용자가 화면에 입력한 값(아직 저장 전)으로 바로 연결을 검증할 수 있게 한다.
     * apiToken이 비어 있으면 저장된 토큰을 사용한다.
     */
    public JiraConfig resolveForTest(JiraSettingsRequest request) {
        if (request == null) {
            return current();
        }
        JiraConfig base = current();
        String token = (request.apiToken() != null && !request.apiToken().isBlank())
                ? request.apiToken().trim()
                : base.apiToken();
        return new JiraConfig(
                firstNonBlank(trimToNull(request.baseUrl()), base.baseUrl()),
                firstNonBlank(trimToNull(request.email()), base.email()),
                token,
                firstNonBlank(trimToNull(request.projectKey()), base.projectKey()),
                firstNonBlank(trimToNull(request.webBaseUrl()), base.webBaseUrl()));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
