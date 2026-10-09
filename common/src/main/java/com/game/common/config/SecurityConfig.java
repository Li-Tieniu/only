package com.game.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.common.response.ApiResult;
import com.game.common.security.JwtAuthenticationFilter;
import com.game.common.security.JwtUtil;
import com.game.common.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 配置：无状态 JWT 认证 + 基于角色的接口权限（app / console 共用）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public SecurityConfig(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + JWT：不需要 CSRF，也不使用 Session
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 注册、登录、错误页：匿名可访问
                        .requestMatchers("/user/register", "/user/login", "/error").permitAll()
                        // 商品浏览 / 查询（GET）：匿名可访问
                        .requestMatchers(HttpMethod.GET, "/gametrading/**").permitAll()
                        // 商品新增 / 修改 / 删除（POST）：仅管理员
                        .requestMatchers("/gametrading/**").hasRole("ADMIN")
                        // 订单等需要登录的接口：预留认证规则
                        .requestMatchers("/order/**").authenticated()
                        // 其余未明确开放的接口：默认需要登录
                        .anyRequest().authenticated()
                )
                // 未登录 / 无权限时也返回统一的 ApiResult
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "没有权限访问该接口"))
                )
                // JWT 过滤器放在用户名密码认证过滤器之前
                // 这里用 new 而不是交给 Spring 管理，避免 Servlet 容器把它再注册一遍
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userMapper),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 直接写出统一响应体。过滤器链里拿不到 GlobalExceptionHandler，只能手动写 JSON。
     */
    private static void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResult.fail(code, message));
    }
}
