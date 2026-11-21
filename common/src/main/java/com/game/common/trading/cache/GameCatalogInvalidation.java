package com.game.common.trading.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** 清理发生在提交之后；CacheEvict 的错误仍交给 CacheErrorHandler，不影响已提交业务。 */
@Component
public class GameCatalogInvalidation {
    public record Changed() {}

    @TransactionalEventListener(fallbackExecution = true)
    @CacheEvict(cacheNames = {GameCacheNames.GAME_NAMES, GameCacheNames.PRODUCT_LIST,
            GameCacheNames.PRODUCT_PAGE}, allEntries = true)
    public void afterChange(Changed event) {
        // 默认为 AFTER_COMMIT；回滚不清缓存。旧列表缓存仅兼容清理，不再读取。
    }
}
