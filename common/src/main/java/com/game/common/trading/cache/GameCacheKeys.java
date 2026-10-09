package com.game.common.trading.cache;

import com.game.common.trading.dto.GameQueryDTO;

/**
 * 商品查询缓存键：把会影响查询结果的参数全部拼进 key。
 * 每个片段都带字段名，避免"关键词里带分隔符"等情况下不同条件撞到同一个缓存。
 */
public final class GameCacheKeys {

    private GameCacheKeys() {
    }

    /** 列表缓存键：关键词 + 价格区间 + 排序 */
    public static String listKey(GameQueryDTO query) {
        return "kw=" + value(query.getKeyword())
                + "|min=" + value(query.getMinPrice())
                + "|max=" + value(query.getMaxPrice())
                + "|sort=" + value(query.getSort());
    }

    /** 分页缓存键：列表条件 + 页码 + 每页条数 */
    public static String pageKey(GameQueryDTO query, long page, long pageSize) {
        return listKey(query) + "|page=" + page + "|size=" + pageSize;
    }

    private static String value(Object value) {
        return value == null ? "-" : value.toString();
    }
}
