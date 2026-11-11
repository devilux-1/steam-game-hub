# Steam 单机游戏资源站 — 设计文档

- 日期：2026-11-11
- 状态：已与用户确认定稿
- 对应蓝图：v3（Steam 直连版）

## 1. 背景与目标

单机玩家在给一款游戏装 Mod、找汉化、找修改器时，要在 Nexus、汉化组站点、风灵月影官网之间反复搜索；给游戏调画质时又要在各处翻评测。本站把这些按「游戏」聚合到一页，并提供「稳 60 帧」画质建议。

目标用户：Steam 单机玩家（以中文用户为主）。

核心价值：**一个游戏一页，资源找得全、跳得到，画质有建议。**

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
| F7 | 画质顾问 | 输入 GPU 档位与分辨率，输出画质选项推荐与预估帧率 |
| F8 | 帧率反馈 | 登录用户回填实际帧率，用于数据校准 |
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
| 访客 | 浏览、搜索、点资源、用画质顾问（不保存配置） |
| 登录用户 | 上述 + 同步 Steam 库、查看我的游戏库、回填帧率、删除自己的库数据 |
| 管理员 | 后台全部 CRUD、触发 Nexus 同步、维护画质数据 |

## 4. 功能需求细节

### F1 游戏条目页

页面分区：顶部游戏信息（封面、中英文名、Steam 链接、画质顾问入口），下方三个 Tab。

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

### F7 画质顾问

输入：GPU 档位（预置列表）、分辨率（1080p/2K/4K）、游戏。输出：逐项推荐档位、预估帧率区间、若达不到 60 帧时的高优先级降档建议。

详细算法见第 8 节。

### F8 帧率反馈

登录用户提交「该游戏 + 该 GPU + 该分辨率 + 实际帧率」，数据进入校准池。

### F9 后台

- 登录：独立账号密码表，与普通用户完全隔离
- 游戏管理：新增/编辑/删除、别名维护、Steam AppID 与 Nexus domain 绑定
- 资源管理：按类型录入、排序、上下架
- Nexus 同步：手动触发，查看上次同步时间与结果
- 画质数据：画质选项与基准帧率维护

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
| 持久层 | Spring Data JPA | 见 6.1 |
| 鉴权 | Spring Security + JJWT 0.12.3 | |
| 数据库 | MySQL 9.3 | 本机已运行 |
| 文档 | springdoc-openapi 2.3.0 | 本地接口文档 |
| 缓存 | Spring 进程内缓存（ConcurrentMap） | 见 6.1 |

### 6.1 与蓝图 v3 的偏差（环境约束）

当前开发机无法访问外网，本地 Maven 仓库中不存在 MyBatis-Plus 与 Redis 的可用构件，因此：

- **MyBatis-Plus → Spring Data JPA**：功能等价，JPA 已在本地仓库中完整可用
- **Redis → 进程内缓存**：用于缓存 Steam/Nexus 拉取结果与热点查询；单机部署下足够，后续若引入 Redis 可替换实现而不改业务代码

此偏差不影响任何产品功能，属实现层替换。

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

### 7.7 graphics_option（画质选项）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| game_id | bigint | |
| option_name | varchar(100) | 如「阴影质量」 |
| levels | varchar(200) | 可选档位，逗号分隔，如 `低,中,高,超高` |
| cost_weight | decimal(4,2) | 每提升一级的帧率代价权重，1.00 为基准 |
| visual_value | decimal(4,2) | 视觉收益权重 |
| default_level_index | int | 默认推荐档位下标 |

唯一索引：`uk_game_option(game_id, option_name)`。

### 7.8 fps_baseline（基准帧率）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| game_id | bigint | |
| gpu_tier | varchar(20) | GPU 档位编码，如 `RTX4060` |
| resolution | varchar(10) | `1080P` / `2K` / `4K` |
| ref_preset | varchar(20) | 基准预设，固定 `MEDIUM` |
| fps | decimal(6,1) | 该组合在 1080P 中画质下实测帧率 |
| sample_count | int | 参与统计的样本数 |

唯一索引：`uk_game_gpu_res(game_id, gpu_tier, resolution)`。

