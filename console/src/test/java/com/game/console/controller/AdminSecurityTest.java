package com.game.console.controller;

import com.game.common.config.SecurityConfig;
import com.game.common.security.JwtUtil;
import com.game.common.security.LoginUser;
import com.game.common.trading.service.GameQueryService;
import com.game.common.user.entity.UserEntity;
import com.game.common.user.mapper.UserMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实 SecurityFilterChain + MockMvc，数据库/业务服务使用 Mock。 */
@WebMvcTest(QueryController.class)
@ContextConfiguration(classes = {QueryController.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class AdminSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean JwtUtil jwt;
    @MockBean UserMapper users;
    @MockBean GameQueryService queries;

    @ParameterizedTest @ValueSource(strings = {"getinfoproduct", "getinfodetails"})
    void anonymousCannotReadAdminCatalog(String path) throws Exception {
        mvc.perform(get("/gametrading/" + path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(queries);
    }
    @ParameterizedTest @ValueSource(strings = {"getinfoproduct", "getinfodetails"})
    void ordinaryUserCannotReadAdminCatalog(String path) throws Exception {
        authenticate("USER", 1);
        mvc.perform(get("/gametrading/" + path).header("Authorization", "Bearer test"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(queries);
    }
    @ParameterizedTest @ValueSource(strings = {"getinfoproduct", "getinfodetails"})
    void adminCanReadCatalog(String path) throws Exception {
        authenticate("ADMIN", 1);
        mvc.perform(get("/gametrading/" + path).header("Authorization", "Bearer test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
    @ParameterizedTest @ValueSource(strings = {"getinfoproduct", "getinfodetails"})
    void disabledAdminCannotReadCatalog(String path) throws Exception {
        authenticate("ADMIN", 0);
        mvc.perform(get("/gametrading/" + path).header("Authorization", "Bearer test"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(queries);
    }
    void authenticate(String role, int status) {
        when(jwt.parseToken("test")).thenReturn(new LoginUser(1L, "test", "USER"));
        var user = new UserEntity(); user.setId(1L); user.setRole(role); user.setStatus(status);
        when(users.selectById(1L)).thenReturn(user);
    }
}
