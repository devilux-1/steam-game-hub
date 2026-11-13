-- Steam 单机游戏资源站 数据库结构
-- 环境：MySQL 8.0+ / 9.x，字符集 utf8mb4

CREATE DATABASE IF NOT EXISTS steam_game_hub
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE steam_game_hub;

-- 游戏主表：一个游戏对应 Nexus、汉化、修改器等全部资源
CREATE TABLE IF NOT EXISTS game (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name_cn       VARCHAR(200) NULL COMMENT '中文名',
  name_en       VARCHAR(200) NOT NULL COMMENT '英文名',
  steam_appid   INT          NULL COMMENT 'Steam AppID，用于库匹配',
  nexus_domain  VARCHAR(100) NULL COMMENT 'Nexus 的 URL 标识，如 eldenring',
  cover_url     VARCHAR(500) NULL COMMENT '封面图地址',
  summary       VARCHAR(1000) NULL COMMENT '简介',
  release_date  DATE         NULL COMMENT '发售日期',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_steam_appid (steam_appid),
  KEY idx_name_en (name_en),
  KEY idx_name_cn (name_cn)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '游戏';

-- 游戏别名：支持「老头环」这类中文俗称检索
CREATE TABLE IF NOT EXISTS game_alias (
  id      BIGINT       NOT NULL AUTO_INCREMENT,
  game_id BIGINT       NOT NULL,
  alias   VARCHAR(200) NOT NULL COMMENT '别名',
  PRIMARY KEY (id),
  KEY idx_alias (alias),
  KEY idx_game (game_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '游戏别名';

-- 资源表：Nexus Mod / 汉化补丁 / 风灵月影统一存储，type 区分
CREATE TABLE IF NOT EXISTS resource (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  game_id            BIGINT        NOT NULL,
  type               VARCHAR(20)   NOT NULL COMMENT 'NEXUS_MOD / HANZI_PATCH / TRAINER',
  title              VARCHAR(300)  NOT NULL COMMENT '资源名称',
  version            VARCHAR(50)   NULL COMMENT '版本号',
  compatible_version VARCHAR(100)  NULL COMMENT '适配的游戏版本',
  author             VARCHAR(100)  NULL COMMENT '作者或汉化组',
  source_site        VARCHAR(50)   NULL COMMENT '来源站标识',
  source_url         VARCHAR(1000) NOT NULL COMMENT '跳转地址',
  description        VARCHAR(500)  NULL COMMENT '一句话说明',
  install_note       VARCHAR(1000) NULL COMMENT '安装要点',
  click_count        BIGINT        NOT NULL DEFAULT 0,
  sort_weight        INT           NOT NULL DEFAULT 0 COMMENT '越大越靠前',
  status             TINYINT       NOT NULL DEFAULT 1 COMMENT '1 上架 / 0 下架',
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_game_type (game_id, type, status, sort_weight)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资源';

-- Nexus 元数据缓存：定时任务写入，页面不直连 Nexus
CREATE TABLE IF NOT EXISTS nexus_mod_cache (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  game_id      BIGINT        NOT NULL,
  mod_id       INT           NOT NULL COMMENT 'Nexus mod id',
  name         VARCHAR(300)  NOT NULL,
  name_cn      VARCHAR(300)  NULL COMMENT '中文标题，由 AI 翻译写入；为空时前端回退英文名',
  author       VARCHAR(100)  NULL,
  endorsements INT           NOT NULL DEFAULT 0 COMMENT '点赞数，用于热度排序',
  downloads    BIGINT        NOT NULL DEFAULT 0,
  summary      VARCHAR(1000) NULL,
  summary_cn   VARCHAR(1000) NULL COMMENT '中文简介，由 AI 翻译写入；为空时前端回退英文简介',
  mod_url      VARCHAR(500)  NULL,
  synced_at    DATETIME      NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_game_mod (game_id, mod_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Nexus Mod 缓存';

-- 用户表：仅 Steam 账号，无密码
CREATE TABLE IF NOT EXISTS app_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  steam_id      VARCHAR(20)  NOT NULL COMMENT 'SteamID64',
  persona_name  VARCHAR(100) NULL,
  avatar_url    VARCHAR(500) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_steam_id (steam_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- 用户游戏库：Steam 同步结果
CREATE TABLE IF NOT EXISTS user_library (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  appid            INT          NOT NULL,
  title            VARCHAR(300) NOT NULL,
  playtime_forever INT          NOT NULL DEFAULT 0 COMMENT '总时长（分钟）',
  playtime_2weeks  INT          NOT NULL DEFAULT 0 COMMENT '近两周（分钟）',
  last_played_at   DATETIME     NULL,
  game_id          BIGINT       NULL COMMENT '匹配到站内条目后填充',
  synced_at        DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_appid (user_id, appid),
  KEY idx_user_time (user_id, playtime_forever)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户游戏库';

-- 画质选项：每个游戏可调选项及其性能/视觉权重
CREATE TABLE IF NOT EXISTS graphics_option (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  game_id             BIGINT       NOT NULL,
  option_name         VARCHAR(100) NOT NULL COMMENT '如 阴影质量',
  levels              VARCHAR(200) NOT NULL COMMENT '档位，逗号分隔：低,中,高,超高',
  cost_weight         DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '每提升一级的帧率代价权重',
  visual_value        DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '视觉收益权重',
  default_level_index INT          NOT NULL DEFAULT 0 COMMENT '默认推荐档位下标',
  PRIMARY KEY (id),
  UNIQUE KEY uk_game_option (game_id, option_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '画质选项';

-- 基准帧率：该游戏在该 GPU 档位 1080P 中画质下的实测帧率
CREATE TABLE IF NOT EXISTS fps_baseline (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  game_id      BIGINT       NOT NULL,
  gpu_tier     VARCHAR(20)  NOT NULL COMMENT 'GPU 档位编码，如 RTX4060',
  resolution   VARCHAR(10)  NOT NULL DEFAULT '1080P' COMMENT '1080P / 2K / 4K',
  ref_preset   VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM' COMMENT '基准预设',
  fps          DECIMAL(6, 1) NOT NULL COMMENT '中画质下实测帧率',
  sample_count INT          NOT NULL DEFAULT 0 COMMENT '参与统计的样本数',
  PRIMARY KEY (id),
  UNIQUE KEY uk_game_gpu_res (game_id, gpu_tier, resolution)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '基准帧率';

-- 用户回填的实际帧率，用于校准基准数据
CREATE TABLE IF NOT EXISTS fps_feedback (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  game_id       BIGINT       NOT NULL,
  gpu_tier      VARCHAR(20)  NOT NULL,
  resolution    VARCHAR(10)  NOT NULL,
  settings_json VARCHAR(2000) NULL COMMENT '提交时的画质设置快照',
  actual_fps    DECIMAL(6, 1) NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_game_gpu (game_id, gpu_tier, resolution)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '帧率反馈';

-- 管理员账号：与普通用户完全隔离
CREATE TABLE IF NOT EXISTS admin_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  last_login_at DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员';
