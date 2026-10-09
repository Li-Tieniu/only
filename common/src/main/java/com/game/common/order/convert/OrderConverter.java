package com.game.common.order.convert;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.game.common.order.entity.OrderEntity;
import com.game.common.order.vo.OrderVO;
import com.game.common.trading.entity.GameEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 订单对象转换：Entity / VO
 */
public final class OrderConverter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderConverter() {
    }

    /**
     * 用商品快照组装新订单（商品名称、价格在下单时冻结）。
     *
     * @param userId 下单用户 ID（来自 JWT）
     * @param game   已确认存在且未删除的商品
     */
    public static OrderEntity toNewEntity(Long userId, GameEntity game) {
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setItemId(game.getItemId());
        order.setGameName(game.getGameName());
        order.setPrice(new BigDecimal(game.getPrice()));
        order.setStatus(OrderEntity.STATUS_PENDING);
        order.setCreateTime(currentEpochSecond());
        return order;
    }

    /** 实体转订单 VO */
    public static OrderVO toVO(OrderEntity order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setItemId(order.getItemId());
        vo.setGameName(order.getGameName());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(formatTime(order.getCreateTime()));
        vo.setPayTime(formatTime(order.getPayTime()));
        return vo;
    }

    /**
     * 订单编号：OD + MyBatis-Plus 内置雪花算法 ID，全局唯一且带时间趋势。
     * 表上还有 uk_game_order_order_no 唯一索引兜底。
     */
    private static String generateOrderNo() {
        return "OD" + IdWorker.getIdStr();
    }

    private static String formatTime(Integer timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER);
    }

    private static int currentEpochSecond() {
        return (int) Instant.now().getEpochSecond();
    }
}
