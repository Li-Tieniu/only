# 游戏交易平台前端（Vue 3 + Vite）

后端是现有的 Spring Boot 多模块项目（app 8080 / console 8081），前端通过 Vite 代理访问，
不需要额外的跨域配置。

## 运行方式

```bash
# 0) 先确保后端与依赖服务已启动：MySQL(3306)、Redis(6379)、app(8080)、console(8081)

# 1) 安装依赖
npm install          # 或者 pnpm install

# 2) 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3) 生产构建（产物在 dist/）
npm run build
```

> 用 pnpm 10+/11 时，它默认会拦截依赖的安装脚本（esbuild），需要先执行一次
> `pnpm approve-builds` 选择允许 esbuild，然后再 `pnpm install` / `pnpm run build`。
> 用 npm 没有这个问题。

## 代理配置（vite.config.js）

| 前端请求 | 转发目标 | 用途 |
| --- | --- | --- |
| `/api/**` | `http://localhost:8080` | app 模块：用户注册登录、商品查询、订单 |
| `/admin-api/**` | `http://localhost:8081` | console 模块：商品管理（仅 ADMIN） |

## 演示账号

- 普通用户：`tieniu / 123456`
- 管理员：`admin01 / 123456`

## 页面

| 路由 | 说明 |
| --- | --- |
| `/login` | 登录 + 注册（登录后 Token 存 localStorage，请求自动带 `Authorization`） |
| `/products` | 商品列表：关键词搜索、价格区间、价格排序 |
| `/products/:itemId` | 商品详情：图片轮播、价格、描述，一键下单 |
| `/orders` | 我的订单：分页、订单详情、模拟支付（需登录） |
| `/admin/games` | 商品管理：新增、编辑、下架（仅 ADMIN） |

## 实际调用的后端接口

| 前端页面 | 接口 |
| --- | --- |
| 登录 / 注册 | `POST /user/login`、`POST /user/register` |
| 商品列表 | `GET /gametrading/product?keyword&min_price&max_price&sort` |
| 商品详情 | `GET /gametrading/details?page=1&page_size=100`（前端按 itemId 匹配） |
| 下单 | `POST /order/create` |
| 我的订单 | `GET /order/my?page&page_size`、`GET /order/{orderNo}`、`POST /order/{orderNo}/pay` |
| 商品管理 | `GET /gametrading/getinfoproduct`、`GET /gametrading/getinfodetails`、`POST /gametrading/addgame`、`POST /gametrading/updategame`、`POST /gametrading/softdeletegame` |

## 已知限制

- 后端没有"按 itemId 查单个商品"的接口，详情页用分页详情接口取前 100 条后在前端匹配；
  商品总数超过 100 时可能匹配不到（可选改进：后端加一个 `GET /gametrading/details/{itemId}`）。
- 商品列表页用的是不分页的 `/gametrading/product`（当前数据量小，够用）。
- 管理端只提供"下架"（逻辑删除），不提供物理删除入口，避免误删业务数据。
- 生产部署时 `/api`、`/admin-api` 需要由 Nginx 等反向代理转发到真实后端地址。
