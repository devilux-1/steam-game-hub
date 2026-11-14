# Steam 单机游戏资源站 — 设计文档

- 日期：2026-11-11
- 状态：已与用户确认定稿
- 对应蓝图：v3（Steam 直连版）

## 1. 背景与目标

单机玩家在给一款游戏装 Mod、找汉化、找修改器时，要在 Nexus、汉化组站点、风灵月影官网之间反复搜索。本站把这些资源按「游戏」聚合到一页，一处找齐。

目标用户：Steam 单机玩家（以中文用户为主）。

核心价值：**一个游戏一页，资源找得全、跳得到，外链不托管。**

## 2. 范围

### 2.1 做

| 编号 | 功能 | 说明 |
| --- | --- | --- |
| F1 | 游戏条目页 | 单个游戏聚合 Nexus Mod / 汉化补丁 / 风灵月影三类资源 |
| F2 | 全局搜索 | 中文名、英文名、别名均可搜到同一游戏 |
| F3 | 资源一键直达 | 点击资源卡片跳转到来源站点的下载位置，本站不托管任何文件 |
| F4 | Steam 登录 | OpenID 登录，无密码，仅用于绑定 Steam 账号 |
| F5 | 游戏库同步 | 拉取 Steam 已拥有游戏、总时长、最近两周时长、最后游玩时间 |
| F6 | 我的游戏库 | 库列表、按时长排序、跳转到站内游戏条目 |
| F9 | 后台管理 | 游戏、别名、资源的录入与编辑，Nexus 同步触发 |

### 2.2 不做（明确排除）

- Playnite 扩展、桌面端任何程序
- Epic / GOG / Xbox / PSN 等非 Steam 平台同步
- 站内文件托管、镜像下载、断点续传
- 价格监控、史低提醒、愿望单
- 社区、评论、发帖、私信、点赞
- 移动端 App、小程序

## 3. 角色

| 角色 | 能力 |
| --- | --- |
| 访客 | 浏览、搜索、点资源 |
| 登录用户 | 上述 + 同步 Steam 库、查看我的游戏库、删除自己的库数据 |
| 管理员 | 后台全部 CRUD、触发 Nexus 同步 |

## 4. 功能需求细节

### F1 游戏条目页

页面分区：顶部游戏信息（封面、中英文名、Steam 链接），下方三个 Tab。

资源卡片字段：名称、版本、适配游戏版本、更新日期、来源站、一句话说明、安装要点（可选）、点击计数。

Tab 为空时展示引导文案，不留空白。

### F2 搜索

匹配顺序：中文名精确 → 英文名精确 → 别名命中 → 中文名/英文名模糊。结果按游戏热度（资源数）排序。

### F3 一键直达

每个资源存一个外链。点击时后端异步计数，前端新窗口打开。外链可能落到来源站的登录页，页面上需说明「Nexus 需登录后才能下载」。

### F4/F5 Steam 登录与同步

登录：跳转 Steam OpenID，回调校验断言，取 SteamID64 与昵称头像，签发 JWT 并写入 httpOnly Cookie。

同步：调 `IPlayerService/GetOwnedGames`，记录 appid、名称、总时长（分钟）、两周时长、最后游玩时间。**用户的 Steam「游戏详情」为私密时接口返回空**，必须给出明确引导与截图说明。

### F6 我的游戏库

展示：总游戏数、总时长、最近两周时长。列表支持按时长/名称排序，支持筛选「从未游玩」。每行可跳到站内条目页（匹配到才有链接）。

### F7/F8 画质顾问与帧率反馈（已砍掉）

2026-11-14 移除：基准帧率与逐项画质代价权重缺少可靠数据源，预测结果不可信；对应的前端页面、后端接口、数据表与样式已全部删除。

### F9 后台

- 登录：独立账号密码表，与普通用户完全隔离
- 游戏管理：新增/编辑/删除、别名维护、Steam AppID 与 Nexus domain 绑定
- 资源管理：按类型录入、排序、上下架
- Nexus 同步：手动触发，查看上次同步时间与结果

## 5. 非功能需求

| 项 | 要求 |
| --- | --- |
| 性能 | 首页与条目页 P95 < 500ms（缓存命中）；Nexus 数据不直连请求 |
| 安全 | 密码 BCrypt；JWT 存 httpOnly Cookie；管理员接口独立鉴权；开放接口限流 |
| 可用性 | 外部接口（Steam/Nexus）超时不影响浏览主流程，降级为缓存数据 |
| 数据 | 用户可删除自己的库数据；后台操作留时间戳 |
| 部署 | 单机部署，Nginx 反代前端静态资源与后端 API |

