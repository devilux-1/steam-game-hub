# 游戏百宝箱 · Steam 单机游戏资源站

按游戏聚合 Nexus 精选 Mod、汉化补丁、风灵月影修改器，并支持 Steam 登录与游戏库同步。

- 设计文档：`docs/superpowers/specs/2026-11-11-steam-game-hub-design.md`
- 后端：Spring Boot 4.1.1（Java 17）+ MyBatis-Plus 3.5.18 + Spring Security + MySQL 9.3
- 前端：Vue 3.5 + Vite 5 + Pinia + Vue Router + Element Plus

## 目录结构

```
backend/    Spring Boot 后端（Java 17，MyBatis-Plus）
frontend/   Vue 3 前端（Vite）
sql/        数据库脚本（schema.sql 建表、data.sql 种子数据）
docs/       设计文档
```

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 17 及以上（当前开发机 21） |
| Maven | 3.9+ |
| Node.js | 18 及以上（当前开发机 v24） |
| MySQL | 8.0 及以上（当前开发机 9.3） |

## 快速开始

### 1. 初始化数据库

```bash
mysql -u <账号> -p --default-character-set=utf8mb4 -e "source sql/schema.sql"
mysql -u <账号> -p --default-character-set=utf8mb4 -e "source sql/data.sql"
```

`schema.sql` 建库 `steam_game_hub` 与 7 张表；`data.sql` 灌入 20 款游戏、75 条资源、55 条别名，两个脚本都可重复执行。

### 2. 配置环境变量

数据库账号密码、各接口密钥都通过环境变量注入，不写进仓库（本地可用 `backend/src/main/resources/application-local.yml`，该文件已被 git 忽略，示例见 `application-local.yml.example`）。

| 变量 | 必填 | 用途 |
| --- | --- | --- |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 是 | 数据库账号密码 |
| `JWT_SECRET` | 生产必填 | 令牌签名密钥，不少于 32 字节；不填则用默认值，上线前必须覆盖 |
| `STEAM_API_KEY` | 同步游戏库必填 | Steam Web API Key，用于读取玩家信息与拥有游戏 |
| `NEXUS_API_KEY` | Nexus 同步必填 | Nexus Mods API Key；为空时走演示数据模式 |
| `DEEPSEEK_API_KEY` | AI 翻译必填 | 用于把 Nexus Mod 名称与简介译成中文 |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 否 | 首个管理员账号，默认 `admin` / `admin123` |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

服务地址 `http://localhost:8080/api`，本地接口文档 `http://localhost:8080/api/swagger-ui.html`。

> 端口若被占用（例如 Steam 客户端的调试端口曾默认使用 8080），可加 `-Dspring-boot.run.arguments=--server.port=8081` 换端口，同时改 `frontend/vite.config.js` 的代理目标。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

开发地址 `http://localhost:5173`，`/api` 已代理到后端 8080。

## 功能现状

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 工程骨架、建表、统一响应与异常 | 已完成 |
| 1 | 游戏条目、搜索、资源展示、后台 CRUD、种子数据 | 已完成 |
| 2 | Nexus 数据同步与 AI 中文翻译 | 已完成 |
| 3 | Steam 登录与游戏库同步 | 已完成 |
| 4 | 画质顾问 | 已砍掉（缺可靠基准数据） |
| 5 | 打磨与部署 | 打磨已完成，部署待做 |

## 接口速查

**公开接口**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/games?keyword=&page=&size=` | 搜索/分页 |
| GET | `/api/games/{id}` | 游戏详情 |
| GET | `/api/games/{id}/resources?type=` | 资源列表 |
| GET | `/api/games/{id}/nexus-mods?limit=20` | 已缓存的 Nexus 精选 Mod |
| POST | `/api/resources/{id}/click` | 点击计数 |
| GET | `/api/stats/overview` | 站内统计 |

**登录与游戏库**（会话由 httpOnly Cookie 承载）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/steam/login` | 跳转 Steam 授权 |
| GET | `/api/auth/steam/callback` | 校验并下发会话 Cookie |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/me` | 当前用户，未登录返回 `data=null` |
| POST / DELETE | `/api/sync/steam` | 同步 / 清空我的游戏库 |
| GET | `/api/me/library?sort=&filter=` | 我的游戏库列表 |
| GET | `/api/me/library/{appid}` | 库内游戏详情（含 Steam 商店简介与截图） |

**后台接口**：`/api/admin/**` 需请求头 `Authorization: Bearer <token>`，令牌由 `POST /api/admin/auth/login` 获取，首次启动自动创建 `admin / admin123`，登录后请尽快改密。

## 实现要点

**Nexus 同步**：遍历 `game.nexus_domain` 非空的游戏，拉取热门 Mod 后按 `(game_id, mod_id)` upsert 到 `nexus_mod_cache`，再用 DeepSeek 批量翻译标题与简介；`nexus.api.mock=true` 或未配置 Key 时不发网络请求，返回内置示例数据。定时同步默认每天 04:00。

**Steam 登录**：OpenID 2.0 校验通过后写入 httpOnly Cookie；`steamcommunity.com` 在国内多数网络下被劫持，需在 `application-local.yml` 配置本地代理（`steam.proxy-host` / `steam.proxy-port`），Web API 本身可直连。

**游戏库**：同步时先按 `game.steam_appid` 匹配，未收录的游戏会自动补录进 `game` 表（英文名 + AppID + Steam 头图，中文名与简介留空待后台补全），因此库内每一项都能打开站内资源聚合页；补录失败或旧数据未刷新时走 `/library/{appid}` 兜底页，引导回游戏库重新同步。

**中文支持**：`game.zh_support` 由 Steam 商店接口的 `supported_languages` 同步（2 官方简中 / 1 仅繁中 / 0 无中文）。「汉化补丁」Tab 先给结论：官方自带简中的直接说明无需补丁，只对确实缺中文的游戏引导到外部补丁站，站内不再收录占位链接。

**前端体积**：除首页外全部路由按需加载，Element Plus 与 Vue 运行时拆成独立 chunk，业务入口约 20KB。

<img width="2466" height="1526" alt="image" src="https://github.com/user-attachments/assets/e867f889-fece-446b-a348-de13edc1ff6e" />

