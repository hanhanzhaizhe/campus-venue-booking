# 场地预约前端（V1）

本仓 `frontend/`：Vue 3 + TypeScript + Vite + Element Plus + Pinia。对照设计 [docs/frontend/v1.md](../docs/frontend/v1.md)。

## 环境

- Node.js 18+（本机已用 Vite 8 脚手架）
- 后端先起在 `127.0.0.1:8080`（见仓库根 README）

## 怎么跑

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认 **http://127.0.0.1:5173**。`vite.config.ts` 把 `/api` 代理到 `http://127.0.0.1:8080`，所以 `.env.development` 里 `VITE_API_BASE_URL` 为空即可（走同源 `/api`）。

构建：

```bash
npm run build
npm run preview
```

生产环境同样走相对路径 `/api`，需由 Nginx 等把 `/api` 反代到后端；不要把 token 写进环境变量。

## 预置账号

与后端种子一致，密码均为 `123456`：

| 用户名 | 角色 | 登录后落点 |
|---|---|---|
| `stu01` / `teacher01` | 学生 / 教师 | `/venues` |
| `admin` | 管理员 | `/admin/reservations` |

Token 存在 `localStorage` 键 `cvb_token`，请求头 `Authorization: Bearer <token>`。HTTP 401 会 `auth.clear()`（清 Pinia + 本地存储）再跳 `/login`，避免守卫把失效会话踢回业务页。

## 页面与接口

| 路由 | 角色 | 对接 |
|---|---|---|
| `/login` | 公开 | `POST /api/auth/login`、`GET /api/auth/me` |
| `/venues`、`/venues/:id` | 已登录 | `GET /api/venues`、详情与占用、`POST /api/reservations` |
| `/me/reservations` | 学生/教师 | `GET /api/reservations/me`、`PUT /api/reservations/{id}`、`POST /api/reservations/{id}/cancel` |
| `/admin/reservations` | ADMIN | `GET /api/admin/reservations`、`PUT /api/admin/reservations/{id}`、`POST /api/admin/reservations/{id}/cancel` |
| `/admin/audit-logs` | ADMIN | `GET /api/admin/audit-logs` |

非 ADMIN 进 `/admin/**` 会被路由守卫拦到 `/venues`。本人改约窗口开始前 2h；管理端改约开始前均可。`purpose` 按设计简化：未填则不传，填了 trim 后提交。场地 CRUD UI、签到为本期不做。

## 目录

```text
frontend/src
  api/          http.ts + 各资源客户端
  stores/auth.ts
  router/index.ts     history 模式
  views/login|user|admin
  layouts/UserLayout.vue、AdminLayout.vue
  components/OccupancyGrid、RescheduleDialog、CancelConfirm
```

冒烟报告：`test/2026-09-21/frontend-v1-smoke.md`。审查：`review/2026-09-21/`。
