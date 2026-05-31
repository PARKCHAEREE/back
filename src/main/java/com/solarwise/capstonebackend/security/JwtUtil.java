package com.solarwise.capstonebackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 * - 토큰 발급, 검증, 클레임 추출
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret-key:your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}")
    private String secretKey;

    @Value("${jwt.expiration-time:86400000}")
    private long expirationTime; // 기본값: 24시간 (밀리초)

    /**
     * 사용자 ID를 기반으로 JWT 토큰 생성
     */
    public String generateToken(String userId) {
        return generateToken(userId, null);
    }

    /**
     * 사용자 ID/role 기반 JWT 토큰 생성
     */
    public String generateToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public String extractUserId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰에서 사용자 role 추출
     */
    public String extractRole(String token) {
        try {
            Object role = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("role");

            return role == null ? null : role.toString();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 role 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰의 유효성 검증
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("토큰 만료: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 서명 키 생성
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

}

