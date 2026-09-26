package com.game.common.trading.dto;

import lombok.Data;

/**
 * 商品新增 / 修改请求体。
 * 新增可不传 itemId；修改需传 itemId。
 */
@Data
public class GameSaveDTO {

    /** 商品主键 ID（修改时必填） */
    private Long itemId;

    /** 列表封面图 */
    private String coverImage;

    /** 详情大图，多图使用 $ 分隔 */
    private String wallImages;

    /** 游戏名称 */
    private String gameName;

    /** 商品价格 */
    private String price;

    /** 商品详情描述 */
    private String details;
}
