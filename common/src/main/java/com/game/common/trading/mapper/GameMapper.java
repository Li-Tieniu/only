package com.game.common.trading.mapper;

import com.game.common.trading.entity.GameEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 游戏交易商品 Mapper，对应 GameMapper.xml
 */
@Mapper
public interface GameMapper {

    /** 查询全部未删除商品 */
    List<GameEntity> listActive();

    /**
     * 分页查询未删除商品
     *
     * @param offset 偏移量
     * @param size   每页条数
     */
    List<GameEntity> page(@Param("offset") int offset, @Param("size") int size);

    /**
     * 瀑布流游标分页查询。
     * 按 item_id 倒序；cursor 为空表示第一屏，否则取 item_id 小于 cursor 的后续数据。
     *
     * @param cursor 上一屏最后一条的 itemId，首页传 null
     * @param size   拉取条数（可多取 1 条用于判断 has_more）
     */
    List<GameEntity> waterfall(@Param("cursor") Long cursor, @Param("size") int size);

    /** 统计未删除商品总数 */
    int count();

    /** 新增商品，插入后回填主键 itemId */
    int insert(GameEntity entity);

    /** 按主键修改未删除商品（空字段不更新） */
    int update(GameEntity entity);

    /** 逻辑删除：将 is_deleted 置为 1 */
    int softDelete(GameEntity entity);

    /** 物理删除：从数据库中删除记录 */
    int hardDelete(GameEntity entity);
}
