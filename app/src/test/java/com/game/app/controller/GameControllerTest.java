package com.game.app.controller;

import com.game.common.exception.BusinessException;
import com.game.common.exception.GlobalExceptionHandler;
import com.game.common.trading.service.GameQueryService;
import com.game.common.trading.vo.DetailsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private GameQueryService gameQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gameQueryService = mock(GameQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void detailShouldReturnSingleProductInApiResult() throws Exception {
        DetailsVO detail = new DetailsVO();
        detail.setItemId(1001L);
        detail.setGameName("王者荣耀");
        when(gameQueryService.getDetailById(1001L)).thenReturn(detail);

        mockMvc.perform(get("/gametrading/details/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.itemId").value(1001))
                .andExpect(jsonPath("$.data.gameName").value("王者荣耀"));

        verify(gameQueryService).getDetailById(1001L);
    }

    @Test
    void detailShouldReturnUnifiedErrorForMissingProduct() throws Exception {
        when(gameQueryService.getDetailById(999L))
                .thenThrow(new BusinessException(404, "商品不存在或已下架"));

        mockMvc.perform(get("/gametrading/details/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在或已下架"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
