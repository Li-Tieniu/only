package com.game.common.trading.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品详情展示对象（详情页）
 */
@Data
public class DetailsVO {

    private Long itemId;
    private String coverImage;
    private List<String> wallImages;
    private String gameName;
    private String price;
    private String details;
    private String createTime;
    private String updateTime;

}
