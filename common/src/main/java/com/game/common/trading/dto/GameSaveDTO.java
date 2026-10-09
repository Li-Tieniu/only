package com.game.common.trading.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 2000, message = "封面图地址长度不能超过 2000")
    private String coverImage;

    /** 详情大图，多图使用 $ 分隔 */
    @Size(max = 2000, message = "详情图地址长度不能超过 2000")
    private String wallImages;

    /** 游戏名称 */
    @Size(max = 50, message = "游戏名称长度不能超过 50")
    private String gameName;

    /** 商品价格 */
    // 空串表示"本次不修改价格"（沿用原有语义），所以正则里放行空串，只拦截非法格式
    @Pattern(regexp = "^$|^\\d+(\\.\\d{1,2})?$", message = "商品价格必须是不小于 0 的数字，最多两位小数")
    private String price;

    /** 商品详情描述 */
    @Size(max = 2000, message = "商品详情长度不能超过 2000")
    private String details;
}
