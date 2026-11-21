package com.game.common.trading.cache;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * 保留原有查询边界，销售实时查询直接查库，不读取或回填 Redis。
 * 仅清缓存无法覆盖事务提交前清理、并发旧查询回填和 Redis 清理失败。
 * 条件构造器由调用方拼好传进来，保证查询条件只有一份实现。
 */
@Service
public class GameQueryCache {
    /** 事务内绕过缓存，避免未提交数据或回滚结果写入 Redis。 */
    @Cacheable(cacheNames = GameCacheNames.GAME_NAMES, key = "#keyword",
            condition = "!T(org.springframework.transaction.support.TransactionSynchronizationManager).isActualTransactionActive()")
    public List<String> gameNames(String keyword) {
        return gameMapper.selectGameNames(keyword);
    }

    private final GameMapper gameMapper;

    public GameQueryCache(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    /**
     * 商品列表（不分页）。
     *
     * @param key       缓存键，见 {@link GameCacheKeys#listKey}
     * @param condition 查询条件（关键词 + 价格区间 + 排序）
     */
    public List<GameEntity> listProducts(String key, Wrapper<GameEntity> condition) {
        return gameMapper.selectList(condition);
    }

    /**
     * 商品分页详情。
     *
     * @param key       缓存键，见 {@link GameCacheKeys#pageKey}
     * @param condition 查询条件
     */
    public GamePageData pageProducts(String key, Wrapper<GameEntity> condition, long page, long pageSize) {
        Page<GameEntity> pageData = gameMapper.selectPage(new Page<>(page, pageSize), condition);
        return new GamePageData(pageData.getRecords(), pageData.getTotal());
    }
}
