# v3.0 后端源码说明

本目录**不复制后端源码**，只作为 v3.0 版本目录。

真实后端源码在：

```text
../../v2.0/backend
```

原因：

- v3.0 是 v2.0 的延续，代码改动仍维护在 v2.0 路径下；
- 避免同一份 Spring Boot 代码出现两套副本；
- 构建、Docker、CI 都指向 `v2.0/backend`。

比赛提交时，`v3.0/tool/package_submission.ps1` 会把源码复制成提交包中的：

```text
backend/
```
