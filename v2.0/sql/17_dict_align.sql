-- ============================================================
-- 17_dict_align.sql  数据字典口径对齐（仅修改列注释，不动数据与结构）
-- 目的：让 DDL 注释与代码/线上数据一致，避免第 4 章数据设计与库内注释互相矛盾
--   1) enrollment.status：代码与数据实际写入 ENROLLED（在册），原注释为 ACTIVE在读
--   2) sys_account.password_hash：实现为 PBKDF2WithHmacSHA256，原注释为 BCrypt
-- 执行：mysql -uroot -proot study_warning_system_v2 < 17_dict_align.sql
-- ============================================================
USE study_warning_system_v2;

ALTER TABLE enrollment
  MODIFY COLUMN status varchar(20) NOT NULL
  COMMENT '状态：ENROLLED在册/REPEAT重修/WITHDRAWN已退课/EXEMPT免修';

ALTER TABLE sys_account
  MODIFY COLUMN password_hash varchar(100) NOT NULL
  COMMENT '口令哈希：PBKDF2WithHmacSHA256，格式 pbkdf2$迭代次数$Base64(盐)$Base64(哈希)';

SELECT '17_dict_align done' AS result;
