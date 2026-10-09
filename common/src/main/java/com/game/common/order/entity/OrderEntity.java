package com.game.common.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 游戏交易订单表实体，对应表 game_order。
 * game_name / price 是下单时的商品快照，商品后续改动不影响历史订单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("game_order")
public class OrderEntity {

    /** 订单状态：待支付 */
    public static final String STATUS_PENDING = "PENDING";

    /** 订单状态：已支付 */
    public static final String STATUS_PAID = "PAID";

    /** 订单 ID，数据库自增，insert 后回填 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单编号（唯一，业务主键） */
    @TableField("order_no")
    private String orderNo;

    /** 下单用户 ID（取自 JWT，不接受前端传入） */
    @TableField("user_id")
    private Long userId;

    /** 商品 ID */
    @TableField("item_id")
    private Long itemId;

    /** 下单时的商品名称快照 */
    @TableField("game_name")
    private String gameName;

    /** 下单时的商品价格快照（金额统一用 BigDecimal） */
    @TableField("price")
    private BigDecimal price;

    /** 订单状态：PENDING-待支付，PAID-已支付 */
    @TableField("status")
    private String status;

    /** 创建时间（Unix 时间戳，秒） */
    @TableField("create_time")
    private Integer createTime;

    /** 支付时间（Unix 时间戳，秒），未支付为 null */
    @TableField("pay_time")
    private Integer payTime;
}
