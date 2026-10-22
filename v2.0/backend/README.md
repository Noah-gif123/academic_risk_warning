# 学情预警系统 v2.0 · 后端（Spring Boot 4 + MyBatis-Plus）

## 技术栈（与本机已安装版本一致）

| 项 | 版本 | 说明 |
|---|---|---|
| JDK | **17.0.8**（`JAVA_HOME=D:\Jdk17\Jdk17`） | Boot 4 要求 17+ |
| 构建工具 | **Gradle 9.4.1**（wrapper 随工程自带） | 不依赖本机安装 Gradle |
| Spring Boot | **4.0.6** | Web 用 `spring-boot-starter-webmvc`（Boot 4 已把 starter-web 更名） |
| MyBatis-Plus | **3.5.15**（`mybatis-plus-spring-boot4-starter`） | Boot 4 专用 starter |
| MySQL 驱动 | **mysql-connector-j 9.2.0** | 用 `strictly` 锁死（Boot 4.0.6 的 BOM 会约束到 9.7.0） |
| 其他 | Lombok、Validation、Apache POI 5.2.5 | POI 用于 Word/Excel 导入 |

> ⚠️ **Spring Boot 4 用的是 Jackson 3**：包名是 `tools.jackson.*`，不再是 `com.fasterxml.jackson.*`。
> 代码里**不要**再引 Jackson 2 的 API（本次就踩过这个坑）。

## 目录结构

```
src/main/java/com/example/academic_risk_warning/
├── AcademicRiskWarningApplication.java   启动类（8080）
├── common/       Result 统一返回、BizException、全局异常处理
├── config/       WebConfig（跨域 + 鉴权拦截器）
├── security/     PasswordHasher（PBKDF2）、LoginUser、TokenStore、UserContext、AuthInterceptor
├── entity/       60 个实体（按 sql/ 下 60 张表自动生成）
├── mapper/       60 个 Mapper（BaseMapper）
├── service/      AuthService（统一登录 / 改密）
├── controller/   AuthController（/api/auth/*）
├── dto/ vo/      请求与返回对象
└── bootstrap/    AccountBootstrap（首启建账号并回填 account_id）
```

## 建库

```bash
cd ../sql
mysql --host=127.0.0.1 -uroot -p study_warning_system_v2 < 01_base_layer.sql
# … 依次 02 → 15（顺序与说明见 ../sql/README.md）
```

## 构建与运行

```bash
cd backend

# 构建（首次需联网；本地镜像已在 build.gradle 里配置）
./gradlew build -x test

# 运行方式一：直接跑 jar
java -jar build/libs/academic-risk-warning-2.0.0-SNAPSHOT.jar

# 运行方式二：热启动
./gradlew bootRun
```

**离线构建**（依赖已缓存时）：把 `GRADLE_USER_HOME` 指向缓存目录再加 `--offline`
```powershell
$env:GRADLE_USER_HOME="..\..\v1.0\.gradle-home"   # 或你自己的缓存目录
.\gradlew build -x test --offline
```

启动后监听 **8080**；首次启动会执行账号初始化（`app.bootstrap.enabled=true`）：
为 800 名学生、9 名教师、2 名辅导员、1 名教务创建统一账号，**初始口令 000000**（PBKDF2 存储），并回填 `account_id`。

