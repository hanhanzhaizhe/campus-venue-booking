# 本人改约（Reschedule）设计

> 模块：reservation  
> 状态：设计完成，**改约窗口已确认：A（开始前 2h）**  
> 目标读者：Coder（可直接落地）  
> 仓库路径：`/workspace/campus-venue-booking/`

## 1. 需求摘要

登录用户修改**自己的、未开始的、CONFIRMED** 预约的时段（及可选用途）。

| 约束 | 说明 |
|------|------|
| 预约 id | 不变（原地 update，不删建） |
| 场地 | 不变（请求体不含 `venueId`） |
| 配额 | 不占新配额（不调用 `QuotaRules`） |
| 表结构 | **不改** `schema.sql` / Entity 字段 |
| 一致性 | 事务 `READ_COMMITTED`；锁序与 create 一致：**先用户行锁，再场地行锁** |
| 冲突 | 查询必须 **排除本单 id**；半开区间，相邻不算冲突（沿用现有 create 条件） |

## 2. 表结构

**无 DDL 变更。**

沿用现有 `reservation` 表字段：`id / venue_id / user_id / start_time / end_time / status / purpose / created_at / updated_at`。  
改约只 `UPDATE`：`start_time`、`end_time`，以及按规则更新的 `purpose`；`updated_at` 由 DB `ON UPDATE` 自动维护。

**设计理由：** 改约是同一预约资源的时段变更，不是新单据；改表会引入无谓迁移，且与「半天走通全流程」目标不符。

## 3. 接口设计

### 3.1 `PUT /api/reservations/{id}`

| 项 | 值 |
|----|-----|
| Method / URL | `PUT /api/reservations/{id}` |
| Auth | 已登录用户（与 create / cancel 相同，走现有 JWT） |
| Path | `id`：预约主键 |
| Content-Type | `application/json` |

#### 请求 Body：`RescheduleReservationRequest`

对齐 create 的时间字段，**不含** `venueId`。

| 字段 | 类型 | 必填 | 校验 / 说明 |
|------|------|------|-------------|
| `date` | `LocalDate` (`yyyy-MM-dd`) | 是 | `@NotNull` |
| `startTime` | `LocalTime` (`HH:mm`) | 是 | `@NotNull` |
| `endTime` | `LocalTime` (`HH:mm`) | 是 | `@NotNull` |
| `purpose` | `String` | 否 | `@Size(max=100)`；见下方语义 |

**`purpose` 语义（Coder 必须按此实现）：**

- JSON **未传**或值为 `null` → **保留**原 `purpose`
- 有传非空白字符串 → `trim` 后写入
- 有传空白字符串（`""` / 仅空白）→ 置为 `null`（与 create 的 `StringUtils.hasText` 行为一致）

#### 响应：`Result<ReservationResponse>`

复用现有 `ReservationResponse`（`id / venueId / userId / startTime / endTime / status / purpose`）。  
成功时 `id`、`venueId`、`userId`、`status=CONFIRMED` 与改约前一致；`startTime`/`endTime`（及可能的 `purpose`）为新值。

#### 错误码

| 场景 | ErrorCode | HTTP |
|------|-----------|------|
| 参数校验失败（Bean Validation） | `PARAM_INVALID` | 400 |
| 时间不合法（整点/半点、时长、起止顺序等，`TimeSlotRules`） | `TIME_INVALID` | 400 |
| 新时段早于当前或超出可预约日窗口（`TimeSlotRules.resolve`） | `TOO_EARLY_OR_TOO_LATE` | 400 |
| 新时段超出场地开放时间 | `OUT_OF_OPEN_HOURS` | 400 |
| 状态不可改 / 已开始 / **超出改约窗口（开始前 2h）** | `RESCHEDULE_NOT_ALLOWED`（**新增**） | 400 |
| 场地已停用 | `VENUE_DISABLED` | 400 |
| 未登录 | `UNAUTHENTICATED` | 401 |
| 预约不存在，或存在但**不属于当前用户**（防枚举，与 cancel 一致） | `RESERVATION_NOT_FOUND` | 404 |
| 场地行锁后发现场地不存在（极端） | `VENUE_NOT_FOUND` | 404 |
| 与他人/其他单时段相交 | `CONFLICT` | 409 |
| 同一用户同一场地同一起止已有**另一条** CONFIRMED（排除本单） | `DUPLICATE_RESERVATION` | 409 |

**新增枚举（`ErrorCode`）：**

```text
RESCHEDULE_NOT_ALLOWED("RESCHEDULE_NOT_ALLOWED", "当前不可改约", 400)
```

不复用 `CANCEL_NOT_ALLOWED`：文案与语义都是「取消」，会误导联调与前端。

## 4. 领域规则

### 4.1 新时段：复用 `TimeSlotRules`

