# 管理端写操作审计（Admin Audit）设计

> 模块：audit（新建包）+ 挂接现有管理端写路径  
> 状态：设计完成，**§7.2 已确认：1B + 2A + 3A（全默认）**  
> 目标读者：Coder（可直接落地）  
> 背景：评级共识要求管理端写操作可追查；本期**不做签到**

## 1. 需求摘要

为管理端**写操作**追加只读审计日志：谁、何时、对哪条资源、做了什么、改前/改后关键字段摘要。

| 约束 | 说明 |
|------|------|
| 存储 | **独立表** `admin_audit_log`，不往 `reservation` / `venue` 业务行塞审计列 |
| 写入时机 | 业务写成功之后、**同一事务内**追加一行；审计失败则整单回滚（避免有业务无审计） |
| 锁序 | **不改**现有 create/改约/取消的用户锁、场地锁顺序；审计 insert 不加行锁争用 |
| 权限 | 仅 ADMIN 可产生与查询；学生/教师 → 403 |
| 依赖 | 不升级；沿用 Spring Boot 2.7 + MyBatis-Plus + 现有 Jackson |
| 分支 | `feature/admin-audit`，不推 main |

### 1.1 本期必做（P0）

| action | 触发点 | resource |
|--------|--------|----------|
| `RESERVATION_CANCEL` | `ReservationService#cancelByAdmin` | `RESERVATION` + reservationId / venueId |
| `RESERVATION_RESCHEDULE` | `ReservationService#rescheduleByAdmin` | 同上 |

### 1.2 二期（已确认不做本期）

| action | 触发点 |
|--------|--------|
| `VENUE_CREATE` | `VenueService#create` |
| `VENUE_UPDATE` | `VenueService#update`（含启停，因 status 在同一 PUT 里改） |

本人侧 create/改约/取消：**不记**（本期只审计管理端写操作）。

## 2. 表结构

新增 DDL，追加到 `src/main/resources/db/schema.sql`，并提供可重复执行的增量片段（注释标明「admin-audit 增量」），方便已有库手工执行。

