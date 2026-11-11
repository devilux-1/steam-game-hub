# 游戏百宝箱 · Steam 单机游戏资源站

按游戏聚合 Nexus 精选 Mod、汉化补丁、风灵月影修改器，并提供「稳 60 帧」画质顾问与 Steam 游戏库同步。

- 设计文档：`docs/superpowers/specs/2026-11-11-steam-game-hub-design.md`
- 技术栈：Vue 3 + Vite + Pinia + Element Plus ｜ Spring Boot 3.2 + MyBatis-Plus 3.5 + Spring Security + MySQL

## 目录结构

```
backend/    Spring Boot 后端（Java 17，MyBatis-Plus）
frontend/   Vue 3 前端（Vite）
sql/        数据库脚本
docs/       设计文档
```

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 17 及以上 |
| Maven | 3.9+ |
| Node.js | 18 及以上（本机 v24） |
| MySQL | 8.0 及以上（本机 9.3） |

## 1. 初始化数据库

```bash
mysql -u <账号> -p --default-character-set=utf8mb4 -e "source sql/schema.sql"
```

脚本会创建库 `steam_game_hub` 与 10 张表。

## 2. 启动后端

数据库账号密码通过环境变量或 `backend/src/main/resources/application-local.yml` 提供（该文件不入库，示例见 `application-local.yml.example`）。

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

服务地址 `http://localhost:8080/api`，接口文档 `http://localhost:8080/api/swagger-ui.html`。

本机离线开发时 Maven 需加 `-o` 参数并指定 `JAVA_HOME`：

```powershell
$env:JAVA_HOME="D:\java"
& "D:\data-project\apache-maven-3.9.4-bin\apache-maven-3.9.4\bin\mvn.cmd" -o -f backend\pom.xml compile
```

## 3. 启动前端

```bash
cd frontend
npm run dev
```

开发地址 `http://localhost:5173`，`/api` 已代理到后端 8080。

## 4. 构建

```bash
cd frontend && npm run build
cd backend && mvn -o package -DskipTests
```

## 开发进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 工程骨架、建表、统一响应与异常 | 进行中 |
| 1 | 游戏条目、搜索、资源展示、后台 CRUD | 未开始 |
| 2 | Nexus 数据同步 | 未开始 |
| 3 | Steam 登录与游戏库同步 | 未开始 |
| 4 | 画质顾问 | 未开始 |
| 5 | 部署上线 | 未开始 |
