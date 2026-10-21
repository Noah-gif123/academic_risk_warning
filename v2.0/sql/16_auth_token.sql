-- =============================================================================
-- 学情预警系统 v2.0 · 第 16 部分：登录令牌落库（后端重启不再全员掉线）
-- -----------------------------------------------------------------------------
-- 背景：v2.0 之前的令牌只存在内存里（TokenStore 的 Map），后端进程一重启，所有已登录
--       令牌立即失效，前端表现为「未登录或登录已过期」，需要重新登录。
-- 方案：令牌落到 sys_account 上（单点登录：一个账号同时只有一个有效令牌），
--       登录写入 token + token_expire_time，注销清空，每次请求按 token 查库校验。
-- 说明：token 可为 NULL（未登录），MySQL 唯一索引允许多个 NULL，因此不会互相冲突。
-- =============================================================================

SET NAMES utf8mb4;
USE study_warning_system_v2;

ALTER TABLE `sys_account`
  ADD COLUMN `token`             varchar(64) NULL COMMENT '当前登录令牌（单点登录）' AFTER `status`,
  ADD COLUMN `token_expire_time` datetime    NULL COMMENT '令牌过期时间' AFTER `token`;

ALTER TABLE `sys_account`
  ADD UNIQUE KEY `uk_token` (`token`);

SELECT COUNT(*) AS accounts, SUM(token IS NOT NULL) AS with_token FROM `sys_account`;
