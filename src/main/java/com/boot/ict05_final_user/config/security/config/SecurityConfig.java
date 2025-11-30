package com.boot.ict05_final_user.config.security.config;

import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.domain.user.entity.UserRoleType;
import com.boot.ict05_final_user.config.security.filter.JWTFilter;
import com.boot.ict05_final_user.config.security.filter.LoginFilter;
import com.boot.ict05_final_user.config.security.handler.RefreshTokenLogoutHandler;
import com.boot.ict05_final_user.config.security.auth.EmailAuthenticationProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;

    @Qualifier("LoginSuccessHandler")
    private final AuthenticationSuccessHandler loginSuccessHandler;

    // AuthenticationManager (커스텀 LoginFilter 용)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 권한 계층 (ADMIN > USER)
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
                .build();
    }

    // CORS (credentials 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 개발/운영 도메인 추가
        cfg.setAllowedOrigins(List.of(
                "capacitor://localhost",              // 안드로이드 앱 기본 주소
                "http://localhost",                   // 안드로이드 앱 대체 주소
                "https://toastlab.duckdns.org",       // ← 추가
                "http://toastlab.duckdns.org",        // ← 추가
                "https://toastlabadmin.duckdns.org",  // ← 추가
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8082"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","X-Refresh-Token"));
        cfg.setExposedHeaders(List.of("Authorization","Set-Cookie"));
        cfg.setAllowCredentials(true);
        // 쿠키 수신 시 브라우저가 확인 가능한 헤더
        cfg.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    // 보안 필터 체인
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, EmailAuthenticationProvider provider) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 인가 규칙
        http.authorizeHttpRequests(auth -> auth
                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers("/api/auth/**").permitAll()

                // 공개 엔드포인트
                .requestMatchers("/login").permitAll()
                .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/exist", "/me","/API/**", "/dashboard/**", "/join", "/member/exist-email", "/member","/API/menu/**", "/api/customer-orders/**", "/register").permitAll()

                .requestMatchers(HttpMethod.POST, "/fcm/notice/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/fcm/token", "/fcm/topic/**", "/fcm/send/**").authenticated()

                // 인증 필요
                .requestMatchers(HttpMethod.PATCH, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/**").hasRole(UserRoleType.USER.name())
                .requestMatchers(HttpMethod.DELETE, "/**").hasRole(UserRoleType.USER.name())

                .anyRequest().authenticated()
        );

        // 예외 처리
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
        );

        // 무상태 세션
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // JWT 필터: UsernamePasswordAuthenticationFilter 보다 앞에서 토큰 검증
        http.addFilterBefore(new JWTFilter(), UsernamePasswordAuthenticationFilter.class);

        // 커스텀 로그인 필터: /login 엔드포인트에서 인증 처리 + 성공시 핸들러
        http.addFilterAt(
                new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler),
                UsernamePasswordAuthenticationFilter.class
        );

        // 로그아웃 (Refresh Token 정리)
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService))
        );

        // 필요 시, H2 콘솔/iframe 등 사용할 때만
        // http.headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        http.authenticationProvider(provider);

        return http.build();
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
