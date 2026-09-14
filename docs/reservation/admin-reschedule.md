# 管理端改约（Admin Reschedule）设计

> 模块：reservation  
> 状态：设计完成，**管理端改约窗口已确认：A（开始前均可）**  
> 目标读者：Coder（可直接落地）  
> 前置：本人改约已合 main（`docs/reservation/reschedule.md`，PR #1）

## 1. 需求摘要

**ADMIN** 修改**任意用户**的、未开始的、CONFIRMED 预约时段（及可选用途）。

| 约束 | 说明 |
|------|------|
| 预约 id | 不变（原地 update） |
| 场地 | 不变（复用 `RescheduleReservationRequest`，无 `venueId`） |
| 配额 | 不占新配额（不调 `QuotaRules`） |
| 表结构 | **不改** |
| 一致性 | 事务 `READ_COMMITTED`；锁序与本人改约一致：**先锁预约所属用户，再锁场地** |
| 冲突 | 排除本单 id；半开区间，相邻不算冲突 |
| 权限 | 仅 `ADMIN`；学生/教师 → **403**（走现有 `/api/admin/**` 安全配置） |

**与本人改约的差异（仅这些）：**

| 点 | 本人改约 | 管理端改约 |
|----|----------|------------|
| URL | `PUT /api/reservations/{id}` | `PUT /api/admin/reservations/{id}` |
| 所有权 | 非本人 → 404 | 任意用户的单均可；不存在 → 404 |
| 窗口 | 开始前 2h（已确认） | **开始前均可**（已确认 §7.2 A） |
| 领域方法 | `RescheduleRules.assertUserCanReschedule` | **新增** `assertAdminCanReschedule` |

## 2. 表结构

**无 DDL。** 字段与本人改约相同，只 `UPDATE` `start_time` / `end_time` /（按语义）`purpose`。

## 3. 接口设计

### 3.1 `PUT /api/admin/reservations/{id}`

| 项 | 值 |
|----|-----|
| Method / URL | `PUT /api/admin/reservations/{id}` |
| Controller | 现有 `AdminReservationController`（类上已有 `@PreAuthorize("hasRole('ADMIN')")`，并受 `SecurityConfig` 中 `/api/admin/**` → `hasRole("ADMIN")` 保护） |
| Auth | 必须 ADMIN；未登录 401；非 ADMIN 403 |
| Body | **复用** `RescheduleReservationRequest`（不要新建几乎一样的 DTO） |
| 响应 | `Result<ReservationResponse>`（复用） |

#### `purpose` 语义

与本人改约 §3.1 **完全一致**：

- `null` / 未传 → 保留原值  
- 非空白 → `trim` 写入  
- 空白串 → 显式置 `null`（必须用 `LambdaUpdateWrapper.set`，禁止只靠 `updateById` 跳过 null）

#### 错误码（全部复用，不新增枚举）

| 场景 | ErrorCode | HTTP |
|------|-----------|------|
| Bean Validation | `PARAM_INVALID` | 400 |
| 时段非法 | `TIME_INVALID` | 400 |
| 超出可预约日 / 新时段不晚于 now | `TOO_EARLY_OR_TOO_LATE` | 400 |
| 超出开放时间 | `OUT_OF_OPEN_HOURS` | 400 |
| 非 CONFIRMED / 已开始 / 超出管理端窗口 | `RESCHEDULE_NOT_ALLOWED` | 400 |
| 场地停用 | `VENUE_DISABLED` | 400 |
| 未登录 | `UNAUTHENTICATED` | 401 |
| 非 ADMIN | `FORBIDDEN` | 403 |
| 预约 id 不存在 | `RESERVATION_NOT_FOUND` | 404 |
| 场地不存在 | `VENUE_NOT_FOUND` | 404 |
| 时段相交 | `CONFLICT` | 409 |
| 同用户同场同段另一条 CONFIRMED | `DUPLICATE_RESERVATION` | 409 |

