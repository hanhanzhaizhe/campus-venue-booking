# 审查报告：前端 V1（feature/frontend-v1）

- 日期：2026-09-21
- 设计：`docs/frontend/v1.md`
- 分支：`feature/frontend-v1`
- 结论：**不通过**（1 Blocker）

---

## [Blocker]

### 1. 401 只清 localStorage，未清 Pinia，可能踢不进登录页

- 位置：`frontend/src/api/http.ts:46-51`
- 原因：响应 401 时只 `removeItem(cvb_token/cvb_user)`，`useAuthStore` 里 `token`/`role` 仍在。`router.beforeEach` 用 `auth.isLoggedIn`（看 Pinia），且访问 `/login` 时若已登录会再踢回 `/venues` 或 `/admin/**`。结果：token 已失效 → 401 → 跳登录 → 守卫认为仍登录 → 又进业务页 → 再 401，形成循环或卡死。设计 §3 / §8.7 要求失效跳登录。
- 修法：401 分支调用 `useAuthStore().clear()`（或等价清空 state + storage），再 `redirectToLogin`；注意避免与 `http` 循环依赖（可动态 import store，或抽 `clearSession()`）。

---

## [Major]

无。

---

## [Minor]

### 1. `RescheduleDialog` 静态 import 了 `adminReschedule`

- 位置：`RescheduleDialog.vue:6`
- 原因：学生端也会加载该 chunk 里的 admin API 符号（虽 mode=user 不会调用）。设计禁的是「写死调用」；当前用法可接受。若要更干净，可按 mode 动态 import。

---

## [Approved]

- 目录与栈对齐 §2/§7：Vite + Pinia + history + Element Plus + axios；代理 `/api` → 8080。
- 路由守卫：无 token 带 redirect；非 ADMIN 拦 `/admin/**`；登录后 ADMIN 默认管理端。
- 启动 `fetchMe` 失败清会话（`main.ts`）。
- `withPurpose` 符合 purpose 简化语义；本人/管理端窗口文案与按钮禁用（`canUserReschedule` / `canAdminReschedule`）。
- 用户页不直接调 `/api/admin/**`；审计 JSON 字符串 parse 失败原样展示。
- `cvb_token` + Bearer 拦截器正确。

打回 @Coder 修 Blocker 后再送审。
