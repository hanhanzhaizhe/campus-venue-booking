# 高校场地资源预约管理系统

独占型场地的查询、预约、改约、取消与管理。提交时强一致防超约（事务 + 场地行锁 + 读已提交）。本仓是 **Maven 后端 + `frontend/` Vue 3 前端** 的 monorepo。

## 环境

- JDK 17
- Maven 3.8+
- MySQL 8（默认 `127.0.0.1:3306`，账号 `root` / `123456`，可用环境变量 `MYSQL_USER`、`MYSQL_PASSWORD` 覆盖）
- Redis（默认 `127.0.0.1:6379`，可选；连不上时场地详情回源 MySQL，预约不受影响）

## 初始化数据库

```bash
mysql --host=127.0.0.1 --user=root -p --default-character-set=utf8mb4 < src/main/resources/db/schema.sql
mysql --host=127.0.0.1 --user=root -p --default-character-set=utf8mb4 < src/main/resources/db/data.sql
```

并发验收（AC4）需要 20 个压测账号时再执行：

```bash
mysql --host=127.0.0.1 --user=root -p --default-character-set=utf8mb4 < src/main/resources/db/ac4-users.sql
```

`occupancy-demo.sql` 仅为占用查询演示，可选。

## 启动

```bash
mvn -DskipTests spring-boot:run
```

默认端口 **8080**，健康检查：`GET http://127.0.0.1:8080/api/health`（无需登录）。

## 前端怎么跑

详情见 [frontend/README.md](frontend/README.md)。后端已在 8080 后：

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 **http://127.0.0.1:5173**。开发时 Vite 把 `/api` 代理到 8080。学生/教师进场地列表，管理员进预约管理。设计稿：[docs/frontend/v1.md](docs/frontend/v1.md)。

## 预置账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `teacher01` | `123456` | 教师 |
| `stu01` | `123456` | 学生 |

登录：`POST /api/auth/login`，之后请求头加 `Authorization: Bearer <token>`。

## 文档

- [接口联调说明](docs/接口联调.md)
- [前端 V1 设计](docs/frontend/v1.md)
- [前端怎么跑](frontend/README.md)
- [变更记录](CHANGELOG.md)
