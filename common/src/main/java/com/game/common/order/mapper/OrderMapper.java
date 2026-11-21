package com.game.common.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.common.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper。
 * 继承 BaseMapper 后即可直接使用单表 CRUD 与条件构造器，无需 XML。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
