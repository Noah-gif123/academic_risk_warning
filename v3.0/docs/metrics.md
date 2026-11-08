# v3.0 统计口径规格（P1-1）

> 适用范围：辅导员端 + 教务端统计模块（P1）。教师端统计推迟 v3.1。
> 数据源优先级：**物化表（P1-2/P1-3）** > 业务表实时聚合（P1-4 的降级实现）。
> 统一返回：`/api/stats` 七接口，`StatsEnvelope{current, previous, deltaRate, dataStatus, reason, sampleSize, asOf, periodStart, periodEnd}`。
> 缺数据时 `dataStatus=PARTIAL/NO_DATA` 并给出 `reason`，**禁止用 0 冒充真实值**。

## 0. 通用口径

| 项 | 定义 |
|---|---|
| 周期 | 默认自然周（周一起始 00:00:00 至周日 23:59:59）；可选 month、term；也支持 startDate/endDate 自定义 |
| 上期 | 与当前期等长的紧邻上一周期 |
| 等级序 | RED(红) > ORANGE(橙) > YELLOW(黄) > GREEN(绿) |
| “等级下降” | 复评后等级在等级序上更低（如 ORANGE→YELLOW）；跨档才算改善，同档不算 |
| 复评窗口 | 干预后 **第 7 天起、第 14 天止**；<7 天不计改善，>14 天标「逾期未复评」 |
| 关闭判定 | `alert_record.close_time is not null` 且 `status='CLOSED'`（两者一致才算关闭；物化时以 close_time 为准） |
| 范围 | 教务=全校；辅导员=`counselor_class` 中本人名下行政班；教师不开放 |

## 1. 预警关闭率

**主口径**：周期内**新增**且已关闭的预警数 ÷ 周期内新增预警数。
**辅助口径**：消化能力 = 周期内关闭总数 ÷ 周期内新增总数（可 >100%，衡量存量消化）。

```sql
-- 主口径（业务表直查版本；物化版读 stat_weekly_kpi.metric='close_rate'）
SELECT COUNT(*) AS new_total,
       SUM(close_time IS NOT NULL) AS new_closed,
       ROUND(SUM(close_time IS NOT NULL) * 100.0 / NULLIF(COUNT(*), 0), 2) AS close_rate
FROM alert_record
WHERE term_id = ? AND is_void = 0
  AND create_time >= ? AND create_time < ?;
-- 辅助：分子改为 close_time 落在周期内（不限 create_time）
```

- 分母固定“周期内新增”，不随关闭时点漂移。
- 无新增 → `NO_DATA`，`current=null`，不返回 0。
- API：`/api/stats/overview`（alertNew/alertClosed）与 `/api/stats/funnel`（首段/关闭段）。

## 2. 平均处置时长

**主口径**：周期内关闭的预警，`close_time − create_time` 的均值（小时）。
**辅助口径**：中位数（小时）与分档分布（<24h / 24–72h / >72h）。

```sql
SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, create_time, close_time)) / 60, 2) AS avg_hours,
       COUNT(*) AS closed_cnt
FROM alert_record
WHERE term_id = ? AND is_void = 0
  AND close_time >= ? AND close_time < ?;

-- 中位数（MySQL 8 无 median，用窗口排序取中间位）
SELECT ROUND(AVG(hours), 2) AS median_hours FROM (
  SELECT TIMESTAMPDIFF(MINUTE, create_time, close_time) / 60 AS hours,
         ROW_NUMBER() OVER (ORDER BY TIMESTAMPDIFF(MINUTE, create_time, close_time)) AS rn,
         COUNT(*) OVER () AS n
  FROM alert_record
  WHERE term_id = ? AND is_void = 0 AND close_time >= ? AND close_time < ?
) x WHERE rn IN ((n + 1) DIV 2, (n + 2) DIV 2);
```

- 只用已关闭样本；关闭数为 0 → `NO_DATA`。
- API：P1-4 `/api/stats/overview` 暂未输出该指标，P1-3 物化后补入 `stat_weekly_kpi`，P1-5/6 看板展示。

## 3. 干预后等级下降比例

**主口径**：周期内干预且完成 T+7 复评的记录中，**等级下降数 ÷ 有效复评数**。
**辅助口径**：持平数、恶化数、逾期未复评数。

依赖 `intervention_record` 新增字段（迁移见 `v3.0/sql/51_p1_intervention_review.sql`）：
`level_before`、`level_after`、`cohort`（TREAT/CONTROL）、`effect_check_time` 已有。

```sql
SELECT COUNT(*) AS due,
       SUM(rank(level_after) < rank(level_before)) AS improved,
       SUM(rank(level_after) = rank(level_before)) AS unchanged,
       SUM(rank(level_after) > rank(level_before)) AS worsened
FROM intervention_record ir
WHERE ir.term_id = ? AND ir.is_void = 0
  AND ir.create_time >= ? AND ir.create_time < ?
  AND ir.effect_check_time IS NOT NULL
  AND ir.effect_check_time BETWEEN ir.create_time + INTERVAL 7 DAY
                               AND ir.create_time + INTERVAL 14 DAY;
-- rank(lv): RED=4, ORANGE=3, YELLOW=2, GREEN=1（可用 CASE 展开）
```

- 复评早于 7 天不计改善；晚于 14 天计入 `overdue`，不进分母。
- 无有效复评 → `NO_DATA`。
- API：`/api/stats/effect`（P1-4 先用 `effective` 字段；P1-3 起改为等级口径）。

## 4. 7 天复评改善率

