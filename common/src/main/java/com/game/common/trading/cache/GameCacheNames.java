package com.game.common.trading.cache;

/**
 * 商品缓存名称：集中管理，避免各个类里硬编码字符串。
 */
public final class GameCacheNames {
    /** 非交易信息：游戏名称搜索建议，独立版本命名空间。 */
    public static final String GAME_NAMES = "only:catalog:names:v1";

    /** 商品列表（不分页） */
    public static final String PRODUCT_LIST = "game:product:list";

    /** 商品分页详情 */
    public static final String PRODUCT_PAGE = "game:product:page";

    private GameCacheNames() {
    }
}
