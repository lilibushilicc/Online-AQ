# 登录转发错误修复总结

## 1. 改动目标

本次改动用于修复“页面始终登录不了”的真实原因。排查后确认，前端开发环境访问的 `http://localhost:8080` 并不是当前仓库的后端，而是另一个同名项目实例，导致前端请求 `/api/auth/login` 时命中错误服务并返回 `404`，从而表现为无法登录。

## 2. 用户视角

用户现在在本地开发环境中重新打开登录页后，登录请求会直接发送到当前项目的后端地址，不再误打到其他项目服务。

如果本机 `8080` 已经被别的项目占用，也可以通过开发环境变量切换后端地址，不需要再改源码里的接口路径。

## 3. 前端实现

- 页面：
  本次未改动页面布局。
- 组件：
  本次未新增组件。
- 路由：
  本次未修改路由结构。
- 状态管理：
  本次未修改状态管理。
- 接口请求：
  `src/utils/request.ts` 的 `baseURL` 改为优先读取 `VITE_API_BASE_URL`，未配置时仍回退到 `/api`。
- 样式与布局：
  本次未修改样式。

## 4. 后端实现

- Controller：
  本次未修改登录接口实现。
- Service：
  本次未修改业务逻辑。
- Mapper：
  本次未修改。
- Entity / DTO / VO：
  本次未修改。
- 配置：
  本次未改后端代码配置；本次主要修正前端开发环境与本项目后端实例的指向关系。

## 5. 数据库与数据流

本次未修改数据库表结构。

登录数据流从原来的：

1. 浏览器访问前端页面。
2. 前端把 `/api/auth/login` 发送到错误的 `8080` 服务。
3. 错误服务返回 `404`。

调整为：

1. 浏览器访问前端页面。
2. 前端通过 `VITE_API_BASE_URL` 把登录请求直接发到当前项目后端。
3. 当前项目后端返回登录结果和 Token。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/auth/login` | POST | 用户登录 | `username`、`password`、`role` | 成功返回用户信息与 Token |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/utils/request.ts` | 允许前端优先读取开发环境变量中的后端地址 |
| `Oline-AQ-vue/.env.development` | 为本地开发环境指定当前项目后端地址 `http://localhost:8081/api` |
| `README.md` | 补充端口冲突时的开发环境配置说明 |

## 8. 验证方式

```sh
Invoke-RestMethod -Uri http://localhost:8081/api/auth/login -Method Post -ContentType 'application/json; charset=utf-8' -Body (@{ username='admin'; password='123456'; role='admin' } | ConvertTo-Json)
```

验证结果：

- 当前项目后端在 `http://localhost:8081` 上返回登录成功和 Token。
- 前端开发服务已重启并重新加载新的开发环境变量。
- 另一个占用 `8080` 的服务路径为 `D:\study\system\low-carbon-login\Oline-AQ-spring`，确认不是当前仓库实例。

## 9. 风险与后续优化

- 当前 `.env.development` 写死为 `8081`，适合当前本机环境；如果后续端口再次变化，需要同步修改该文件。
- 若希望团队环境更统一，后续建议补充 `.env.example` 或在前端增加更明确的环境区分说明。
- 如果不再需要另一个占用 `8080` 的项目实例，建议手动关闭，避免后续继续混淆。
