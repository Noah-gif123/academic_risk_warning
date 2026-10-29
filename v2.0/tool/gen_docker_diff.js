/* gen_docker_diff.js — 《Docker 部署合规差异记录（整改前后对照）》
 * 用法: node gen_docker_diff.js → docker_diff.json（再用 build_report.js 渲染 docx）
 */
'use strict';
const fs = require('fs');
const path = require('path');

const B = [];
const h1 = (x) => B.push({ t: 'h1', x });
const h2 = (x) => B.push({ t: 'h2', x });
const h3 = (x) => B.push({ t: 'h3', x });
const p = (x) => B.push({ t: 'p', x });
const note = (x) => B.push({ t: 'note', x });
const ul = (x) => B.push({ t: 'ul', x });
const ol = (x) => B.push({ t: 'ol', x });
const table = (cap, head, rows, widths) => B.push({ t: 'table', cap, head, rows, widths });

/* ==================== 1 结论 ==================== */
h1('1 结论一览');
p('按《使用 Docker 进行项目部署指导手册》逐条核对后，本轮整改了三处最关键的缺口，并已在本机**实测通过**（镜像构建成功、容器 healthy、前端反向代理可用）。');
table('整改前后对比（总览）',
  ['维度', '整改前', '整改后'],
  [
    ['应用是否容器化', '否（后端 java -jar、前端 vite dev）', '**是**：后端、前端各有镜像，compose 统一编排'],
    ['Dockerfile / .dockerignore', '无', '**有**：前后端各一套，后端为多阶段构建'],
    ['资源限制与日志轮转', '无（13 个 Dify 容器 memlimit 全为 0，compose 里 0 处 logging）', '**有**：应用栈 mem_limit 1g/256m，日志 50m×3 / 20m×2 轮转'],
    ['第三方镜像 latest 标签', 'nginx:latest、ubuntu/squid:latest 在跑', '**已固定**：nginx:1.31.8、ubuntu/squid:6.13-25.04_edge'],
    ['健康检查', '仅 Dify 4 个容器有；应用无', '**有**：后端 /api/ai/health、前端 /healthz，均为 healthy'],
    ['非 root / JVM 内存', '无（无镜像）', '**有**：appuser(uid 999)、-XX:MaxRAMPercentage=75.0'],
    ['上传目录持久化', '宿主 ./file-store', '**命名卷** arw-app-file-store → /app/file-store']
  ], [22, 34, 44]);

/* ==================== 2 整改一 ==================== */
h1('2 整改一：后端多阶段 Dockerfile + .dockerignore');
table('新增文件',
  ['文件', '作用'],
  [
    ['`v2.0/backend/Dockerfile`', '两阶段：gradle:9.8.2-jdk17 构建 → eclipse-temurin:17-jre 运行'],
    ['`v2.0/backend/.dockerignore`', '剔除 build/.gradle/.git/.idea/file-store/*.log 等，加速构建并避免把宿主二进制带进镜像']
  ], [30, 70]);
h2('2.1 对照手册的关键写法');
table('Dockerfile 与手册要点逐条对齐',
  ['手册要点', '本实现'],
  [
    ['多阶段构建（builder + runtime）', '`FROM gradle:9.8.2-jdk17 AS builder` → `FROM eclipse-temurin:17-jre`'],
    ['依赖层缓存', '先 `COPY settings.gradle build.gradle` 并 `gradle dependencies`，再 COPY 源码'],
    ['非 root 运行', '`groupadd appgroup` + `useradd appuser` + `USER appuser`'],
    ['LABEL 维护者/描述', '`LABEL maintainer=... description=... version=2.0.0`'],
    ['精确 COPY，不用 *.jar', '`COPY --from=builder /build/build/libs/academic-risk-warning-2.0.0-SNAPSHOT.jar app.jar`'],
    ['EXPOSE 端口', '`EXPOSE 8080`'],
    ['JVM 按百分比设堆', '`ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"`（不写死 -Xmx）'],
    ['ENTRYPOINT 用 shell 形式', '`ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]`'],
    ['HEALTHCHECK', '`curl -fsS http://localhost:8080/api/ai/health`，interval 30s / timeout 5s / start-period 60s / retries 3']
  ], [34, 66]);
