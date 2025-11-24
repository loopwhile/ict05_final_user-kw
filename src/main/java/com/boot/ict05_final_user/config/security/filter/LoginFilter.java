package com.boot.ict05_final_user.config.security.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginFilter extends AbstractAuthenticationProcessingFilter {

    public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    private static final RequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/login");

    private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;
    private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;

    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    public LoginFilter(AuthenticationManager authenticationManager,
                       AuthenticationSuccessHandler authenticationSuccessHandler) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        String contentType = request.getContentType();
        String principal;   // email 또는 username
        String password;
        String loginType;   // "HQ" | "Store" | null

        if (contentType != null && contentType.contains("application/json")) {
            // JSON 본문 처리
            Map<String, String> loginMap;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ServletInputStream inputStream = request.getInputStream();
                String body = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                loginMap = objectMapper.readValue(body, new TypeReference<>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 새 포맷: email/password/loginType 우선
            String email = trimOrNull(loginMap.get("email"));
            String username = trimOrNull(loginMap.get("username")); // 구포맷 호환
            principal = StringUtils.hasText(email) ? email : nvl(username, "");
            password = nvl(loginMap.get(passwordParameter), "");
            loginType = trimOrNull(loginMap.get("loginType")); // 선택
        } else {
            // 폼 파라미터 처리(x-www-form-urlencoded/multipart)
            String email = trimOrNull(request.getParameter("email"));
            String username = trimOrNull(request.getParameter(usernameParameter));
            principal = StringUtils.hasText(email) ? email : nvl(username, "");
            password = nvl(request.getParameter(passwordParameter), "");
            loginType = trimOrNull(request.getParameter("loginType")); // 선택
        }

        if (!StringUtils.hasText(principal) || !StringUtils.hasText(password)) {
            throw new AuthenticationServiceException("로그인 파라미터가 올바르지 않습니다.");
        }

        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(principal, password);

        // loginType이 있으면 Provider가 분기할 수 있게 details에 담아 전달
        setDetails(request, authRequest, loginType);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected void setDetails(HttpServletRequest request,
                              UsernamePasswordAuthenticationToken authRequest,
                              String loginType) {
        if (StringUtils.hasText(loginType)) {
            authRequest.setDetails(loginType);
        } else {
            // 기존 방식 유지
            authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
    }

    // ===== helpers =====
    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private static String nvl(String s, String def) {
        return s == null ? def : s;
    }
}
