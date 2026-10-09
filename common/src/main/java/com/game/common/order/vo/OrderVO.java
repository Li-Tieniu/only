package com.game.common.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单展示对象。
 */
@Data
public class OrderVO {

    /** 订单 ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 商品 ID */
    private Long itemId;

    /** 下单时的商品名称 */
    private String gameName;

    /** 下单时的商品价格 */
    private BigDecimal price;

    /** 订单状态：PENDING-待支付，PAID-已支付 */
    private String status;

    /** 创建时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;

    /** 支付时间 yyyy-MM-dd HH:mm:ss，未支付为 null */
    private String payTime;
}
