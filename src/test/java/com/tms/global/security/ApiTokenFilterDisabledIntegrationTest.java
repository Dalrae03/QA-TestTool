package com.tms.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 토큰을 설정하지 않은 기본 상태에서는 토큰 검증이 비활성화되어
 * 기존 동작(헤더 없이 /api 접근 가능)이 유지되는지 검증한다.
 * 보안 헤더는 토큰과 무관하게 항상 적용되어야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTokenFilterDisabledIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void allowsApiRequestWithoutTokenWhenDisabled() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/testcases", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void stillAddsSecurityHeadersWhenTokenDisabled() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/testcases", String.class);

        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }
}
