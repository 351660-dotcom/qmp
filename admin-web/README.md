# 景区文旅 SaaS · 管理后台（admin-web）

Vue3 + Vite + Element Plus + Pinia 的中后台控制台，对接各微服务 `/admin/v1` 接口。

## 运行

```bash
cd admin-web
npm install
npm run dev        # http://localhost:5173
```

> 需先 `docker compose up -d` 起后端各服务（product 8081 / pricing 8082 / inventory 8084 /
> payment 8085 / ticket 8086 / order 8087 / reconciliation 8093）。
> 开发期 Vite 通过 `/svc/*` 前缀代理到各服务端口（见 `vite.config.js`），规避浏览器跨域。

## 登录

v1 用「管理员令牌 + 租户 ID」（对接后端 `AdminAuthFilter` 的 `X-Admin-Token` + `X-Tenant-Id`）。
本地默认令牌 `scenic-admin-dev-token`，租户 `1001`。细粒度 RBAC（ADR-011）后续替换为账号体系。

## 已覆盖模块

- 概览（跨业态日对账查询）
- 商品 / 票种：建品（含**核销规则**：有效期 + 核销介质 二维码/IC卡/人脸；**退改签**：是否支持 + 时间窗口 + 手续费）、上下架、票种管理
- 价格日历：按 票种+日期+类型 维护
- 库存桶：按 票种+日期+场次 维护配额
- 订单：列表 + 详情（含已付/已退/支付截止）
- 商户分账：按商户设抽成比例
- 核销密钥：按景区轮换

## 生产部署待办

- 前端打包后由网关/Nginx 托管；各服务通过**网关统一路由 + CORS/鉴权**（开发期的 Vite proxy 仅本地用）。
- 接入真实账号体系 + RBAC 后替换「令牌登录」。
