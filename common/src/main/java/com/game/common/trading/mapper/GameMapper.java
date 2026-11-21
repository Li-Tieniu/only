package com.game.common.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.common.trading.entity.GameEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 游戏交易商品 Mapper。
 * 继承 BaseMapper 后即可直接使用单表 CRUD 与条件构造器，原来的 GameMapper.xml 已移除。
 */
@Mapper
public interface GameMapper extends BaseMapper<GameEntity> {
    /** 名称建议不表示有可购买商品，固定最多 20 项。 */
    @Select("SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 "
            + "AND game_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY game_name LIMIT 20")
    List<String> selectGameNames(@Param("keyword") String keyword);

    @Update("UPDATE game_trading SET sale_status = 'LOCKED', update_time = UNIX_TIMESTAMP() "
            + "WHERE item_id = #{itemId} AND is_deleted = 0 AND sale_status = 'ON_SALE'")
    int lockForOrder(@Param("itemId") Long itemId);

    @Update("UPDATE game_trading SET sale_status = 'SOLD', update_time = UNIX_TIMESTAMP() "
            + "WHERE item_id = #{itemId} AND is_deleted = 0 AND sale_status = 'LOCKED'")
    int markSold(@Param("itemId") Long itemId);

    /** 仅在订单 PENDING → CANCELLED 成功的同一事务中调用。 */
    @Update("UPDATE game_trading SET sale_status = 'ON_SALE', update_time = UNIX_TIMESTAMP() "
            + "WHERE item_id = #{itemId} AND is_deleted = 0 AND sale_status = 'LOCKED'")
    int releaseForCancelledOrder(@Param("itemId") Long itemId);

    /**
     * 物理删除：绕过逻辑删除，直接从数据库删除记录。
     *
     * @param itemId 商品主键 ID
     * @return 影响行数
     */
    @Delete("delete from game_trading where item_id = #{itemId} AND sale_status = 'ON_SALE'")
    int hardDelete(@Param("itemId") Long itemId);
}
