package com.game.common.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单请求体：只接收商品 ID。
 * 用户 ID 从 JWT 中取，商品名称与价格从数据库取，都不接受前端传入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    /** 商品 ID */
    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须大于 0")
    private Long itemId;
}
