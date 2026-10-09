package com.game.common.trading.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.game.common.exception.BusinessException;
import com.game.common.trading.cache.GameCacheNames;
import com.game.common.trading.convert.GameConverter;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 交易商品写操作服务（新增 / 修改 / 删除）。
 * 每个写操作都会清空商品查询缓存，避免用户继续看到旧数据。
 */
@Service
public class GameCommandService {

    private static final Logger log = LoggerFactory.getLogger(GameCommandService.class);

    private final GameMapper gameMapper;

    public GameCommandService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    /** 新增商品，返回生成的主键 ID（insert 后 MyBatis-Plus 自动回填 itemId） */
    @CacheEvict(cacheNames = {GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE}, allEntries = true)
    public Long add(GameSaveDTO dto) {
        GameEntity entity = GameConverter.toNewEntity(dto);
        gameMapper.insert(entity);
        log.info("商品新增成功, itemId={}, gameName={}", entity.getItemId(), entity.getGameName());
        return entity.getItemId();
    }

    /** 按主键修改商品：null 字段不参与更新，返回影响行数 */
    @CacheEvict(cacheNames = {GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE}, allEntries = true)
    public int update(GameSaveDTO dto) {
        if (dto == null || dto.getItemId() == null) {
            throw new BusinessException("商品 ID 不能为空");
        }
        int rows = gameMapper.updateById(GameConverter.toUpdateEntity(dto));
        if (rows == 0) {
            log.warn("商品修改未命中任何记录, itemId={}", dto.getItemId());
        } else {
            log.info("商品修改成功, itemId={}", dto.getItemId());
        }
        return rows;
    }

    /**
     * 逻辑删除商品：把 is_deleted 置为 1，并刷新修改时间。
     * @TableLogic 会自动补上 is_deleted = 0 的过滤条件，与原来的删除 SQL 行为一致。
     *
     * @return 影响行数
     */
    @CacheEvict(cacheNames = {GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE}, allEntries = true)
    public int softDelete(Long itemId) {
        GameEntity patch = GameConverter.toSoftDeleteEntity(itemId);
        LambdaUpdateWrapper<GameEntity> wrapper = Wrappers.<GameEntity>lambdaUpdate()
                .eq(GameEntity::getItemId, itemId)
                .set(GameEntity::getIsDeleted, 1)
                .set(GameEntity::getUpdateTime, patch.getUpdateTime());
        int rows = gameMapper.update(null, wrapper);
        if (rows == 0) {
            log.warn("商品软删除未命中任何记录, itemId={}", itemId);
        } else {
            log.info("商品软删除成功, itemId={}", itemId);
        }
        return rows;
    }

    /** 物理删除商品，返回影响行数 */
    @CacheEvict(cacheNames = {GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE}, allEntries = true)
    public int hardDelete(Long itemId) {
        int rows = gameMapper.hardDelete(itemId);
        log.info("商品物理删除, itemId={}, rows={}", itemId, rows);
        return rows;
    }
}
