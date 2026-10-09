# 游戏交易平台（Spring Boot 3 + MyBatis-Plus + Vue 3）

一个可以完整演示的**游戏账号交易平台**后端 + 前端项目：支持用户注册登录、商品浏览与搜索、
下单、模拟支付、订单查询，以及管理端的商品维护。后端 JWT 认证 + Redis 缓存，
前端 Vue 3 单页应用，跨模块通过 Maven 多模块组织。

> 说明：本文档内容以当前代码为准。凡代码中未实现或未验证的部分，都在
> [已知限制](#已知限制) 里明确标注，不做夸大描述。

## 1. 功能列表

**用户端**

- 注册（用户名唯一校验、BCrypt 加密存储）、登录（返回 JWT Token）
- 商品列表：关键词搜索、最低/最高价筛选、价格升序/降序排序
- 商品详情：多图轮播、价格、描述、上架时间
- 下单：只需选择商品，用户身份由后端从 Token 解析
- 我的订单：分页查询、订单详情、对待支付订单执行「模拟支付」

**管理端（仅 ADMIN）**

- 商品列表 / 分页详情查询
- 新增商品、修改商品、下架商品（逻辑删除，数据保留）

**平台能力**

- 统一响应体 `ApiResult`、全局异常处理（参数错误 / 业务错误 / 401 / 403 / 404 / 500 分层）
- Jakarta Validation 参数校验（DTO 注解 + 查询参数范围限制）
- JWT 无状态认证 + Spring Security 接口级权限；被禁用用户的旧 Token 立即失效
- Spring Cache + Redis 缓存商品查询，商品写操作自动清理缓存
- SLF4J 业务日志（不含密码、Token、密钥）

## 2. 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 / 构建 | Java 21、Maven 多模块 |
| 框架 | Spring Boot 3.2.10、Spring Security、Spring Cache |
| 持久层 | MyBatis-Plus 3.5.7（BaseMapper + 条件构造器 + 分页插件） |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Spring Boot Data Redis，Lettuce） |
| 认证 | JWT（jjwt 0.12.6，HS256） |
| 参数校验 | Jakarta Validation（hibernate-validator） |
| 前端 | Vue 3 + Vite 5 + Vue Router 4 + Axios + Element Plus |

## 3. 模块职责

| 模块 | 职责 |
| --- | --- |
| `common` | 共享能力：统一响应体与分页结构、全局异常处理、JWT 工具与认证过滤器、Spring Security 配置、MyBatis-Plus / Redis 缓存配置；以及 `trading`（商品）、`user`（用户）、`order`（订单）三个领域的分层实现 |
| `app` | 用户端 HTTP 适配层（端口 **8080**）：`/user/**`、`/gametrading/**`（查询）、`/order/**` |
| `console` | 管理端 HTTP 适配层（端口 **8081**）：商品查询与新增/修改/删除接口（写操作需要 ADMIN） |
| `frontend` | Vue 3 单页应用：用户端页面 + 管理端页面，通过 Vite 代理调用上面两个服务 |

`common` 内部的领域分层统一为：`entity / dto / vo / mapper / service / convert`（商品另有 `cache`、`enums`）。

## 4. 架构说明

```
浏览器 (Vue 3 SPA, localhost:5173)
   │  /api/**        → Vite 代理 → app   (localhost:8080)  ─┐
   │  /admin-api/**  → Vite 代理 → console (localhost:8081) ─┤
   └────────────────────────────────────────────────────────┴─→ common（Controller → Service → Mapper）
                                                                     │            │
                                                                  MySQL 8      Redis 7
```

- **认证链路**：登录成功后签发 JWT（载荷含用户 id、username、role）→ 前端保存 Token 并在
  `Authorization: Bearer <token>` 中携带 → `JwtAuthenticationFilter` 解析 Token 并**回查用户表**
  （用户不存在或 `status != 1` 一律拒绝）→ 通过后把 `LoginUser` 与角色放入 `SecurityContext`。
- **权限**：`/user/register`、`/user/login` 匿名可访问；商品查询（GET `/gametrading/**`）匿名可访问；
  商品新增/修改/删除需要 `ROLE_ADMIN`；`/order/**` 需要登录；其余接口默认需要登录。
- **缓存链路**：商品列表与分页详情走 `@Cacheable`（缓存名 `game:product:list` / `game:product:page`，
  键包含关键词、价格区间、排序、页码、分页大小），管理端的增删改方法带 `@CacheEvict(allEntries = true)`，
  app 与 console 共用同一个 Redis，所以管理端改完用户端立刻能看到新数据。Redis 不可用时自动降级为直接查库。

## 5. 环境要求

| 依赖 | 版本 / 说明 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+（也可用 IDE 内置 Maven） |
| MySQL | 8.x，库名 `game` |
| Redis | 6+（项目使用 ACL 用户名 + 密码连接；本机没有 Redis 时可把 `CACHE_TYPE` 设为 `none`，功能不受影响） |
| Node.js | 18+（仅前端需要；建议 LTS） |

## 6. 配置项说明

配置在两个服务的 `application.yml` 中：`app/src/main/resources/application.yml`、
`console/src/main/resources/application.yml`（两个文件里的相关配置保持一致）。

**三个必填环境变量（没有默认值，缺失会导致服务启动失败）：`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`。**
仓库里不保存任何真实口令与密钥，本地开发时也请通过环境变量提供（见下一节的第 0 步）。

| 配置项 | 环境变量 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 数据库口令 | `DB_PASSWORD` | **必填** | 不设默认值；缺失时占位符解析失败，服务无法启动 |
| Redis 口令 | `REDIS_PASSWORD` | **必填** | 同上 |
| JWT 签名密钥 | `JWT_SECRET` | **必填** | 同上；HS256 要求**至少 32 字节**，app 与 console 必须使用**同一个值**，否则一边签发的 Token 另一边校验失败 |
| 数据库用户名 | `DB_USERNAME` | 可选 | 默认 `root` |
| 数据库地址 | 直接写在 `spring.datasource.url` | — | 默认 `jdbc:mysql://localhost:3306/game` |
| Redis 地址 | `REDIS_HOST` / `REDIS_PORT` / `REDIS_USERNAME` / `REDIS_DATABASE` | 可选 | 默认连接本机 6379，用户名默认 `root` |
| 缓存开关 | `CACHE_TYPE` | 可选 | 默认 `redis`；设为 `none` 时不启用缓存（缓存注解变成空操作） |
| JWT 有效期 | 写在 `jwt.expire-seconds` | — | 默认 7200 秒（2 小时） |

如果本机没有 Redis，两种做法任选：

1. 启动 Redis，并保证用户名/密码与配置一致（Redis 6+ 可用 ACL 创建账号）；
2. 用 `CACHE_TYPE=none` 启动（缓存关闭，接口功能不受影响，只是每次都查库）。

## 7. 启动步骤

0. **设置必填环境变量**（先做这一步，否则服务起不来）
   - **IDEA 里设置**：`Run → Edit Configurations…`，分别选中 `AppApplication` 与 `ConsoleApplication`
     两个运行配置，在 **Environment variables** 一栏填入下面三个变量（两个配置都要填，`JWT_SECRET` 必须相同）：

     ```
     DB_PASSWORD=<你的数据库口令>;REDIS_PASSWORD=<你的 Redis 口令>;JWT_SECRET=<至少 32 字节的随机字符串>
     ```

     如果还要在 IDEA 里跑单元测试（`AppApplicationTests` / `ConsoleApplicationTests` 会加载
     `application.yml`），同样要在 `Settings → Build, Execution, Deployment → Build Tools → Maven → Runner`
     的 Environment variables 里补上这三个变量，否则测试会因为占位符无法解析而失败。
   - **命令行设置**（只对当前窗口有效）：
     ```powershell
     # PowerShell
     $env:DB_PASSWORD = "<你的数据库口令>"
     $env:REDIS_PASSWORD = "<你的 Redis 口令>"
     $env:JWT_SECRET = "<至少 32 字节的随机字符串>"
     ```
     ```bash
     # bash / zsh
     export DB_PASSWORD='<你的数据库口令>'
     export REDIS_PASSWORD='<你的 Redis 口令>'
     export JWT_SECRET='<至少 32 字节的随机字符串>'
     ```
   - **生成本地演示用的密钥**（生成结果只放本机，不要提交到仓库）：
     ```powershell
     $bytes = New-Object byte[] 48
     [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
     [Convert]::ToBase64String($bytes)
     ```
   - 注意：环境变量只在设置它的进程/配置里生效。换终端、换运行配置、重启 IDE 后都要重新确认。

1. **建库建表**
   - 先创建数据库：
     ```sql
     create database game default charset utf8mb4 collate utf8mb4_unicode_ci;
     ```
   - 然后**按顺序**执行 `sql/` 目录下的脚本（脚本只建表 / 插示例数据，不含任何账号口令）：

     | 顺序 | 脚本 | 内容 |
     | --- | --- | --- |
     | 1 | `sql/v3_game_trading.sql` | 商品表 `game_trading`（含 1 条示例商品） |
     | 2 | `sql/v1_sys_user.sql` | 用户表 `sys_user` |
     | 3 | `sql/v4_game_order.sql` | 订单表 `game_order` |

     ```bash
     # 命令示例（cmd；PowerShell 可用 Get-Content <脚本> -Raw | mysql ...）
     mysql -h 127.0.0.1 -P 3306 -u <你的账号> -p game < sql/v3_game_trading.sql
     mysql -h 127.0.0.1 -P 3306 -u <你的账号> -p game < sql/v1_sys_user.sql
     mysql -h 127.0.0.1 -P 3306 -u <你的账号> -p game < sql/v4_game_order.sql
     ```
   - 数据库连接通过 `spring.datasource.url` + 环境变量 `DB_USERNAME` / `DB_PASSWORD` 配置，详见上一节。
2. **启动 Redis**（或设置 `CACHE_TYPE=none` 跳过）
3. **启动后端**（在项目根目录）
   ```bash
   mvn clean package -DskipTests          # 打包
   # 方式一：IDE 里分别运行 AppApplication / ConsoleApplication
   # 方式二：命令行
   java -jar app/target/app-0.0.1-SNAPSHOT.jar --server.port=8080
   java -jar console/target/console-0.0.1-SNAPSHOT.jar --server.port=8081
   ```
4. **启动前端**
   ```bash
   cd frontend
   pnpm install       # 依赖版本以仓库内 pnpm-lock.yaml 为准，见第 11 节
   pnpm run dev       # 打开 http://localhost:5173
   ```

## 8. 首次部署：创建测试账号（含 ADMIN）

**脚本里不写任何明文口令**：用户的密码必须由注册接口写入（服务端用 BCrypt 加密），
所以 ADMIN 账号的正确做法是「先注册、再改角色」。

1. 启动 app 后注册一个普通用户（前端注册页或直接调接口都行）：
   ```bash
   curl -X POST http://localhost:8080/user/register \
        -H "Content-Type: application/json" \
        -d '{"username":"admin01","password":"<自行设置>","nickname":"管理员"}'
   ```
2. 需要管理员权限时，登录 MySQL 把角色改成 `ADMIN`（这一步只改角色，不涉及口令）：
   ```sql
   use game;
   update sys_user set role = 'ADMIN' where username = 'admin01';
   select id, username, role, status from sys_user;   -- 确认 role=ADMIN、status=1
   ```
3. 用该账号登录前端，顶部会出现「商品管理」入口；若用普通用户访问 `/admin/games`，
   前端会拦截，直接调 console 的写接口也会被后端返回 403。
4. 本地演示用的普通用户同样通过注册接口创建即可（默认角色 `USER`）。

> 注意：`sys_user.status = 0` 表示禁用。被禁用的用户即使手里还有未过期的 Token，
> 也会在过滤器校验时被拒绝（返回 401）。

## 9. 页面与主要功能

| 端 | 路由 / 接口入口 | 功能 |
| --- | --- | --- |
| 用户端 | `/login` | 登录 + 注册，登录后 Token 存 localStorage |
| 用户端 | `/products` | 商品列表（搜索、价格筛选、排序） |
| 用户端 | `/products/:itemId` | 商品详情 + 下单入口 |
| 用户端 | `/orders` | 我的订单（分页、详情、模拟支付） |
| 管理端 | `/admin/games` | 商品管理（新增、编辑、下架），仅 ADMIN 可见 |

## 10. 核心 API

所有接口统一返回 `{"code":200,"message":"success","data":...}`；失败时 `code` 与 HTTP 状态码一致，
`data` 为 `null`。

**用户（app :8080）**

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/user/register` | 注册（用户名、密码、昵称），返回新用户 ID | 匿名 |
| POST | `/user/login` | 登录，返回 `{token, expireSeconds, user}` | 匿名 |
| GET | `/user/info` | 查询当前登录用户信息 | 登录 |

**商品（app :8080）**

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/gametrading/product` | 商品列表，支持 `keyword`、`min_price`、`max_price`、`sort`（`time_desc`/`price_asc`/`price_desc`） | 匿名 |
| GET | `/gametrading/details` | 商品分页详情，额外支持 `page`、`page_size`（1-100） | 匿名 |
| GET | `/gametrading/product/waterfall` | 游标分页（`cursor` + `page_size`），按 `item_id` 倒序 | 匿名 |

**订单（app :8080）**

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/order/create` | 创建订单（请求体只传 `itemId`） | 登录 |
| GET | `/order/my` | 我的订单分页（`page`、`page_size`） | 登录 |
| GET | `/order/{orderNo}` | 订单详情（校验订单归属） | 登录 |
| POST | `/order/{orderNo}/pay` | 模拟支付（仅自己的待支付订单） | 登录 |

**管理端（console :8081）**

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/gametrading/getinfoproduct` | 商品列表（支持同样的筛选与排序参数） | 匿名 |
| GET | `/gametrading/getinfodetails` | 商品分页详情 | 匿名 |
| POST | `/gametrading/addgame` | 新增商品 | ADMIN |
| POST | `/gametrading/updategame` | 修改商品（null 字段不更新） | ADMIN |
| POST | `/gametrading/softdeletegame` | 下架商品（逻辑删除） | ADMIN |

> console 还提供了物理删除接口 `/gametrading/harddeletegame`，前端**没有**接入，避免误删数据。

## 11. 测试与构建

**后端**（在项目根目录）：

```bash
mvn clean test
```

当前用例分布：`common` 33 个（订单业务 10、缓存 5、JWT 过滤器 5、参数校验 8、异常处理 5）、
`app` 与 `console` 各 1 个上下文启动测试。

> 运行后端测试前请确认 **`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET` 三个环境变量已设置**：
> `app` 与 `console` 的上下文启动测试会加载各自的 `application.yml`，缺少任何一个都会因为占位符无法解析而失败。
> 测试不需要 MySQL / Redis 真的在运行（连接是懒加载的），但变量必须有值。

**前端**（在 `frontend` 目录）：

```bash
pnpm install                 # 按仓库内 pnpm-lock.yaml 安装，保证版本可复现
pnpm install --frozen-lockfile   # 需要严格校验锁文件时使用（CI 场景）
pnpm run dev                 # 开发服务器 http://localhost:5173
pnpm run build               # 生产构建，产物在 frontend/dist
```

说明：

- 仓库里提交的是 **pnpm 锁文件**（`frontend/pnpm-lock.yaml`），所以推荐用 pnpm 安装，版本才可复现。
  用 `npm install` 也能跑起来，但 npm 会忽略该锁文件并生成自己的 `package-lock.json`。
- pnpm 10+/11 默认会拦截依赖的安装脚本（esbuild），首次安装若报
  `ERR_PNPM_IGNORED_BUILDS: esbuild`，先执行一次 `pnpm approve-builds` 允许 esbuild，再重新安装。
- 前端依赖 Node.js 18+（建议 LTS）。

## 12. 已知限制

1. **模拟支付**：`/order/{orderNo}/pay` 只是把订单状态从 `PENDING` 改为 `PAID` 并记录支付时间，
   没有接入真实支付渠道，也没有回调验签、退款、幂等键。
2. **商品详情页用分页结果匹配**：后端目前没有「按 itemId 查询单个商品」的接口，前端详情页调用
   `/gametrading/details?page=1&page_size=100` 后在内存中按 `itemId` 匹配。**商品总数超过 100 时该方案不可靠**；
   彻底解决需要后端补一个 `GET /gametrading/details/{itemId}`。
3. **开发环境使用 Vite 代理**：`/api`、`/admin-api` 的转发只在 `vite.config.js` 的开发服务器里生效；
   生产部署需要 Nginx 之类的反向代理做同样转发（前端代码保持同源相对路径，无需改代码）。
4. **尚无浏览器自动化测试**：前端验证方式是构建 + 手工/内置浏览器实跑；没有引入 Vitest / Playwright 测试工程。
5. **未做库存、购物车、优惠券、退款、超时关单**：同一商品可以重复下单，订单状态只有 `PENDING` / `PAID`。
6. **认证每次请求都回查用户表**：换来的是「禁用用户 / 改角色」立即生效；用户量大了需要加短 TTL 缓存。
7. **登录失败没有次数限制**，JWT 没有撤销列表（禁用与改角色靠回查用户表生效）。
8. **密码强度只做了非空与长度上限**，没有强制复杂度规则。
9. **Redis 不可用时**每个被缓存的请求会打两条 warn 日志（无限流），并降级为直接查库。

## 13. 演示流程与测试账号

**测试账号**：演示库中通常有两个账号——`tieniu`（普通用户 USER）与 `admin01`（管理员 ADMIN）。
它们的密码是你本地注册时设置的口令，**仓库和文档里都不保存任何明文口令**；首次部署按第 8 节
「先注册、再改角色」的方式创建即可。

**推荐演示顺序**

1. 打开 `http://localhost:5173` → 商品列表页：用关键词「王者」搜索、价格区间 200-400 筛选、
   切换「价格从高到低」，说明后端用 MyBatis-Plus 条件构造器 + 价格白名单排序。
2. 点任意商品进详情页 → 讲图片轮播、商品快照（名称/价格在下单时冻结）。
3. 未登录点「立即下单」→ 自动跳登录页，演示前端的登录态与路由守卫。
4. 用普通用户登录后下单 → 「我的订单」里看到待支付订单 → 点「模拟支付」→ 状态变已支付；
   再用详情弹窗说明后端会校验订单归属（别人的订单返回 403）。
5. 退出后用管理员登录 → 顶部出现「商品管理」→ 新增一个商品 → 回商品列表刷新能看到（说明缓存已被后端清理）
   → 回管理页「下架」该商品 → 列表里消失（逻辑删除）。
6. 想演示权限：用普通用户访问 `/admin/games` 会被前端拦下；即使绕过前端，console 的写接口也会返回 403。
7. 可选加分项：直接改数据库把某用户 `status` 置为 0，再用他原来的 Token 访问 `/user/info`，
   会立即返回 401（说明过滤器每次都会校验用户状态）。

## 14. 目录结构

```
only/
├── pom.xml                     # Maven 聚合父工程
├── README.md                   # 本文档
├── 游戏交易平台列表.md          # 早期接口/建表记录（含 game_trading 建表语句）
├── sql/
│   ├── v1_sys_user.sql         # 用户表
│   ├── v3_game_trading.sql     # 商品表（含 1 条示例数据）
│   └── v4_game_order.sql       # 订单表
├── common/                     # 共享能力 + 领域服务（无启动类）
│   └── src/main/java/com/game/common/
│       ├── config/             # MyBatis-Plus、Redis 缓存、Security、密码加密配置
│       ├── exception/          # BusinessException + 全局异常处理
│       ├── response/           # ApiResult、ListVO、PageResult、CursorPageResult
│       ├── security/           # JwtUtil、JwtAuthenticationFilter、LoginUser
│       ├── trading/            # 商品：entity/dto/vo/mapper/service/convert/cache/enums
│       ├── user/               # 用户：entity/dto/vo/mapper/service/convert
│       └── order/              # 订单：entity/dto/vo/mapper/service/convert
├── app/                        # 用户端应用（8080）
├── console/                    # 管理端应用（8081）
└── frontend/                   # Vue 3 + Vite 前端（5173）
    ├── vite.config.js          # 代理 /api → 8080，/admin-api → 8081
    └── src/{api,router,stores,utils,views,styles}
```
