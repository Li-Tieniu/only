package com.game.common.trading.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 按主键操作请求体（删除等）
 */
@Data
public class ItemIdDTO {

    /** 商品主键 ID */
    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须大于 0")
    private Long itemId;
}