## 已实现接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/auth/captcha` | 取图形验证码（captchaId + PNG data URL，5 分钟有效、一次一用） |
| POST | `/api/auth/login` | 四端统一登录（需带 captchaId / captchaCode），返回令牌 + 角色 + 归属 id + 是否首次登录 |
| POST | `/api/auth/logout` | 注销（清空库中令牌与内存缓存） |
| GET | `/api/auth/me` | 当前登录用户 |
| POST | `/api/auth/change-password` | 修改口令（校验旧密码、新旧不同、长度 6~32） |
| GET | `/api/notifications?unreadOnly=&page=&size=` | 四端共用消息中心：我的消息（按 `notification_receipt.recipient_id/role` 过滤分页） |
| GET | `/api/notifications/unread-count` | 未读数（顶栏红点，前端 60 秒轮询） |
| POST | `/api/notifications/{id}/read`、`/api/notifications/read-all` | 标记单条已读 / 全部已读（幂等，只能改自己的回执） |
| DELETE | `/api/teacher/papers/{id}` | 删除试卷草稿（仅草稿、且无交卷；已发布需先撤回为草稿） |
| POST | `/api/engine/evaluate?termId=&offeringId=&teachingClassId=&enrollmentId=&trigger=` | **风险判定引擎**（第二阶段第 1 步）：按冻结口径算 A/B 通道并写 `risk_state`（一人一课一行、覆盖刷新、幂等）；教师只能按自己的教学班触发，教务可按学期/开课/选课触发 |
| GET | `/api/teacher/risks?teachingClassId=&level=&keyword=` | 教师端风险名单（按等级/学号姓名过滤，按风险分倒序） |
| GET | `/api/teacher/risks/stats?teachingClassId=` | 教师端预警总览（红/橙/黄/绿计数、硬门槛人数、最近判定时间、引擎版本） |
| GET | `/api/teacher/alerts?offeringId=&teachingClassId=&level=&status=` | **预警事件列表**（第二阶段第 2 步）：只返回本人教学班的预警，按风险分倒序 |
| GET | `/api/teacher/alerts/stats?teachingClassId=` | 预警总览计数（红橙黄、待处理/处理中/已关闭、已升级、今日新增、已判定人数、最近判定时间、引擎版本） |
| GET | `/api/teacher/alerts/{id}` | 预警详情：预警本体 + 时间线（alert_flow_log）+ 处置记录（intervention_record）+ 当前风险状态 |
| POST | `/api/teacher/alerts/{id}/claim`、`/handle`、`/close`、`/dismiss`、`/remind` | 认领 → 记录处理（写干预记录）→ 关闭（填结论）/ 忽略（必填原因）/ 提醒学生（站内消息，已结束的预警不可再提醒） |
| GET | `/api/teacher/alerts/export?teachingClassId=&level=&status=` | **第 3 步**：教师端预警名单导出（xlsx，字段与列表一致：学号/姓名/教学班/课程/等级/通道/规则/风险分/置信度/缺课额度/期末需考/状态/处置/依据/生成时间） |
| GET/POST | `/api/admin/terms`、`/api/admin/terms/current`、`/api/admin/terms/{id}/calendar[/generate]` | 学期与教学周历 |
| GET/POST | `/api/admin/majors`、`/api/admin/classes`、`/api/admin/counselor-assignments` | 专业、行政班、辅导员分配 |
| GET/POST | `/api/admin/offerings`、`/api/admin/offerings/{id}/confirm-actual` | 开课实例与有效课次确认 |
| GET/POST | `/api/admin/teaching-classes`、`.../{id}/enroll/admin-classes`、`.../{id}/students` | 教学班、按行政班选课、名单 |
| POST | `/api/admin/offerings/{id}/sessions/generate?teachingClassId=` | 课次自动生成（跳过假期周） |
| GET | `/api/teacher/offerings` | 教师端我的教学课程 |
| GET | `/api/student/courses` | 学生端我的课程 |
| GET/POST | `/api/teacher/score-scheme?offeringId=` | 成绩构成（教师：查看 / 保存草稿） |
| POST | `/api/teacher/score-scheme/{id}/confirm`、`.../{id}/lock` | 课程组确认与锁定 |
| GET | `/api/teacher/score-templates?courseType=` | 教师可读的构成模板 |
| GET | `/api/admin/score-templates`、`/api/admin/score-schemes?termId=` | 教务端模板与全量构成 |
| POST | `/api/admin/score-schemes/{id}/lock` | 教务裁定锁定 |
| GET/POST | `/api/teacher/guide?teachingClassId=` | 课程导学（富文本，保存版本递增，可同时发布） |
| GET/POST | `/api/teacher/objectives` | 教学目标（整批保存，删掉的转 DISABLED） |
| GET | `/api/teacher/lessons?teachingClassId=` | 目录（一行 = 一次课，含课前 / 课中 / 课后三段） |
| POST | `/api/teacher/lessons/sync?teachingClassId=` | 按课次生成目录（幂等） |
| POST | `/api/teacher/lessons/{id}` | 调整课时标题 / 顺序 / 状态 |
| POST | `/api/teacher/lesson-resources`、`.../{id}/publish`、`DELETE .../{id}` | 课时资源与任务的增删与发布 |
| GET/POST | `/api/teacher/resources`、`/api/teacher/resources/upload`、`.../{id}/void` | 资料库（上传、列表、逻辑作废） |
| GET | `/api/teacher/papers?teachingClassId=` | 本班测验 / 作业（供挂到课时；1-7 后才有数据） |
| GET | `/api/files/{id}` | 文件下载（需登录，不走静态目录） |
| GET/POST | `/api/teacher/papers`、`/api/teacher/papers/{id}` | 测验 / 作业（列表、详情、保存草稿与题目） |
| POST | `/api/teacher/papers/import` | 导入题目（.docx / .txt 解析，只回草稿供校对，不入库） |
| POST | `/api/teacher/papers/{id}/publish?publish=`、`.../{id}/close` | 发布 / 撤回、截止并登记缺考（记 0 分） |
| GET | `/api/teacher/papers/{id}/submissions`、`/api/teacher/submissions/{id}` | 交卷列表与作答明细（含标准答案、判卷人） |
| POST | `/api/teacher/submissions/{id}/grade`、`/api/teacher/papers/{id}/absent?studentId=` | 主观题批改、单个学生记缺考 |
| GET | `/api/student/papers`、`/api/student/papers/{id}` | 我的测验与作业、取卷作答（不含答案） |
| POST | `/api/student/papers/{id}/submit` | 交卷：客观题自动判分，简答题待批 |
| GET | `/api/student/papers/{id}/result` | 结果（按「分数 / 看卷 / 答案」三个开关裁剪） |
| GET/POST | `/api/teacher/projects`、`/api/teacher/projects/{id}` | 项目任务（列表、详情、保存草稿） |
| POST | `/api/teacher/projects/{id}/publish?publish=`、`.../{id}/close` | 发布 / 撤回、截止 |
| POST | `/api/teacher/project-submissions/{id}/grade` | 项目评分（团队项目为整组分，个人得分按贡献折算） |
| GET/POST | `/api/student/projects`、`/api/student/projects/{id}` | 我的项目（含我的组、我的提交、成员得分） |
| GET | `/api/student/projects/teammates?teachingClassId=` | 同班同学（组队选人用） |
| POST | `/api/student/projects/{id}/team`、`.../upload`、`.../submit` | 组队（本人为组长）、上传项目文件、提交 |
| POST | `/api/student/projects/{id}/contribution` | 组长提交贡献表（提交即锁定，无修改接口） |
| GET | `/api/teacher/attendance/sessions`、`.../roster` | 课次列表与点名单（默认全班出勤） |
| POST | `/api/teacher/attendance/call`、`.../records/{id}` | 提交点名（一节课一个批次）、单行修改（写行级留痕） |
| GET | `/api/teacher/attendance/stats` | 考勤汇总（缺课/迟到/请假、扣分、N_max、剩余可缺） |
| POST | `/api/teacher/attendance/import` | 批量导入（.xlsx / .csv：学号,课次序号,状态,请假类型） |
| GET/POST | `/api/teacher/attendance/archives` | 归档批次查询、提交期中 / 期末归档 |
| GET | `/api/admin/attendance/archives`、`/api/admin/attendance/coverage` | 教务审核列表、覆盖率督办 |
| POST | `/api/admin/attendance/archives/{id}/review`、`.../remind` | 确认 / 退回归档、催办 |
| GET | `/api/student/attendance` | 我的考勤（明细 + 汇总，临界只提示不给次数） |
| GET | `/api/teacher/grading/pending?teachingClassId=` | 待批改汇总（待判主观题的测验/作业 + 待评分项目） |
| GET | `/api/teacher/scores/matrix?teachingClassId=` | 成绩矩阵（分项 + 加权 + 扣分 + 总评 + 状态） |
| POST | `/api/teacher/scores/items` | 手工评分项批量录入（自动项拒绝手工录） |
| POST | `/api/teacher/scores/summary?teachingClassId=` | 汇总生成总评（写入 score_result，状态 FINAL） |
| GET | `/api/teacher/scores/export?teachingClassId=` | 导出成绩矩阵 CSV（UTF-8 BOM） |
| GET | `/api/student/scores` | 我的成绩（分项 + 总评 + 判定说明） |
| GET | `/api/student/questionnaires`、`/{id}` | 我的问卷（只含本人行政班、收集中的问卷）与题目 |
| POST | `/api/student/questionnaires/{id}/answer` | 匿名作答（不落 student_id，仅哈希令牌去重，重复提交被拒） |
| GET | `/api/student/terms`、`/api/student/course-cards` | 学期下拉、课程卡片（分页 / 关键词 / 状态筛选） |
| GET | `/api/student/course/{teachingClassId}` | 课程详情（六页签数据一次返回，页签开关随成绩构成） |
| GET | `/api/student/profile` | 个人中心（学号、专业、行政班、联系方式） |
| POST/GET | `/api/student/join-requests` | 邀请码申请入班、我的申请 |
| POST | `/api/teacher/teaching-classes/{id}/invite` | 生成 / 保存 6 位邀请码与入班审核开关 |
| GET/POST | `/api/teacher/join-requests`、`.../{id}/review` | 入班申请列表、同意 / 拒绝（同意即入班并记知会） |
| GET | `/api/teacher/terms` | 教师端学期列表（教师首页学期下拉用，不再借用学生接口） |
| GET | `/api/teacher/home?termId=` | 教师首页：我的教学课程（按课程聚合）+ 待批改（作业/测试/项目） |
| GET | `/api/teacher/course-stats?teachingClassId=` | 课程详情·教学统计（活跃度/出勤率/任务完成度/平均成绩） |
| GET | `/api/teacher/course-export?teachingClassId=` | 导出学生数据 CSV |
| POST | `/api/teacher/teaching-classes/{id}/close` | 结课（结课后学生端不再显示该课程） |
| GET | `/api/admin/dashboard?termId=` | 教务工作台概览（开课/教学班/规模/待审归档/低覆盖率/知会/批次/今日审计） |
| GET/POST | `/api/admin/import-batches`、`.../{id}/rollback` | 导入批次列表与整批回滚（写审计） |
| GET | `/api/admin/join-notices` | 入课知会（教师同意入班后产生，教务不审批） |
| GET | `/api/admin/audit` | 审计日志（业务类型/动作/关键词/时间范围 + 分页） |
| POST | `/api/admin/coverage/{teachingClassId}/urge` | 覆盖率督办（记审计并催办未提交归档） |
| GET | `/api/counselor/profile`、`/api/counselor/terms` | 导员个人中心、学期列表 |
| GET | `/api/counselor/overview` | 导员看板（班级挂科率、风险学生、班集体提醒、汇总提醒） |
| GET | `/api/counselor/students`、`/api/counselor/students/{id}` | 学生名单（按危险度排序）、单科详情 + 处理记录 |
| POST | `/api/counselor/interventions`、`.../{id}/refer`、`.../{id}/terminate` | 录入处理、上报教务（需两次无效）、终止干预 |
| GET | `/api/counselor/intervention-records` | 本人名下学生的全部处理记录 |
| GET/POST | `/api/counselor/questionnaires`、`/api/counselor/offering-options` | 问卷列表与发布（教师风格判定，匿名，须绑定开课） |
| GET/POST | `/api/counselor/notices`、`.../{id}/read` | 汇总通知与已读回执 |
| POST | `/api/admin/synthetic/generate` | 合成数据生成（默认 720 学生 × 6 门课 × 每生 6 门 × 32 课次，含教师尺度真值） |
| POST | `/api/admin/synthetic/clear?termId=` | 清理合成数据（只允许 `SYN-` 开头的独立学期） |