note('健康检查端点用的是 /api/ai/health，不是 /api/health——后者在本项目里并不存在（实测返回 “No static resource api/health”），手册示例里的 /api/xx/health 需要按项目实际替换。');
h2('2.2 与手册示例的差异（有意为之）');
ul([
  '手册示例是 **Maven + Spring Boot 3.2**，本项目是 **Gradle 9.4.1 wrapper + Spring Boot 4.0.6**，因此构建命令改为 `gradle bootJar`；',
  '运行时显式安装 `curl`（eclipse-temurin:17-jre 不保证自带），否则 HEALTHCHECK 无法执行；',
  '多建了 `/app/file-store` 目录并 chown 给 appuser，用于挂载上传资料的命名卷。'
]);

/* ==================== 3 整改二 ==================== */
h1('3 整改二：应用栈 compose（资源限制 + 日志轮转）');
table('新增文件',
  ['文件', '作用'],
  [
    ['`v2.0/docker-compose.app.yml`', 'app（后端）+ web（前端）两个服务：端口、资源、健康检查、日志、卷、网络'],
    ['`v2.0/frontend/Dockerfile`', 'node:20-alpine 构建 → nginx:1.31.8 托管 dist'],
    ['`v2.0/frontend/nginx.conf`', '静态托管 + SPA 回退 + `/api` 反代到 `http://app:8080` + `/healthz`'],
    ['`v2.0/frontend/.dockerignore`', '剔除 node_modules/dist 等']
  ], [30, 70]);
h2('3.1 手册要求的四件套');
table('compose 关键配置',
  ['条款', '本实现'],
  [
    ['镜像标签不用 latest', '`academic-risk-warning-backend:2.0.0` / `academic-risk-warning-frontend:2.0.0`'],
    ['资源限制', '后端 `mem_limit: 1g`，前端 `mem_limit: 256m`；实测 inspect 显示 mem=1073741824'],
    ['日志轮转', '后端 `max-size 50m / max-file 3`，前端 `20m / 2`；实测 logopts 生效'],
    ['依赖健康检查', 'web `depends_on: app: condition: service_healthy`'],
    ['命名数据卷', '`arw-app-file-store` → `/app/file-store`（上传资料不随容器销毁）'],
    ['自定义网络', '`arw-app-net`（bridge），容器间用服务名 `app` 互访'],
    ['服务名互访（铁律）', '前端 nginx 反代 `http://app:8080`，不用 localhost'],
    ['重启策略', '两个服务均 `restart: unless-stopped`']
  ], [28, 72]);
h2('3.2 跨栈访问的处理');
p('我们的 MySQL 和 Dify 都跑在**宿主**上，不在这个 compose 里，所以容器内必须走 `host.docker.internal`：');
ul([
  '`SPRING_DATASOURCE_URL` 指向 `jdbc:mysql://host.docker.internal:3306/study_warning_system_v2`，并加 `extra_hosts: host.docker.internal:host-gateway`；',
  '`AI_DIFY_BASE_URL=http://host.docker.internal:8090`；',
  '密钥（`AI_LLM_KEY`、`AI_REPORT_APP_KEY`）用 `${...}` 从环境变量注入，**不写进 compose 文件、不进 Git**。'
]);

/* ==================== 4 整改三 ==================== */
h1('4 整改三：把 latest 标签固定下来');
table('镜像固定对照',
  ['服务', '官方 compose', '整改后（override）', '依据'],
  [
    ['nginx', '`nginx:latest`', '`nginx:1.31.8`', '本机实测运行版本 1.31.8，镜像内自带 curl'],
    ['ssrf_proxy', '`ubuntu/squid:latest`', '`ubuntu/squid:6.13-25.04_edge`', '本机实测 squid 6.13'],
    ['agent_ssrf_proxy', '`ubuntu/squid:latest`', '`ubuntu/squid:6.13-25.04_edge`', '同上']
  ], [18, 24, 34, 24]);
