-- =====================================================================
-- P0-3  λ 随样本量生效 + 熔断案例（v3.0）
-- 依据：v3.0/PLAN_v3.0.md §B1；v3.0/HANDOFF.md §五-3
--
-- 口径（表注释）：λ = n/(n+k)，k = risk_engine_config.delta_shrink_k；
--                 Δ̂ = raw_delta × λ（收缩后的 Δ 才是实际折算用的）
-- 现状问题：6 行全部 λ=0.620、sample_size=45（收缩"假生效"）。
-- 本脚本：
--   1) 按样本量重算所有非熔断行的 λ 与 Δ̂；
--   2) 造 2 个"样本 <10"的熔断案例（fused=1 + fuse_reason + status=FUSED），
--      引擎读到时按 1.00 处理并记 WARN，不采用其 Δ。
--
-- 幂等：重复执行结果一致（已熔断行不再被改写）。
-- =====================================================================

-- 1) 正常行：λ = n/(n+k)，Δ̂ = raw_delta × λ
UPDATE teacher_style ts
JOIN risk_engine_config c ON c.is_active = 1
SET ts.shrink_lambda = ROUND(ts.sample_size / (ts.sample_size + c.delta_shrink_k), 3),
    ts.delta = ROUND(ts.raw_delta * (ts.sample_size / (ts.sample_size + c.delta_shrink_k)), 2),
    ts.engine_version = c.engine_version,
    ts.calc_time = NOW()
WHERE ts.raw_delta IS NOT NULL
  AND ts.sample_size > 0
  AND ts.fused = 0;

-- 2) 熔断案例：teacher 1/offering 101 与 teacher 4/offering 104
--    模拟"该组合样本量仅 8 人（<10），Δ 不可信"，停用该 Δ 并送教务复核
UPDATE teacher_style ts
JOIN risk_engine_config c ON c.is_active = 1
SET ts.sample_size = 8,
    ts.shrink_lambda = ROUND(8 / (8 + c.delta_shrink_k), 3),
    ts.delta = ROUND(ts.raw_delta * 8 / (8 + c.delta_shrink_k), 2),
    ts.fused = 1,
    ts.fuse_reason = '样本量不足（8 < 10），Δ 不可信：已熔断，按 1.00 处理并送教务复核',
    ts.status = 'FUSED',
    ts.review_by = NULL,
    ts.review_time = NULL,
    ts.engine_version = c.engine_version,
    ts.calc_time = NOW()
WHERE ts.item_code = 'OVERALL'
  AND ts.fused = 0
  AND ((ts.teacher_id = 1 AND ts.offering_id = 101)
    OR (ts.teacher_id = 4 AND ts.offering_id = 104));

-- 自检：λ 应不再恒为 0.620；2 行 fused=1 且有原因
SELECT id, teacher_id, offering_id, raw_delta, delta, shrink_lambda, sample_size, fused, status, fuse_reason
FROM teacher_style
ORDER BY id;
