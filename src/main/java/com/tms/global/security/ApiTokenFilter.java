package com.tms.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 데스크톱 셸(Electron)과 백엔드가 공유하는 토큰을 검증하는 필터.
 * 로컬 바인딩(127.0.0.1)과 함께, 브라우저발 CSRF나 같은 머신의 다른 프로세스로부터
 * `/api/**`를 보호한다.
 *
 * <p>토큰이 설정되지 않은 경우(빈 값) 필터는 비활성화되어 기존 동작/테스트를 깨지 않는다.
 * 토큰은 환경변수 {@code TMS_API_TOKEN}로 주입한다.
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-TMS-Token";

    private final String expectedToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 토큰 미설정 시 검증 비활성화. /api 이외 경로는 검증하지 않는다.
        return expectedToken == null || expectedToken.isBlank()
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, expectedToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of("message", "인증 토큰이 유효하지 않습니다."));
            return;
        }
        chain.doFilter(request, response);
    }

    /** 타이밍 공격을 피하기 위한 상수 시간 비교. */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
