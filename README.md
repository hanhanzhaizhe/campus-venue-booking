# 高校场地资源预约管理系统（P0 后端）

独占型场地的查询、预约、改约、取消与管理。提交时强一致防超约（事务 + 场地行锁 + 读已提交）。

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

接口调试用 Swagger（推荐）：浏览器打开 [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)

1. 展开「1. 登录」→ `POST /api/auth/login`，账号 `teacher01` / `123456`，Execute  
2. 复制返回的 `data.token`  
3. 点页面右上角 **Authorize**，粘贴 token（不要加 `Bearer`），Authorize → Close  
4. 之后点其它接口 Execute 会自动带 Token  

管理接口请用 `admin` 登录后再 Authorize（换账号要先 Logout 再填新 token）。

首次启动会把种子用户的 `PENDING_BCRYPT` 写成密码 `123456` 的 BCrypt。

## 预置账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `teacher01` | `123456` | 教师 |
| `stu01` | `123456` | 学生 |
| `stress1` … `stress20` | `123456` | 学生（仅 AC4） |

登录：`POST /api/auth/login`，之后请求头加 `Authorization: Bearer <token>`。

## 用 Postman 测 AC1–AC10

1. 导入 `postman/venue-booking.postman_collection.json`
2. 导入 `postman/venue-booking.postman_environment.json`，选中该环境
3. 确认 `baseUrl` 为 `http://127.0.0.1:8080`
4. 按集合顺序执行：**先跑「0. 登录」**（会写入 token），再跑各 AC

教师「未结束预约」须少于 2 条，学生同理，否则 AC1/AC5 会先撞上配额。本地若已反复测过，可换干净库或先取消旧单。

集合会按「明天」的日期组预约，避免约到已过去的时段。AC4 无法用 Postman 同时发 20 路，请用下面的脚本。

## AC4 并发验收

服务已启动、`ac4-users.sql` 已执行后：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ac4-concurrent.ps1 -BaseUrl http://127.0.0.1:8080
```

期望：HTTP 成功恰好 1 次，其余 `CONFLICT`；库中该时段 `CONFIRMED` 为 1。

## 验收对照

| 编号 | 内容 | 期望 |
|---|---|---|
| AC1 | 正常预约 | `CONFIRMED` |
| AC2 | 时段相交 | 409 `CONFLICT` |
| AC3 | 相邻时段 | 两条都成功 |
| AC4 | 20 并发同槽 | 成功 1 条 |
| AC5 | 取消后释放 | 他人可再约 |
| AC6 | 开始前不足 2 小时 | 用户 `CANCEL_NOT_ALLOWED`，管理员可取消 |
| AC7 | 停用场地 | 新约 `VENUE_DISABLED`，旧约仍在 |
| AC8 | 学生创建场地 | 403 `FORBIDDEN` |
| AC9 | 未结束已有 2 条再约 | `QUOTA_EXCEEDED` |
| AC10 | 超出开放时间 | `OUT_OF_OPEN_HOURS` |
| AC11 | 本人改约（窗口内、无冲突） | 200，id 不变 |
| AC12 | 改约相交 / 相邻 | 409 `CONFLICT` / 200 |
| AC13 | 改他人或窗口外 / 已开始 | 404 或 `RESCHEDULE_NOT_ALLOWED` |

## 主要接口

| 方法 | 路径 | 角色 |
|---|---|---|
| POST | `/api/auth/login` | 公开 |
| GET | `/api/auth/me` | 登录 |
| GET | `/api/venues` | 登录，仅启用 |
| GET | `/api/venues/{id}/occupancy?date=` | 登录 |
| POST | `/api/reservations` | 登录 |
| PUT | `/api/reservations/{id}` | 本人改约，提前 2 小时；不换场、不占新配额 |
| GET | `/api/reservations/me` | 本人 |
| POST | `/api/reservations/{id}/cancel` | 本人，提前 2 小时 |
| GET/POST/PUT | `/api/admin/venues` | 管理员 |
| GET | `/api/admin/reservations` | 管理员 |
| POST | `/api/admin/reservations/{id}/cancel` | 管理员，开始前即可 |

统一响应：`{ "code", "message", "data" }`。业务冲突用 `CONFLICT`（HTTP 409）。

## 领域单测

```bash
mvn test
```

覆盖时间对齐、开放时间、区间相交、配额、取消窗口、改约窗口（开始前 2h，恰好 2h 允许）。

## 文档

- [接口联调说明](docs/接口联调.md)
- [本人改约设计](docs/reservation/reschedule.md)
- [变更记录](CHANGELOG.md)
- [面试讲法](docs/面试讲法.md)
- [面试准备：提问 / 学习 / 知识点](docs/面试准备.md)
- [面试题库（问答）](docs/面试题库.md)
- [简历项目描述](docs/简历项目.md)
