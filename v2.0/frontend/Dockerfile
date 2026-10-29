# syntax=docker/dockerfile:1
# =============================================================================
# 学情预警系统 v2.0 · 前端镜像（多阶段构建）
#   阶段一 builder：Node 构建 Vite 产物
#   阶段二 runtime：nginx 托管静态页 + 反向代理 /api 到后端容器
# =============================================================================

# ---------------------------- 阶段一：构建 ----------------------------
FROM node:20-alpine AS builder
WORKDIR /app

# 先装依赖，利用层缓存
COPY package.json ./
# 仓库未提交 lockfile，用 install；补齐 lockfile 后可改成 npm ci
RUN npm install --no-audit --no-fund

COPY . .
RUN node node_modules/vite/bin/vite.js build

# ---------------------------- 阶段二：运行 ----------------------------
FROM nginx:1.31.8

LABEL maintainer="academic-risk-warning" \
      description="学情预警系统 v2.0 · 前端（Vite 产物 + Nginx 反向代理）" \
      version="2.0.0"

# 站点配置：静态托管 + /api 反代到 compose 里的 app 服务
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# 官方 nginx 镜像自带 curl，直接用于健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -fsS http://127.0.0.1/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
