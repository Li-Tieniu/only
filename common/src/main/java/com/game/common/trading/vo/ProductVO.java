package com.game.common.trading.vo;

import lombok.Data;

/**
 * 商品列表项展示对象（列表页）
 */
@Data
public class ProductVO {

    /** 商品主键 ID */
    private Long itemId;

    /** 列表封面图 */
    private String coverImage;

    /** 游戏名称 */
    private String gameName;

    /** 商品价格 */
    private String price;
}
