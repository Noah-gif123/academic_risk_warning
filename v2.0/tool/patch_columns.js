/* patch_columns.js — 把 columns.tsv 同步到当前库结构（一次性、可重跑）
 *   1) sys_account 增补 16_auth_token.sql 新增的 token / token_expire_time 两列（置于 status 之后）
 *   2) 同步 17_dict_align.sql 修改的两条列注释（enrollment.status、sys_account.password_hash）
 * 用法: node patch_columns.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, 'columns.tsv');
const raw = fs.readFileSync(file, 'utf8');
let lines = raw.split(/\r?\n/).filter(l => l !== '');

// ---- 1) 注释口径对齐 ----
lines = lines.map(l => {
  const c = l.split('\t');
  if (c[0] === 'enrollment' && c[2] === 'status') {
    c[8] = '状态：ENROLLED在册/REPEAT重修/WITHDRAWN已退课/EXEMPT免修';
  }
  if (c[0] === 'sys_account' && c[2] === 'password_hash') {
    c[8] = '口令哈希：PBKDF2WithHmacSHA256，格式 pbkdf2$迭代次数$Base64(盐)$Base64(哈希)';
  }
  return c.join('\t');
});

// ---- 2) sys_account 补齐 token 两列 ----
const has = lines.some(l => {
  const c = l.split('\t');
  return c[0] === 'sys_account' && c[2] === 'token';
});
if (!has) {
  const block = [];
  const others = [];
  for (const l of lines) {
    const c = l.split('\t');
    if (c[0] === 'sys_account') block.push(c); else others.push(l);
  }
  const comment = block[0][9];
  const tokenRows = [
    ['sys_account', '', 'token', 'varchar', 'varchar(64)', 'YES', 'UNI', '', '登录令牌（32 位十六进制，落库保存，服务重启后仍有效）', comment],
    ['sys_account', '', 'token_expire_time', 'datetime', 'datetime', 'YES', '', '', '令牌过期时间（默认签发后 12 小时）', comment]
  ];
  // 插到 status 之后
  const pos = block.findIndex(c => c[2] === 'status') + 1;
  block.splice(pos, 0, tokenRows[0], tokenRows[1]);
  // 重新编号 ordinal
  block.forEach((c, i) => { c[1] = String(i + 1); });
  const merged = others.concat(block.map(c => c.join('\t')));
  // 保持“按表名+序号”排序后再写回
  merged.sort((a, b) => {
    const x = a.split('\t'), y = b.split('\t');
    if (x[0] !== y[0]) return x[0] < y[0] ? -1 : 1;
    return Number(x[1]) - Number(y[1]);
  });
  lines = merged;
}

fs.writeFileSync(file, lines.join('\n') + '\n', 'utf8');
console.log('patched columns.tsv rows=' + lines.length);
