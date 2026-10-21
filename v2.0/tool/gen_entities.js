/* gen_entities.js — 按 study_warning_system_v2 的实际表结构生成 MyBatis-Plus 实体与 Mapper
 * 输入：v2.0/tool/columns.tsv（由 q_columns.sql 导出，制表符分隔）
 * 输出：v2.0/backend/src/main/java/com/example/academic_risk_warning/{entity,mapper}
 */
'use strict';
const fs = require('fs');
const path = require('path');

const root = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0';
const tsv = fs.readFileSync(path.join(root, 'tool/columns.tsv'), 'utf8').replace(/^\uFEFF/, '');
const basePkg = 'com.example.academic_risk_warning';
const entityDir = path.join(root, 'backend/src/main/java/com/example/academic_risk_warning/entity');
const mapperDir = path.join(root, 'backend/src/main/java/com/example/academic_risk_warning/mapper');
fs.mkdirSync(entityDir, { recursive: true });
fs.mkdirSync(mapperDir, { recursive: true });

const camel = s => s.replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase());
const upperFirst = s => s.charAt(0).toUpperCase() + s.slice(1);
const classOf = t => upperFirst(camel(t));

const TYPE = {
  bigint: 'Long', int: 'Integer', integer: 'Integer', mediumint: 'Integer', smallint: 'Integer', tinyint: 'Integer',
  decimal: 'BigDecimal', numeric: 'BigDecimal', double: 'Double', float: 'Float', bit: 'Boolean',
  varchar: 'String', char: 'String', text: 'String', tinytext: 'String', mediumtext: 'String', longtext: 'String',
  json: 'String', enum: 'String', set: 'String',
  datetime: 'LocalDateTime', timestamp: 'LocalDateTime', date: 'LocalDate', time: 'LocalTime', year: 'Integer'
};

const tables = new Map();
for (const line of tsv.split(/\r?\n/)) {
  if (!line.trim()) continue;
  const f = line.split('\t');
  if (f.length < 10) continue;
  const [table, ord, col, dataType, colType, nullable, key, extra, comment, tblComment] = f;
  if (!tables.has(table)) tables.set(table, { name: table, comment: tblComment, cols: [] });
  tables.get(table).cols.push({ col, dataType, colType, nullable: nullable === 'YES', key, extra, comment });
}

function typeOf(c) {
  if (c.key === 'PRI' && c.col === 'id') return 'Long';
  const t = TYPE[c.dataType] || 'String';
  return t;
}

let nEnt = 0, nMap = 0;
const warnings = [];

for (const t of [...tables.values()].sort((a, b) => a.name.localeCompare(b.name))) {
  const cls = classOf(t.name);
  const imports = new Set([
    'com.baomidou.mybatisplus.annotation.IdType',
    'com.baomidou.mybatisplus.annotation.TableId',
    'com.baomidou.mybatisplus.annotation.TableName',
    'lombok.Data'
  ]);
  const fields = [];
  for (const c of t.cols) {
    const type = typeOf(c);
    if (['BigDecimal', 'LocalDateTime', 'LocalDate', 'LocalTime'].includes(type)) imports.add('java.' + (type === 'BigDecimal' ? 'math.BigDecimal' : 'time.' + type));
    const name = camel(c.col);
    if (name !== c.col && !/^[a-z][a-zA-Z0-9]*$/.test(name)) warnings.push(`${t.name}.${c.col} -> 非法字段名 ${name}`);
    const doc = c.comment ? `    /** ${c.comment.replace(/\*\//g, '')} */\n` : '';
    const ann = c.key === 'PRI'
      ? `    @TableId(value = "${c.col}", type = IdType.AUTO)\n`
      : '';
    fields.push(`${doc}${ann}    private ${type} ${name};`);
  }
  const javaImports = [...imports].sort().map(i => `import ${i};`).join('\n');
  const ent = `package ${basePkg}.entity;

${javaImports}

/**
 * ${t.comment || t.name}
 * <p>对应表：${t.name}</p>
 */
@Data
@TableName("${t.name}")
public class ${cls} {

${fields.join('\n\n')}
}
`;
  fs.writeFileSync(path.join(entityDir, cls + '.java'), ent, 'utf8');
  nEnt++;

  const mapp = `package ${basePkg}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${basePkg}.entity.${cls};
import org.apache.ibatis.annotations.Mapper;

/** ${t.comment || t.name} 数据访问 */
@Mapper
public interface ${cls}Mapper extends BaseMapper<${cls}> {
}
`;
  fs.writeFileSync(path.join(mapperDir, cls + 'Mapper.java'), mapp, 'utf8');
  nMap++;
}

console.log('表数量: ' + tables.size);
console.log('生成实体: ' + nEnt + ' 个，Mapper: ' + nMap + ' 个');
if (warnings.length) console.log('警告:\n  ' + warnings.join('\n  '));