### 7.9 fps_feedback（帧率反馈）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| user_id | bigint | 可空（匿名不允许，当前必填） |
| game_id | bigint | |
| gpu_tier | varchar(20) | |
| resolution | varchar(10) | |
| settings_json | varchar(2000) | 当时的画质设置快照 |
| actual_fps | decimal(6,1) | |
| created_at | datetime | |

### 7.10 admin_user（管理员）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | |
| username | varchar(50) | 唯一 |
| password_hash | varchar(100) | BCrypt |
| last_login_at | datetime | |

## 8. 画质顾问算法

### 8.1 输入

- `gpuTier`：GPU 档位编码
- `resolution`：1080P / 2K / 4K
- `gameId`

### 8.2 计算步骤

1. **取基准**：查 `fps_baseline` 中该游戏 + 该 GPU + 1080P 的帧率 `F0`。
2. **分辨率修正**：设目标分辨率像素量为 1080P 的 `k` 倍（1080P=1，2K≈1.78，4K≈4.0），
   预估帧率 `F1 = F0 / (k ^ 0.75)`。
   - 指数 0.75 是经验值：帧率随像素量增长并非线性下降，具体由历史反馈拟合。
3. **逐项定档**：
   - 若 `F1 >= 60`，所有选项取「性价比优先」策略：按 `visual_value / cost_weight` 从高到低，在不跌破 60 帧的前提下尽量拉高档位。
   - 若 `F1 < 60`，反向操作：优先降低 `cost_weight` 高且 `visual_value` 低的选项，直到预估帧率回到 60。
4. **输出**：
   - 每个选项的推荐档位
   - 预估帧率区间：`[F*0.9, F*1.1]`
   - 若仍不足 60：给出「优先关这三项」的前三项降档建议

### 8.3 光追特判

存在 `option_name` 含「光追/RT」的选项时，直接推荐关闭，并在返回中附加提示「开启光追后 60 帧难以保证」。

### 8.4 校准

`fps_feedback` 累积后，按 `(game, gpu, resolution)` 分组，样本数 ≥ 5 时用中位数修正对应基准值，并累加 `sample_count`。

### 8.5 免责

前端固定展示：预估值为社区数据推算，实际帧率受 CPU、内存、驱动、场景影响。

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

### 9.3 画质

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/graphics/gpu-tiers` | GPU 档位列表 |
| POST | `/api/graphics/recommend` | 入参 gameId/gpuTier/resolution |
| POST | `/api/graphics/feedback` | 登录用户回填 |

### 9.4 后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/auth/login` | 管理员登录 |
| GET/POST/PUT/DELETE | `/api/admin/games` | 游戏 CRUD |
| POST | `/api/admin/games/{id}/aliases` | 维护别名 |
| GET/POST/PUT/DELETE | `/api/admin/resources` | 资源 CRUD |
| POST | `/api/admin/sync/nexus` | 触发 Nexus 同步 |
| GET/POST/PUT | `/api/admin/graphics/**` | 画质数据维护 |

## 10. 页面清单

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 首页 | 搜索框、热门游戏、最近更新资源、（登录后）右上角「同步游戏库」 |
| `/games/:id` | 游戏条目页 | 三 Tab 资源 + 画质顾问入口 |
| `/games/:id/graphics` | 画质顾问 | 配置输入与结果 |
| `/me/library` | 我的游戏库 | 需登录 |
| `/admin/login` | 后台登录 | |
| `/admin/games` | 游戏管理 | |
| `/admin/resources` | 资源管理 | |
| `/admin/graphics` | 画质数据 | |

## 11. 开发阶段

| 阶段 | 内容 | 预估 |
| --- | --- | --- |
| 0 | 工程骨架、数据库建表、统一响应与异常、前后端联调通路 | 2 天 |
| 1 | F1 F2 F3 + 后台 F9 基础 CRUD | 1 周 |
| 2 | Nexus 数据同步与缓存 | 3 天 |
| 3 | F4 F5 F6 Steam 登录与库同步 | 1 周 |
| 4 | F7 F8 画质顾问 + 首批 20 游戏数据 | 1.5 周 |
| 5 | 打磨与部署 | 3 天 |

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
| 画质数据偏差 | 用户不信任 | 区间展示 + 免责 + 众包校准 |
| 版权 | 站点被投诉 | 只链接不托管，提供投诉入口与免责声明 |
| 无外网 | 依赖需在线下载 | 已确认全栈可离线构建；Steam/Nexus 接口在联网后联调 |
