package com.game.common.trading.service;

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

    /** 新增商品，返回生成的主键 ID */
    public Long add(GameSaveDTO dto) {
        GameEntity entity = GameConverter.toNewEntity(dto);
        gameMapper.insert(entity);
        return entity.getItemId();
    }

    /** 按主键修改商品，返回影响行数 */
    public int update(GameSaveDTO dto) {
        return gameMapper.update(GameConverter.toUpdateEntity(dto));
    }

    /** 逻辑删除商品，返回影响行数 */
    public int softDelete(Long itemId) {
        return gameMapper.softDelete(GameConverter.toSoftDeleteEntity(itemId));
    }

    /** 物理删除商品，返回影响行数 */
    public int hardDelete(Long itemId) {
        return gameMapper.hardDelete(GameConverter.toHardDeleteEntity(itemId));
    }
}
