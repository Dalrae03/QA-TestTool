package com.tms.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 토큰이 설정된 상태에서 {@link ApiTokenFilter}와 {@link SecurityHeadersFilter}가
 * 실제 서블릿 체인에서 동작하는지 검증한다.
 * FilterRegistrationBean 으로 등록한 필터는 MockMvc에 자동 포함되지 않으므로
 * 실제 내장 Tomcat(RANDOM_PORT) + TestRestTemplate 로 검증한다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "tms.security.api-token=test-secret-token"
)
class ApiTokenFilterIntegrationTest {

    private static final String VALID_TOKEN = "test-secret-token";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void rejectsApiRequestWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/testcases", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsApiRequestWithWrongToken() {
        ResponseEntity<String> response = getWithToken("/api/testcases", "wrong-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsApiRequestWithCorrectToken() {
        ResponseEntity<String> response = getWithToken("/api/testcases", VALID_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addsSecurityHeadersEvenOnRejectedResponse() {
        HttpHeaders headers = restTemplate.getForEntity("/api/testcases", String.class).getHeaders();

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(headers.getFirst("Content-Security-Policy")).contains("default-src 'none'");
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-TMS-Token", token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