**主口径**：周期内**到期**干预（`create_time <= 周期末 − 7 天`）中，复评改善数 ÷ 到期数。
**辅助口径**：未复评数、逾期数（>14 天）。

```sql
SELECT COUNT(*) AS due_total,
       SUM(ir.effect_check_time IS NOT NULL
           AND ir.effect_check_time <= ir.create_time + INTERVAL 14 DAY
           AND rank(ir.level_after) < rank(ir.level_before)) AS improved,
       SUM(ir.effect_check_time IS NULL) AS not_reviewed,
       SUM(ir.effect_check_time > ir.create_time + INTERVAL 14 DAY) AS overdue
FROM intervention_record ir
WHERE ir.term_id = ? AND ir.is_void = 0
  AND ir.create_time <= ? - INTERVAL 7 DAY;
```

- 分母=到期数（不是全部干预数）；未复评单列，不混入改善率。
- API：`/api/stats/effect`。

## 5. 风险面变化率

**主口径**：红黄灯占比，本周期 vs 上周期（百分点差）。
`红黄占比 = (RED + ORANGE + YELLOW) ÷ student_count`。

**数据源**：`stat_daily_class`（P1-2）按周期取最后一天快照，按 scope 汇总；无物化表时 P1-4 仅返回当前风险面（`distribution`/`overview`），`previous/deltaRate=null`。

```sql
-- 单班单日快照落表（P1-3 物化）
INSERT INTO stat_daily_class(stat_date, teaching_class_id, student_count, red, orange, yellow, green,
                             avg_score, alert_new, alert_closed, intervention_count,
                             engine_version, source_max_update_time, is_stale)
SELECT CURDATE(), rs.teaching_class_id, COUNT(*),
       SUM(rs.risk_level='RED'), SUM(rs.risk_level='ORANGE'), SUM(rs.risk_level='YELLOW'), SUM(rs.risk_level='GREEN'),
       ROUND(AVG(rs.risk_score), 2),
       (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id
          AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.create_time) = CURDATE()),
       (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id
          AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.close_time) = CURDATE()),
       (SELECT COUNT(*) FROM intervention_record ir WHERE ir.term_id = rs.term_id AND ir.is_void = 0
          AND DATE(ir.create_time) = CURDATE() AND ir.student_id IN
            (SELECT student_id FROM risk_state r2 WHERE r2.teaching_class_id = rs.teaching_class_id)),
       ?, ?, 0
FROM risk_state rs
WHERE rs.term_id = ? AND rs.is_void = 0
GROUP BY rs.teaching_class_id;
```

- 周期对比：当前周期末日快照 vs 上一周期末日快照；跨周期班级新增/撤销需在 Service 对齐（按 teaching_class_id 交集）。
- API：`/api/stats/overview`（current/previous/deltaRate）与 `/api/stats/trend`。

## 6. 对照组（可归因评估）

**构造（P1-0 数据侧）**：同一批红黄灯学生中，60% 进入干预组（TREAT），40% 进入延迟干预组（CONTROL，本周期不干预、仅 T+7 复评）。`v3_cohort.json` 为确定性分桶依据（seed 固定）。

**统计**：
```
干预组改善率 = TREAT improved ÷ TREAT due
对照组改善率 = CONTROL improved ÷ CONTROL due
差值 = 干预组 − 对照组（目标 ≥20 个百分点，演示数据构造）
```

- 口径边界：准实验、非净效应；答辩需说明选择偏倚与延迟干预定义。
- API：`/api/stats/effect` 增加 cohort 分组（P1-3 后）；P1-5/6 单列“对照组对比”图。

## 7. 接口映射与降级

| 接口 | 指标 | 物化前（P1-4 现状） | 物化后（P1-3 起） |
|---|---|---|---|
| `/api/stats/overview` | 关闭率、风险面、干预数 | 实时聚合；previous 无风险历史 | 读 `stat_weekly_kpi` + `stat_daily_class` |
| `/api/stats/trend` | 风险面/事件周序列 | 仅预警/干预事件 | 读 `stat_daily_class` 周聚合 |
| `/api/stats/distribution` | 当前分布 | 实时 risk_state | 实时或最新日快照 |
| `/api/stats/compare` | 班级/课程对比 | 实时 | 最新快照 |
| `/api/stats/funnel` | 新增→处置→关闭→干预→改善 | 实时；关闭/干预为 0 时 PARTIAL | 完整 |
| `/api/stats/effect` | 指标 3/4 + 对照组 | 无干预 → NO_DATA；`effective` 字段近似 | 等级口径 + cohort |
| `/api/stats/export` | 导出 | CSV | CSV / Excel（P1-7） |

## 8. 物化与失效（P1-2/P1-3 约定）

- 三表：`stat_daily_class`（日快照，班级）、`stat_weekly_kpi`（周 KPI，scope 级）、`stat_intervention_effect`（周成效，班×cohort）。
- 每行带 `engine_version`、`source_max_update_time`、`is_stale`。
- 失效判定：最新 `risk_state.engine_version ≠ 行.engine_version` 或 `max(risk_state.update_time/alert_record.update_time/intervention_record.update_time) > source_max_update_time` → `is_stale=1`；读取时自动重算（幂等 upsert），界面显示“数据重算于 …”。
- 触发：每日定时（`@Scheduled`）+ 手动重算接口（仅教务）。

## 9. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-11-08 | P1-1 初版：五指标主/辅口径 + SQL；明确 P1-4 降级与 P1-2/P1-3 升级路径；写入 T+7/对照组/失效约定 |