## 6. 技术选型

| 层 | 选型 | 说明 |
| --- | --- | --- |
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Element Plus + Axios | |
| 后端 | Spring Boot 3.2.0（Java 17） | |
| 持久层 | MyBatis-Plus 3.5.17 + mybatis-spring 4.0.0 | 见 6.1 |
| 鉴权 | Spring Security + JJWT 0.12.3 | |
| 数据库 | MySQL 9.3 | 本机已运行 |
| 文档 | springdoc-openapi 2.3.0 | 本地接口文档 |
| 缓存 | Spring 进程内缓存（ConcurrentMap） | 见 6.1 |

### 6.1 与蓝图 v3 的偏差（环境约束 + 用户决策）

当前开发机无法访问外网，本地 Maven 仓库缺少部分构件，经实测后确定如下方案：

- **MyBatis-Plus 3.5.17（用户指定，已恢复）**：官方 SB3 starter 未缓存，采用手动装配（`MybatisPlusConfig` 提供 `MybatisSqlSessionFactoryBean` + `@MapperScan`），并搭配 **mybatis-spring 4.0.0**。注意：2.1.2 与 Spring 6 不兼容（实测报 `factoryBeanObjectType` 错误），4.0.0 官方面向 Spring 7，但实测在 Spring 6.1 下启动、查库、增删全通过，属经验证的非官方组合，后续如遇异常优先排查此点
- **Redis → 进程内缓存**：用于缓存 Steam/Nexus 拉取结果与热点查询；单机部署下足够，后续若引入 Redis 可替换实现而不改业务代码

此偏差不影响任何产品功能，属实现层替换。

### 6.2 代码分层约定

后端对象按职责分为三类，统一放在 `com.steamhub.entity` 下：

| 层 | 包 | 职责 | 示例 |
| --- | --- | --- | --- |
| PO | `entity.PO` | 与数据库表一一对应，只做持久化映射，不直接返回前端 | `Game`、`Resource` |
| VO | `entity.VO` | 返回给前端的展示对象，只含页面需要的字段 | `GameVO`、`ResourceVO`、`StatsOverviewVO` |
| DTO | `entity.DTO` | 接收前端入参，承载参数校验注解 | `GameQueryDTO`、`GameSaveDTO`、`ResourceSaveDTO` |

约定：Controller 只收发 VO/DTO，Service 内部完成 PO 与 VO/DTO 的转换，Mapper 只操作 PO。

## 7. 数据模型

### 7.1 game（游戏）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | 自增 |
| name_cn | varchar(200) | 中文名，可空 |
| name_en | varchar(200) | 英文名，非空 |
| steam_appid | int | 可空，用于库匹配与商店信息拉取 |
| nexus_domain | varchar(100) | 可空，Nexus 的 URL 标识 |
| cover_url | varchar(500) | 封面图地址 |
| summary | varchar(1000) | 简介 |
| release_date | date | 可空 |
| created_at / updated_at | datetime | |

索引：`uk_steam_appid(steam_appid)`、`idx_name_en(name_en)`。

### 7.2 game_alias（别名）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| game_id | bigint | 外键 game |
| alias | varchar(200) | 别名，如「老头环」 |

索引：`idx_alias(alias)`。

### 7.3 resource（资源）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| game_id | bigint | 外键 game |
| type | varchar(20) | `NEXUS_MOD` / `HANZI_PATCH` / `TRAINER` |
| title | varchar(300) | 资源名称 |
| version | varchar(50) | 版本号，可空 |
| compatible_version | varchar(100) | 适配的游戏版本，可空 |
| author | varchar(100) | 作者/汉化组，可空 |
| source_site | varchar(50) | 来源站标识 |
| source_url | varchar(1000) | 跳转地址 |
| description | varchar(500) | 一句话说明 |
| install_note | varchar(1000) | 安装要点，可空 |
| click_count | bigint | 默认 0 |
| sort_weight | int | 默认 0，越大越靠前 |
| status | tinyint | 1 上架 / 0 下架 |
| created_at / updated_at | datetime | |

索引：`idx_game_type(game_id, type, status, sort_weight)`。

