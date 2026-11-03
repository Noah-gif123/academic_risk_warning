-- =====================================================================
-- 40_term_synthetic_flag.sql —— 给学期加「合成数据」标记
--
-- 背景：term 8（2027-2028 学年-第1学期）是合成数据生成器的产物，名称里写着
--       「合成数据·勿混用」，但它是合法的 term 记录，会出现在学生端/导员端的
--       学期下拉里，选过去就能看到 6 门假课——之前导员端"课程数对不上"部分也源于此。
--
-- 做法：加显式标记列，学生端/导员端/教师端的学期下拉过滤掉合成学期；
--       教务端（基础数据/合成数据生成器）仍然能看到并管理它。
--       这样不依赖"名称里带不带合成"这种脆弱判断。
--
-- 幂等：列已存在则跳过；标记按名称匹配（仅首次设置）。
-- =====================================================================

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'term' AND COLUMN_NAME = 'is_synthetic');
SET @s := IF(@c = 0,
  'ALTER TABLE term ADD COLUMN is_synthetic tinyint(1) NOT NULL DEFAULT 0 COMMENT ''1=合成数据生成器的产物，不对学生/导员/教师展示'' AFTER is_current',
  'SELECT ''is_synthetic 已存在'' AS msg');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 首次标记：只处理还没被标记过、且名称里明确写着合成的学期
UPDATE term SET is_synthetic = 1
WHERE is_synthetic = 0 AND term_name LIKE '%合成%';

SELECT id, term_name, is_current, is_synthetic FROM term ORDER BY id;
