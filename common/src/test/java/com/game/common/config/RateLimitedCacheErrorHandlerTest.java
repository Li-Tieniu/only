package com.game.common.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitedCacheErrorHandlerTest {
    @Test void repeatedCacheFailuresProduceOneWarning() {
        Logger logger = (Logger) LoggerFactory.getLogger(RateLimitedCacheErrorHandler.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            var handler = new RateLimitedCacheErrorHandler();
            var cache = new ConcurrentMapCache("test");
            for (int i = 0; i < 100; i++) {
                handler.handleCacheGetError(new IllegalStateException(), cache, "key");
                handler.handleCachePutError(new IllegalStateException(), cache, "key", "value");
            }
            assertThat(appender.list).hasSize(1);
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