约定：业务错误返回 **HTTP 200 + `code`**；未登录 / 令牌失效返回**真实 HTTP 401**（`{"code":401,...}`）。
响应体统一为 `{code, message, data}`，`code=0` 表示成功。

**成绩构成（1-5）口径**：同一层级权重之和必须 = 100（根层与每个父项各算各的）；考勤（出勤）不得作为评分项，
出勤只通过缺课硬门槛与违纪扣分影响结果；课程组全体教师确认后才可锁定（`lock_type=TEACHER_COLLECTIVE`），
未达成一致由教务裁定锁定（`lock_type=ADMIN_RULED`）；锁定后本学期内不可修改，改构成即产生新版本（`version+1`）。

**测验与作业（1-7）口径**：单选题型单/多/判断/填空交卷即判，**全对得分、否则 0 分**；简答题由教师批阅，
每题记录判卷人（`marked_by_type=SYSTEM/TEACHER`）；题目导入先落草稿，教师校对后保存再发布；
发布即不允许迟交（必须设置截止时间），截止后未交按 `ABSENT` 记 0 分，教师不代交；
答案与解析的可见性由 `show_answer=NEVER/AFTER_MARK` 决定，成绩由 `show_score`、看卷由 `allow_view_paper` 控制。

**项目与贡献度（1-8）口径**：项目只收文件（Word/PDF/代码/PPT/压缩包/仓库链接），逾期不允许补交；
团队项目由**组长提交一份贡献表**，合计必须 100%，提交即 `is_locked=1`，**教师也没有修改接口**；
个人得分 = 教师给的项目总分 × 贡献占比（个人项目占比视为 100%）；已批阅的提交不允许再改，
允许的提交形式按 `allow_formats` 校验扩展名。

