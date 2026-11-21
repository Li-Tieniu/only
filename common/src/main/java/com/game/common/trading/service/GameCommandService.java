package com.game.common.trading.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.game.common.exception.BusinessException;
import com.game.common.trading.cache.GameCatalogInvalidation;
import com.game.common.trading.convert.GameConverter;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * 交易商品写操作服务（新增 / 修改 / 删除）。
 * 每个写操作都会清空商品查询缓存，避免用户继续看到旧数据。
 */
@Service
@Transactional
public class GameCommandService {

    private static final Logger log = LoggerFactory.getLogger(GameCommandService.class);

    private final GameMapper gameMapper;
    private final ApplicationEventPublisher events;

    public GameCommandService(GameMapper gameMapper, ApplicationEventPublisher events) {
        this.gameMapper = gameMapper;
        this.events = events;
    }

    /** 新增商品，返回生成的主键 ID（insert 后 MyBatis-Plus 自动回填 itemId） */
    public Long add(GameSaveDTO dto) {
        GameEntity entity = GameConverter.toNewEntity(dto);
        if (gameMapper.insert(entity) == 1) events.publishEvent(new GameCatalogInvalidation.Changed());
        log.info("商品新增成功, itemId={}, gameName={}", entity.getItemId(), entity.getGameName());
        return entity.getItemId();
    }

    /** 仅编辑在售商品：null 字段不参与更新，返回影响行数 */
    public int update(GameSaveDTO dto) {
        if (dto == null || dto.getItemId() == null) {
            throw new BusinessException("商品 ID 不能为空");
        }
        int rows = gameMapper.update(GameConverter.toUpdateEntity(dto), Wrappers.<GameEntity>lambdaUpdate()
                .eq(GameEntity::getItemId, dto.getItemId())
                .eq(GameEntity::getSaleStatus, GameEntity.ON_SALE));
        if (rows == 0) {
            log.warn("商品修改未命中任何记录, itemId={}", dto.getItemId());
        } else {
            log.info("商品修改成功, itemId={}", dto.getItemId());
        }
        if (rows > 0) events.publishEvent(new GameCatalogInvalidation.Changed());
        return rows;
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
                .eq(GameEntity::getSaleStatus, GameEntity.ON_SALE)
                .set(GameEntity::getIsDeleted, 1)
                .set(GameEntity::getUpdateTime, patch.getUpdateTime());
        int rows = gameMapper.update(null, wrapper);
        if (rows == 0) {
            log.warn("商品软删除未命中任何记录, itemId={}", itemId);
        } else {
            log.info("商品软删除成功, itemId={}", itemId);
        }
        if (rows > 0) events.publishEvent(new GameCatalogInvalidation.Changed());
        return rows;
    }

    /** 仅物理删除在售商品，保留有效订单关联商品，返回影响行数 */
    public int hardDelete(Long itemId) {
        int rows = gameMapper.hardDelete(itemId);
        log.info("商品物理删除, itemId={}, rows={}", itemId, rows);
        if (rows > 0) events.publishEvent(new GameCatalogInvalidation.Changed());
        return rows;
    }
}