对 body 的 `date/startTime/endTime` 调用：

1. `TimeSlotRules.resolve(date, startTime, endTime, now)`  
2. `TimeSlotRules.assertWithinOpenHours(range, venue.openStart, venue.openEnd)`  

场地取**本单原 `venueId`** 对应场地（锁后的 `lockedVenue`），不接受换场。

### 4.2 改约资格：新建 `RescheduleRules`

新建 `com.campus.venue.reservation.domain.RescheduleRules`，风格对齐 `CancelRules`。

```text
assertUserCanReschedule(Reservation reservation, LocalDateTime now)
```

必须校验：

1. `status == CONFIRMED`，否则 `RESCHEDULE_NOT_ALLOWED`
2. `now.isBefore(reservation.getStartTime())`（**已开始含恰好到点不可改**），否则 `RESCHEDULE_NOT_ALLOWED`
3. **改约窗口（已确认方案 A，见 §7.2）**：`deadline = startTime - 2h`，`now.isAfter(deadline)` 则 `RESCHEDULE_NOT_ALLOWED`（恰好 2h 前允许，对齐 `CancelRules`）

锁内再次调用本方法，避免 TOCTOU。

### 4.3 冲突与重复（排除本单）

在已持有用户锁 + 场地锁之后：

**冲突（场地维度）：**

```text
venue_id = lockedVenue.id
AND status = CONFIRMED
AND start_time < newEnd
AND end_time > newStart
AND id <> currentReservationId
```

`count > 0` → `CONFLICT`  
（与 create 相同半开语义：`start < otherEnd && end > otherStart`；相邻 `end == otherStart` 成功。）

**重复（本人同场同段）：**

```text
user_id = lockedUser.id
AND venue_id = lockedVenue.id
AND start_time = newStart
AND end_time = newEnd
AND status = CONFIRMED
AND id <> currentReservationId
```

`count > 0` → `DUPLICATE_RESERVATION`

本单改到与自己当前完全相同的起止：排除本单后 count=0，允许（可仅更新 `purpose`）。

### 4.4 配额

**不调用** `QuotaRules.assertWithinQuota`。  
理由：单据仍是同一条 CONFIRMED，未结束条数不增加。

### 4.5 不改的内容

- 不改 `venueId` / `userId` / `status`
- 不做「先取消再新建」
- 不引入审批 / 管理端改约（本任务仅本人改约）

## 5. 模块划分与改动清单

| 层 | 文件 | 职责 |
|----|------|------|
| Controller | `ReservationController` | 新增 `PUT /{id}`，校验 body，调用 Service |
| DTO | **新建** `RescheduleReservationRequest` | 见 §3.1；不要复用 `CreateReservationRequest`（其强制 `venueId`） |
| DTO | `ReservationResponse` | 不改，继续 `from(entity)` |
| Domain | **新建** `RescheduleRules` | 改约资格（状态 / 未开始 / 窗口） |
| Domain | `TimeSlotRules` / `ConflictChecker` / `QuotaRules` / `CancelRules` | **不改**（冲突用 Mapper 条件表达即可；`ConflictChecker` 可在单测里复用） |
| Service | `ReservationService` | **新增** `reschedule(Long id, RescheduleReservationRequest)`，见 §6 |
| Mapper | `ReservationMapper` | 无需新方法；`LambdaQueryWrapper` + `.ne(Reservation::getId, id)` + 现有 `selectByIdForUpdate`（User/Venue） |
| Entity | `Reservation` | 不改 |
| ErrorCode | `ErrorCode` | 新增 `RESCHEDULE_NOT_ALLOWED` |
| DB | `schema.sql` | **不改** |
| 单测（建议，交给 QA/可顺手） | **新建** `RescheduleRulesTest` | 对齐 `CancelRulesTest` 风格 |

**配置 / 依赖：** 无 `application.yml` 变更；**禁止升级依赖**。

## 6. Service 实现流程（Coder 按序落地）

方法签名建议：

```text
@Transactional(isolation = Isolation.READ_COMMITTED)
public ReservationResponse reschedule(Long id, RescheduleReservationRequest request)
```

**推荐步骤（锁序不可颠倒）：**

1. `LoginUser loginUser = SecurityUtils.requireCurrentUser()`
2. `Reservation existing = reservationMapper.selectById(id)`  
   - `existing == null` 或 `!loginUser.getUserId().equals(existing.getUserId())` → `RESERVATION_NOT_FOUND`
3. （可选预检）`RescheduleRules.assertUserCanReschedule(existing, LocalDateTime.now())` —— 快速失败；**真正生效以锁内为准**
4. `User lockedUser = userMapper.selectByIdForUpdate(loginUser.getUserId())`  
   - 用户不存在或非 `ACTIVE` → `UNAUTHENTICATED`（与 create 一致）
