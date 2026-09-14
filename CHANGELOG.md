# Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/) 风格。

## [Unreleased] — 2026-09-13

分支：`feature/reschedule`（未合 main）

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
