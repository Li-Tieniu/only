package com.game.app.controller;

import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.order.service.OrderService;
import com.game.common.order.vo.OrderVO;
import com.game.common.response.ApiResult;
import com.game.common.response.PageResult;
import com.game.common.security.LoginUser;
import com.game.common.util.RequestUrls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端订单接口（下单 / 我的订单 / 订单详情 / 模拟支付）。
 * /order/** 在 SecurityConfig 中要求登录，用户身份统一从 JWT 解析出的 LoginUser 获取。
 */
@RequestMapping("/order")
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     * POST /order/create  body: {"itemId":1}
     */
    @PostMapping("/create")
    public ApiResult<OrderVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody OrderCreateDTO dto) {
        return ApiResult.success(orderService.create(loginUser.getId(), dto));
    }

    /**
     * 查询我的订单
     * GET /order/my?page=1&amp;page_size=10
     */
    @GetMapping("/my")
    public ApiResult<PageResult<OrderVO>> myOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                   @RequestParam(defaultValue = "1")
                                                   @Min(value = 1, message = "页码必须大于 0") Integer page,
                                                   @RequestParam(name = "page_size", defaultValue = "10")
                                                   @Min(value = 1, message = "每页条数必须大于 0")
                                                   @Max(value = 100, message = "每页条数不能超过 100") Integer pageSize) {
        return ApiResult.success(orderService.pageMyOrders(
                loginUser.getId(), page, pageSize, RequestUrls.currentWithout("page", "page_size")));
    }

    /**
     * 查询订单详情（只能查自己的订单）
     * GET /order/{orderNo}
     */
    @GetMapping("/{orderNo}")
    public ApiResult<OrderVO> detail(@AuthenticationPrincipal LoginUser loginUser,
                                     @PathVariable String orderNo) {
        return ApiResult.success(orderService.getByOrderNo(loginUser.getId(), orderNo));
    }

    /**
     * 模拟支付（只能支付自己的待支付订单）
     * POST /order/{orderNo}/pay
     */
    @PostMapping("/{orderNo}/pay")
    public ApiResult<OrderVO> pay(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable String orderNo) {
        return ApiResult.success(orderService.pay(loginUser.getId(), orderNo));
    }
}
