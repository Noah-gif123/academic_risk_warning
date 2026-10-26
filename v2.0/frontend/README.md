# 学情预警系统 v2.0 · 前端（Vue 3 + Vite + Tailwind）

## 技术栈（与本机已装版本对齐）

| 项 | 版本 |
|---|---|
| Node / npm | 22.19.0 / 10.9.3 |
| Vue | 3.5.34 |
| Vue Router | 4.6.4（hash 模式） |
| Vite | 4.5.14 |
| Tailwind CSS | 3.4.19（+ PostCSS 8 / Autoprefixer） |

> **不引入任何新包**：当前机器无外网，`element-plus`、`axios` 等都没有本地缓存；
> 请求统一用原生 `fetch` 封装（`src/api/http.js`），组件用 Tailwind 手写。

## 目录结构

```
src/
├── main.js / App.vue / style.css     入口、根组件、Tailwind 组件类
├── session.js                        会话（token/角色/姓名/归属）+ 角色首页映射
├── nav.js                            四端导航项（按口径冻结版《功能整理》）
├── api/http.js                       fetch 封装：带令牌、401 自动跳登录
├── api/auth.js                       登录 / 注销 / me / 改密
├── layouts/PortLayout.vue            四端共用外壳（顶栏 + 导航 + 头像菜单）
├── router/index.js                   路由与守卫（未登录跳登录、越端跳回本端首页）
└── pages/
    ├── Login.vue                     统一登录页
    ├── Placeholder.vue               建设中占位页（带页面标题）
    └── {student,teacher,counselor,admin}/Home.vue   四端首页骨架
```

## 开发与构建

```bash
cd frontend
npm run dev      # http://localhost:5173 ，/api 已代理到 8080
npm run build    # 产物在 dist/
npm run preview
```

> 若机器离线导致 `npm install` 失败：本工程已随附可用的 `node_modules`（自 v1 复制），
> 直接 `npm run dev` 即可。

## 四端导航（第二阶段已补齐预警三页）

| 端 | 导航项 |
|---|---|
| 学生 | 首页、课程学习、考试（我的考试 / 问卷调查）、**学情预警**（`/student/warnings`） |
| 教师 | 首页、课程、**预警监控**（`/teacher/monitor`） |
| 辅导员 | 首页看板、重点关注、班级管理、干预记录、问卷、通知 |
| 教务 | 首页、基础数据、排课与教学班、成绩构成、考勤归档、**规则配置**（`/admin/rules`）、流程与问卷 |

预警相关的三个页面（教师端预警监控、学生端学情预警、教务端规则配置）已替换为真实页面；
其余未实现的菜单仍指向 `Placeholder`。

与学生端/教师端口径相关的展示约定：
- 学生端**只给结论与建议**，不展示"剩余可缺次数"与教师处置记录；
- 教师端名单与导出显示「缺课 / 允许 / 剩余」完整额度，详情里给三通道证据与时间线。

教师端「教学详情」页的约定：
- 页签只有一排（导学 / 教学目标 / 目录 / 资料库 / 测验 / 作业 / 项目），课程内容页嵌入时不再重复自己的页签；
- 导学用内置富文本编辑器 `components/RichTextEditor.vue`（加粗 / 斜体 / 下划线 / 字号 / 标题 / 列表 / 链接 / 清除格式），保存为 HTML；
- 资料库按**课程组（开课实例）共享**：列表显示上传人与教学班，可勾「只看我上传」，非本人上传的资料只读（可下载、不可作废）。

## 登录联调

1. 启动后端（8080）与前端（5173）；
2. 打开 `http://localhost:5173`，用学号 / 工号 / 教务账号登录，初始口令 `000000`；
3. 登录成功后按角色跳转：`/student/home`、`/teacher/home`、`/counselor/home`、`/admin/home`。

## 约定

- 令牌存 `localStorage`，请求头 `Authorization: Bearer <token>`；
- 后端返回真实 401 时，`http.js` 清会话并跳登录页；
- 颜色与卡片、按钮、输入框等基础样式集中在 `src/style.css` 的 `@layer components`，四端共用。
