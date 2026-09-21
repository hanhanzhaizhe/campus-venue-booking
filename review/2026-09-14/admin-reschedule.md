# 审查报告：管理端改约（feature/admin-reschedule）

- 日期：2026-09-14
- 设计：`docs/reservation/admin-reschedule.md`
- 分支：`feature/admin-reschedule`
- 结论：**通过（Approved）**

改动范围：`AdminReservationController` `PUT /{id}`、`RescheduleRules#assertAdminCanReschedule`、`ReservationService#rescheduleByAdmin` + `applyRescheduleAfterLocks`、`RescheduleRulesTest` 扩展

---

## [Blocker]

无。

---

## [Major]

无。

---

## [Minor]

### 1. 未补 Service 层管理端改约用例

- 位置：无 `ReservationServiceAdminRescheduleTest`（设计 §5 为「建议」）
- 原因：领域规则已测，但「锁预约所属用户 + 非 ACTIVE → `RESCHEDULE_NOT_ALLOWED`」「无所有权伪装 404」等服务行为未在本分支单测覆盖。不挡交付，可交 QA 按 §8 补。

---

## [Approved]

- `assertAdminCanReschedule` 对齐 §7.2 A：仅 `CONFIRMED` + `now.isBefore(start)`；本人 `assertUserCanReschedule` 未改、2h 窗口仍在。
- `rescheduleByAdmin`：无所有权校验；锁的是 `existing.userId` 非管理员；用户缺失 → `RESERVATION_NOT_FOUND`，非 ACTIVE → `RESCHEDULE_NOT_ALLOWED`（未误用 `UNAUTHENTICATED`）。
- `applyRescheduleAfterLocks` 共用冲突/重复 `.ne(id)` 与 `LambdaUpdateWrapper` purpose 语义，避免双份 SQL 漂移。
- `AdminReservationController` 复用 `RescheduleReservationRequest`，类级 `@PreAuthorize("hasRole('ADMIN')")` 支撑学生 403。
- 未改表、未新错误码、未升依赖；`RescheduleRulesTest` 覆盖开始前 1 分钟可改 / 到点不可 / 已取消。

可交 @QA。