p('改法：只改我们自己的 `docker-compose.override.yaml`（加 `image:` 覆盖），**不动 Dify 官方 compose**，这样 Dify 升级时不会冲突。');
note('Dify 自己的业务镜像本来就是固定版本（dify-api/web/worker 均为 1.18.0），需要固定的只是这两个第三方基础镜像。');

/* ==================== 5 实测记录 ==================== */
h1('5 实测记录（本机验证）');
table('验证项与结果',
  ['#', '验证项', '命令', '结果'],
  [
    ['1', 'compose 语法', '`docker compose -f docker-compose.app.yml config`', '通过（exit 0）'],
    ['2', '后端镜像构建', '`docker compose -f docker-compose.app.yml build app`', '成功，镜像 `academic-risk-warning-backend:2.0.0`'],
    ['3', '前端镜像构建', '`... build web`', '成功，镜像 `academic-risk-warning-frontend:2.0.0`'],
    ['4', '后端容器状态', '`docker compose ps`', '`arw-app ... Up (healthy)`'],
    ['5', '前端容器状态', '同上', '`arw-web ... Up (healthy)`'],
    ['6', '后端接口（宿主）', '`curl http://localhost:8080/api/ai/health`', '200，`reportWorkflowReady=true`、`reachable=true`'],
    ['7', '前端页面', '`curl http://localhost:8081/`', '200'],
    ['8', '前端反代后端', '`curl http://localhost:8081/api/ai/health`', '200（nginx 反代到 app:8080 成功）'],
    ['9', '非 root 校验', '`docker exec arw-app id`', '`uid=999(appuser) gid=999(appgroup)`'],
    ['10', 'JVM 参数', '`docker exec arw-app ps -o args= -p 1`', '`sh -c java $JAVA_OPTS -jar app.jar`，JAVA_OPTS 已展开'],
    ['11', '资源与日志', '`docker inspect arw-app`', '`mem=1073741824`、`logopts={"max-file":"3","max-size":"50m"}`、`restart=unless-stopped`'],
    ['12', '数据卷', '`docker inspect arw-app` / 容器内写入', '`arw-app-file-store → /app/file-store`，写测试通过'],
    ['13', 'Dify 固定标签后', '`docker ps` + `curl 8090`', '`nginx:1.31.8`、`ubuntu/squid:6.13-25.04_edge`；8090 仍返回 200']
  ], [5, 20, 42, 33]);

/* ==================== 6 尚未覆盖 ==================== */
h1('6 仍未覆盖 / 有意不做的部分');
table('剩余差距',
  ['条目', '现状', '是否需要改', '建议'],
  [
    ['前端 lockfile', '仓库未提交 `package-lock.json`，构建用 `npm install` 而非 `npm ci`', '建议改', '提交 lockfile 后把 Dockerfile 改回 `npm ci`，构建可复现'],
    ['只读根文件系统（进阶项）', '两个容器均 `readonly=false`', '可选', '后端需写 file-store，需先挂 tmp 目录再开 `read_only: true`'],
    ['非应用镜像的 latest', 'Dify compose 里仍有可选服务的 latest（如 busybox、unstructured、opensearch 等，未启用）', '可选', '实际运行只有 nginx/squid 两处已固定；未启用的 profile 不影响'],
    ['密钥出现在文档/聊天', 'Dify app key、DeepSeek key 曾以明文出现', '建议改', '到控制台轮换一次；后续只走环境变量，文档中写占位符'],
    ['离线环境构建', '构建过程需联网拉 gradle/node 依赖与基础镜像', '需注意', '内网部署前用 `docker save` 打包镜像，或先在有网机器 build 好再迁移'],
    ['数据库仍未容器化', 'MySQL 在宿主（3306），compose 通过 host.docker.internal 访问', '按现状可接受', '若要全容器化，需把 MySQL 加入 compose 并初始化脚本，注意数据迁移'],
    ['HTTPS / 反向代理（Step 40）', '本机用 8081 直接暴露 nginx', '按需', '公网部署时上 HTTPS + 域名，手册第十部分有步骤']
  ], [22, 34, 14, 30]);

