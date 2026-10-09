package com.game.common.order.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.exception.BusinessException;
import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.order.entity.OrderEntity;
import com.game.common.order.mapper.OrderMapper;
import com.game.common.order.vo.OrderVO;
import com.game.common.response.PageResult;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单核心逻辑单元测试：Mapper 用 Mockito 替换，不依赖数据库和 Spring 容器。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private GameMapper gameMapper;

    private OrderService orderService;

    /**
     * 纯单元测试里没有 Spring 容器，需要手动初始化实体的 TableInfo，
     * 否则 lambda 条件构造器解析列名时会抛 "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrderEntity.class);
    }

    @BeforeEach
    void setUp() {
        // 构造器注入，和运行时 Bean 的装配方式一致
        orderService = new OrderService(orderMapper, gameMapper);
    }

    @Test
    @DisplayName("创建订单：保存商品名称与价格快照，状态为待支付")
    void createShouldSnapshotGameAndReturnPendingOrder() {
        when(gameMapper.selectById(9L)).thenReturn(game(9L, "王者荣耀", "888.00"));

        OrderVO vo = orderService.create(1L, new OrderCreateDTO(9L));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).insert(captor.capture());
        OrderEntity saved = captor.getValue();
        assertThat(saved.getOrderNo()).startsWith("OD");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getItemId()).isEqualTo(9L);
        assertThat(saved.getGameName()).isEqualTo("王者荣耀");
        assertThat(saved.getPrice()).isEqualByComparingTo("888.00");
        assertThat(saved.getStatus()).isEqualTo(OrderEntity.STATUS_PENDING);
        assertThat(saved.getCreateTime()).isPositive();
        assertThat(saved.getPayTime()).isNull();

        assertThat(vo.getOrderNo()).isEqualTo(saved.getOrderNo());
        assertThat(vo.getPrice()).isEqualByComparingTo("888.00");
        assertThat(vo.getStatus()).isEqualTo(OrderEntity.STATUS_PENDING);
        assertThat(vo.getPayTime()).isNull();
    }

    @Test
    @DisplayName("创建订单：商品不存在或已删除时拒绝下单")
    void createShouldFailWhenGameNotFound() {
        when(gameMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.create(1L, new OrderCreateDTO(404L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品不存在或已删除");
        verify(orderMapper, never()).insert(any(OrderEntity.class));
    }

    @Test
    @DisplayName("创建订单：商品 ID 为空时拒绝下单")
    void createShouldFailWhenItemIdIsNull() {
        assertThatThrownBy(() -> orderService.create(1L, new OrderCreateDTO(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品 ID 不能为空");
        verify(gameMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("创建订单：读商品 + 写订单必须在一个事务里")
    void createShouldBeTransactional() throws NoSuchMethodException {
        Method create = OrderService.class.getMethod("create", Long.class, OrderCreateDTO.class);
        assertThat(create.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    @DisplayName("订单详情：不能查看别人的订单")
    void getByOrderNoShouldRejectOtherUsersOrder() {
        when(orderMapper.selectOne(any())).thenReturn(order(2L, OrderEntity.STATUS_PENDING));

        assertThatThrownBy(() -> orderService.getByOrderNo(1L, "OD-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看该订单");
    }

    @Test
    @DisplayName("我的订单：只按当前用户过滤")
    void pageMyOrdersShouldOnlyQueryCurrentUser() {
        when(orderMapper.selectPage(ArgumentMatchers.<Page<OrderEntity>>any(), any()))
                .thenReturn(new Page<>(1, 10));

        PageResult<OrderVO> result = orderService.pageMyOrders(7L, 1, 10, "http://localhost/order/my");

        ArgumentCaptor<Wrapper<OrderEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectPage(ArgumentMatchers.<Page<OrderEntity>>any(), captor.capture());
        LambdaQueryWrapper<OrderEntity> wrapper = (LambdaQueryWrapper<OrderEntity>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("user_id");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(7L);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    @DisplayName("模拟支付：不能支付别人的订单")
    void payShouldRejectOtherUsersOrder() {
        when(orderMapper.selectOne(any())).thenReturn(order(2L, OrderEntity.STATUS_PENDING));

        assertThatThrownBy(() -> orderService.pay(1L, "OD-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权支付该订单");
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("模拟支付：已支付订单不能重复支付")
    void payShouldRejectPaidOrder() {
        when(orderMapper.selectOne(any())).thenReturn(order(1L, OrderEntity.STATUS_PAID));

        assertThatThrownBy(() -> orderService.pay(1L, "OD-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单已支付，请勿重复支付");
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("模拟支付：带订单号 + 用户 + 待支付三个条件更新，避免无条件覆盖")
    void payShouldUpdateWithConditions() {
        when(orderMapper.selectOne(any())).thenReturn(order(1L, OrderEntity.STATUS_PENDING));
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        OrderVO vo = orderService.pay(1L, "OD-1");

        ArgumentCaptor<Wrapper<OrderEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<OrderEntity> wrapper = (LambdaUpdateWrapper<OrderEntity>) captor.getValue();
        assertThat(wrapper.getSqlSegment())
                .contains("order_no")
                .contains("user_id")
                .contains("status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("OD-1", 1L, OrderEntity.STATUS_PENDING, OrderEntity.STATUS_PAID);
        assertThat(wrapper.getSqlSet())
                .contains("status")
                .contains("pay_time");

        assertThat(vo.getStatus()).isEqualTo(OrderEntity.STATUS_PAID);
        assertThat(vo.getPayTime()).isNotNull();
    }

    @Test
    @DisplayName("模拟支付：条件更新没命中（并发重复支付）时明确报错")
    void payShouldFailWhenConditionalUpdateAffectsNoRow() {
        when(orderMapper.selectOne(any())).thenReturn(order(1L, OrderEntity.STATUS_PENDING));
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> orderService.pay(1L, "OD-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单状态已变更");
    }

    private static GameEntity game(Long itemId, String gameName, String price) {
        GameEntity game = new GameEntity();
        game.setItemId(itemId);
        game.setGameName(gameName);
        game.setPrice(price);
        return game;
    }

    private static OrderEntity order(Long userId, String status) {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("OD-1");
        order.setUserId(userId);
        order.setItemId(9L);
        order.setGameName("王者荣耀");
        order.setPrice(new BigDecimal("888.00"));
        order.setStatus(status);
        order.setCreateTime(1791000000);
        return order;
    }
}
