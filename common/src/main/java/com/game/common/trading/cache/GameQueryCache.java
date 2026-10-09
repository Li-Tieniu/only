package com.game.common.trading.cache;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品查询缓存层：命中缓存就不再查库，未命中则查库并写缓存。
 * 条件构造器由调用方拼好传进来，保证查询条件只有一份实现。
 */
@Service
public class GameQueryCache {

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
    @Cacheable(cacheNames = GameCacheNames.PRODUCT_LIST, key = "#key")
    public List<GameEntity> listProducts(String key, Wrapper<GameEntity> condition) {
        return gameMapper.selectList(condition);
    }

    /**
     * 商品分页详情。
     *
     * @param key       缓存键，见 {@link GameCacheKeys#pageKey}
     * @param condition 查询条件
     */
    @Cacheable(cacheNames = GameCacheNames.PRODUCT_PAGE, key = "#key")
    public GamePageData pageProducts(String key, Wrapper<GameEntity> condition, long page, long pageSize) {
        Page<GameEntity> pageData = gameMapper.selectPage(new Page<>(page, pageSize), condition);
        return new GamePageData(pageData.getRecords(), pageData.getTotal());
    }
}
