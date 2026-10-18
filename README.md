# 游戏账号交易平台

一个前后端分离的个人项目，把「注册登录 → 浏览商品 → 下单 → 模拟支付」这条链路完整跑了一遍。

后端是两个 Spring Boot 服务，共用 `common` 里的业务代码：用户端跑在 8080，管理端跑在 8081。前端是 Vue 3。
数据库用 MySQL，商品查询的缓存放在 Redis 上。

## 用到的技术

- Java 21 + Maven 多模块
- Spring Boot 3.2.10
- Spring Security + JWT，做登录和权限
- MyBatis-Plus 3.5.7，只用注解和条件构造器，没写 XML
- MySQL 8
- Redis，用 Spring Cache 注解缓存商品查询
- Vue 3 + Vite + Vue Router + Axios + Element Plus
- JUnit 5 + Mockito

## 目录

```
only/
├── common/    公共代码：实体、DTO、VO、Mapper、Service、JWT、异常处理、缓存配置
├── app/       用户端，端口 8080
├── console/   管理端，端口 8081
├── frontend/  前端，端口 5173
└── sql/       建表脚本
```

## 跑之前要准备什么

- JDK 21
- Maven 3.9+
- MySQL 8，库名叫 `game`
- Redis 6+（本机没装也能跑，见下面第 3 步）
- Node 18+，只有跑前端才需要

## 三个环境变量必须先设

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | 数据库密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | JWT 签名密钥，HS256 要求至少 32 个字符 |

三个都没有默认值，不设的话服务起不来。

`JWT_SECRET` 两个服务要用同一个值，不然一边签发的 token 另一边验不过。

IDEA 里的填法：Run → Edit Configurations，分别选中 `AppApplication` 和 `ConsoleApplication`，在 Environment variables 里填，两个都要填一遍。

只在 IDEA 里跑单元测试的话，Maven Runner 的 Environment variables 里也要补上，否则测试读不到配置会直接失败。

生成本地用的密钥：

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## 启动

**1. 建库**

```sql
create database game default charset utf8mb4 collate utf8mb4_unicode_ci;
```

**2. 建表**，按这个顺序执行 `sql/` 下的脚本

```bash
mysql -u root -p game < sql/v3_game_trading.sql
mysql -u root -p game < sql/v1_sys_user.sql
mysql -u root -p game < sql/v4_game_order.sql
```

**3. 启动 Redis**

本机没有 Redis 的话，加一个 `CACHE_TYPE=none` 环境变量，缓存注解会变成空操作，接口功能不受影响，只是每次都查库。

**4. 启动后端**

```bash
mvn clean package -DskipTests
```

然后在 IDEA 里跑 `AppApplication`（8080）和 `ConsoleApplication`（8081）。不想用 IDEA 就命令行：

```bash
java -jar app/target/app-0.0.1-SNAPSHOT.jar
java -jar console/target/console-0.0.1-SNAPSHOT.jar
```

**5. 启动前端**

```bash
cd frontend
pnpm install
pnpm run dev
```

打开 http://localhost:5173 。仓库里提交的是 `pnpm-lock.yaml`，所以推荐用 pnpm，版本才可复现。

## 管理员账号

脚本里没有写死任何密码，管理员的做法是「先注册、再改角色」：

1. 在页面上注册一个普通用户
2. 进数据库把角色改成 ADMIN

```sql
update sys_user set role = 'ADMIN' where username = 'admin01';
```

3. 重新登录，顶部就会出现「商品管理」入口

## 页面

| 地址 | 页面 |
| --- | --- |
| `/login` | 登录、注册 |
| `/products` | 商品列表，支持搜索、价格区间、排序 |
| `/products/:itemId` | 商品详情、下单 |
| `/orders` | 我的订单、模拟支付 |
| `/admin/games` | 商品管理，只有 ADMIN 能看到 |

## 接口

全部接口、请求参数和返回示例都写在 **`游戏交易平台列表.md`** 里。

## 测试

```bash
mvn clean test
```

`common` 里有 33 个用例，主要测订单逻辑、缓存命中与清理、JWT 过滤器和参数校验；`app` 和 `console` 各有一个启动测试。

跑测试前同样要把三个环境变量设好。测试不需要 MySQL 和 Redis 真的在运行（连接是懒加载的），但变量必须有值。

## 还没做的 / 已知问题

- 支付是模拟的，只把订单状态从 `PENDING` 改成 `PAID`，没接真实支付
- 没有库存和购物车，同一个商品可以重复下单
- 认证时每个请求都会回查一次用户表，用户量大了要加短 TTL 缓存
- 登录没有失败次数限制，JWT 也没有黑名单
- 密码只校验了非空和长度，没做复杂度规则
- 价格在实体里存的是 `String`，下单时才转 `BigDecimal`，后面应该统一改成 `BigDecimal`
- Redis 挂掉的时候，每个请求会打两条 warn 日志，高频下会刷日志
- 前端只在开发时用 Vite 代理，生产部署要配 Nginx 做同样的转发
- 没写前端的自动化测试
