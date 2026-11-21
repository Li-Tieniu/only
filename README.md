# only · 游戏账号交易平台

only 是一个围绕游戏账号交易开发的个人项目，包含用户端和管理后台。用户可以浏览商品、下单、取消订单和模拟支付，管理员可以维护商品。

一个账号对应一件商品，不能重复出售。因此，这个项目主要解决几件具体的事：多人同时下单时只能有一个人成功；支付和取消不能把状态改乱；商品改价不能影响历史订单；Redis 出故障时交易仍能继续。支付使用模拟流程，不涉及真实扣款。

## 主要实现

- **用数据库保证商品不会被重复占用。** 下单时通过 MySQL 条件 UPDATE 抢占在售商品，检查影响行数后再创建订单。两步放在同一个事务里，插单失败就回滚占用。支付和取消也先竞争订单状态，再更新商品，避免已支付的商品被错误释放。实现见 [OrderService](common/src/main/java/com/game/common/order/service/OrderService.java) 和 [GameMapper](common/src/main/java/com/game/common/trading/mapper/GameMapper.java)。
- **保留订单成交时的价格。** 商品和订单统一使用 BigDecimal，下单时保存价格快照，之后商品改价不影响旧订单。输入金额由后端校验，拒绝负数、超过两位小数和超出数据库范围的值。
- **把缓存用在允许短暂过时的地方。** Redis 只缓存游戏名称建议，商品价格和能否购买始终查 MySQL。名称变更在事务提交后清缓存；Redis 读写失败时查询仍能返回，恢复后继续使用缓存。接受名称建议短暂过时，不让缓存参与交易状态判断。
- **先看执行计划，再加索引。** 在隔离 MySQL 中准备 1 万、10 万条模拟数据，用 EXPLAIN ANALYZE 和每组 7 次查询测量比较结果，为商品时间、价格排序增加两个联合索引。同时补上唯一 ID 排序，避免同价商品的分页顺序不确定。包含搜索和深分页仍有成本，具体结果保留在 [SQL 分析记录](sql/perf/results.md)中。

这些实现不只做了 Mock 测试。真实 MySQL 测试覆盖双用户抢占、支付与取消竞争、失败回滚和金额快照；真实 Redis 测试检查首次查库、再次命中、故障降级和恢复。
2026-11-18 的完整验收共执行 185 项 Maven 测试，全部通过，具体分类见[验收摘要](docs/acceptance/phase6-review.md)。这是一组本地验证结果，不代表生产负载下的性能或可用性。

## 现在能做什么

- **用户登录和权限控制**：使用 Spring Security + JWT。普通用户只能查看、支付和取消自己的订单，商品管理需要管理员权限。
- **商品浏览**：支持关键词搜索、价格区间、价格和时间排序，以及普通分页、游标分页。
- **下单和支付**：下单成功后，商品会被占用；其他用户不能再下单。模拟支付成功后，商品变成已售出。
- **取消订单**：只能取消自己的待支付订单。取消后商品重新上架；已经支付的订单不能取消。
- **金额处理**：使用 BigDecimal 和 MySQL DECIMAL(10,2)。价格可以为 0，不能为负，最多两位小数，最大为 99999999.99。
- **后台管理**：管理员可以维护在售商品。已占用、已售出的商品不能随意修改或删除。

可以在本地体验这条流程：**注册登录 → 浏览商品 → 下单 → 取消 → 重新下单 → 模拟支付**。

## 用了哪些技术

后端使用 Java 21、Spring Boot 3.2.10、Spring Security、JWT、MyBatis-Plus 3.5.7、MySQL 8 和 Redis。
前端使用 Vue 3、Vite、Vue Router、Axios 和 Element Plus。测试使用 JUnit 5、Mockito，以及真实 MySQL、Redis 集成测试。

```text
only/
├── app/          用户端后端，默认端口 8080
├── console/      管理端后端，默认端口 8081
├── common/       两个后端共用的业务、数据库访问、认证和缓存代码
├── frontend/     Vue 前端，开发端口 5173
├── sql/          建表、迁移和 SQL 性能分析材料
├── scripts/      本地 HTTP 验收脚本
└── docs/         部署、测试和设计说明
```

