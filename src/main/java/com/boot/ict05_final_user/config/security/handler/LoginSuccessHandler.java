package com.boot.ict05_final_user.config.security.handler;

import com.boot.ict05_final_user.domain.user.service.UserReadService;
import com.boot.ict05_final_user.domain.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.config.security.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Qualifier("LoginSuccessHandler")
public class LoginSuccessHandler implements AuthenticationSuccessHandler {


    private final JwtService jwtService;
    private final UserReadService userReadService;

    public LoginSuccessHandler(JwtService jwtService, UserReadService userReadService) {
        this.jwtService = jwtService;
        this.userReadService = userReadService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // username, role
        String username =  authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        Long storeId = userReadService.findStoreIdByUsername(username);
        Long memberId = userReadService.findMemberIdByUsername(username);
        String memberName = userReadService.findMemberNameByUsername(username);

        String storeName = userReadService.findStoreNameByUsername(username);

        // JWT(Access/Refresh) 발급
        String accessToken = JWTUtil.createJWT(username, role, storeId, memberId, memberName, true);
        String refreshToken = JWTUtil.createJWT(username, role, storeId, memberId, memberName, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

        // 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken,
                memberName != null ? memberName : "",
                storeName != null ? storeName : "");
        response.getWriter().write(json);
        response.getWriter().flush();
    }

}