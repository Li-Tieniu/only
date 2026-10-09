package com.game.common.security;

import com.game.common.user.entity.UserEntity;
import com.game.common.user.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：请求进入 Controller 前解析 Authorization: Bearer &lt;token&gt;，
 * 验证通过后把当前用户（含角色）放入 Spring Security 的 SecurityContext。
 * 每次请求都会回查一次用户：用户被禁用或删除后，旧 Token 立即失效。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            authenticate(header.substring(TOKEN_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 解析 Token 并校验用户当前状态。
     * 任何一步不通过都不写入认证信息，后面由 SecurityConfig 统一返回 401。
     */
    private void authenticate(String token) {
        SecurityContextHolder.clearContext();
        try {
            LoginUser loginUser = jwtUtil.parseToken(token);
            // Token 里是签发时的信息，这里回查数据库：用户被禁用或删除后，旧 Token 不能继续用
            UserEntity user = userMapper.selectById(loginUser.getId());
            if (user == null) {
                log.warn("Token 对应用户不存在，拒绝访问, userId={}", loginUser.getId());
                return;
            }
            if (!Integer.valueOf(UserEntity.STATUS_ENABLED).equals(user.getStatus())) {
                log.warn("账号已被禁用，拒绝访问, userId={}, username={}", user.getId(), user.getUsername());
                return;
            }

            // Spring Security 约定：权限名前缀 ROLE_；角色以数据库最新值为准，改角色不用等 Token 过期
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            // 只记录失败原因，绝不打印 Token 原文
            log.warn("JWT 校验失败，拒绝访问: {}", e.getMessage());
        }
    }
}
