# 管理端改约（Admin Reschedule）测试报告

> 日期：2026-09-14  
> 对照设计：`docs/reservation/admin-reschedule.md` §8（窗口 **A**：开始前均可）  
> 分支：`feature/admin-reschedule`  
> 结论：**通过**

## 测试代码

| 文件 | 说明 |
|------|------|
| `src/test/.../domain/RescheduleRulesTest.java` | 扩：管理端开始前 90 分钟可改、1 分钟可改、到点/已取消不可 |
| `src/test/.../service/ReservationServiceAdminRescheduleTest.java` | **新建**：Service mock 覆盖 §8 主路径 + purpose + 停用所有者 |
| 既有 `ReservationServiceRescheduleTest` | 回归：本人 2h 规则未被改坏 |

命令：

```bash
mvn -Dtest=RescheduleRulesTest,ReservationServiceRescheduleTest,ReservationServiceAdminRescheduleTest,ReservationDomainTest,CancelRulesTest test
```

结果：全部通过（Errors 0 / Failures 0）。

## 覆盖场景

| # | 场景 | 期望 | 结果 | 用例 |
|---|------|------|------|------|
| 1 | ADMIN、CONFIRMED、窗口内、无冲突 | 200，id/userId/venueId 不变 | 通过 | `adminRescheduleSuccess_keepsOwnerAndVenue` |
| 2 | 学生/教师调该 URL | 403 | **配置覆盖** | `@PreAuthorize` + `/api/admin/**`（设计允许不测 MVC） |
| 3 | 预约不存在 | 404 | 通过 | `adminRescheduleNotFound` |
| 4 | 已取消 | 400 | 通过 | `adminRescheduleCancelled_notAllowed` / `adminCannotRescheduleCancelled` |
| 5 | 已开始 | 400 | 通过 | `adminRescheduleAlreadyStarted_notAllowed` / `adminCannotRescheduleAtOrAfterStart` |
| 6 | 相交冲突 | 409 | 通过 | `adminRescheduleConflict_returns409` |
| 7 | 相邻 | 200 | 通过 | `adminRescheduleAdjacent_succeeds` |
| 8 | 超出开放时间 | 400 | 通过 | `adminRescheduleOutOfOpenHours` |
| 9 | 开始前 1 分钟 / 90 分钟（窗口 A） | 200 | 通过 | `adminCanRescheduleOneMinuteBeforeStart` / `adminRescheduleWithinOneMinuteWindow_succeeds` / `adminCanRescheduleWithinUserTwoHourWindow` |
| 10 | 恰好到点 | 400 | 通过 | `adminCannotRescheduleAtOrAfterStart` |
| 11 | `purpose` 空白 → null | 通过 | `adminPurposeBlank_clearsToNull` |
| 额外 | `purpose` 未传保留 | 通过 | `adminPurposeOmitted_keepsOriginal` |
| 额外 | 所有者非 ACTIVE | 400 `RESCHEDULE_NOT_ALLOWED` | 通过 | `inactiveOwner_notAllowed` |
| 回归 | 本人 2h 规则仍在 | 通过 | 既有 `ReservationServiceRescheduleTest` / `userCannotRescheduleWithinTwoHours` |

## 未覆盖

- MVC 层学生 403：依赖现有 Security 配置，未起 Spring MVC 测试。
- 真实 DB 落库：mock `update`，联调见 Docs。

## 结论

按设计 §8 + 窗口 A / purpose / 锁预约所有者，当前实现符合预期；本人改约回归未坏。@Tech Lead 可汇总。