**考勤与归档（1-9）口径**：一节课一张点名单，**默认全班出勤只勾异常**，一次提交一个 `batch_key`；
每一行的 before/after 都写 `attendance_record_log`；扣分不落库，`P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)`（无上限）；
硬门槛 `N_max = floor(有效课次/3)`，缺课 ≥ N_max+1 即挂；点名覆盖率 = 已点名课次 / 已过日期课次，
低于 **1/3** 判低置信度并督办；期中 / 期末归档由教师提交、教务确认后记录变 `phase=ARCHIVE` 并**冻结**
（教师无修改权限，只有教务更正并留痕）；学生端只给结论（如「再缺一次即挂」），**不给剩余可缺次数**。

**成绩与批改（1-10）口径**：加权分 `W = Σ(得分/满分×100×权重)`（两级构成逐层折算：先按二级项权重合成一级项，
再按一级项权重合成总分）；`P = 2×缺课 + 1×迟到 + 1×max(0,请假−3)`；`T = W − P`（不截断）；
及格线取 `course_offering.pass_score`；缺课 > `N_max` 直接 `HARD_LINE` 硬门槛挂科；
**缺考/未交按 0 分计**（与「未录入」区分），有任一分项未录入时总评为 `NO_SCORE` 且不允许汇总；
汇总后写入 `score_result`（`score_status=FINAL`，记录引擎版本），**折算分不落库**。