app 和 console 分别启动，共用数据库和 common 模块中的业务代码。这样可以统一交易规则和权限处理，减少两端重复实现；两个服务仍有共享代码和数据库带来的发布耦合。

## 怎么在本地跑起来

### 1. 准备环境

需要 JDK 21、Maven 3.9+、MySQL 8、Node.js 18+ 和与锁文件兼容的 pnpm。
之前的完整验证使用 Node.js 24.21.0、pnpm 12.10.1，具体环境见[发布准备说明](docs/publication-readiness.md)。

Redis 使用 6+。如果暂时不想安装，可以设置 `CACHE_TYPE=none`，先把项目跑起来；此时查询直接走数据库。

### 2. 初始化数据库

**下面的步骤只适用于新建的空数据库。已有数据的数据库，请看后面的“已有数据库怎么升级”，不要重新执行建表脚本。**

在项目根目录打开终端，登录本地 MySQL：

```bash
mysql -u root -p
```

输入你自己的数据库密码，然后在 MySQL 客户端执行：

```sql
CREATE DATABASE game DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE game;
SOURCE sql/v1_sys_user.sql;
SOURCE sql/v3_game_trading.sql;
SOURCE sql/v4_game_order.sql;
SOURCE sql/v5_game_sale_status.sql;
SOURCE sql/v6_game_query_indexes.sql;
```

脚本编号里没有 V2，这是项目原有的编号。V5 增加商品销售状态，V6 增加查询索引。
脚本不会创建带固定密码的用户或管理员账号。

### 3. 配置数据库和登录密钥

不要把自己的密码直接写进 application.yml，配置通过环境变量传入。

| 环境变量 | 怎么填写 |
| --- | --- |
| `DB_USERNAME` | 你的数据库用户名；默认 root，正式部署应使用权限更小的专用账号 |
| `DB_PASSWORD` | 你的数据库密码，必填 |
| `JWT_SECRET` | 自己生成的随机签名密钥，UTF-8 编码至少 32 字节；不要用测试代码里的固定值 |
| `SPRING_DATASOURCE_URL` | 默认连接本机 3306 端口的 game 库；地址、端口或库名不同时需要设置 |
| `CACHE_TYPE` | 使用 Redis 填 redis；不使用填 none，默认 redis |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址和端口，默认 localhost / 6379 |
| `REDIS_PASSWORD` | Redis 设置了密码时填写，否则留空 |

**app 和 console 必须使用相同的 JWT_SECRET，也要连接同一个数据库。**

在 IDEA 中，打开 Run → Edit Configurations，分别为 `AppApplication` 和 `ConsoleApplication` 设置 Environment variables。
如果通过终端启动，就在对应终端设置环境变量；IDEA 中的配置不会自动传给外面的终端。

[.env.example](.env.example) 列出了配置项，但 Spring Boot 不会自动读取这个文件。仅复制或修改它，不能代替设置环境变量。
Redis 用户名、超时等其他配置见[部署说明](docs/deployment.md)。

### 4. 启动两个后端

在 IDEA 中导入根目录的 pom.xml，等待 Maven 下载依赖，然后分别运行：

- `app` 模块的 `AppApplication`。
- `console` 模块的 `ConsoleApplication`。

也可以在已经设置好环境变量的终端中打包：

```bash
mvn -B package
```

然后开两个终端，分别执行以下命令。两个终端都要有前面配置的环境变量：

```bash
java -jar app/target/app-0.0.1-SNAPSHOT.jar
```

```bash
java -jar console/target/console-0.0.1-SNAPSHOT.jar
```

### 5. 启动前端

