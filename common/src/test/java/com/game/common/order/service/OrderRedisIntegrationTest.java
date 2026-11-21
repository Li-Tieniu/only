package com.game.common.order.service;

import com.game.common.config.RedisCacheConfig;
import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.trading.cache.*;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import javax.sql.DataSource;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.ResultHandler;
import static org.assertj.core.api.Assertions.*;

/** 每次测试类自行启动独立 Redis 进程，只终止自己持有的 Process，从不 FLUSHDB/FLUSHALL。
 * 继承前三阶段全部真实 MySQL 用例，在 Redis 已启用配置下再次验证。
 */
@EnabledIfEnvironmentVariable(named = "ONLY_MYSQL_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ONLY_REDIS_SERVER", matches = ".+")
class OrderRedisIntegrationTest extends OrderMySqlIntegrationTest {
    static int port;
    Process redis;
    Path config;
    LettuceConnectionFactory connection;
    NameQueryCounter counter;

    @Configuration
    @Import(RedisCacheConfig.class)
    static class RedisTestConfig {
        @Bean LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", port),
                    LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(250))
                            .shutdownTimeout(Duration.ZERO).build());
        }
    }

    @Intercepts(@Signature(type = Executor.class, method = "query", args = {
            MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    static class NameQueryCounter implements Interceptor {
        final AtomicInteger count = new AtomicInteger();
        volatile boolean stopRedisAfterQuery;
        Runnable stop;
        @Override public Object intercept(Invocation invocation) throws Throwable {
            boolean names = ((MappedStatement) invocation.getArgs()[0]).getId().endsWith(".selectGameNames");
            if (names) count.incrementAndGet();
            Object result = invocation.proceed();
            if (names && stopRedisAfterQuery) { stopRedisAfterQuery = false; stop.run(); }
            return result;
        }
    }

    @Override @BeforeAll void open() {
        try {
            try (var socket = new java.net.ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
            Path dir = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "only-redis-test-");
            config = dir.resolve("redis.conf");
            Files.writeString(config, "bind 127.0.0.1\nport " + port
                    + "\nsave \"\"\nappendonly no\ndir .\n");
            startRedis();
            context = new AnnotationConfigApplicationContext();
            context.register(Config.class, RedisTestConfig.class);
            context.refresh();
            assertThat(context.getBean("cacheManager")).isInstanceOf(RedisCacheManager.class);
            connection = context.getBean(LettuceConnectionFactory.class);
            jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            orders = context.getBean(OrderService.class); queries = context.getBean(GameQueryService.class);
            commands = context.getBean(GameCommandService.class);
            counter = new NameQueryCounter(); counter.stop = this::stopRedis;
            context.getBean(org.apache.ibatis.session.SqlSessionFactory.class).getConfiguration().addInterceptor(counter);
        } catch (Exception e) { stopRedis(); throw new IllegalStateException(e); }
    }

    void startRedis() throws Exception {
        redis = new ProcessBuilder(System.getenv("ONLY_REDIS_SERVER"), "redis.conf").directory(config.getParent().toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(config.resolveSibling("redis.log").toFile())).start();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (!redis.isAlive()) throw new IllegalStateException("Isolated Redis exited; inspect " + config);
            try (var socket = new Socket()) { socket.connect(new InetSocketAddress("127.0.0.1", port), 100); return; }
            catch (java.io.IOException ignored) { Thread.sleep(50); }
        }
        throw new IllegalStateException("Isolated Redis did not start");
    }
    void stopRedis() {
        if (redis != null && redis.isAlive()) {
            redis.destroyForcibly();
            try { if (!redis.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Redis did not stop"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        }
    }
    void restartRedis() throws Exception {
        startRedis();
        // 不重建客户端、不 resetConnection；验证现有 Lettuce 连接自动恢复。
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            try (var c = connection.getConnection()) { if ("PONG".equals(c.ping())) return; }
            catch (org.springframework.dao.DataAccessException ignored) { /* 仍在自动重连 */ }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Redis client did not recover automatically");
    }
    @Override @AfterAll void close() { try { super.close(); } finally { stopRedis(); } }

    @BeforeEach void clearOwnSuggestionCache() {
        context.getBean(RedisCacheManager.class).getCache(GameCacheNames.GAME_NAMES).clear();
    }

    @Test void realRedisHitsSeparatesKeywordsAndInvalidatesAfterCommit() {
        String key = "王者";
        int before = counter.count.get();
        assertThat(queries.gameNames(key).getItems()).contains("王者荣耀");
        queries.gameNames(key);
        assertThat(counter.count.get() - before).isEqualTo(1);
        assertThat(queries.gameNames("no-match").getItems()).isEmpty();
        assertThat(counter.count.get() - before).isEqualTo(2);
        try (var c = connection.getConnection()) {
            Long ttl = c.keyCommands().ttl((GameCacheNames.GAME_NAMES + "::" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(ttl).isBetween(1L, 600L);
        }
        var patch = new GameSaveDTO(); patch.setItemId(1L); patch.setGameName("renamed");
        var tx = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
        tx.executeWithoutResult(status -> {
            commands.update(patch);
            assertThat(context.getBean(RedisCacheManager.class).getCache(GameCacheNames.GAME_NAMES).get(key)).isNotNull();
        });
        assertThat(context.getBean(RedisCacheManager.class).getCache(GameCacheNames.GAME_NAMES).get(key)).isNull();
        assertThat(queries.gameNames(key).getItems()).isEmpty();
        assertThat(counter.count.get() - before).isEqualTo(3);
    }

    @Test void rollbackDoesNotInvalidateOrCacheUncommittedNames() {
        queries.gameNames("王者");
        var tx = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
        tx.executeWithoutResult(status -> {
            var patch = new GameSaveDTO(); patch.setItemId(1L); patch.setGameName("uncommitted"); commands.update(patch);
            assertThat(queries.gameNames("uncommitted").getItems()).contains("uncommitted");
            status.setRollbackOnly();
        });
        var cache = context.getBean(RedisCacheManager.class).getCache(GameCacheNames.GAME_NAMES);
        assertThat(cache.get("王者")).isNotNull();
        assertThat(cache.get("uncommitted")).isNull();
        assertThat(queries.gameNames("uncommitted").getItems()).isEmpty();
    }

    @Test void redisOutageFallsBackAndTransactionsRemainSafeThenRecovers() throws Exception {
        stopRedis();
        try {
            int before = counter.count.get();
            assertThat(queries.gameNames("王者").getItems()).contains("王者荣耀");
            queries.gameNames("王者");
            assertThat(counter.count.get() - before).isEqualTo(2);
            twoUsersRacingOnlyOneCanCreate();
            var order = jdbc.queryForMap("SELECT order_no,user_id FROM game_order");
            long owner = ((Number) order.get("user_id")).longValue();
            orders.cancel(owner, (String) order.get("order_no"));
            var next = orders.create(3L, new OrderCreateDTO(1L)); orders.pay(3L, next.getOrderNo());
            assertThat(state()).isEqualTo("SOLD");
            addPricedProduct("3.00"); // 提交后清缓存失败也不能使写业务失败
        } finally { restartRedis(); }
        int before = counter.count.get();
        queries.gameNames("王者"); queries.gameNames("王者");
        assertThat(counter.count.get() - before).isEqualTo(1);
    }

    @Test void redisPutFailureDoesNotFailQuery() throws Exception {
        counter.stopRedisAfterQuery = true;
        try { assertThat(queries.gameNames("荣耀").getItems()).contains("王者荣耀"); }
        finally { counter.stopRedisAfterQuery = false; restartRedis(); }
        int before = counter.count.get();
        queries.gameNames("荣耀"); queries.gameNames("荣耀");
        assertThat(counter.count.get() - before).isEqualTo(1);
    }

    @Test void additionsAndBothDeletionsInvalidateSuggestions() {
        assertThat(queries.gameNames("price test").getItems()).isEmpty();
        Long id = addPricedProduct("1.00");
        assertThat(queries.gameNames("price test").getItems()).containsExactly("price test");
        assertThat(commands.softDelete(id)).isEqualTo(1);
        assertThat(queries.gameNames("price test").getItems()).isEmpty();
        Long other = addPricedProduct("2.00");
        queries.gameNames("price test");
        assertThat(commands.hardDelete(other)).isEqualTo(1);
        assertThat(queries.gameNames("price test").getItems()).isEmpty();
    }

    @Test void databaseFailureIsNotHiddenByRedisFallback() throws Exception {
        stopRedis();
        try {
            jdbc.execute("RENAME TABLE game_trading TO game_trading_failure_test");
            try {
                assertThatThrownBy(() -> queries.gameNames("database-failure"))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
            } finally { jdbc.execute("RENAME TABLE game_trading_failure_test TO game_trading"); }
        } finally { restartRedis(); }
    }
}
