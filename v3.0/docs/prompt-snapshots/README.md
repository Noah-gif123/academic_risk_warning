# AI 对话 / Prompt 历史快照

本目录按官网要求整理核心功能迭代的 Prompt 链。主线 3 组均有 DeepSeek Harness 原始界面截图，另附 1 组 CI 补充案例。

> 说明：Markdown 记录“用户 Prompt → AI 建议 → 人工修正 → 最终落地”。
> 原始截图从 DSH 桌面界面截取，提交时不要把整理图说成未加工截图。

## 主线三组

| 编号 | 功能迭代 | 核心文件 | 证据 |
|---|---|---|---|
| 01 | 固定录制数据与可启动交付包 | `v3.0/sql/seeds/demo_cleanup.sql`、`v3.0/sql/snapshot_demo.sql` | 1 学期 / 1 班 / 12 人 / 4 课；三容器 healthy |
| 02 | 行政班报告数字口径与 Prompt v2 | `GroupFactBuilder.java`、`AiProperties.java`、`GroupReportService.java` | 红灯 3 / 黄灯 3 / 绿灯 6；`checks=7/7` |
| 03 | 交付包真实启动验证 | `docker-compose.demo.yml`、`snapshot_demo.sql` | 三容器 healthy；四端登录；`ai_run_step` 修复 |

## 补充案例

| 编号 | 功能迭代 | 核心文件 | 证据 |
|---|---|---|---|
| 04 | P1 集成验收进入 CI | `verify_p1.mjs`、`.github/workflows/ci.yml` | 本地 `PASS=14 FAIL=0` |

## 原始 DSH 截图

```text
images/raw/01-data/       3 张：Prompt、目标数据、删除清单
images/raw/02-report/     3 张：Prompt、AI Key/报告、Prompt v2/交付包
images/raw/03-package/    3 张：Prompt、验证结果、ai_run_step 修复
images/raw/original/      用户提供的原始文件名备份
```

三组主线均已有原生界面截图。第 04 组 CI 为补充案例，暂用整理快照 `images/03-chat.png`。

> 公开仓库版本对 DSH 左侧账户名、余额等私人信息做了遮挡；未遮挡原件只保留在本地交付材料 `images/raw/original/`，不进入 GitHub。

## 整理快照

```text
images/01-chat.png
images/02-chat.png
images/03-chat.png
```

整理快照只用于阅读方便；官网要求原始凭证时，优先使用 `images/raw/`。
