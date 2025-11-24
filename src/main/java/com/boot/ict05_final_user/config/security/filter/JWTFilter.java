package com.boot.ict05_final_user.config.security.filter;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.boot.ict05_final_user.config.security.jwt.JWTUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JWTFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //System.out.println("[JWTFilter] Path: " + request.getRequestURI() + ", Auth Header: " + request.getHeader("Authorization"));

        // 공개 경로 (JWT 검사 건너뛰기)
        String requestURI = request.getRequestURI();
        // 컨텍스트 경로를 동적으로 가져와서 비교
        String contextPath = request.getContextPath(); // /user
        if (requestURI.equals(contextPath) || // /user
                requestURI.equals(contextPath + "/") || // /user/
                requestURI.equals(contextPath + "/index.html") ||
                requestURI.equals(contextPath + "/vite.svg") ||
                requestURI.equals(contextPath + "/manifest.json") ||
                requestURI.equals(contextPath + "/robots.txt") ||
                requestURI.startsWith(contextPath + "/assets/") ||
                requestURI.startsWith(contextPath + "/api/auth/") || // /user/api/auth/
                requestURI.equals(contextPath + "/login") ||
                requestURI.equals(contextPath + "/jwt/exchange") ||
                requestURI.equals(contextPath + "/jwt/refresh") ||
                (request.getMethod().equals("POST") && (
                        requestURI.equals(contextPath + "/exist") ||
                        requestURI.equals(contextPath) || // POST /user
                        requestURI.equals(contextPath + "/me") ||
                        requestURI.startsWith(contextPath + "/dashboard/") ||
                        requestURI.equals(contextPath + "/join") ||
                        requestURI.equals(contextPath + "/member/exist-email") ||
                        requestURI.equals(contextPath + "/member")
                ))
        ) {
            System.out.println("[JWTFilter] Public path, skipping JWT check: " + requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new ServletException("Invalid JWT token");
        }

        // 토큰 파싱
        String accessToken = authorization.split(" ")[1];

        if (JWTUtil.isValid(accessToken, true)) {

            String username = JWTUtil.getUsername(accessToken);
            String role = JWTUtil.getRole(accessToken);
            Long storeId = JWTUtil.getStoreId(accessToken);
            Long memberId = JWTUtil.getMemberId(accessToken);
            String memberName = JWTUtil.getMemberName(accessToken);

            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

            AppUser principal = new AppUser(username, storeId, memberId, memberName, authorities);
            Authentication auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"토큰 만료 또는 유효하지 않은 토큰\"}");
            return;
        }

    }

}