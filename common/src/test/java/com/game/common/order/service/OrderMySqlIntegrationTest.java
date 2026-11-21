package com.game.common.order.service;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.game.common.config.MybatisPlusConfig;
import com.game.common.exception.BusinessException;
import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.order.mapper.OrderMapper;
import com.game.common.trading.cache.*;
import com.game.common.trading.dto.*;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import com.game.common.trading.service.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import javax.sql.DataSource;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** 真实 MySQL/InnoDB + MyBatis Mapper + Spring 事务代理；不 Mock 数据库。
 * 仅允许专用 only_order_test 库；未设置连接变量时明确跳过。
 */
@EnabledIfEnvironmentVariable(named = "ONLY_MYSQL_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderMySqlIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    @EnableCaching
    @MapperScan(basePackageClasses = {GameMapper.class, OrderMapper.class})
    @Import({OrderService.class, GameQueryService.class, GameQueryCache.class, GameCommandService.class, GameCatalogInvalidation.class})
    static class Config {
        @Bean DataSource dataSource() {
            String url = System.getenv("ONLY_MYSQL_URL");
            if (!url.matches("jdbc:mysql://[^/]+/only_order_test(?:\\?.*)?")) {
                throw new IllegalArgumentException("Integration tests require isolated only_order_test database");
            }
            return new DriverManagerDataSource(url,
                    System.getenv().getOrDefault("ONLY_MYSQL_USER", "root"),
                    System.getenv().getOrDefault("ONLY_MYSQL_PASSWORD", ""));
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(ds);
            factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor());
            return factory.getObject();
        }
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
        @Bean ConcurrentMapCacheManager cacheManager() {
            return new ConcurrentMapCacheManager(GameCacheNames.PRODUCT_LIST, GameCacheNames.PRODUCT_PAGE, GameCacheNames.GAME_NAMES);
        }
    }

    AnnotationConfigApplicationContext context;
    JdbcTemplate jdbc;
    OrderService orders;
    GameQueryService queries;
    GameCommandService commands;
    final GameQueryDTO query = GameQueryDTO.of(null, null, null, null);

    @BeforeAll void open() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        orders = context.getBean(OrderService.class);
        queries = context.getBean(GameQueryService.class);
        commands = context.getBean(GameCommandService.class);
    }
    @AfterAll void close() { if (context != null) context.close(); }

    @BeforeEach void schema() throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS game_order");
        jdbc.execute("DROP TABLE IF EXISTS game_trading");
        jdbc.execute("DROP PROCEDURE IF EXISTS migrate_game_sale_status");
        Path sql = Path.of("../sql");
        var scripts = new ResourceDatabasePopulator(
                new FileSystemResource(sql.resolve("v3_game_trading.sql")),
                new FileSystemResource(sql.resolve("v4_game_order.sql")));
        scripts.setSqlScriptEncoding("UTF-8");
        scripts.execute(context.getBean(DataSource.class));
        String migration = Files.readString(sql.resolve("v5_game_sale_status.sql"));
        jdbc.execute(migration.substring(migration.indexOf("CREATE PROCEDURE"), migration.indexOf("END$$") + 3));
        jdbc.execute("CALL migrate_game_sale_status()");
        jdbc.execute("DROP PROCEDURE migrate_game_sale_status");
        var indexes = new ResourceDatabasePopulator(new FileSystemResource(sql.resolve("v6_game_query_indexes.sql")));
        indexes.setSqlScriptEncoding("UTF-8");
        indexes.execute(context.getBean(DataSource.class));
        // V3 的示例商品 ID=1；所有用例独立重建测试表。
        assertThat(jdbc.queryForObject("SELECT ENGINE FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='game_trading'", String.class)).isEqualTo("InnoDB");
    }

    @Test void twoUsersRacingOnlyOneCanCreate() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = List.of(pool.submit(() -> race(1L, ready, start)),
                    pool.submit(() -> race(2L, ready, start)));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM game_order", Integer.class)).isEqualTo(1);
            assertThat(state()).isEqualTo(GameEntity.LOCKED);
        } finally { start.countDown(); pool.shutdownNow(); }
    }
    boolean race(long user, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new TimeoutException();
        try { orders.create(user, new OrderCreateDTO(1L)); return true; }
        catch (BusinessException e) { assertThat(e.getCode()).isEqualTo(409); return false; }
    }

    @Test void lockPayAndRejectFurtherPurchases() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        assertThat(state()).isEqualTo(GameEntity.LOCKED);
        assertThatThrownBy(() -> orders.create(2L, new OrderCreateDTO(1L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> orders.pay(2L, order.getOrderNo())).isInstanceOf(BusinessException.class);
        orders.pay(1L, order.getOrderNo());
        assertThat(state()).isEqualTo(GameEntity.SOLD);
        assertThatThrownBy(() -> orders.create(2L, new OrderCreateDTO(1L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> orders.pay(1L, order.getOrderNo())).isInstanceOf(BusinessException.class);
    }

    @Test void deletedProductCannotBeLocked() {
        assertThat(commands.softDelete(1L)).isEqualTo(1);
        assertThatThrownBy(() -> orders.create(1L, new OrderCreateDTO(1L))).isInstanceOf(BusinessException.class);
        assertThat(state()).isEqualTo(GameEntity.ON_SALE);
        assertThat(queries.listAdminProducts(query).getItems()).isEmpty();
    }

    @Test void migrationBackfillsPendingAndPaidWithoutDeletingOrders() throws Exception {
        orders.create(1L, new OrderCreateDTO(1L));
        var dto = new GameSaveDTO();
        dto.setGameName("second"); dto.setPrice(new BigDecimal("2.00")); dto.setDetails("details");
        dto.setCoverImage("cover"); dto.setWallImages("wall");
        Long second = commands.add(dto);
        var paid = orders.create(2L, new OrderCreateDTO(second));
        orders.pay(2L, paid.getOrderNo());
        removeSaleColumn();
        createMigrationProcedure();
        jdbc.execute("CALL migrate_game_sale_status()");
        assertThat(state()).isEqualTo("LOCKED");
        assertThat(jdbc.queryForObject("SELECT sale_status FROM game_trading WHERE item_id=?", String.class, second)).isEqualTo("SOLD");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM game_order", Integer.class)).isEqualTo(2);
    }

    @Test void migrationRejectsHistoricalDuplicateOrdersBeforeAlter() throws Exception {
        orders.create(1L, new OrderCreateDTO(1L));
        jdbc.execute("INSERT INTO game_order (order_no,user_id,item_id,game_name,price,status,create_time) "
                + "SELECT 'legacy-duplicate',2,item_id,game_name,price,status,create_time FROM game_order");
        removeSaleColumn();
        createMigrationProcedure();
        assertThatThrownBy(() -> jdbc.execute("CALL migrate_game_sale_status()"))
                .hasMessageContaining("Duplicate active orders");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM game_order", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() "
                + "AND table_name='game_trading' AND column_name='sale_status'", Integer.class)).isZero();
    }

    void removeSaleColumn() {
        jdbc.execute("DROP INDEX idx_game_sale_time ON game_trading");
        jdbc.execute("DROP INDEX idx_game_sale_price ON game_trading");
        jdbc.execute("DROP INDEX idx_game_sale_status ON game_trading");
        jdbc.execute("ALTER TABLE game_trading DROP COLUMN sale_status");
    }
    void createMigrationProcedure() throws Exception {
        String migration = Files.readString(Path.of("../sql/v5_game_sale_status.sql"));
        jdbc.execute(migration.substring(migration.indexOf("CREATE PROCEDURE"), migration.indexOf("END$$") + 3));
    }

    @Test void insertFailureRollsBackLock() {
        // 真实 NOT NULL 约束失败发生在抢占之后。
        assertThatThrownBy(() -> orders.create(null, new OrderCreateDTO(1L))).isInstanceOf(RuntimeException.class);
        assertThat(state()).isEqualTo(GameEntity.ON_SALE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM game_order", Integer.class)).isZero();
        orders.create(2L, new OrderCreateDTO(1L));
    }

    @Test void failedSaleRollsBackPayment() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        jdbc.update("UPDATE game_trading SET sale_status='ON_SALE' WHERE item_id=1");
        assertThatThrownBy(() -> orders.pay(1L, order.getOrderNo())).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM game_order", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT pay_time FROM game_order", Integer.class)).isNull();
    }

    @Test void queriesBypassStaleCacheAndAdminRetainsVisibility() {
        assertThat(queries.listProducts(query).getItems()).hasSize(1);
        assertThat(queries.pageDetails(query, 1, 10, "/products").getTotal()).isEqualTo(1);
        var cache = context.getBean(org.springframework.cache.CacheManager.class);
        var stale = context.getBean(GameMapper.class).selectById(1L);
        cache.getCache(GameCacheNames.PRODUCT_LIST).put(GameCacheKeys.listKey(query), List.of(stale));
        cache.getCache(GameCacheNames.PRODUCT_PAGE).put(GameCacheKeys.pageKey(query, 1, 10), new GamePageData(List.of(stale), 1));
        var order = orders.create(1L, new OrderCreateDTO(1L));
        assertNotPurchasable();
        assertThat(queries.listAdminProducts(query).getItems()).extracting("saleStatus").containsExactly("LOCKED");
        var patch = new GameSaveDTO(); patch.setItemId(1L); patch.setPrice(new BigDecimal("1.00"));
        assertThat(commands.update(patch)).isZero();
        assertThat(commands.softDelete(1L)).isZero();
        assertThat(commands.hardDelete(1L)).isZero();
        orders.pay(1L, order.getOrderNo());
        assertNotPurchasable();
        assertThat(queries.pageAdminDetails(query, 1, 10, "/admin").getItems()).extracting("saleStatus").containsExactly("SOLD");
    }
    @Test void cancelReleasesProductAndAnotherUserCanPurchase() {
        var first = orders.create(1L, new OrderCreateDTO(1L));
        assertNotPurchasable();
        assertThat(orders.cancel(1L, first.getOrderNo()).getStatus()).isEqualTo("CANCELLED");
        assertThat(orders.getByOrderNo(1L, first.getOrderNo()).getStatus()).isEqualTo("CANCELLED");
        assertThat(state()).isEqualTo("ON_SALE");
        assertThat(queries.listProducts(query).getItems()).hasSize(1);
        assertThat(queries.pageDetails(query, 1, 10, "/products").getTotal()).isEqualTo(1);
        assertThat(queries.waterfallProducts(query, null, 10, "/products").getItems()).hasSize(1);
        assertThat(queries.getDetailById(1L).getSaleStatus()).isEqualTo("ON_SALE");
        var second = orders.create(2L, new OrderCreateDTO(1L));
        assertThat(state()).isEqualTo("LOCKED");
        // 旧订单的重试不得释放新订单占用，也不能再次支付。
        assertThatThrownBy(() -> orders.cancel(1L, first.getOrderNo())).hasMessageContaining("订单已取消");
        assertThatThrownBy(() -> orders.pay(1L, first.getOrderNo())).isInstanceOf(BusinessException.class);
        assertThat(state()).isEqualTo("LOCKED");
        orders.pay(2L, second.getOrderNo());
        assertThat(state()).isEqualTo("SOLD");
    }

    @Test void paidOrderCannotCancel() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        orders.pay(1L, order.getOrderNo());
        assertThatThrownBy(() -> orders.cancel(1L, order.getOrderNo())).hasMessageContaining("只有待支付");
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getStatus()).isEqualTo("PAID");
        assertThat(state()).isEqualTo("SOLD");
    }

    @Test void nonOwnerCannotCancel() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        assertThatThrownBy(() -> orders.cancel(2L, order.getOrderNo())).hasMessageContaining("无权取消");
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getStatus()).isEqualTo("PENDING");
        assertThat(state()).isEqualTo("LOCKED");
    }

    @Test void failedReleaseRollsBackCancellation() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        // 保留 LOCKED 但模拟异常删除，释放 SQL 的 is_deleted 条件应失败。
        jdbc.update("UPDATE game_trading SET is_deleted=1 WHERE item_id=1");
        assertThatThrownBy(() -> orders.cancel(1L, order.getOrderNo())).hasMessageContaining("商品释放失败");
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getStatus()).isEqualTo("PENDING");
        assertThat(state()).isEqualTo("LOCKED");
    }

    @RepeatedTest(10) void payAndCancelRaceHasExactlyOneWinner() throws Exception {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        runCompetingActions(() -> orders.pay(1L, order.getOrderNo()),
                () -> orders.cancel(1L, order.getOrderNo()));
        String status = orders.getByOrderNo(1L, order.getOrderNo()).getStatus();
        assertThat(status).isIn("PAID", "CANCELLED");
        assertThat(state()).isEqualTo(status.equals("PAID") ? "SOLD" : "ON_SALE");
        assertThat(jdbc.queryForObject("SELECT pay_time FROM game_order", Integer.class) != null)
                .isEqualTo(status.equals("PAID"));
    }

    @Test void concurrentDuplicateCancellationOnlyReleasesOnce() throws Exception {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        runCompetingActions(() -> orders.cancel(1L, order.getOrderNo()),
                () -> orders.cancel(1L, order.getOrderNo()));
        assertThat(state()).isEqualTo("ON_SALE");
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getStatus()).isEqualTo("CANCELLED");
    }

    void runCompetingActions(Runnable first, Runnable second) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> compete(first, ready, start));
            var b = pool.submit(() -> compete(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    boolean compete(Runnable action, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new TimeoutException();
        try { action.run(); return true; }
        catch (BusinessException e) {
            assertThat(e.getMessage()).containsAnyOf("订单状态已变更", "只有待支付", "订单已取消", "订单状态不是待支付");
            return false;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "12", "12.3", "12.34", "99999999.99"})
    void decimalPriceRoundTripsAndSnapshots(String price) {
        Long id = addPricedProduct(price);
        BigDecimal stored = context.getBean(GameMapper.class).selectById(id).getPrice();
        assertThat(stored).isEqualByComparingTo(price);
        assertThat(stored.scale()).isEqualTo(2);
        var order = orders.create(1L, new OrderCreateDTO(id));
        assertThat(order.getPrice()).isEqualByComparingTo(price);
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getPrice()).isEqualByComparingTo(price);
        assertThat(jdbc.queryForObject("SELECT price FROM game_order WHERE order_no=?", BigDecimal.class, order.getOrderNo()))
                .isEqualByComparingTo(price);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"itemId\":1,\"gameName\":\"edited\"}",
            "{\"itemId\":1,\"gameName\":\"edited\",\"price\":null}",
            "{\"itemId\":1,\"gameName\":\"edited\",\"price\":\"\"}"})
    void omittedOrEmptyPriceNeverOverwritesWithZero(String json) throws Exception {
        var patch = new ObjectMapper().readValue(json, GameSaveDTO.class);
        assertThat(commands.update(patch)).isEqualTo(1);
        assertThat(context.getBean(GameMapper.class).selectById(1L).getPrice()).isEqualByComparingTo("888.00");
    }

    @Test void changingResalePriceKeepsHistoricalOrderSnapshot() {
        var order = orders.create(1L, new OrderCreateDTO(1L));
        var patch = new GameSaveDTO(); patch.setItemId(1L); patch.setPrice(new BigDecimal("0.00"));
        assertThat(commands.update(patch)).isZero(); // LOCKED 不能改价
        orders.cancel(1L, order.getOrderNo());
        assertThat(commands.update(patch)).isEqualTo(1); // 取消后在售才允许改价
        assertThat(queries.getDetailById(1L).getPrice()).isEqualByComparingTo("0");
        assertThat(orders.getByOrderNo(1L, order.getOrderNo()).getPrice()).isEqualByComparingTo("888");
        var next = orders.create(2L, new OrderCreateDTO(1L));
        assertThat(next.getPrice()).isEqualByComparingTo("0");
        orders.pay(2L, next.getOrderNo());
        patch.setPrice(new BigDecimal("10.00"));
        assertThat(commands.update(patch)).isZero(); // SOLD 不能改价
        assertThat(orders.getByOrderNo(2L, next.getOrderNo()).getPrice()).isEqualByComparingTo("0");
    }

    @Test void decimalFiltersAndSortUseNumericOrder() {
        Long a = addPricedProduct("2.10"); Long b = addPricedProduct("10.02"); Long c = addPricedProduct("2.10");
        var filter = GameQueryDTO.of(null, new BigDecimal("2.1"), new BigDecimal("10.020"), "price_asc");
        assertThat(queries.listProducts(filter).getItems()).extracting("itemId").containsExactly(a, c, b);
        filter.setSort("price_desc");
        assertThat(queries.pageDetails(filter, 1, 10, "/products").getItems()).extracting("itemId").containsExactly(b, c, a);
        filter.setMinPrice(new BigDecimal("2.10")); filter.setMaxPrice(new BigDecimal("2.1"));
        assertThat(queries.listProducts(filter).getItems()).hasSize(2); // compareTo，不能按 scale 判不等
    }

    @ParameterizedTest
    @ValueSource(strings = {"time_desc", "price_asc", "price_desc"})
    void equalPriceAndTimePagesHaveStableUniqueOrdering(String sort) {
        List<Long> ids = List.of(addPricedProduct("12.34"), addPricedProduct("12.34"),
                addPricedProduct("12.34"), addPricedProduct("12.34"), addPricedProduct("12.34"));
        jdbc.update("UPDATE game_trading SET create_time=1700000000 WHERE game_name='price test'");
        var filter = GameQueryDTO.of("price test", new BigDecimal("12.34"), new BigDecimal("12.34"), sort);
        var expected = ids.stream().sorted(sort.equals("price_asc") ? Comparator.naturalOrder() : Comparator.reverseOrder()).toList();
        for (int repeat = 0; repeat < 2; repeat++) {
            List<Long> actual = new ArrayList<>();
            for (int page = 1; page <= 3; page++) {
                var result = queries.pageDetails(filter, page, 2, "/products");
                assertThat(result.getTotal()).isEqualTo(5);
                actual.addAll(result.getItems().stream().map(item -> item.getItemId()).toList());
            }
            assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
        }
        // 不传 sort 与显式 time_desc 的规则一致。
        if (sort.equals("time_desc")) {
            filter.setSort(null);
            assertThat(queries.pageDetails(filter, 1, 2, "/products").getItems())
                    .extracting("itemId").containsExactlyElementsOf(expected.subList(0, 2));
        }
    }

    @Test void filteredStaticCursorTraversalHasNoMissingOrDuplicateProducts() {
        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 7; i++) expected.add(addPricedProduct("15.00"));
        Long locked = addPricedProduct("15.00");
        orders.create(1L, new OrderCreateDTO(locked));
        Long sold = addPricedProduct("15.00");
        var paid = orders.create(2L, new OrderCreateDTO(sold));
        orders.pay(2L, paid.getOrderNo());
        Long deleted = addPricedProduct("15.00");
        assertThat(commands.softDelete(deleted)).isEqualTo(1);
        addPricedProduct("16.00"); // 价格过滤；V3 示例商品由关键词过滤。
        var filter = GameQueryDTO.of("price test", new BigDecimal("15"), new BigDecimal("15"), null);
        expected.sort(Comparator.reverseOrder());
        List<Long> actual = new ArrayList<>();
        Long cursor = null;
        for (int page = 0; page < 10; page++) {
            var result = queries.waterfallProducts(filter, cursor, 2, "/products");
            actual.addAll(result.getItems().stream().map(item -> item.getItemId()).toList());
            if (!result.getHasMore()) {
                assertThat(result.getNextCursor()).isNull();
                break;
            }
            assertThat(result.getNextCursor()).isEqualTo(result.getItems().get(1).getItemId());
            if (cursor != null) assertThat(result.getNextCursor()).isLessThan(cursor);
            cursor = result.getNextCursor();
            assertThat(page).isLessThan(9); // 错误游标不能导致无限循环。
        }
        assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
        assertThat(queries.pageDetails(filter, 1, 20, "/products").getTotal()).isEqualTo(7);
        // 后台复用筛选条件，但保留 LOCKED/SOLD；仍排除已删除和价格不匹配的数据。
        assertThat(queries.pageAdminDetails(filter, 1, 20, "/admin").getTotal()).isEqualTo(9);
        assertThat(queries.listAdminProducts(filter).getItems()).extracting("itemId")
                .contains(locked, sold).doesNotContain(deleted);
    }

    @Test void orderPagesAndDetailsEnforceOwnershipWithExistingIndexes() {
        var first = orders.create(1L, new OrderCreateDTO(1L));
        orders.cancel(1L, first.getOrderNo());
        var other = orders.create(2L, new OrderCreateDTO(1L));
        var latest = orders.create(1L, new OrderCreateDTO(addPricedProduct("2.00")));
        var page = orders.pageMyOrders(1L, 1, 1, "/orders");
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getItems()).extracting("orderNo").containsExactly(latest.getOrderNo());
        assertThat(orders.pageMyOrders(1L, 2, 1, "/orders").getItems())
                .extracting("orderNo").containsExactly(first.getOrderNo());
        assertThat(orders.pageMyOrders(2L, 1, 10, "/orders").getItems())
                .extracting("orderNo").containsExactly(other.getOrderNo());
        assertThat(orders.pageMyOrders(3L, 1, 10, "/orders").getItems()).isEmpty();
        assertThatThrownBy(() -> orders.getByOrderNo(1L, other.getOrderNo()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权查看");
        assertThat(orders.getByOrderNo(2L, other.getOrderNo()).getOrderNo()).isEqualTo(other.getOrderNo());
    }

    Long addPricedProduct(String price) {
        var dto = new GameSaveDTO(); dto.setGameName("price test"); dto.setPrice(new BigDecimal(price));
        dto.setCoverImage("cover"); dto.setWallImages("wall"); dto.setDetails("details");
        return commands.add(dto);
    }

    void assertNotPurchasable() {
        assertThat(queries.listProducts(query).getItems()).isEmpty();
        assertThat(queries.pageDetails(query, 1, 10, "/products").getTotal()).isZero();
        assertThat(queries.waterfallProducts(query, null, 10, "/products").getItems()).isEmpty();
        assertThatThrownBy(() -> queries.getDetailById(1L)).isInstanceOf(BusinessException.class);
    }
    String state() { return jdbc.queryForObject("SELECT sale_status FROM game_trading WHERE item_id=1", String.class); }
}