另开一个终端，在项目根目录执行：

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm run dev
```

打开 http://localhost:5173 。前端开发服务器会把用户端和管理端请求分别转发到 8080、8081。
如果安装时提示 esbuild 的构建脚本被拦截，按所用 pnpm 版本的提示允许它，处理方法见[前端说明](frontend/README.md)。

### 6. 注册账号，体验后台

先在页面上注册普通用户并登录。项目没有预置管理员，也没有通用演示密码。

需要管理商品时，在自己的本地数据库中找到刚注册的用户，确认用户 ID 后，把该记录的 `role` 改成 `ADMIN`，再重新登录。
不要修改不认识的账号，也不要通过前端请求给自己传管理员角色，后端不会以此授权。

| 页面 | 地址 |
| --- | --- |
| 登录、注册 | `/login` |
| 商品列表 | `/products` |
| 商品详情 | `/products/:itemId` |
| 我的订单 | `/orders` |
| 商品管理 | `/admin/games` |

## 已有数据库怎么升级

如果只是移动或重命名项目目录，**不需要重建数据库，也不需要重跑已经执行过的迁移**。

先备份，再用 [acceptance_readonly.sql](sql/acceptance_readonly.sql) 检查表结构、索引和订单状态。
若还没有 `sale_status` 列，不要运行脚本中依赖该字段的状态查询。

- **缺少 V5**：先停止旧程序写入，核对是否存在同一商品对应多个有效订单、有效订单关联不存在或已删除商品等异常，再执行 [V5](sql/v5_game_sale_status.sql)。脚本也会做预检查，有异常会中止，不会替你取消订单。
- **缺少 V6**：确认已经完成 V5，并且两个新索引尚不存在，再执行 [V6](sql/v6_game_query_indexes.sql)。它只添加索引，但仍需要时间和磁盘空间。
- **两者都已完成**：不需要再次执行。不要为了解决重复列、重复索引错误而删除业务数据。

V5 的 ALTER 操作不能当作普通事务整体回滚。执行失败后先检查实际结构和回填结果，不要直接重跑；也不要手工把已占用或已售出的商品改成在售。
完整的备份、停机、迁移和验证步骤见[部署说明](docs/deployment.md)。

## 怎么运行测试

在项目根目录执行：

```bash
mvn -B test
```

普通单元测试不需要 MySQL 或 Redis 进程，但全模块中的应用启动测试仍需要 `DB_PASSWORD`、`JWT_SECRET` 等配置。
测试时使用专用配置；没有 Redis 可以设置 `CACHE_TYPE=none`。

要运行真实数据库和缓存测试，还需要设置：

| 变量 | 用途 |
| --- | --- |
| `ONLY_MYSQL_URL` | 指向隔离测试实例中的 `only_order_test` 库 |
| `ONLY_MYSQL_USER` / `ONLY_MYSQL_PASSWORD` | 该测试库的账号和密码 |
| `ONLY_REDIS_SERVER` | 本机 redis-server 可执行文件的路径 |

**数据库测试会重建测试表，不能连接业务库。** Redis 测试会自行启动独立进程，并只停止自己启动的实例。
缺少这些变量时，相应集成测试会跳过；跳过不等于通过。具体命令见[部署说明中的测试步骤](docs/deployment.md#测试复现与安全边界)。

前端构建在 frontend 目录执行：

```bash
npm run build
```

2026-11-18 的候选源码验收记录为：Maven 共 **185 项执行通过，失败 0、跳过 0**，其中真实 MySQL 38 项、Redis + MySQL 44 项；前端生产构建通过。
这是当时的实际记录，不代表之后每次修改都已重新验证。用例数量包含参数化和重复执行，详细汇总见[测试记录](docs/acceptance/candidate-test-summary.json)。

## 还有哪些不足

- 支付是模拟的，没有真实扣款、退款和支付平台回调。
- 订单没有自动超时关闭。用户下单后不支付也不取消，商品会一直被占用。
- 登录没有限流和密码复杂度要求；退出登录不会立即让已经签发的 Token 失效。前端 Token 存在 localStorage 中。
- 名称建议缓存不是强一致的；关键词包含搜索、深分页和不分页列表仍有性能限制。
- 多次分页请求之间数据可能发生变化，不能保证遍历的是同一份快照。
- 没有可一键重跑的完整浏览器自动化套件，也没有生产压力、渗透或公网部署验收。
- 前端开发代理只方便本地运行。正式部署还需要反向代理、HTTPS、备份等配置，不能直接把开发服务器当生产服务。