说明：管理端**不**把「别人的单」伪装成 404；管理员可以改任何人的单。学生打这个 URL → 403，不是 404。

## 4. 领域规则

### 4.1 复用

- `TimeSlotRules.resolve` + `assertWithinOpenHours`
- 冲突 / 重复查询条件与 `ReservationService#reschedule` 相同（含 `.ne(id)`）
- `purpose` 更新写法与本人改约相同（`LambdaUpdateWrapper`）

### 4.2 新增：`RescheduleRules.assertAdminCanReschedule`

对齐 `CancelRules.assertAdminCanCancel` 的结构，放在现有 `RescheduleRules` 内（不要新建类）。

```text
assertAdminCanReschedule(Reservation reservation, LocalDateTime now)
1. status == CONFIRMED，否则 RESCHEDULE_NOT_ALLOWED
2. now.isBefore(startTime)，否则 RESCHEDULE_NOT_ALLOWED
   （已确认方案 A：无额外 2h 窗口，见 §7.2）
```

本人侧的 `assertUserCanReschedule` **不要改**。

### 4.3 锁与用户

- 行锁对象是**预约所属用户** `reservation.userId`，不是管理员自己。  
  理由：重复校验按预约用户维度；与 create/本人改约锁序一致，降低死锁。
- 再锁 `reservation.venueId`。
- 锁内重新 `selectById` 预约，再跑 `assertAdminCanReschedule`。

### 4.4 不做的事

- 不换场、不改 `userId`、不改 `status`
- 不调 `QuotaRules`
- 不做「先取消再约」
- 不升级依赖、不推 main（开 `feature/admin-reschedule`）

## 5. 模块改动清单

| 层 | 文件 | 改动 |
|----|------|------|
| Controller | `AdminReservationController` | 新增 `PUT /{id}`，body=`RescheduleReservationRequest`，调 Service |
| DTO | `RescheduleReservationRequest` | **不改**，复用 |
| Domain | `RescheduleRules` | 新增 `assertAdminCanReschedule` |
| Service | `ReservationService` | 新增 `rescheduleByAdmin(Long id, RescheduleReservationRequest)` |
| ErrorCode / schema | — | **不改** |
| 单测 | `RescheduleRulesTest`（扩）+ 建议 `ReservationServiceAdminRescheduleTest` | 窗口边界、403 由安全层覆盖可不测 MVC；Service 测资格与冲突 |

**推荐实现结构（避免两份冲突 SQL 漂移）：**

```text
reschedule(id, req)          // 本人：校验所有权 + assertUserCanReschedule
rescheduleByAdmin(id, req)   // 管理：无所有权校验 + assertAdminCanReschedule
二者在锁用户/场地之后，调用同一 private 方法完成：
  resolve 时段 → 开放时间 → duplicate → conflict → LambdaUpdateWrapper 更新
```

private 方法名建议：`applyRescheduleAfterLocks(...)`。若 Coder 觉得抽 private 风险大，允许先复制本人改约后半段，但**冲突条件必须与本人改约逐字一致**。

## 6. `rescheduleByAdmin` 流程

```text
@Transactional(isolation = READ_COMMITTED)
rescheduleByAdmin(id, request):
  1. SecurityUtils.requireCurrentUser()  // 控制器已限制 ADMIN；此处保证有登录态
  2. existing = selectById(id)；null → RESERVATION_NOT_FOUND
  3. （可选预检）assertAdminCanReschedule(existing, now)
  4. lockedUser = userMapper.selectByIdForUpdate(existing.getUserId())
     - null 或非 ACTIVE → 仍用 UNAUTHENTICATED？不合适。
       约定：预约用户缺失 → RESERVATION_NOT_FOUND；非 ACTIVE → RESCHEDULE_NOT_ALLOWED
       （管理员改约不应因学生停用而报「未登录」）
  5. lockedVenue = venueMapper.selectByIdForUpdate(existing.getVenueId())
     - null → VENUE_NOT_FOUND；非 ENABLED → VENUE_DISABLED
  6. locked = selectById(id)；null → RESERVATION_NOT_FOUND
  7. 锁内 assertAdminCanReschedule(locked, now)
  8. 与本人改约相同的 resolve / openHours / duplicate(排除本单，userId=locked.userId) / conflict / update
  9. return ReservationResponse.from(locked)
```