### 7.4 nexus_mod_cache（Nexus 缓存）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| game_id | bigint | |
| mod_id | int | Nexus mod id |
| name | varchar(300) | |
| author | varchar(100) | |
| endorsements | int | 点赞数，用于热度排序 |
| downloads | bigint | |
| summary | varchar(1000) | |
| mod_url | varchar(500) | |
| synced_at | datetime | |

唯一索引：`uk_game_mod(game_id, mod_id)`。

### 7.5 app_user（用户）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| steam_id | varchar(20) | 唯一 |
| persona_name | varchar(100) | |
| avatar_url | varchar(500) | |
| created_at / last_login_at | datetime | |

唯一索引：`uk_steam_id(steam_id)`。

### 7.6 user_library（用户游戏库）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint | |
| appid | int | Steam AppID |
| title | varchar(300) | Steam 上的名称 |
| playtime_forever | int | 总时长（分钟） |
| playtime_2weeks | int | 近两周（分钟） |
| last_played_at | datetime | 可空 |
| game_id | bigint | 可空，匹配到站内条目后填充 |
| synced_at | datetime | |

唯一索引：`uk_user_appid(user_id, appid)`。

### 7.7–7.9 画质相关表（已砍掉）

原 `graphics_option`、`fps_baseline`、`fps_feedback` 三张表随 F7/F8 一并移除，`sql/schema.sql` 不再包含。

### 7.10 admin_user（管理员）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| username | varchar(50) | 唯一 |
| password_hash | varchar(100) | BCrypt |
| last_login_at | datetime | |

## 8. 画质顾问算法（已砍掉）

算法与数据模型随 F7/F8 一并移除，本节不再保留。

## 9. API 契约

统一响应体：`{ code, message, data }`，`code=0` 表示成功。

### 9.1 公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/games?keyword=&page=&size=` | 搜索/分页 |
| GET | `/api/games/{id}` | 游戏详情 |
| GET | `/api/games/{id}/resources?type=` | 资源列表 |
| POST | `/api/resources/{id}/click` | 点击计数 |
| GET | `/api/stats/overview` | 站内游戏数、资源数 |

