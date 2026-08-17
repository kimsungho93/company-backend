package com.ksh.companybackend.config;

import com.ksh.companybackend.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// 필터 계층의 401 은 @RestControllerAdvice 를 타지 않아 본문이 비어 나간다. 여기서 직접 쓴다.
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = switch (String.valueOf(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE))) {
            case JwtAuthenticationFilter.ERROR_TOKEN_EXPIRED -> new ErrorResponse("TOKEN_EXPIRED", "인증이 만료되었습니다.");
            case JwtAuthenticationFilter.ERROR_INVALID_TOKEN -> new ErrorResponse("INVALID_TOKEN", "유효하지 않은 인증입니다.");
            default -> new ErrorResponse("UNAUTHENTICATED", "로그인이 필요합니다.");
        };
        objectMapper.writeValue(response.getWriter(), body);
    }
}
