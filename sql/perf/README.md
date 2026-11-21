# 第五阶段 SQL 分析与复现

本目录仅用于隔离测试库。`results.md` 保留每个 SQL 在 1 万/10 万规模下、V6 前后的
EXPLAIN、EXPLAIN ANALYZE、7 次耗时、最小值/中位数/最大值。`candidate` 就是实际 V6 索引。
主 README 提供本次结果摘要；不要将单机 SQL 测量当成接口响应时间或吞吐量。

## 当前查询与索引

MyBatis-Plus 为商品查询追加 `is_deleted=0`，用户商品查询另有 `sale_status='ON_SALE'`。
下表中的 `live` 表示这两个条件，`filters` 表示可选的名称包含匹配与价格上下界。
参数由 Mapper 绑定；基准中固定字面量只是便于复现实验。`SELECT *` 对应当前实体的全部字段。

| 调用路径 | SQL 形状 | V5 时的索引/问题与本阶段处理 |
| --- | --- | --- |
| app 商品列表 → GameQueryService → GameQueryCache.listProducts → BaseMapper | SELECT 全部字段 WHERE live AND filters ORDER BY 指定排序 | 没有 LIMIT；不改变既有接口，数据大时仍有传输/内存成本 |
| app 商品分页 → GameQueryCache.pageProducts → BaseMapper.selectPage | SELECT COUNT(*) WHERE live AND filters；SELECT 全部字段 WHERE live AND filters ORDER BY … LIMIT offset,size | 原状态索引只能筛状态，时间/价格需要 filesort；V6 增加两个联合索引 |
| 瀑布流 → GameMapper.selectPage | WHERE live AND filters AND item_id < cursor ORDER BY item_id DESC LIMIT size+1 | 不查询 COUNT；既有 PRIMARY/V5 状态索引已够用，无需新增游标索引 |
| 商品详情 → GameMapper.selectById | WHERE item_id=? AND is_deleted=0 | 主键定位，服务继续校验 ON_SALE，无需新索引 |
| console 商品列表/分页 | WHERE is_deleted=0 AND filters ORDER BY …；分页另查 COUNT | 不过滤销售状态；V6 的 sale_status 未固定，不能直接满足后续排序，仍可能扫描/排序 |
| OrderService.pageMyOrders → OrderMapper.selectPage | WHERE user_id=? ORDER BY id DESC LIMIT offset,size，另查 COUNT | idx_game_order_user_id 已包含 InnoDB 主键，倒序扫描即可；不重复建 (user_id,id) |
| OrderService.findByOrderNo → OrderMapper.selectOne | WHERE order_no=? | uk_game_order_order_no 唯一定位，服务继续校验所属用户 |
| 名称建议 → GameQueryCache.gameNames → GameMapper.selectGameNames | SELECT DISTINCT game_name WHERE is_deleted=0 AND game_name LIKE '%kw%' ORDER BY game_name LIMIT 20 | 只有这一类查询使用 Redis；冷查询仍有扫描/去重/排序成本，本阶段不重构缓存 |
| 下单、支付、取消、商品写入 | 既有按 item_id 或 order_no 的原子条件 UPDATE | 主键或唯一索引定位，事务与受影响行数检查不变 |

V3/V4/V5 原索引：商品 PRIMARY(item_id)、idx_game_name(game_name)、
idx_game_sale_status(is_deleted,sale_status,item_id)；订单 PRIMARY(id)、
uk_game_order_order_no(order_no)、idx_game_order_user_id(user_id)。全部保留。

新增索引只服务两个主要用户排序路径：

- `idx_game_sale_time(is_deleted,sale_status,create_time DESC,item_id DESC)`。
- `idx_game_sale_price(is_deleted,sale_status,price ASC,item_id ASC)`，价格升序正向扫描，降序反向扫描。

等值条件放在联合索引前缀，之后是排序字段和唯一 ID。没有为了每一种筛选组合增加索引。
`price_asc_original` 特意保留原来的 price ASC / item_id DESC，展示混合方向不能共享同向索引的原因。
默认时间排序新增 item_id DESC；price_asc 的同价顺序由 ID 倒序改为正序，price_desc 保持倒序。
API 字段、价格数值语义、查询权限及销售状态限制保持不变。

