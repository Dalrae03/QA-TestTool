package com.tms.jira.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 런타임에 앱 설정 화면에서 입력·저장하는 Jira 연동 설정. 단일 행(싱글턴)으로만 존재한다.
 * 저장된 행이 있으면 환경변수보다 우선한다.
 */
@Entity
@Table(name = "jira_settings")
public class JiraSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String baseUrl;

    @Column(length = 200)
    private String email;

    // API 토큰은 응답으로 다시 내려주지 않고(마스킹) DB에만 보관한다.
    @Column(length = 500)
    private String apiToken;

    @Column(length = 50)
    private String projectKey;

    @Column(length = 500)
    private String webBaseUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public JiraSetting() {
    }

    public void update(String baseUrl, String email, String apiToken, String projectKey, String webBaseUrl, boolean enabled) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.apiToken = apiToken;
        this.projectKey = projectKey;
        this.webBaseUrl = webBaseUrl;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getBaseUrl() { return baseUrl; }
    public String getEmail() { return email; }
    public String getApiToken() { return apiToken; }
    public String getProjectKey() { return projectKey; }
    public String getWebBaseUrl() { return webBaseUrl; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
