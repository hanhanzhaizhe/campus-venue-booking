# Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/) 风格。

## [Unreleased]

### Added

- 管理端改约接口 `PUT /api/admin/reservations/{id}`：ADMIN 改任意用户未开始的 `CONFIRMED` 单；开始前均可；复用本人改约锁序与冲突排除本单
- 设计稿 `docs/reservation/admin-reschedule.md`；审查 `review/2026-09-14/`；测试报告 `test/2026-09-14/admin-reschedule.md`

### Changed

- `README.md`、`docs/接口联调.md` 补上管理端改约路径与验收项

### Notes

- 无表结构变更，无新错误码，无依赖升级
- 分支：`feature/admin-reschedule`（未合 main）

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
