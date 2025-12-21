package com.bookmark.security;

import com.bookmark.entity.User;
import com.bookmark.mapper.UserMapper;
import com.bookmark.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            log.debug("Processing request: {}", request.getRequestURI());

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                String tokenId = tokenProvider.getTokenIdFromToken(jwt);
                String tokenType = tokenProvider.getTokenTypeFromToken(jwt);

                // 处理旧版 Token（没有 tokenId 的 Token）
                if (tokenId == null) {
                    // 兼容旧版本：直接使用用户名获取用户
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                    filterChain.doFilter(request, response);
                    return;
                }

                // 只允许 Access Token 访问 API（除了刷新接口）
                boolean isRefreshEndpoint = request.getRequestURI().contains("/auth/refresh");
                if ("refresh".equals(tokenType) && !isRefreshEndpoint) {
                    log.warn("尝试使用 Refresh Token 访问 API: userId={}", userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "INVALID_TOKEN", "请使用 Access Token");
                    return;
                }

                // 验证 Token 是否在 Redis 中存在
                boolean tokenValid;
                if ("access".equals(tokenType)) {
                    tokenValid = tokenService.validateAccessToken(userId, tokenId);
                } else {
                    tokenValid = tokenService.validateRefreshToken(userId, tokenId);
                }

                if (!tokenValid) {
                    log.warn("Token 已被注销或过期: userId={}, tokenId={}", userId, tokenId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "TOKEN_REVOKED", "Token 已失效，请重新登录");
                    return;
                }

                // 检查用户状态
                User user = userMapper.selectById(userId);
                if (user == null) {
                    log.warn("用户不存在: userId={}", userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "USER_NOT_FOUND", "用户不存在");
                    return;
                }

                if (user.getStatus() != null && user.getStatus() == 0) {
                    log.warn("用户已被禁用: userId={}", userId);
                    // 注销该用户所有 Token
                    tokenService.revokeAllUserTokens(userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "USER_DISABLED", "账户已被禁用，请联系管理员");
                    return;
                }

                // 检查是否需要刷新 Token（滑动过期）
                if ("access".equals(tokenType) && tokenService.shouldRefreshAccessToken(userId, tokenId)) {
                    tokenService.refreshAccessToken(userId, tokenId);
                    log.debug("自动刷新 Access Token: userId={}", userId);
                }

                // 设置认证信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set successfully for userId: {}", userId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status,
            String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("code", status);
        body.put("error", errorCode);
        body.put("message", message);

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
