package com.game.common.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.common.trading.entity.GameEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 游戏交易商品 Mapper。
 * 继承 BaseMapper 后即可直接使用单表 CRUD 与条件构造器，原来的 GameMapper.xml 已移除。
 */
@Mapper
public interface GameMapper extends BaseMapper<GameEntity> {

    /**
     * 物理删除：绕过逻辑删除，直接从数据库删除记录。
     *
     * @param itemId 商品主键 ID
     * @return 影响行数
     */
    @Delete("delete from game_trading where item_id = #{itemId}")
    int hardDelete(@Param("itemId") Long itemId);
}
