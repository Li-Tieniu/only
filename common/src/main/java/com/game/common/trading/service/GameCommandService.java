package com.game.common.trading.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.game.common.trading.convert.GameConverter;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.springframework.stereotype.Service;

/**
 * 交易商品写操作服务（新增 / 修改 / 删除）
 */
@Service
public class GameCommandService {

    private final GameMapper gameMapper;

    public GameCommandService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    /** 新增商品，返回生成的主键 ID（insert 后 MyBatis-Plus 自动回填 itemId） */
    public Long add(GameSaveDTO dto) {
        GameEntity entity = GameConverter.toNewEntity(dto);
        gameMapper.insert(entity);
        return entity.getItemId();
    }

    /** 按主键修改商品：null 字段不参与更新，返回影响行数 */
    public int update(GameSaveDTO dto) {
        return gameMapper.updateById(GameConverter.toUpdateEntity(dto));
    }

    /**
     * 逻辑删除商品：把 is_deleted 置为 1，并刷新修改时间。
     * @TableLogic 会自动补上 is_deleted = 0 的过滤条件，与原来的删除 SQL 行为一致。
     *
     * @return 影响行数
     */
    public int softDelete(Long itemId) {
        GameEntity patch = GameConverter.toSoftDeleteEntity(itemId);
        LambdaUpdateWrapper<GameEntity> wrapper = Wrappers.<GameEntity>lambdaUpdate()
                .eq(GameEntity::getItemId, itemId)
                .set(GameEntity::getIsDeleted, 1)
                .set(GameEntity::getUpdateTime, patch.getUpdateTime());
        return gameMapper.update(null, wrapper);
    }

    /** 物理删除商品，返回影响行数 */
    public int hardDelete(Long itemId) {
        return gameMapper.hardDelete(itemId);
    }
}
