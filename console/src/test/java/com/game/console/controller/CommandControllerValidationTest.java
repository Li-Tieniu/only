package com.game.console.controller;

import com.game.common.trading.service.GameCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 CommandController 上的分组校验在 HTTP 层确实生效（standalone MockMvc：
 * 不启动 Spring 容器、不连数据库，Service 用 Mock 替身）。
 */
class CommandControllerValidationTest {

    private MockMvc mockMvc;
    private GameCommandService gameCommandService;

    @BeforeEach
    void setUp() {
        gameCommandService = mock(GameCommandService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommandController(gameCommandService))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("新增商品缺必填字段：返回 400，且不会走到 Service（不会产生数据库异常）")
    void addGameShouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/gametrading/addgame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(gameCommandService);
    }

    @Test
    @DisplayName("修改商品只传部分字段（空串价格=不修改）：校验通过，正常调用 Service")
    void updateGameShouldAllowPartialFields() throws Exception {
        mockMvc.perform(post("/gametrading/updategame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"price\":\"\"}"))
                .andExpect(status().isOk());

        Mockito.verify(gameCommandService).update(Mockito.any());
    }

    @Test
    @DisplayName("修改商品缺 itemId：返回 400")
    void updateGameShouldRequireItemId() throws Exception {
        mockMvc.perform(post("/gametrading/updategame")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameName\":\"王者荣耀\"}"))
                .andExpect(status().isBadRequest());
    }
}
