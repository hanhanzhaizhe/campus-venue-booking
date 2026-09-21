# 管理端写操作审计（Admin Audit）测试报告

> 日期：2026-09-14  
> 对照设计：`docs/reservation/admin-audit.md` §6 / §8（1B+2A+3A）  
> 分支：`feature/admin-audit`  
> 结论：**通过**

## 测试代码

| 文件 | 说明 |
|------|------|
| `src/test/.../audit/AdminAuditLogMapperIT.java` | **新建** §6.5 真实库 insert（`@MybatisPlusTest` + `venue_booking_test`） |
| `src/test/.../audit/service/AdminAuditServiceTest.java` | 扩：cancel/reschedule 字段断言、list 过滤、limit 校验 |
| `src/test/.../audit/AdminAuditSnapshotsTest.java` | 既有快照 JSON |
| `ReservationServiceAdminRescheduleTest` | 成功调 `recordReservationReschedule`；冲突不写审计 |
| `ReservationServiceRescheduleTest` | 本人改约 **不** 写审计 |

命令：

```bash
mvn -Dtest=AdminAuditSnapshotsTest,AdminAuditServiceTest,AdminAuditLogMapperIT,ReservationServiceRescheduleTest,ReservationServiceAdminRescheduleTest test
```

结果：全部通过（Errors 0 / Failures 0）。真实库用例对 `venue_booking_test.admin_audit_log` 执行 insert 后 select 断言，事务回滚。

## 覆盖场景

| # | 场景 | 期望 | 结果 | 用例 |
|---|------|------|------|------|
| 1 | ADMIN 取消落库 | +1 `RESERVATION_CANCEL`，operator/资源正确，after=CANCELLED | 通过 | `recordCancel_persistsRowInMysql` + Service mock |
| 2 | ADMIN 改约落库 | +1 `RESERVATION_RESCHEDULE`，前后时段不同 | 通过 | `recordReschedule_persistsBeforeAfterSlots` + AdminReschedule 挂接 verify |
| 3 | 本人改约 | 无审计 | 通过 | `ReservationServiceRescheduleTest` never verify |
| 4 | 按 reservationId / operatorId 查 | 过滤正确 | 通过 | `listFiltersByReservationAndOperator` |
| 5 | 学生调审计查询 | 403 | **配置覆盖** | `@PreAuthorize` + `/api/admin/**` |
| 6 | 业务失败（冲突） | 无审计 | 通过 | AdminReschedule conflict never verify |
| 额外 | reason 恒 null、limit>200 拒 | 通过 | Service 断言 |

## 工程说明

- 为跑 `@MybatisPlusTest`，`pom.xml` **test scope** 增加了 `mybatis-plus-boot-starter-test`（不进运行时）。
- 本机需 MySQL/`venue_booking_test` 且已建 `admin_audit_log`（见 `schema.sql` 增量）。

## 结论

§6.5 真实库落库已覆盖；P0 挂接与本人路径隔离符合设计。@Tech Lead 可汇总交付。