```sql
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `operator_id`    BIGINT        NOT NULL COMMENT '管理员 user.id',
  `action`         VARCHAR(32)   NOT NULL COMMENT '见 AdminAuditActions',
  `resource_type`  VARCHAR(16)   NOT NULL COMMENT 'RESERVATION / VENUE',
  `resource_id`    BIGINT        NOT NULL COMMENT '对应资源主键',
  `reservation_id` BIGINT        DEFAULT NULL COMMENT '预约维度查询冗余；场地类为空',
  `venue_id`       BIGINT        DEFAULT NULL COMMENT '场地 id 冗余，便于按场地查',
  `before_data`    VARCHAR(1000) DEFAULT NULL COMMENT '改前关键字段 JSON 摘要',
  `after_data`     VARCHAR(1000) DEFAULT NULL COMMENT '改后关键字段 JSON 摘要',
  `reason`         VARCHAR(200)  DEFAULT NULL COMMENT '可选原因；P0 接口无入参时恒为 null',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_reservation_time` (`reservation_id`, `created_at`),
  KEY `idx_audit_operator_time` (`operator_id`, `created_at`),
  KEY `idx_audit_resource_time` (`resource_type`, `resource_id`, `created_at`)
) ENGINE=InnoDB COMMENT='管理端写操作审计（只追加）';
```

**设计理由：**

- 独立表：业务表保持瘦，审计可归档、可扩字段而不影响预约热点行。  
- `before_data` / `after_data` 用短 JSON 字符串：免引入新中间件；只存关键字段，控制 1000 字符。  
- `reservation_id` / `venue_id` 冗余：验收「按预约或操作者查」不必解析 JSON。  
- **无 UPDATE/DELETE API**；Mapper 只提供 `insert` + 条件查询。

### 2.1 JSON 摘要字段约定（Coder 按此拼，不要 dump 整 Entity）

**预约类**（cancel / reschedule）：

```json
{
  "status": "CONFIRMED",
  "startTime": "2026-09-20 10:00:00",
  "endTime": "2026-09-20 11:00:00",
  "purpose": "社团活动",
  "userId": 2,
  "venueId": 1
}
```

- cancel：`before_data` 含取消前状态与时段；`after_data` 中 `status=CANCELLED`，时段同前。  
- reschedule：`before_data` / `after_data` 主要为 `startTime`/`endTime`/`purpose`（及 status=CONFIRMED）。

**场地类**（若纳入）：

```json
{
  "name": "...",
  "status": "ENABLED",
  "openStart": "08:00",
  "openEnd": "22:00",
  "type": "MEETING_ROOM",
  "campus": "..."
}
```

时间格式与 `ReservationResponse` 一致：`yyyy-MM-dd HH:mm:ss`；`LocalTime` 用 `HH:mm`。  
使用项目已有 `ObjectMapper`（Spring 注入）或手工拼接均可，**禁止**把 password 等无关字段写入。

## 3. 接口设计

### 3.1 写路径（无新写 API）

不新增「写审计」的对外接口。审计在 Service 内隐式写入。

### 3.2 查询（已确认本期做）

`GET /api/admin/audit-logs`

| 项 | 值 |
|----|-----|
| Controller | **新建** `AdminAuditController`，`@RequestMapping("/api/admin/audit-logs")` + `@PreAuthorize("hasRole('ADMIN')")` |
| Auth | 仅 ADMIN；学生 403 |

Query 参数（均可选，组合过滤）：

| 参数 | 类型 | 说明 |
|------|------|------|
| `reservationId` | Long | 按预约 |
| `operatorId` | Long | 按管理员 |
| `action` | String | 精确匹配 action 常量 |
| `resourceType` | String | `RESERVATION` / `VENUE` |
| `resourceId` | Long | 与 resourceType 联用 |
| `limit` | int | 默认 50，最大 200 |

响应：`Result<List<AdminAuditLogResponse>>`，按 `created_at DESC, id DESC`。

`AdminAuditLogResponse` 字段：`id, operatorId, action, resourceType, resourceId, reservationId, venueId, beforeData, afterData, reason, createdAt`。  
`beforeData`/`afterData` 以 **字符串** 返回（已是 JSON 文本），前端自行 parse；不在服务端再包一层 Map（减少噪音）。

### 3.3 错误码

不新增业务错误码。权限走现有 401/403；参数非法用 `PARAM_INVALID`（如 limit>200、非法 action 过滤值）。

## 4. 模块划分

新建包 `com.campus.venue.audit`：

| 层 | 文件 | 职责 |
|----|------|------|
| constant | `AdminAuditActions` / `AdminAuditResourceTypes` | 字符串常量，禁止魔法值 |
| entity | `AdminAuditLog` | 映射表 |
| mapper | `AdminAuditLogMapper` | `BaseMapper`；只需 insert + selectList |
| dto | `AdminAuditLogResponse` | 查询出参 |
| service | `AdminAuditService` | `record(...)` 组装并 insert；`list(...)` 查询 |
| util | `AdminAuditSnapshots`（可选） | 从 Reservation/Venue 生成 before/after JSON |
| controller | `AdminAuditController` | 仅查询（若本期做） |

**挂接（改动现有 Service，保持锁序不变）：**

1. `cancelByAdmin`：在最终 `updateById` **成功意图之后**（同一方法、同事务末尾）调用  
   `adminAuditService.recordCancel(operatorId, before, after)`。  
   快照：`before` 在改 status 前拷贝关键字段；`after` 用更新后实体。
2. `rescheduleByAdmin`：在 `applyRescheduleAfterLocks` 返回前，若调用来源是管理端则记审计。  
   **推荐**：给 `applyRescheduleAfterLocks` 增加参数 `boolean auditAsAdmin`，或拆：  
   - 本人改约：`auditAsAdmin=false`  
   - 管理端：`true`，传入 operatorId，方法内在 update 后 record reschedule。  
   不要在本人改约路径写审计。
3. 场地 create/update：**本期不挂**（§7.2）。

`AdminAuditService#record*` **只 insert，不开启新事务**（用调用方事务）。禁止 `@Async`。

伪签名：

```text
void record(Long operatorId, String action, String resourceType, Long resourceId,
            Long reservationId, Long venueId, String beforeJson, String afterJson, String reason);

void recordReservationCancel(Long operatorId, Reservation before, Reservation after);
void recordReservationReschedule(Long operatorId, Reservation before, Reservation after);
```

