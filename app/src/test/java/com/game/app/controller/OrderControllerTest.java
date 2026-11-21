package com.game.app.controller;

import com.game.common.exception.BusinessException;
import com.game.common.exception.GlobalExceptionHandler;
import com.game.common.order.service.OrderService;
import com.game.common.order.vo.OrderVO;
import com.game.common.security.LoginUser;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {
    OrderService service;
    MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(OrderService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(7L, "buyer", "USER"), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void cancellationUsesAuthenticatedIdentityAndPreservesResponseFormat() throws Exception {
        var result = new OrderVO(); result.setOrderNo("OD-1"); result.setStatus("CANCELLED");
        when(service.cancel(7L, "OD-1")).thenReturn(result);
        mvc.perform(post("/order/OD-1/cancel?userId=999")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        verify(service).cancel(7L, "OD-1");
        verifyNoMoreInteractions(service);
    }

    @Test void duplicateCancellationReturnsBusinessConflict() throws Exception {
        when(service.cancel(7L, "OD-1")).thenThrow(new BusinessException(409, "订单已取消，请勿重复取消"));
        mvc.perform(post("/order/OD-1/cancel"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("订单已取消，请勿重复取消"));
    }
}
