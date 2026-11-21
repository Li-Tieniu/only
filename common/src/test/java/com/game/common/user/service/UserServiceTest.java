package com.game.common.user.service;

import com.game.common.security.JwtUtil;
import com.game.common.user.dto.UserDTO;
import com.game.common.user.dto.UserLoginDTO;
import com.game.common.user.entity.UserEntity;
import com.game.common.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    final UserMapper mapper = mock(UserMapper.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    final UserService service = new UserService(mapper, encoder,
            new JwtUtil("test-only-secret-never-used-by-app-12345", 3600));

    @Test void registeredPasswordRetainsWhitespaceAndCanLoginUnchanged() {
        var dto = new UserDTO(); dto.setUsername(" alice "); dto.setPassword(" secret ");
        when(mapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0); saved.setId(1L);
            assertThat(saved.getUsername()).isEqualTo("alice");
            assertThat(saved.getRole()).isEqualTo("USER");
            assertThat(encoder.matches(" secret ", saved.getPassword())).isTrue();
            assertThat(encoder.matches("secret", saved.getPassword())).isFalse();
            when(mapper.selectOne(any())).thenReturn(saved);
            return 1;
        });
        service.register(dto);
        var login = new UserLoginDTO(); login.setUsername("alice"); login.setPassword(" secret ");
        assertThat(service.login(login).getUser().getId()).isEqualTo(1L);
    }

    @Test void rejectsPasswordsBeyondBcryptByteLimitBeforeDatabaseWrite() {
        var dto = new UserDTO(); dto.setUsername("alice"); dto.setPassword("密".repeat(25));
        assertThatThrownBy(() -> service.register(dto)).hasMessageContaining("72 字节");
        verifyNoInteractions(mapper);
    }

    @Test void blankPasswordIsRejected() {
        var dto = new UserDTO(); dto.setUsername("alice"); dto.setPassword("   ");
        assertThatThrownBy(() -> service.register(dto)).hasMessageContaining("密码不能为空");
        verifyNoInteractions(mapper);
    }
}
