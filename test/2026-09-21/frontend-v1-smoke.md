# 前端 V1 冒烟报告

- 分支：`feature/frontend-v1`
- 依据：`docs/frontend/v1.md`；401 修复复审 `review/2026-09-21/frontend-v1-r2.md`
- 环境：MySQL + Spring Boot `:8080`（dev）+ Vite `http://127.0.0.1:5174`（5173 已被占用）
- 日期：2026-09-21（Asia/Shanghai）
- **结论：通过**（无 Blocker；有 1 条 Minor 观察）

## 场景与结果

| # | 场景 | 方式 | 结果 | 说明 |
|---|---|---|---|---|
| 1 | `npm run build`（vue-tsc + vite） | CLI | **通过** | Exit 0；仅 chunk >500kB 警告 |
| 2 | 登录 stu01 / admin（密码 123456） | HTTP | **通过** | `POST /api/auth/login` → `code=OK` |
| 3 | 学生调管理端接口 | HTTP | **通过** | stu token → `GET /api/admin/reservations` → **403** |
| 4 | 坏 token → 401 | HTTP | **通过** | `GET /api/auth/me` → **401** `UNAUTHENTICATED` |
| 5 | 预约 / 本人改约 / 管理端改约 / 审计 / 取消 | HTTP | **通过** | 场地 1 明日时段：约→改→管理改→audit 1 条→取消 |
| 6 | 窗口工具与 purpose 语义 | 等价脚本 | **通过** | 对齐 `time.ts` / `withPurpose` |
| 7 | 401 先 `auth.clear()` 再跳登录 | 静态 | **通过** | `http.ts`：`clear()` 先于 `redirectToLogin()` |
| 8 | 非 ADMIN 访问 `/admin/**` 前端拦 | 静态 | **通过** | 守卫回 `/venues`；user views 无 admin API |
| 9 | 学生登录落地 `/venues` | 浏览器 | **通过** | 截图见下 |
| 10 | 学生进 `/admin/reservations` 被拦 | 浏览器 | **通过** | 回 `/venues`，提示无管理权限 |
| 11 | `/me/reservations` 可打开 | 浏览器 | **通过** | 菜单点击未跳转，直链可开（见 Minor） |
| 12 | 管理端登录落地 + 审计页 | 浏览器 | **通过** | `/admin/reservations`；审计有 1 行 |
| 13 | 401 清会话无死循环 | 浏览器 | **通过** | 假 token 后最终 `/login?redirect=...`，无环 |

## 浏览器证据（截图路径）

1. 学生登录 `/venues`：`/workspace/screenshots/shot-call_qxDKXStqbnKbWTLRCu18fi7bfc_03ff3eb3156a4754.png`
2. 学生拦 admin：`/workspace/screenshots/shot-call_SdBnSrHUZ9tAY53aGvoBI1I2fc_03ff3eb3156a4754.png`
3. 我的预约：`/workspace/screenshots/shot-call_Wh8yiZxdifelAAYKI071tXeufc_03ff3eb3156a4754.png`
4. 管理端预约：`/workspace/screenshots/shot-call_PwN5Oa49uNiyCF2pVwyJ4W1Rfc_03ff3eb3156a4754.png`
5. 审计列表：`/workspace/screenshots/shot-call_R0c9ZvJ0UDelwEDPaQkrJS2rfc_03ff3eb3156a4754.png`
6. 401 → 登录页：`/workspace/screenshots/shot-call_fzblcuwoKtoI1m5yuVlyWAhCfc_03ff3eb3156a4754.png`

## Minor（不挡交付）

1. **UserLayout「我的预约」菜单点击未导航**：直链 `/me/reservations` 正常。建议 @Coder 查 `el-menu` `router` / `index` 是否与路由 path 一致（非 Blocker）。
2. **401 时 ElMessage 可能连弹多次**（假 token + reload 场景）：无死循环，会话已清。可考虑限流提示；非 Blocker。

## 给 Tech Lead

- 冒烟 **通过**，报告：`test/2026-09-21/frontend-v1-smoke.md`
- 可 @Docs 补前端 README，再推 `feature/frontend-v1` 开 PR
- Minor 两项可选进后续修，不挡本迭代交付
