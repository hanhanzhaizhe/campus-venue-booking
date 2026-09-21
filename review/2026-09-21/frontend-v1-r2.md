# 复审报告：前端 V1（feature/frontend-v1）r2

- 日期：2026-09-21
- 对照：`review/2026-09-21/frontend-v1.md` Blocker #1
- 设计：`docs/frontend/v1.md`
- 结论：**通过**

## Blocker 复查

原问题：401 只清 localStorage，Pinia 仍认为已登录。

现实现（`http.ts:46-53`）：401 时动态 import `useAuthStore().clear()`（清 state + storage），再 `redirectToLogin()`。守卫不会再把失效会话从 `/login` 踢回业务页。

**Blocker 关闭。**

## 结论

无新增 Blocker / Major。Minor（共享 Dialog 静态 import admin API）不挡交付。

**Approved** — 可交 @QA。
