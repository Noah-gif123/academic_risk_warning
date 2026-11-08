-- =====================================================================
-- P0-2  course_factor 初始化（v3.0）
-- 依据：v3.0/PLAN_v3.0.md §B1；v3.0/HANDOFF.md §五-2
--
-- 公式（与引擎映射一致，见 RiskEngineService#failRateFactor）：
--   factor = clamp(1 + k * (fail_rate - baseline), 0.90, 1.15)
--   baseline = risk_engine_config.fail_rate_base（历史挂科率基准，%）
--   k        = (fail_rate_step - 1) / 5   -- 每高 5 个百分点乘一次 step 的线性近似
--   上限取 min(fail_rate_cap, 1.15)：PLAN 写死 1.15，若配置更严则听配置
--
-- 口径说明：该列是「已结束学期的课程级挂科率 → 课程因子 c」的落库值，
-- 供下学期 C 通道与教务端校准散点使用；引擎只读已结束学期行（当学期与合成学期排除）。
--
-- 幂等：仅处理 course_factor 为空或 is_initialized=0 的行，可重复执行。
-- =====================================================================

UPDATE course_fail_rate_history h
JOIN risk_engine_config c ON c.is_active = 1
SET h.course_factor = ROUND(
        LEAST(
            GREATEST(1 + ((h.fail_rate - c.fail_rate_base) / 5) * (c.fail_rate_step - 1), 0.90),
            LEAST(c.fail_rate_cap, 1.15)
        ), 3),
    h.is_initialized = 1,
    h.calc_time = NOW()
WHERE h.course_factor IS NULL
   OR h.is_initialized = 0;

-- 自检：应输出 5 行，course_factor 非空、is_initialized=1
SELECT id, course_id, term_id, fail_rate, course_factor, is_initialized
FROM course_fail_rate_history
ORDER BY id;
