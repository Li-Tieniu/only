package com.game.common.trading.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏交易商品表实体，对应表 game_trading
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEntity {

    /** 主键 ID */
    private Long itemId;

    /** 是否删除：0-未删除，1-已删除 */
    private Integer isDeleted;

    /** 列表展示小图 */
    private String coverImage;

    /** 详情展示大图，多图使用 $ 分隔 */
    private String wallImages;

    /** 游戏名称 */
    private String gameName;

    /** 商品价格 */
    private String price;

    /** 商品详情 */
    private String details;

    /** 创建时间（Unix 时间戳，秒） */
    private Integer createTime;

    /** 修改时间（Unix 时间戳，秒） */
    private Integer updateTime;
}
