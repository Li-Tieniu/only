package com.game.common.trading.dto;

import lombok.Data;

/**
 * 按主键操作请求体（删除等）
 */
@Data
public class ItemIdDTO {

    /** 商品主键 ID */
    private Long itemId;
}
