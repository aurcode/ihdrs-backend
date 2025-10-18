// JwtUtil.java - Jwt工具类
package com.ihdrs.backend.common.utils;

import com.ihdrs.backend.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        // 确保密钥长度足够（至少256位）
        String secret = jwtConfig.getSecret();
        if (secret.length() < 32) {
            secret = secret + "0123456789abcdef".repeat((32 - secret.length()) / 16 + 1);
            secret = secret.substring(0, 32);
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration() * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT Token解析失败: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("JWT Token过期检查失败: {}", e.getMessage());
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !isTokenExpired(token) && claims.getSubject() != null;
        } catch (Exception e) {
            log.error("JWT Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从Token获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            return null;
        } catch (Exception e) {
            log.error("从Token获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("从Token获取角色失败: {}", e.getMessage());
            return null;
        }
    }
}