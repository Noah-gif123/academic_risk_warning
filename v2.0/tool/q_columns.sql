SELECT c.table_name,
       c.ordinal_position,
       c.column_name,
       c.data_type,
       c.column_type,
       c.is_nullable,
       c.column_key,
       c.extra,
       IFNULL(c.column_comment, '') AS col_comment,
       IFNULL(t.table_comment, '') AS tbl_comment
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
WHERE c.table_schema = 'study_warning_system_v2'
  AND t.table_type = 'BASE TABLE'
ORDER BY c.table_name, c.ordinal_position;