### 9.2 登录与同步

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/steam/login` | 302 到 Steam |
| GET | `/api/auth/steam/callback` | 校验并下发 Cookie |
| POST | `/api/auth/logout` | 清除 Cookie |
| GET | `/api/me` | 当前用户，未登录返回 data=null |
| POST | `/api/sync/steam` | 同步 Steam 库，限流 1 次/分钟 |
| DELETE | `/api/sync/steam` | 删除我的库数据 |
| GET | `/api/me/library?sort=&filter=` | 我的游戏库 |

### 9.3 画质（已砍掉）

原 `/api/graphics/**` 接口已移除。

### 9.4 后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/auth/login` | 管理员登录 |
| GET/POST/PUT/DELETE | `/api/admin/games` | 游戏 CRUD |
| POST | `/api/admin/games/{id}/aliases` | 维护别名 |
| GET/POST/PUT/DELETE | `/api/admin/resources` | 资源 CRUD |
| POST | `/api/admin/sync/nexus` | 触发 Nexus 同步 |

## 10. 页面清单

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 首页 | 搜索框、热门游戏、最近更新资源、（登录后）右上角「同步游戏库」 |
| `/games/:id` | 游戏条目页 | 三 Tab 资源 |
| `/me/library` | 我的游戏库 | 需登录 |
| `/admin/login` | 后台登录 | |
| `/admin/games` | 游戏管理 | |
| `/admin/resources` | 资源管理 | |

## 11. 开发阶段

| 阶段 | 内容 | 预估 | 状态 |
| --- | --- | --- | --- |
| 0 | 工程骨架、数据库建表、统一响应与异常、前后端联调通路 | 2 天 | 已完成 |
| 1 | F1 F2 F3 + 后台 F9 基础 CRUD + 种子数据 | 1 周 | 已完成 |
| 2 | Nexus 数据同步与缓存 | 3 天 | 已完成（真实 Key 待申请，当前为演示数据模式） |
| 3 | F4 F5 F6 Steam 登录与库同步 | 1 周 | 已完成 |
| 4 | 画质顾问（已砍掉） | — | 已取消 |
| 5 | 打磨与部署 | 3 天 | 打磨已完成，部署待做 |

### 11.1 阶段 1 实现说明

- 查询链路：搜索/详情/资源列表/点击计数四个公开接口，热度按上架资源数排序（LEFT JOIN 计数，无 N+1）
- 后台鉴权：HS256 JWT + 拦截器，仅接管 `/admin/**`，登录接口排除；首次启动自动创建管理员 `admin/admin123`（可用 `app.admin.*` 覆盖）
- 后台 CRUD：游戏（别名全量覆盖、Steam AppID 唯一校验、删除时级联清理资源与别名）、资源（类型白名单、上下架、排序权重）
- 种子数据：`sql/data.sql` 收录 20 款单机游戏、75 条资源、55 条别名，脚本幂等可重复执行
- 踩坑记录：手动装配 `MybatisSqlSessionFactoryBean` 时必须 `setPlugins` 注入 `MybatisPlusInterceptor`，否则分页插件静默失效（不执行 count，`total` 恒为 0）

### 11.2 阶段 2 实现说明（Nexus 同步）

**流程**：遍历 `game.nexus_domain` 非空的游戏 → 调 `GET /v1/games/{domain}/mods/trending.json` → 按 `(game_id, mod_id)` upsert 到 `nexus_mod_cache` → 清理本次未出现的旧记录。

| 组件 | 职责 |
| --- | --- |
| `NexusProperties` | 配置：Key、baseUrl、应用名/版本、cron、超时、每游戏条数、请求间隔、mock 开关 |
| `NexusClient` | JDK HttpClient 发请求，带 `apikey`/`Application-Name`/`Application-Version` 头；状态码翻译成可执行提示；字段名按候选名容错解析 |
| `NexusSyncService` | 单线程异步执行，`AtomicBoolean` 保证同一时刻只有一个任务；单游戏失败不影响其它游戏，错误汇总进状态对象 |
| `NexusSyncScheduler` | `@Scheduled` 按 `nexus.api.cron` 触发，`enabled=false` 时整体不加载 |
| `NexusModService` | 只读本地缓存，按点赞数倒序返回，页面不直连 Nexus |
| `AdminSyncController` | 手动触发 + 状态查询（running/成功数/失败数/写入数/错误摘要） |

**演示数据模式**：`nexus.api.mock=true` 或未配置 Key 时，客户端不发起网络请求，返回内置示例数据，用于无 Key 环境验证链路；生产环境必须配置 `NEXUS_API_KEY` 并把 `mock` 置为 false。

**接口**：
- `GET /api/games/{id}/nexus-mods?limit=20`（公开，返回点赞数倒序的缓存数据）
- `POST /api/admin/sync/nexus`（异步触发，立即返回 `started`）
- `GET /api/admin/sync/nexus/status`（轮询进度）

### 11.3 阶段 3 与阶段 5 实现说明

- **Steam 登录**：`check_authentication` 重放回调参数时必须排除 `openid.mode`（重复的 mode 会被按 id_res 解析而判签名无效）；本地代理隧道只支持 HTTP/1.1，HttpClient 需固定 `Version.HTTP_1_1`，否则握手被中断
- **游戏库详情**：匹配到站内条目的跳 `/games/{id}`；未匹配的跳 `/library/{appid}`，封面回落到 `cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg`，简介与截图按需拉 Steam 商店接口，进程内缓存 24 小时（失败缓存 10 分钟）
- **前端体积**：除首页外全部路由懒加载，Element Plus 与 Vue 运行时拆独立 chunk，业务入口由 1134KB 降到约 20KB

## 12. 外部依赖

| 依赖 | 用途 | 备注 |
| --- | --- | --- |
| Steam Web API Key | 拉取拥有游戏 | 站方申请，公开接口 |
| Steam Store API | 游戏信息 | 无需 Key |
| Nexus API Key | 拉取精选 Mod | 站方申请，有每日配额 |
| 风灵月影 / 汉化组站点 | 外链目标 | 纯人工录入 |

## 13. 风险

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 数据冷启动 | 页面上线时内容过少 | 首批只做 50 个游戏，宁少勿滥 |
| Steam 资料私密 | 同步失败 | 明确引导文案 + 设置截图 |
| Nexus 登录墙 | 「一键直达」落到登录页 | 文案标注，不承诺免登录下载 |
| 版权 | 站点被投诉 | 只链接不托管，提供投诉入口与免责声明 |
| 无外网 | 依赖需在线下载 | 已确认全栈可离线构建；Steam/Nexus 接口在联网后联调 |