`operatorId`：`SecurityUtils.requireCurrentUser().getUserId()`（管理端接口已保证 ADMIN）。

## 5. 与现有流程的关系

```text
cancelByAdmin / rescheduleByAdmin
  └─ 现有锁与校验（不变）
  └─ 业务 UPDATE
  └─ admin_audit_log INSERT   ← 新增，同事务
```

- 审计 **不是** 行锁参与者，不 `SELECT FOR UPDATE` 审计表。  
- 不在 AOP 全局切所有 Controller（避免误伤、难测）；P0 **显式**在两个（或四个）Service 方法里调用，清晰可审。

## 6. 测试要求（写入设计，供 QA）

至少：

1. 领域/Service：管理端取消后 `admin_audit_log` 有 1 行，`action=RESERVATION_CANCEL`，`operator_id` 正确，`before_data`/`after_data` 状态变化可断言。  
2. 管理端改约后有 `RESERVATION_RESCHEDULE`，前后时段不同。  
3. 本人改约/取消 **不产生** 审计行。  
4. 学生调 `GET /api/admin/audit-logs` → 403（若本期有查询）。  
5. **真实库或 MyBatis 级**（呼应评级缺口）：至少一条测验证 insert 真落库（可用 `@MybatisPlusTest` / 测试库），避免只 mock Mapper。

## 7. 技术决策

### 7.1 已定

| 决策 | 选择 | 理由 |
|------|------|------|
| 存储 | 独立 `admin_audit_log` | Tech Lead 指定；不污染业务行 |
| P0 覆盖 | 管理端取消 + 管理端改约 | 评级点名的写操作 |
| 事务 | 与业务同事务追加 | 有业务必有审计 |
| 锁序 | 不改 | 红线 |
| 写入方式 | Service 显式调用 | 可审、可测，胜于盲目 AOP |
| 依赖 | 不升级 | 交底 |
| 场地写操作 | **二期**（已确认） | 缩小 P0 |
| 查询 API | **本期薄查询**（已确认） | 支撑验收 |
| reason 入参 | **不加**（已确认） | 不改现有 API |

### 7.2 范围确认（用户已选全默认：1B + 2A + 3A）

| # | 决议 | 落地 |
|---|------|------|
| 1 | **B 二期**：场地 create/update（含启停）本期不做 | `VenueService` 不挂钩；`AdminAuditActions` **预留** `VENUE_CREATE` / `VENUE_UPDATE` 常量即可 |
| 2 | **A 本期**：提供薄查询 `GET /api/admin/audit-logs` | 按 §3.2 实现 |
| 3 | **A 不加**：取消/改约不增加 `reason` 请求字段 | `reason` 列恒为 `null`；**不要**改现有 API body |

不要再留「待确认」TODO。场地若二期纳入，只加 Service 挂钩，不必改表。

## 8. 验收标准

| # | 场景 | 期望 |
|---|------|------|
| 1 | ADMIN 取消预约成功 | `admin_audit_log` +1，`RESERVATION_CANCEL`，operator/reservation/venue 正确，after.status=CANCELLED |
| 2 | ADMIN 改约成功 | +1，`RESERVATION_RESCHEDULE`，before/after 时段反映变更 |
| 3 | 学生本人取消/改约 | **无**新审计行 |
| 4 | 按 `reservationId` 查询（若有 API） | 仅该预约相关记录，时间倒序 |
| 5 | 按 `operatorId` 查询 | 仅该管理员 |
| 6 | 学生调审计查询 | 403 |
| 7 | 业务更新失败（校验/冲突） | **无**审计行（同事务未提交） |

## 9. 交给 Coder 的输入

- 本文：`docs/reservation/admin-audit.md`  
- 挂接点：`ReservationService#cancelByAdmin`、`#rescheduleByAdmin` / `applyRescheduleAfterLocks`  
- 权限范本：`AdminReservationController`、`SecurityConfig` `/api/admin/**`  
- DDL：`src/main/resources/db/schema.sql`  
- 操作者：`SecurityUtils.requireCurrentUser()`

写完自报：DDL、新包文件列表、挂接的 Service 方法、怎么跑测；开 `feature/admin-audit`。
