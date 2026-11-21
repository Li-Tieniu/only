package com.game.common.config;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/** 只处理 Spring Cache 操作异常；不会拦截 Mapper/业务方法异常。每进程最多每 30 秒一条告警。 */
public class RateLimitedCacheErrorHandler implements CacheErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(RateLimitedCacheErrorHandler.class);
    private final AtomicLong lastLog = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong suppressed = new AtomicLong();

    private void report(String operation, RuntimeException error, Cache cache) {
        long now = System.nanoTime();
        long last = lastLog.get();
        if ((last == Long.MIN_VALUE || now - last >= 30_000_000_000L) && lastLog.compareAndSet(last, now)) {
            log.warn("缓存操作失败，降级查库/忽略缓存写入: operation={}, cache={}, error={}, suppressed={}",
                    operation, cache.getName(), error.getClass().getSimpleName(), suppressed.getAndSet(0));
        } else {
            suppressed.incrementAndGet();
        }
    }
    @Override public void handleCacheGetError(RuntimeException e, Cache c, Object key) { report("get", e, c); }
    @Override public void handleCachePutError(RuntimeException e, Cache c, Object key, Object value) { report("put", e, c); }
    @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object key) { report("evict", e, c); }
    @Override public void handleCacheClearError(RuntimeException e, Cache c) { report("clear", e, c); }
}
