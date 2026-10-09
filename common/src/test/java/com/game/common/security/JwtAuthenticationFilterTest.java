package com.game.common.security;

import com.game.common.user.entity.UserEntity;
import com.game.common.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 过滤器单元测试：重点是"用户被禁用/删除后，未过期的旧 Token 也不能再访问"。
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "only-game-trading-platform-jwt-secret-key-2026";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3600);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("正常用户：认证通过，权限取数据库中最新的角色")
    void shouldAuthenticateEnabledUserWithLatestRole() throws Exception {
        MockHttpServletRequest request = requestWithToken(jwtUtil.generateToken(1L, "tieniu", "USER"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "tieniu", "ADMIN", UserEntity.STATUS_ENABLED));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
        });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("用户被禁用：Token 未过期也不能访问")
    void shouldRejectDisabledUserEvenWithValidToken() throws Exception {
        MockHttpServletRequest request = requestWithToken(jwtUtil.generateToken(2L, "tieniu", "USER"));
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tieniu", "USER", UserEntity.STATUS_DISABLED));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("用户已被删除：Token 失效")
    void shouldRejectDeletedUser() throws Exception {
        MockHttpServletRequest request = requestWithToken(jwtUtil.generateToken(3L, "gone", "USER"));
        when(userMapper.selectById(3L)).thenReturn(null);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("非法 Token：不设置认证信息")
    void shouldRejectInvalidToken() throws Exception {
        MockHttpServletRequest request = requestWithToken("not-a-real-token");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("没有 Token：保持未认证")
    void shouldStayUnauthenticatedWithoutToken() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static UserEntity user(Long id, String username, String role, int status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