`LIKE '%keyword%'` 无法从普通 B-tree 名称索引形成关键词范围定位。
本次计划选择状态/时间索引后逐行过滤，不能将“用了其他索引”解释成名称搜索已被索引加速。
命中足够多的前 20 项时可提前结束；不存在的关键词、分页 COUNT 仍需遍历候选集。
没有更换为前缀匹配或全文检索，以免改变接口搜索语义。

## 隔离基准复现

要求 JDK 21、MySQL 8、已下载的 Connector/J（由项目 Maven 依赖提供）。使用专用 MySQL
实例和**全新空库**，不要指定业务实例。下面的 13317 是本次独立测试实例端口，不是生产配置：

```powershell
# 先自行启动专用 MySQL 8 实例；以下命令不会启动或停止已有服务。
mysql --protocol=TCP -h 127.0.0.1 -P 13317 -u root -p -e "CREATE DATABASE only_perf_test_run CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
$env:ONLY_PERF_MYSQL_URL='jdbc:mysql://127.0.0.1:13317/only_perf_test_run?allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true'
$env:ONLY_MYSQL_USER='root'
$env:ONLY_MYSQL_PASSWORD='<测试库密码>'
# 在仓库根目录执行；按本机 Maven 仓库位置调整驱动路径。
$driver="$env:USERPROFILE/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"
java -cp $driver sql/perf/QueryBenchmark.java
```

程序只接受 127.0.0.1、库名 only_perf_test 或 only_perf_test_小写字母数字后缀，并拒绝非空库。
从原 V3/V4 建表、执行 V5，写入确定性的模拟数据，按原索引和实际 V6 分别测量。
仅在自己创建的测试表上删除本次新增的两个索引以进行下一规模的对照；不删除表、记录或数据库。
结束保留 10 万商品和 10 万订单供检查，索引回到 V5 状态。再次运行请另建空测试库，
不要为了复跑清空现有库。结果覆盖本目录 results.md，可先保存上次报告。

方法与边界：

- MySQL 8.0.46 / InnoDB、128 MiB buffer pool、REPEATABLE-READ；Windows 本机单客户端，JDK 21。
- 两个规模分别有 1 万/10 万商品，及同等数量的订单。5% 商品已删除，65% 总商品可购买；
  100 个循环名称，100 条商品共用一个创建秒数，确定性分散价格，用户 7 占 20% 订单。
  性能数据的价格在 10 万规模内不重复；同价分页正确性另外用真实 MySQL 集成测试验证。
- 每阶段 ANALYZE TABLE 更新统计，每条 SQL 预热 2 次、测量 7 次，完整消费结果。
  毫秒数包含 JDBC 往返/结果读取；EXPLAIN ANALYZE 另列 MySQL 算子实际耗时及行数。
- 对比同一规模、相同 SQL、相同数据，差异为 V6；旧/新排序 SQL 也分别保留。
  索引创建和预热会影响缓存状态，先 baseline 后 candidate，有顺序效应；不代表冷启动或并发性能。
- `time_offset_deep` 是实际普通时间分页形状。`id_offset_deep` 是用于控制变量的 ID 排序 OFFSET
  对照，**不是新增 API**；`id_cursor_same_page` 用事先获得的游标读取同一页，程序断言两者结果相同。
  游标边界获取成本不计入该次查询（真实客户端必须从前一页取得，不能凭空跳页）。
  `id_cursor_fetch_extra` 使用真实瀑布流的 size+1，多读一行判断 has_more。
- 普通分页实际还需 COUNT；report 单列 count_live/count_range/count_keyword/count_user_orders。
  不把 20 条 records 查询的时间当作整个分页接口时间。
- 不运行并发写负载、生产数据倾斜、网络 HTTP 压测或新增索引写放大测试；不宣称 QPS 或整体提速比例。

## 分页一致性边界

唯一 ID 作为第二排序条件保证**相同静态数据**下的确定顺序。
跨请求新增、删除、改价、占用/释放仍会改变普通 OFFSET 页面的边界，可能重复或漏看。
COUNT 与 records 是两个查询，也没有承诺同一个一致性快照。

ID 游标严格递减，静态数据完整遍历无重复无遗漏。跨请求新增的更大 ID 不会出现在后续旧游标中；
尚未访问的记录可能被删除/售出，旧 ID 也可能因取消恢复在售；改价/改名会改变筛选集合。
这不是多请求快照，不承诺遍历所有并发变更；需要刷新首页查看新集合。
最终购买仍由 MySQL 原子条件 UPDATE 判定，不依赖列表是否刚刚显示在售。
