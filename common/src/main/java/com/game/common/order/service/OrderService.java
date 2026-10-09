package com.game.common.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.exception.BusinessException;
import com.game.common.order.convert.OrderConverter;
import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.order.entity.OrderEntity;
import com.game.common.order.mapper.OrderMapper;
import com.game.common.order.vo.OrderVO;
import com.game.common.response.PageResult;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 订单服务（app / console 共用）。
 * 当前阶段只有：创建订单、查询我的订单、查询订单详情、模拟支付。
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final GameMapper gameMapper;

    public OrderService(OrderMapper orderMapper, GameMapper gameMapper) {
        this.orderMapper = orderMapper;
        this.gameMapper = gameMapper;
    }

    /**
     * 创建订单：校验商品存在且未删除，保存商品名称与价格快照。
     * 读商品 + 写订单放在同一个事务里。
     *
     * @param userId 当前登录用户 ID（来自 JWT）
     * @param dto    只包含商品 ID
     */
    @Transactional
    public OrderVO create(Long userId, OrderCreateDTO dto) {
        if (dto == null || dto.getItemId() == null) {
            log.warn("创建订单失败，商品 ID 为空, userId={}", userId);
            throw new BusinessException("商品 ID 不能为空");
        }
        // @TableLogic 会自动过滤 is_deleted = 1，查不到即商品不存在或已删除
        GameEntity game = gameMapper.selectById(dto.getItemId());
        if (game == null) {
            log.warn("创建订单失败，商品不存在或已删除, userId={}, itemId={}", userId, dto.getItemId());
            throw new BusinessException(404, "商品不存在或已删除");
        }

        OrderEntity order = OrderConverter.toNewEntity(userId, game);
        orderMapper.insert(order);
        log.info("订单创建成功, orderNo={}, userId={}, itemId={}, price={}",
                order.getOrderNo(), userId, order.getItemId(), order.getPrice());
        return OrderConverter.toVO(order);
    }

    /**
     * 分页查询当前用户自己的订单（按订单 ID 倒序，最新下单的在前面）。
     */
    public PageResult<OrderVO> pageMyOrders(Long userId, Integer page, Integer pageSize, String baseUrl) {
        Page<OrderEntity> pageData = orderMapper.selectPage(
                new Page<>(page, pageSize),
                Wrappers.<OrderEntity>lambdaQuery()
                        .eq(OrderEntity::getUserId, userId)
                        .orderByDesc(OrderEntity::getId)
        );
        List<OrderVO> items = pageData.getRecords().stream()
                .map(OrderConverter::toVO)
                .toList();
        return PageResult.of(items, pageData.getTotal(), page, pageSize, baseUrl);
    }

    /**
     * 查询订单详情，并校验订单归属：不是自己的订单直接拒绝。
     */
    public OrderVO getByOrderNo(Long userId, String orderNo) {
        OrderEntity order = findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return OrderConverter.toVO(order);
    }

    /**
     * 模拟支付：只能把「当前用户自己的、待支付的」订单改成已支付。
     * 状态更新带 order_no + user_id + status = PENDING 三个条件，
     * 影响行数为 0 说明订单不是自己的、或状态已经变了，不会无条件覆盖。
     */
    public OrderVO pay(Long userId, String orderNo) {
        OrderEntity order = findByOrderNo(orderNo);
        if (order == null) {
            log.warn("支付失败，订单不存在, orderNo={}, userId={}", orderNo, userId);
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            log.warn("支付失败，订单不属于当前用户, orderNo={}, userId={}", orderNo, userId);
            throw new BusinessException(403, "无权支付该订单");
        }
        if (OrderEntity.STATUS_PAID.equals(order.getStatus())) {
            log.warn("支付失败，订单已支付, orderNo={}, userId={}", orderNo, userId);
            throw new BusinessException("订单已支付，请勿重复支付");
        }
        if (!OrderEntity.STATUS_PENDING.equals(order.getStatus())) {
            log.warn("支付失败，订单状态不是待支付, orderNo={}, userId={}, status={}", orderNo, userId, order.getStatus());
            throw new BusinessException("订单状态不是待支付，无法支付");
        }

        int now = (int) Instant.now().getEpochSecond();
        LambdaUpdateWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaUpdate()
                .eq(OrderEntity::getOrderNo, orderNo)
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, OrderEntity.STATUS_PENDING)
                .set(OrderEntity::getStatus, OrderEntity.STATUS_PAID)
                .set(OrderEntity::getPayTime, now);
        int rows = orderMapper.update(null, wrapper);
        if (rows == 0) {
            // 并发下被别人抢先支付：条件没命中，状态保持已支付，这里给调用方明确提示
            log.warn("支付失败，订单状态已变更, orderNo={}, userId={}", orderNo, userId);
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        order.setStatus(OrderEntity.STATUS_PAID);
        order.setPayTime(now);
        log.info("订单支付成功, orderNo={}, userId={}, price={}", orderNo, userId, order.getPrice());
        return OrderConverter.toVO(order);
    }

    /** 按订单编号查询（订单编号唯一，最多一条） */
    private OrderEntity findByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("订单编号不能为空");
        }
        return orderMapper.selectOne(
                Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getOrderNo, orderNo));
    }
}
