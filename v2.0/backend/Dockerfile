# syntax=docker/dockerfile:1
# =============================================================================
# 学情预警系统 v2.0 · 后端镜像（多阶段构建）
#   阶段一 builder：用 Gradle 镜像编译，产出可执行 bootJar
#   阶段二 runtime：仅 JRE + jar，非 root 运行，带健康检查
# 说明：镜像标签由 compose 指定（academic-risk-warning-backend:2.0.0），不使用 latest
# =============================================================================

# ---------------------------- 阶段一：构建 ----------------------------
FROM gradle:9.8.2-jdk17 AS builder
WORKDIR /build

# 先只复制构建脚本，让依赖解析这一层可以复用缓存
COPY settings.gradle build.gradle ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

# 再复制源码并打包（测试在 CI/本机已执行，这里跳过以缩短构建时间）
COPY src ./src
RUN gradle --no-daemon clean bootJar -x test

# ---------------------------- 阶段二：运行 ----------------------------
FROM eclipse-temurin:17-jre

LABEL maintainer="academic-risk-warning" \
      description="学情预警系统 v2.0 · 后端服务（Spring Boot 4 / Java 17）" \
      version="2.0.0"

# curl 用于容器健康检查（官方镜像默认不含，显式安装并清理缓存）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 非 root 用户：缩小容器逃逸后的影响范围
RUN groupadd --system appgroup \
    && useradd --system --gid appgroup --create-home appuser

# 精确复制 jar（不用 *.jar 通配，避免误伤 plain-jar）
COPY --from=builder /build/build/libs/academic-risk-warning-2.0.0-SNAPSHOT.jar app.jar

# 上传资料目录：容器内固定路径，compose 挂命名卷持久化
RUN mkdir -p /app/file-store && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# 按容器可用内存百分比设置堆上限（不写死 -Xmx，换规格无需改镜像）
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    TZ=Asia/Shanghai

# 容器进程存在 ≠ 应用可用：用免登录的 AI 体检接口做健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/ai/health || exit 1

# shell 形式，$JAVA_OPTS 才会在启动时被展开
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
