# 本人改约（Reschedule）测试报告

> 日期：2026-09-13  
> 对照设计：`docs/reservation/reschedule.md` §8（窗口 **A**：开始前 2h，恰好 2h 允许）  
> 分支：`feature/reschedule`  
> 结论：**通过**

## 测试代码

| 文件 | 说明 |
|------|------|
| `src/test/java/com/campus/venue/reservation/domain/RescheduleRulesTest.java` | 改约窗口 / 状态 / 已开始（领域规则） |
| `src/test/java/com/campus/venue/reservation/service/ReservationServiceRescheduleTest.java` | Service 层 mock：§8 主路径 + purpose 语义 |
| 既有 `ReservationDomainTest`（ConflictChecker 相邻/相交） | 半开区间冲突语义 |

命令：

```bash
mvn -Dtest=RescheduleRulesTest,ReservationServiceRescheduleTest,ReservationDomainTest,CancelRulesTest test
```

结果：全部通过（Errors 0 / Failures 0）。

## 覆盖场景

| # | 场景 | 期望 | 结果 | 用例 |
|---|------|------|------|------|
| 1 | 本人、CONFIRMED、窗口内、合法无冲突 | 成功，id 不变，时段更新 | 通过 | `rescheduleSuccess_keepsIdAndUpdatesSlot` |
| 2 | 新时段与其他 CONFIRMED 相交 | 409 `CONFLICT` | 通过 | `rescheduleConflict_returns409` |
| 3 | 相邻半开相接 | 成功（ConflictChecker + count=0） | 通过 | `adjacentSlotsDoNotOverlap` + `rescheduleAdjacent_succeedsWhenConflictCountZero` |
| 4 | 改他人预约 | 404 `RESERVATION_NOT_FOUND` | 通过 | `rescheduleOthersReservation_returns404` |
| 5 | 已取消 | 400 `RESCHEDULE_NOT_ALLOWED` | 通过 | `rescheduleCancelled_notAllowed` / `cancelledReservationCannotReschedule` |
| 6 | 已开始（`now >= start`） | 400 `RESCHEDULE_NOT_ALLOWED` | 通过 | `rescheduleAlreadyStarted_notAllowed` / `userCannotRescheduleAtOrAfterStart` |
| 7 | 超出开放时间 | 400 `OUT_OF_OPEN_HOURS` | 通过 | `rescheduleOutOfOpenHours_returns400` |
| 8 | 开始前不足 2h（窗口 A） | 400 `RESCHEDULE_NOT_ALLOWED` | 通过 | `rescheduleWithinTwoHourWindow_notAllowed` / `userCannotRescheduleWithinTwoHours` |
| 8b | 恰好开始前 2h | 允许 | 通过 | `userCanRescheduleExactlyTwoHoursBefore` |
| 8c | 开始前超过 2h | 允许 | 通过 | `userCanRescheduleMoreThanTwoHoursBefore` |
| P1 | `purpose` 未传 / null | 保留原用途 | 通过 | `purposeOmitted_keepsOriginal` |
| P2 | `purpose` 空白 | 置 null | 通过 | `purposeBlank_clearsToNull` |
| P3 | `purpose` 有值 | trim 后写入 | 通过 | `purposeProvided_trimsAndUpdates` |
| P4 | 改到原起止只改用途 | 成功 | 通过 | `sameSlotOnlyChangePurpose_succeeds` |

## 未覆盖（说明）

- 真实 DB 下 `LambdaUpdateWrapper` 写 null 的 SQL 落库：单测 mock 了 `update`，实体侧目的已断言；联调以 Docs/`docs/接口联调.md` 为准。
- 并发双人同场改约：无集成/压测环境，未跑。

## 结论

按设计 §8 + purpose / 恰好 2h 边界，当前实现行为符合预期。@Tech Lead 可汇总交付。