/* ==================== 7 使用说明 ==================== */
h1('7 怎么用（两条命令）');
p('在 `v2.0` 目录下执行：');
note('docker compose -f docker-compose.app.yml build      # 构建前后端镜像（首次需联网）\ndocker compose -f docker-compose.app.yml up -d          # 启动\ndocker compose -f docker-compose.app.yml ps             # 看 healthy\ndocker compose -f docker-compose.app.yml down           # 停止（保留数据卷）');
table('端口与地址',
  ['服务', '地址', '说明'],
  [
    ['后端', 'http://localhost:8080', '与 IDEA 直跑端口相同，二选一'],
    ['前端', 'http://localhost:8081', 'nginx 托管 dist，并把 /api 反代到后端容器'],
    ['健康检查', '/api/ai/health', '免登录，容器 HEALTHCHECK 用的就是它'],
    ['Dify', 'http://localhost:8090', '不在本 compose 内，需单独起（桌面快捷方式）']
  ], [14, 30, 56]);
note('注意：容器占用 8080，用 IDEA 调试前先 `docker compose -f docker-compose.app.yml down`，否则会端口冲突。');

/* ==================== 8 与手册的差异说明 ==================== */
h1('8 与手册示例的差异（答辩可直接照读）');
table('差异与理由',
  ['手册写法', '本项目', '理由'],
  [
    ['Maven + Spring Boot 3.2', 'Gradle wrapper + Spring Boot 4.0.6', '项目技术栈不同，多阶段结构与其余规范不变'],
    ['`/api/grade/health` 作健康检查', '`/api/ai/health`', '本项目能提供免登录探活的只有 AI 体检接口'],
    ['app + mysql + redis 三服务', 'app + web 两服务，MySQL 复用宿主', 'MySQL 已有独立实例与数据；容器内用 host.docker.internal 访问'],
    ['单服务 8080', '后端 8080 + 前端 8081', '前端是独立 SPA，用 nginx 托管更贴近真实部署'],
    ['镜像推送到阿里云 ACR', '本机构建本地镜像（未推送）', '尚未做云部署，Step 37–41 留待上线时补']
  ], [24, 34, 42]);
note('一句话总结：结构与规范（多阶段、非 root、健康检查、资源限制、日志轮转、固定标签、卷持久化、服务名互访）已与手册一致；差异集中在“技术栈与部署形态”，不影响合规判定。');

const meta = {
  title: '学情预警系统 v2.0',
  subtitle: 'Docker 部署合规差异记录',
  sub2: '对照《使用 Docker 进行项目部署指导手册》· 整改前后对照 · 本机实测记录',
  tip: '本文件记录三条整改的改动内容与验证结果，并列出仍存在的差距与建议',
  info: [
    ['文件名称', '学情预警系统 v2.0 · Docker 部署合规差异记录'],
    ['对照依据', '《使用 Docker 进行项目部署指导手册》（11 部分 / 45 个 Step）'],
    ['整改范围', '① 后端多阶段 Dockerfile + .dockerignore；② 应用栈 compose（资源限制 + 日志轮转）；③ Dify 第三方镜像固定标签'],
    ['验证环境', 'Windows 11 + Docker Desktop 28.5.1 / Compose v2.40、WSL2、镜像加速器已配置'],
    ['编写日期', '2026-12']
  ]
};

const out = path.join(__dirname, 'docker_diff.json');
fs.writeFileSync(out, JSON.stringify({ meta, blocks: B }, null, 1), 'utf8');
const stat = { h: 0, p: 0, tbl: 0, li: 0 };
B.forEach((b) => {
  if (b.t[0] === 'h') stat.h++;
  else if (b.t === 'p' || b.t === 'note') stat.p++;
  else if (b.t === 'table') stat.tbl++;
  else stat.li += b.x.length;
});
console.log('WROTE ' + out);
console.log(JSON.stringify(stat));
