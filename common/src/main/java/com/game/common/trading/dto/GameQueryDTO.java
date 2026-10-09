package com.game.common.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品查询条件：关键词、价格区间、排序方式。
 * 由 Controller 从请求参数组装，列表 / 分页 / 瀑布流共用同一份条件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameQueryDTO {

    /** 关键词：按游戏名称模糊匹配 */
    private String keyword;

    /** 最低价（含），null 表示不限 */
    private BigDecimal minPrice;

    /** 最高价（含），null 表示不限 */
    private BigDecimal maxPrice;

    /** 排序方式：time_desc（默认）/ price_asc / price_desc，非法值会被拒绝 */
    private String sort;

    /**
     * 组装查询条件。
     * keyword / sort 去掉首尾空格，空串统一转成 null，避免写成模糊匹配全表或空排序。
     */
    public static GameQueryDTO of(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        return new GameQueryDTO(trimToNull(keyword), minPrice, maxPrice, trimToNull(sort));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
