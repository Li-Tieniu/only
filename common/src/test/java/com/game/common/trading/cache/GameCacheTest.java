package com.game.common.trading.cache;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.trading.dto.GameQueryDTO;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import com.game.common.trading.service.GameCommandService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 缓存行为单元测试：用内存缓存（ConcurrentMapCacheManager）代替 Redis，
 * 验证 @Cacheable 命中/区分参数、以及管理端写操作 @CacheEvict 清理缓存。
 * 真实 Redis 的连通性验证不在这里（需要本机有 Redis，见交付说明）。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GameCacheTest.TestConfig.class)
class GameCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        GameMapper gameMapper() {
            return mock(GameMapper.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE);
        }

        @Bean
        GameQueryCache gameQueryCache(GameMapper gameMapper) {
            return new GameQueryCache(gameMapper);
        }

        @Bean
        GameCommandService gameCommandService(GameMapper gameMapper) {
            return new GameCommandService(gameMapper);
        }
    }

    @Autowired
    private GameQueryCache gameQueryCache;

    @Autowired
    private GameCommandService gameCommandService;

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private CacheManager cacheManager;

    /** 纯 Spring 上下文里没有 MyBatis-Plus 初始化，lambda 列名缓存需要手动建一次 */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GameEntity.class);
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(gameMapper);
        listCache().clear();
        pageCache().clear();
    }

    @Test
    @DisplayName("列表查询：相同条件第二次走缓存，不再查库")
    void listShouldBeCachedForSameCondition() {
        when(gameMapper.selectList(any())).thenReturn(List.of(game(1L, "王者荣耀"), game(4L, "原神")));
        GameQueryDTO query = GameQueryDTO.of("王者", new BigDecimal("100"), new BigDecimal("900"), "price_asc");
        LambdaQueryWrapper<GameEntity> condition = Wrappers.lambdaQuery();

        List<GameEntity> first = gameQueryCache.listProducts(GameCacheKeys.listKey(query), condition);
        List<GameEntity> second = gameQueryCache.listProducts(GameCacheKeys.listKey(query), condition);

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        verify(gameMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("列表查询：关键词不同会落到不同缓存条目")
    void listShouldUseDifferentCacheEntryWhenKeywordDiffers() {
        when(gameMapper.selectList(any())).thenReturn(List.of(game(1L, "王者荣耀")));
        LambdaQueryWrapper<GameEntity> condition = Wrappers.lambdaQuery();

        gameQueryCache.listProducts(
                GameCacheKeys.listKey(GameQueryDTO.of("王者", null, null, null)), condition);
        gameQueryCache.listProducts(
                GameCacheKeys.listKey(GameQueryDTO.of("原神", null, null, null)), condition);

        verify(gameMapper, times(2)).selectList(any());
    }

    @Test
    @DisplayName("分页查询：同页命中缓存，换页重新查库")
    void pageShouldBeCachedByPageParams() {
        when(gameMapper.selectPage(ArgumentMatchers.<Page<GameEntity>>any(), any()))
                .thenReturn(pageOf(game(1L, "王者荣耀"), game(2L, "和平精英")));
        GameQueryDTO query = GameQueryDTO.of(null, new BigDecimal("100"), null, "price_desc");

        GamePageData first = gameQueryCache.pageProducts(GameCacheKeys.pageKey(query, 1, 2), Wrappers.lambdaQuery(), 1, 2);
        GamePageData second = gameQueryCache.pageProducts(GameCacheKeys.pageKey(query, 1, 2), Wrappers.lambdaQuery(), 1, 2);
        gameQueryCache.pageProducts(GameCacheKeys.pageKey(query, 2, 2), Wrappers.lambdaQuery(), 2, 2);

        assertThat(first.getRecords()).hasSize(2);
        assertThat(first.getTotal()).isEqualTo(2);
        assertThat(second.getRecords()).hasSize(2);
        verify(gameMapper, times(2)).selectPage(ArgumentMatchers.<Page<GameEntity>>any(), any());
    }

    @Test
    @DisplayName("管理端新增商品：列表与分页缓存都被清空")
    void writingGameShouldEvictProductCache() {
        when(gameMapper.selectList(any())).thenReturn(List.of(game(1L, "王者荣耀")));
        when(gameMapper.selectPage(ArgumentMatchers.<Page<GameEntity>>any(), any())).thenReturn(pageOf(game(1L, "王者荣耀")));
        GameQueryDTO query = GameQueryDTO.of(null, null, null, null);
        gameQueryCache.listProducts(GameCacheKeys.listKey(query), Wrappers.lambdaQuery());
        gameQueryCache.pageProducts(GameCacheKeys.pageKey(query, 1, 10), Wrappers.lambdaQuery(), 1, 10);
        assertThat(listCache().getNativeCache()).isNotEmpty();
        assertThat(pageCache().getNativeCache()).isNotEmpty();

        gameCommandService.add(new GameSaveDTO());

        assertThat(listCache().getNativeCache()).isEmpty();
        assertThat(pageCache().getNativeCache()).isEmpty();
    }

    @Test
    @DisplayName("缓存键包含关键词、价格区间、排序、页码、分页大小")
    void cacheKeyShouldContainAllQueryParams() {
        GameQueryDTO query = GameQueryDTO.of("王者", new BigDecimal("100"), new BigDecimal("900"), "price_asc");

        assertThat(GameCacheKeys.listKey(query))
                .contains("kw=王者").contains("min=100").contains("max=900").contains("sort=price_asc");
        assertThat(GameCacheKeys.pageKey(query, 2, 20))
                .contains("kw=王者").contains("page=2").contains("size=20");
        assertThat(GameCacheKeys.listKey(GameQueryDTO.of(null, null, null, null)))
                .isEqualTo("kw=-|min=-|max=-|sort=-");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMapCache listCache() {
        return (ConcurrentMapCache) cacheManager.getCache(GameCacheNames.PRODUCT_LIST);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMapCache pageCache() {
        return (ConcurrentMapCache) cacheManager.getCache(GameCacheNames.PRODUCT_PAGE);
    }

    private static GameEntity game(Long itemId, String gameName) {
        GameEntity game = new GameEntity();
        game.setItemId(itemId);
        game.setGameName(gameName);
        return game;
    }

    private static Page<GameEntity> pageOf(GameEntity... records) {
        Page<GameEntity> page = new Page<>(1, 10);
        page.setRecords(List.of(records));
        page.setTotal(records.length);
        return page;
    }
}
