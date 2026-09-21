# Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/) 风格。

## [Unreleased]

### Added

- 管理端改约接口 `PUT /api/admin/reservations/{id}`：ADMIN 改任意用户未开始的 `CONFIRMED` 单；开始前均可；复用本人改约锁序与冲突排除本单
- 管理端写操作审计：独立表 `admin_audit_log`；管理端取消/改约同事务追加；`GET /api/admin/audit-logs` 薄查询
- 设计稿 `docs/reservation/admin-reschedule.md`、`docs/reservation/admin-audit.md`；审查 `review/2026-09-14/`；测试报告见 `test/2026-09-14/`

### Changed

- `README.md`、`docs/接口联调.md` 补上管理端改约与审计路径及验收项
- `schema.sql` 增加 `admin_audit_log`（含已有库增量注释）

### Notes

- 审计不记本人路径；场地写操作审计二期；取消/改约不加 `reason` 入参
- test scope 增加 `mybatis-plus-boot-starter-test`（仅测试，未升业务依赖）
- 管理端改约：无新错误码，无依赖升级
- 分支：`feature/admin-audit`

## [1.1.0] — 2026-09-14

已合入 `main`（[PR #1](https://github.com/hanhanzhaizhe/campus-venue-booking/pull/1)，merge commit `52b9842`）。

### Added

- 本人改约接口 `PUT /api/reservations/{id}`：改自己未开始的 `CONFIRMED` 单时段；id / 场地 / 配额不变
- 请求体 `RescheduleReservationRequest`：`date`、`startTime`、`endTime` 必填；`purpose` 可选（未传保留，空白清空）
- 错误码 `RESCHEDULE_NOT_ALLOWED`（400）：窗口外、已开始或状态不可改
- 改约窗口与取消一致：开始前 2 小时，恰好 2h 允许
- 设计稿 `docs/reservation/reschedule.md`；审查记录 `review/2026-09-13/`

### Changed

- `README.md`、`docs/接口联调.md` 补上改约路径、验收项和错误码

### Notes

- 无表结构变更，无依赖升级
- 冲突查询排除本单；相邻半开区间成功
