package com.boot.ict05_final_user.config.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTUtil {

    private static final SecretKey secretKey;
    private static final Long accessTokenExpiresIn;
    private static final Long refreshTokenExpiresIn;

    static  {
        String secretKeyString = "himynameiskimjihunmyyoutubechann";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

        accessTokenExpiresIn = 50000L * 1000; // 1시간
        refreshTokenExpiresIn = 604800L * 1000; // 7일
    }

    private static Claims parseClaims(String token) {
        // jjwt 0.12+ API: verifyWith(secretKey)
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Long toLongOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Long l)     return l;
        if (v instanceof Integer i)  return i.longValue();
        if (v instanceof Double d)   return d.longValue();
        if (v instanceof String s) {
            try { return Long.valueOf(s); } catch (NumberFormatException ignore) { return null; }
        }
        return null;
    }

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("sub", String.class);
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public static Long getStoreId(String token) {
        Object v = parseClaims(token).get("storeId");
        if (v == null) return null;
        if (v instanceof Long l)    return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Double d)  return d.longValue();
        return Long.valueOf(v.toString());
    }

    // ★ 추가: memberId
    public static Long getMemberId(String token) {
        Object v = parseClaims(token).get("memberId");
        return toLongOrNull(v);
    }

    // ★ 추가: memberName
    public static String getMemberName(String token) {
        return parseClaims(token).get("memberName", String.class);
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
    public static Boolean isValid(String token, Boolean isAccess) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (type == null) return false;

            if (isAccess && !type.equals("access")) return false;
            if (!isAccess && !type.equals("refresh")) return false;

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT(Access/Refresh) 생성
    public static String createJWT(
            String username, String role,
            Long storeId, Long memberId, String memberName,
            Boolean isAccess
    ) {

        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                .claim("sub", username)
                .claim("role", role)
                .claim("storeId", storeId)
                .claim("memberId", memberId)
                .claim("memberName", memberName)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }

}