5. `Venue lockedVenue = venueMapper.selectByIdForUpdate(existing.getVenueId())`  
   - null → `VENUE_NOT_FOUND`；非 `ENABLED` → `VENUE_DISABLED`
6. 重新加载预约行，建议：`Reservation locked = reservationMapper.selectById(id)`（已在用户+场地锁保护下；若项目后续有 `selectByIdForUpdate` 可改用，当前 Mapper 无则与 cancel 一样 `selectById` 即可）  
   - null 或 `userId` 不匹配 → `RESERVATION_NOT_FOUND`
7. 锁内再次 `RescheduleRules.assertUserCanReschedule(locked, now)`
8. `TimeRange range = TimeSlotRules.resolve(request.getDate(), request.getStartTime(), request.getEndTime(), now)`
9. `TimeSlotRules.assertWithinOpenHours(range, lockedVenue.getOpenStart(), lockedVenue.getOpenEnd())`
10. 重复查询（§4.3，排除 `locked.getId()`）
11. 冲突查询（§4.3，排除 `locked.getId()`）
12. `locked.setStartTime(range.getStart()); locked.setEndTime(range.getEnd());`  
    按 §3.1 更新 `purpose`
13. `reservationMapper.updateById(locked)`
14. `return ReservationResponse.from(locked)`

**不要**在本方法里改无关文件；**不要**调用 `QuotaRules`。

## 7. 技术决策

### 7.1 已定

| 决策 | 选择 | 理由 |
|------|------|------|
| HTTP 语义 | `PUT /api/reservations/{id}` | Tech Lead 指定；幂等更新同一资源 |
| 独立 Request DTO | `RescheduleReservationRequest` | 避免 create 的必填 `venueId` 被误用换场 |
| 锁序 | 先 user 再 venue | 与 create 一致，降低死锁风险 |
| 隔离级别 | `READ_COMMITTED` | 与 create/cancel 一致 |
| 冲突排除本单 | `id <> self` | 否则自己与自己永远 409 |
| 错误码 | 新增 `RESCHEDULE_NOT_ALLOWED` | 与取消文案分离 |
| 改约窗口 | **A：开始前 2h**（已确认） | 与取消一致 |

### 7.2 改约窗口（已确认）

**用户已选方案 A**：与取消一致，开始前 2 小时可改。

| 项 | 约定 |
|----|------|
| 规则 | `deadline = startTime.minusHours(2)`；若 `now.isAfter(deadline)` → `RESCHEDULE_NOT_ALLOWED` |
| 边界 | 恰好 `startTime - 2h` **允许**（对齐 `CancelRules.assertUserCanCancel`） |
| 常量 | `RescheduleRules.USER_RESCHEDULE_HOURS = 2` |
| 未采纳 | 方案 B（开始前均可）已否决，无需再留 TODO |

实现应对齐：

```text
public static final int USER_RESCHEDULE_HOURS = 2;

// 在 CONFIRMED + now.isBefore(startTime) 之后：
LocalDateTime deadline = reservation.getStartTime().minusHours(USER_RESCHEDULE_HOURS);
if (now.isAfter(deadline)) {
    throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
}
```

## 8. 验收标准（对照 Tech Lead）

| # | 场景 | 期望 |
|---|------|------|
| 1 | 本人、CONFIRMED、窗口内、新时段合法且无冲突 | 200，id 不变，时段更新 |
| 2 | 新时段与其他 CONFIRMED **相交** | 409 `CONFLICT` |
| 3 | 新时段与其他单 **相邻**（半开相接） | 200 成功 |
| 4 | 改他人预约 | 404 `RESERVATION_NOT_FOUND` |
| 5 | 已取消 | 400 `RESCHEDULE_NOT_ALLOWED` |
| 6 | 已开始（`now >= startTime`） | 400 `RESCHEDULE_NOT_ALLOWED` |
| 7 | 新时段超出开放时间 | 400 `OUT_OF_OPEN_HOURS` |
| 8 | 开始前不足 2h | 400 `RESCHEDULE_NOT_ALLOWED` |
| 9 | 恰好开始前 2h | 200 允许 |

## 9. 交给 Coder 的输入清单

- 本文档：`docs/reservation/reschedule.md`
- 范本：`ReservationService#create`（锁序、冲突、开放时间）、`cancelByUser`（所有权 → 404）
- 相关路径：  
  `ReservationController` / `CreateReservationRequest` / `ReservationResponse` /  
  `TimeSlotRules` / `CancelRules` / `QuotaRules` / `ReservationMapper` /  
  `ErrorCode` / `UserMapper#selectByIdForUpdate` / `VenueMapper#selectByIdForUpdate`

写完后自报改动文件列表；开 `feature/` 分支，**不推 main**。  
代码完成后交 Reviewer。