**注意：** 步骤 4 与本人改约不同——本人改约锁的是 `loginUser.userId` 且非 ACTIVE → `UNAUTHENTICATED`；管理端锁的是预约所有者，错误码按上表，**不要**把管理员会话报成未登录。

## 7. 技术决策

### 7.1 已定

| 决策 | 选择 | 理由 |
|------|------|------|
| URL | `PUT /api/admin/reservations/{id}` | Tech Lead 指定；挂管理控制器 |
| Body | 复用 `RescheduleReservationRequest` | 字段相同，避免双 DTO |
| 权限 | 类级 `@PreAuthorize` + `/api/admin/**` | 学生 403，零额外配置 |
| 锁序 | 先预约用户，后场地 | 与 create/本人改约一致 |
| 更新 | `LambdaUpdateWrapper` | 修复过的 purpose 清空语义 |
| 错误码 | 复用 `RESCHEDULE_NOT_ALLOWED` | 无需新枚举 |
| 管理端窗口 | **A：开始前均可**（已确认） | 对齐管理端取消 |

### 7.2 管理端改约窗口（已确认）

**用户已选方案 A**：与管理端取消一致，只要 `now.isBefore(startTime)` 即可改。

| 项 | 约定 |
|----|------|
| 规则 | `CONFIRMED` 且 `now.isBefore(startTime)`；否则 `RESCHEDULE_NOT_ALLOWED` |
| 边界 | 恰好到 `startTime` **不可**改（与 `CancelRules.assertAdminCanCancel` 一致） |
| 未采纳 | 方案 B（开始前 2h）已否决，**不要**留 TODO |

实现应对齐：

```text
public static void assertAdminCanReschedule(Reservation reservation, LocalDateTime now) {
    if (!ReservationStatuses.CONFIRMED.equals(reservation.getStatus())) {
        throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
    }
    if (!now.isBefore(reservation.getStartTime())) {
        throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
    }
}
```

## 8. 验收标准

| # | 场景 | 期望 |
|---|------|------|
| 1 | ADMIN、CONFIRMED、窗口内、无冲突 | 200，id/userId/venueId 不变，时段更新 |
| 2 | 学生/教师调该 URL | 403 `FORBIDDEN` |
| 3 | 预约不存在 | 404 `RESERVATION_NOT_FOUND` |
| 4 | 已取消 | 400 `RESCHEDULE_NOT_ALLOWED` |
| 5 | 已开始（`now >= start`） | 400 `RESCHEDULE_NOT_ALLOWED` |
| 6 | 新时段与其他单相交 | 409 `CONFLICT` |
| 7 | 新时段与其他单相邻 | 200 |
| 8 | 超出开放时间 | 400 `OUT_OF_OPEN_HOURS` |
| 9 | 开始前 1 分钟仍可改 | 200 |
| 10 | 恰好到点 / 已开始 | 400 `RESCHEDULE_NOT_ALLOWED` |
| 11 | `purpose` 传 `""` | 库中 purpose 为 null |

## 9. 交给 Coder 的输入

- 本文：`docs/reservation/admin-reschedule.md`
- 本人改约：`docs/reservation/reschedule.md`、`ReservationService#reschedule`、`RescheduleRules`、`RescheduleReservationRequest`
- 管理端范本：`AdminReservationController`、`cancelByAdmin` / `CancelRules.assertAdminCanCancel`
- 安全：`SecurityConfig` 中 `/api/admin/**`

分支：`feature/admin-reschedule`；写完自报文件列表与验证方式；交 Reviewer 前勿改无关文件。
