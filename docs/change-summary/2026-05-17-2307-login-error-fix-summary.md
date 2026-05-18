# 登录错误修复总结

## 1. 改动目标

本次改动用于修复登录链路中的错误处理问题。此前后端在登录失败或鉴权失败时会返回业务失败数据，但前端没有统一识别非 `200` 业务码，导致失败响应被当作成功结果继续处理，进而在读取 `data.token` 等字段时出现前端报错，表现为“登录异常”而不是明确的账号或鉴权提示。

## 2. 用户视角

用户现在在以下场景会得到稳定且明确的反馈：

- 账号、密码或身份不正确时，登录页会直接提示后端返回的错误信息，不再出现前端空数据报错。
- Token 缺失或失效后，请求会收到 `401`，前端会清理本地登录态并跳回登录页。
- 登录成功时，仍然按原流程跳转到教师端或学生端页面。

## 3. 前端实现

- 页面：
  登录页在提交失败时，不再写死错误文案，而是优先展示真实异常消息。
- 组件：
  `LoginView.vue` 的 `submit` 捕获逻辑改为读取 `Error.message`。
- 路由：
  本次未修改登录后跳转规则。
- 状态管理：
  本次未修改 `Pinia` 登录态结构。
- 接口请求：
  `src/utils/request.ts` 新增统一的 `ApiResponse` 业务码判断。当后端返回 `code !== 200` 时，前端会主动抛出异常，阻止错误数据继续进入页面逻辑。
- 样式与布局：
  本次未修改样式。

## 4. 后端实现

- Controller：
  本次未改动登录接口路径与返回结构。
- Service：
  `AuthService` 在账号、密码或身份校验失败时，改为抛出 `UnauthorizedException`。
- Mapper：
  本次未改动。
- Entity / DTO / VO：
  新增 `UnauthorizedException`，用于区分未登录/登录失败与普通业务异常。
- 配置：
  `GlobalExceptionHandler` 新增未授权异常处理并返回 HTTP `401`；普通 `RuntimeException` 改为返回 HTTP `400`。
  `JwtInterceptor` 在缺少 `Authorization` 或 Bearer Token 时改为抛出未授权异常。

## 5. 数据库与数据流

本次未修改表结构和字段。

登录数据流调整为：

1. 前端提交 `username`、`password`、`role` 到 `POST /api/auth/login`。
2. 后端校验账号身份。
3. 校验成功则返回用户信息和 JWT。
4. 校验失败则返回 `401` 与错误信息。
5. 前端请求层识别业务失败或 `401`，停止后续写入 Token 的流程，并向页面抛出明确错误。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/auth/login` | POST | 用户登录 | `username`、`password`、`role` | 成功返回用户信息与 `token`，失败返回错误信息 |
| `/api/**` | 多种 | 鉴权接口访问 | 请求头 `Authorization: Bearer token` | Token 缺失或无效时返回 `401` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/utils/request.ts` | 统一拦截后端业务失败响应，避免失败结果被当作成功结果使用 |
| `Oline-AQ-vue/src/views/login/LoginView.vue` | 登录失败时展示真实错误消息 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/exception/UnauthorizedException.java` | 定义未授权异常类型 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/AuthService.java` | 登录失败时抛出未授权异常 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/config/JwtInterceptor.java` | Token 缺失时抛出未授权异常 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/config/GlobalExceptionHandler.java` | 将未授权异常映射为 `401`，普通运行时异常映射为 `400` |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm run build

cd ../Oline-AQ-spring
.\mvnw.cmd -q -DskipTests compile
```

验证结果：

- 前端 `npm run build` 通过。
- 后端 `.\mvnw.cmd -q -DskipTests compile` 通过。
- 由于当前未在本次任务中实际启动数据库与完整服务链路，未执行真实登录接口联调。

## 9. 风险与后续优化

- 当前后端仍有较多业务校验直接使用 `RuntimeException`，虽然前端已能正确识别失败响应，但后续更适合继续拆分为更明确的业务异常类型。
- 本次只做了编译级验证，建议后续补充登录成功、密码错误、角色错误、Token 失效四类接口联调或自动化测试。
- 如果后续要统一错误规范，建议将错误码、HTTP 状态码、前端提示文案做成明确的接口约定文档。
