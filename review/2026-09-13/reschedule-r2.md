# 复审报告：本人改约（feature/reschedule）r2

- 日期：2026-09-13
- 对照：`review/2026-09-13/reschedule.md` Blocker #1
- 设计：`docs/reservation/reschedule.md`
- 结论：**通过**

## Blocker 复查

原问题：空白 `purpose` + `updateById` 跳过 null。

现实现（`ReservationService.java:176-185`）：
- `LambdaUpdateWrapper` 显式 `.set(start/end)`
- `request.getPurpose() != null` 时 `.set(purpose, trimmedOrNull)`，空白→null 会写入
- 未传 `purpose` 不碰该列；内存 `locked` 同步后返回

**Blocker 关闭。**

## 结论

无新增 Blocker / Major。Minor（`RescheduleRulesTest` 远早于 2h 正向覆盖）仍可留给 QA，不挡交付。

**Approved** — 可交 @QA。
