# 审查报告：本人改约（feature/reschedule）

- 日期：2026-09-13
- 设计：`docs/reservation/reschedule.md`
- 分支：`feature/reschedule`
- 结论：**不通过**（1 Blocker）

改动范围：`RescheduleReservationRequest` / `RescheduleRules` / `RescheduleRulesTest` / `ErrorCode` / `ReservationService#reschedule` / `ReservationController` `PUT /{id}`

---

## [Blocker]

### 1. 空白 `purpose` 无法落库为 null

- 位置：`ReservationService.java:175-178`
- 原因：设计 §3.1 要求传 `""` / 仅空白时把 `purpose` 置为 `null`。代码里对实体做了 `setPurpose(null)`，但 MyBatis-Plus 3.5.5 默认 `updateStrategy=NOT_NULL`（项目未改 `db-config` / 字段注解），`updateById` 会跳过 null 字段，数据库仍保留旧用途。接口返回的内存对象会显示 `null`，再查库却还是旧值，造成读写不一致。
- 修法（任选其一，推荐 1）：
  1. 改约更新改用 `LambdaUpdateWrapper`，在 `purpose != null` 时显式 `.set(Reservation::getPurpose, trimmedOrNull)`，再 `update(null, wrapper)`；响应可再 `selectById` 或手动同步实体。
  2. 仅在 `Reservation.purpose` 上加 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`（或 `IGNORED`），确保 null 会写入；确认不影响其他 `updateById` 路径。
- 建议 QA 补用例：原有 purpose 非空 → 改约 body `purpose:""` → 再 GET/list 断言库中为 null。

---

## [Major]

无。锁序（先 user 再 venue）、冲突/重复排除本单、不调 `QuotaRules`、窗口 A（恰好 2h 允许）、RC 事务与设计一致。

---

## [Minor]

### 1. `RescheduleRulesTest` 未覆盖「远早于 2h」正向路径

- 位置：`RescheduleRulesTest.java`（仅有恰好 2h / 窗口内失败 / 已开始 / 已取消）
- 原因：缺「开始前 >2h 允许」用例，回归时窗口常量改错不易发现。属覆盖缺口，非逻辑错误；可留给 QA。

---

## [Approved]

- `RescheduleRules` 与设计 §4.2 / §7.2 一致：CONFIRMED、`now.isBefore(start)`、`deadline=start-2h`、`isAfter` 才拒（恰好 2h 过）。
- `PUT /api/reservations/{id}` + 独立 DTO（无 `venueId`）正确，避免误复用 create。
- `ErrorCode.RESCHEDULE_NOT_ALLOWED` 文案与取消分离。
- 冲突/重复查询均 `.ne(Reservation::getId, locked.getId())`，相邻半开语义与 create 一致。
- `purpose == null`（未传）时保留原值的分支意图正确（落库问题见 Blocker）。
- 未改表、未改依赖、未推 main。

---

打回 @Coder 修 Blocker 后再送审。
