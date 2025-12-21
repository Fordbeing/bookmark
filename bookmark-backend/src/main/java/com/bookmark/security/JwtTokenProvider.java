package com.bookmark.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Access Token 有效期: 2小时
    private static final long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000L;

    // Refresh Token 有效期: 7天
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     * 
     * @param userId  用户 ID
     * @param tokenId Token ID (用于 Redis 存储)
     */
    public String generateAccessToken(Long userId, String tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenId", tokenId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成 Refresh Token
     * 
     * @param userId  用户 ID
     * @param tokenId Token ID (用于 Redis 存储)
     */
    public String generateRefreshToken(Long userId, String tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenId", tokenId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 兼容旧版本：生成单一 Token（用于邮箱登录等）
     * 
     * @deprecated 使用 generateAccessToken + generateRefreshToken 代替
     */
    @Deprecated
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("Token subject 不是用户 ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 获取 Token ID
     */
    public String getTokenIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("tokenId", String.class);
        } catch (Exception e) {
            log.warn("获取 tokenId 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 获取类型 (access/refresh)
     */
    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("type", String.class);
        } catch (Exception e) {
            log.warn("获取 token 类型失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 兼容旧版本：从 Token 获取用户名/邮箱
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            log.warn("获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 签名是否有效（不检查 Redis）
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否为 Access Token
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenTypeFromToken(token));
    }

    /**
     * 检查 Token 是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenTypeFromToken(token));
    }

    /**
     * 获取 Access Token 有效期（毫秒）
     */
    public long getAccessTokenExpiration() {
        return ACCESS_TOKEN_EXPIRATION;
    }

    /**
     * 获取 Refresh Token 有效期（毫秒）
     */
    public long getRefreshTokenExpiration() {
        return REFRESH_TOKEN_EXPIRATION;
    }
}
