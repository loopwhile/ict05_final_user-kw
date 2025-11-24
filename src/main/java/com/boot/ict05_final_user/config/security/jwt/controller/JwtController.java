package com.boot.ict05_final_user.config.security.jwt.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.boot.ict05_final_user.config.security.jwt.dto.JWTResponseDTO;
import com.boot.ict05_final_user.config.security.jwt.dto.RefreshRequestDTO;
import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class JwtController {

    private final JwtService jwtService;

    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // (기존) 쿠키→헤더 교환 유지
    @PostMapping(value = "/jwt/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JWTResponseDTO jwtExchangeApi(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return jwtService.cookie2Header(request, response);
    }

    // ✅ 헤더로 받은 Refresh 토큰으로 Access 재발급 (회전 포함)
    //    헤더 키: X-Refresh-Token
    @PostMapping(value = "/jwt/refresh")
    public JWTResponseDTO jwtRefreshHeaderApi(
            @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(refreshToken);
        return jwtService.refreshRotate(dto);
    }
}
