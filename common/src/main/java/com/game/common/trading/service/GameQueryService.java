package com.game.common.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.game.common.exception.BusinessException;
import com.game.common.response.CursorPageResult;
import com.game.common.response.ListVO;
import com.game.common.response.PageResult;
import com.game.common.trading.convert.GameConverter;
import com.game.common.trading.dto.GameQueryDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.enums.GameSortType;
import com.game.common.trading.mapper.GameMapper;
import com.game.common.trading.cache.GameCacheKeys;
import com.game.common.trading.cache.GamePageData;
import com.game.common.trading.cache.GameQueryCache;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易商品查询服务（app / console 共用）
 */
@Service
public class GameQueryService {

    private final GameMapper gameMapper;
    private final GameQueryCache gameQueryCache;

    public GameQueryService(GameMapper gameMapper, GameQueryCache gameQueryCache) {
        this.gameMapper = gameMapper;
        this.gameQueryCache = gameQueryCache;
    }

    /**
     * 查询商品列表（支持关键词、价格区间、排序）
     *
     * @param query 查询条件
     */
    public ListVO<ProductVO> listProducts(GameQueryDTO query) {
        LambdaQueryWrapper<GameEntity> wrapper = buildCondition(query);
        applySort(wrapper, query);
        // 先查缓存：命中就不查库；@TableLogic 会自动追加 is_deleted = 0，无需手写 where
        List<ProductVO> items = gameQueryCache.listProducts(GameCacheKeys.listKey(query), wrapper)
                .stream()
                .map(GameConverter::toProductVO)
                .toList();
        return ListVO.of(items);
    }

    /** 按主键查询商品详情，已下架商品不对外展示 */
    public DetailsVO getDetailById(Long itemId) {
        // selectById 会通过 @TableLogic 自动过滤已逻辑删除的商品。
        GameEntity entity = gameMapper.selectById(itemId);
        if (entity == null || Integer.valueOf(1).equals(entity.getIsDeleted())) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        return GameConverter.toDetailsVO(entity);
    }

    /**
     * 分页查询商品详情（支持关键词、价格区间、排序）
     *
     * @param query    查询条件
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页条数
     * @param baseUrl  当前接口基础 URL（用于生成上下页链接）
     */
    public PageResult<DetailsVO> pageDetails(GameQueryDTO query, Integer page, Integer pageSize, String baseUrl) {
        LambdaQueryWrapper<GameEntity> wrapper = buildCondition(query);
        applySort(wrapper, query);
        // 先查缓存：缓存里存的是 records + total，翻页链接仍然按当前请求拼
        GamePageData pageData = gameQueryCache.pageProducts(
                GameCacheKeys.pageKey(query, page, pageSize), wrapper, page, pageSize);
        List<DetailsVO> items = pageData.getRecords().stream()
                .map(GameConverter::toDetailsVO)
                .toList();
        return PageResult.of(items, pageData.getTotal(), page, pageSize, baseUrl);
    }

    /**
     * 瀑布流商品列表（游标分页，适合无限下滑，支持关键词与价格区间）。
     * 游标分页依赖 item_id 倒序，所以不支持自定义排序。
     *
     * @param query    查询条件（sort 必须为空）
     * @param cursor   上一屏返回的 next_cursor，首页不传
     * @param pageSize 每屏条数
     * @param baseUrl  当前接口基础 URL
     */
    public CursorPageResult<ProductVO> waterfallProducts(GameQueryDTO query, Long cursor, Integer pageSize, String baseUrl) {
        if (StringUtils.hasText(query.getSort())) {
            throw new BusinessException("瀑布流接口按 item_id 倒序分页，不支持 sort 参数");
        }
        // 多查 1 条用于判断是否还有下一屏；游标分页不需要总数，第三个参数 false 关闭 count 查询
        Page<GameEntity> pageData = gameMapper.selectPage(
                new Page<>(1, pageSize + 1L, false),
                buildCondition(query)
                        .lt(cursor != null, GameEntity::getItemId, cursor)
                        .orderByDesc(GameEntity::getItemId)
        );

        List<GameEntity> entities = new ArrayList<>(pageData.getRecords());
        boolean hasMore = entities.size() > pageSize;
        if (hasMore) {
            entities = entities.subList(0, pageSize);
        }

        List<ProductVO> items = entities.stream()
                .map(GameConverter::toProductVO)
                .toList();

        Long nextCursor = null;
        if (hasMore && !entities.isEmpty()) {
            nextCursor = entities.get(entities.size() - 1).getItemId();
        }

        return CursorPageResult.of(items, pageSize, hasMore, nextCursor, baseUrl);
    }

    /**
     * 组装 where 条件：关键词按 game_name 模糊匹配 + 价格区间，列表 / 分页 / 瀑布流共用。
     * 条件构造器里的值都以 ? 占位传参，不做任何 SQL 字符串拼接。
     */
    private LambdaQueryWrapper<GameEntity> buildCondition(GameQueryDTO query) {
        validate(query);
        String keyword = query.getKeyword();
        BigDecimal minPrice = query.getMinPrice();
        BigDecimal maxPrice = query.getMaxPrice();
        return Wrappers.<GameEntity>lambdaQuery()
                .like(StringUtils.hasText(keyword), GameEntity::getGameName, keyword)
                .ge(minPrice != null, GameEntity::getPrice, minPrice)
                .le(maxPrice != null, GameEntity::getPrice, maxPrice);
    }

    /**
     * 排序：白名单枚举决定排序列，前端只能传 time_desc / price_asc / price_desc。
     * 价格排序时再按 item_id 倒序兜底，保证同价商品分页顺序稳定。
     */
    private void applySort(LambdaQueryWrapper<GameEntity> wrapper, GameQueryDTO query) {
        switch (GameSortType.of(query.getSort())) {
            case PRICE_ASC -> wrapper.orderByAsc(GameEntity::getPrice).orderByDesc(GameEntity::getItemId);
            case PRICE_DESC -> wrapper.orderByDesc(GameEntity::getPrice).orderByDesc(GameEntity::getItemId);
            default -> wrapper.orderByDesc(GameEntity::getCreateTime);
        }
    }

    /** 参数校验：价格不能为负，最低价不能大于最高价 */
    private void validate(GameQueryDTO query) {
        BigDecimal minPrice = query.getMinPrice();
        BigDecimal maxPrice = query.getMaxPrice();
        if (minPrice != null && minPrice.signum() < 0) {
            throw new BusinessException("最低价不能小于 0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BusinessException("最高价不能小于 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("最低价不能大于最高价");
        }
    }
}