**风险判定与预警事件（第二阶段 1-2）口径**：`N_max = floor(有效课次/3)`；A 通道按缺课硬线
（缺课 > N_max 即 `FAIL`、剩余额度 ≤1 为危险、期末需考 > 100 分视作不可达）优先给出结论，
B 通道 `S_B = clamp(0,100, 100 − T + 3×缺课)` 兜底并算班内排名；`置信度 = 覆盖率 × 完整性 × 样本量`，
覆盖率 < 1/3 或置信度 < 0.15 时只输出 `BEHAVIOR_ONLY`（仅行为预警，不给分数结论）；分级线红 75 / 橙 60 / 黄 40，
判定结果一人一课一行写入 `risk_state`（覆盖刷新、幂等、记录 `engine_version`）。事件层按「判定前等级 → 判定后等级」
生成 / 升级 / 降级 / 自动关闭 `alert_record`，同等级只刷新分数与证据不刷时间线；一人一课只保留一条 `is_current=1`；
**人工关闭或忽略后，同等级风险在 7 天冷却期内不再重复生成预警**，避免反复判定刷出重复告警。

## 配置要点（`src/main/resources/application.yml`）

- 数据源：`jdbc:mysql://127.0.0.1:3306/study_warning_system_v2`，账号口令可用环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖（默认 root/root，仅本地开发）。
- `app.auth.token-expire-hours`：令牌有效期（默认 12 小时）。
- `app.bootstrap.enabled`：是否执行账号初始化（幂等，可关闭）。

## 构建配置的三个注意点（踩过坑）

1. **BOM 要挂到每个 configuration**：`platform(spring-boot-dependencies)` 只挂在 `implementation` 上时，
   `compileOnly` / `annotationProcessor` 拿不到版本，Lombok 会解析失败。现已在四处都挂上。
2. **mysql 驱动用 `strictly` 锁版本**：否则被 BOM 约束到 9.7.0，离线缓存里没有就会构建失败。
3. **分页插件暂未启用**：`PaginationInnerInterceptor` 在 `mybatis-plus-jsqlparser` 模块里，
   联网后把 build.gradle 里那行注释打开、并加回 `MybatisPlusConfig` 即可。

## 已知约束

1. 口令哈希用 **PBKDF2WithHmacSHA256**（JDK 自带），替代文档中的 BCrypt——离线环境没有 `spring-security-crypto`。
2. 令牌**落库**（`sys_account.token` / `token_expire_time`，脚本 `sql/16_auth_token.sql`）：后端重启后已登录用户不会被踢下线；
   单点登录，同一账号新登录会顶掉旧令牌；内存里保留一份缓存以减少查库。
3. 登录**图形验证码**由 JDK AWT 生成（离线可用），5 分钟有效、校验一次即作废；
   `--app.auth.captcha-enabled=false` 可临时关闭，`--app.auth.captcha-debug=true` 才开放调试取码接口（仅本机联调）。