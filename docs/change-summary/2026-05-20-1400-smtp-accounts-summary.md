# 多 SMTP 账号管理改动总结

## 1. 改动目标

之前的 SMTP 配置存储在 `sys_config` 的 K-V 条目中（`smtp.host`、`smtp.port`、`smtp.username`、`smtp.password`、`smtp.from`），只能配置一套 SMTP 账号。免费邮箱服务商（如 QQ、163、Gmail）通常有每日发送上限，单账号容易超限。本次改动将 SMTP 配置从 `sys_config` 独立为 `smtp_account` 表，支持多账号管理、动态切换和单个账号测试。

## 2. 用户视角

- 管理后台 → 邮箱注册设置页面：旧的 SMTP 配置表单（服务器、账号、密码、发件人）被替换为 SMTP 账号列表
- 列表展示每个账号的发件人地址、SMTP 服务器、激活状态
- 可添加/编辑/删除账号，编辑时可保留密码不修改
- 点击"切换为此账号"可将目标账号设为当前激活账号（全局唯一 active）
- 每个账号可单独"测试"连接（调用后端真实 SMTP 握手）
- 发送统计行保持不变
- 邮件内容自定义区域（系统名称、标题、模板）保持不变，与 SMTP 账号解耦

## 3. 前端实现

- **状态变量**：`accounts`（账号列表）、`testingEmailId`（测试中账号 ID）、`dialogVisible`（弹窗）、`editingAccount`（表单绑定）、`isEditing`（区分新增/编辑）
- **方法**：`loadAccounts`、`openAddDialog`、`openEditDialog`、`saveAccount`、`deleteAccount`、`activateAccount`、`testAccountConnection`
- **组件**：`el-table` 展示账号列表，`el-dialog` 包含表单（host/port/username/password/fromAddress）
- **路由**：不变，仍在 `/admin/config/email`
- **API**：新增 `loadSmtpAccountsApi`、`createSmtpAccountApi`、`updateSmtpAccountApi`、`deleteSmtpAccountApi`、`activateSmtpAccountApi`

## 4. 后端实现

- **Controller（ConfigController）**：
  - `POST /api/config/test-email` — 接受可选请求体 `{ "accountId": number }`，无 ID 时测试当前激活账号
  - `GET /api/config/smtp-accounts` — 查询所有账号
  - `POST /api/config/smtp-accounts` — 新增账号
  - `PUT /api/config/smtp-accounts/{id}` — 更新账号
  - `DELETE /api/config/smtp-accounts/{id}` — 删除账号
  - `PUT /api/config/smtp-accounts/{id}/activate` — 切换激活账号
- **Service（EmailService）**：已有 `listAccounts`、`saveAccount`、`updateAccount`、`deleteAccount`、`activateAccount`、`testConnection(Integer accountId)`

## 5. 数据库与数据流

- `smtp_account` 表：id（自增主键）、host、port、username、password（加密存储）、from_address、active（布尔，全局唯一 true）、create_time、update_time
- 激活逻辑：`activateAccount` 先 `UPDATE smtp_account SET active = false`（全量设为 false），再 `UPDATE smtp_account SET active = true WHERE id = ?`
- 数据流：前端表单 → API → Service → Mapper → `smtp_account` 表
- 发送邮件时 `EmailService` 读取 `active = true` 的账号配置

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/config/test-email` | POST | 测试一个 SMTP 账号连接 | `{ "accountId": number }`（可选） | `{ "result": "ok" }` |
| `/api/config/smtp-accounts` | GET | 查询所有 SMTP 账号 | 无 | SmtpAccount 数组 |
| `/api/config/smtp-accounts` | POST | 新增 SMTP 账号 | SmtpAccount 对象 | 成功消息 |
| `/api/config/smtp-accounts/{id}` | PUT | 更新 SMTP 账号 | SmtpAccount 对象 | 成功消息 |
| `/api/config/smtp-accounts/{id}` | DELETE | 删除 SMTP 账号 | 无 | 成功消息 |
| `/api/config/smtp-accounts/{id}/activate` | PUT | 切换激活账号 | 无 | 成功消息 |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../controller/ConfigController.java` | SMTP 账号 CRUD + test-email 端点和路由 |
| `Oline-AQ-spring/.../service/EmailService.java` | 多账号管理核心逻辑、`testConnection(Integer)` 支持指定账号 |
| `Oline-AQ-vue/src/views/admin/AdminConfig.vue` | 邮箱 tab 重写：账号列表 + 弹窗表单 + 测试/切换/删除 |
| `Oline-AQ-vue/src/api/index.ts` | 新增 SmtpAccountData 类型和 CRUD API 函数 |
| `Oline-AQ-vue/src/stores/exam.ts` | 新增 SMTP 账号相关 action |
| `Oline-AQ-spring/.../entity/SmtpAccount.java` | 多账号实体（已有） |
| `Oline-AQ-spring/.../mapper/SmtpAccountMapper.java` | 多账号 Mapper（已有） |
| `Oline-AQ-spring/.../config/SchemaInitializer.java` | smtp_account 建表 + 旧配置迁移（已有） |

## 8. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring && .\mvnw.cmd compile -q

# 前端类型检查
cd Oline-AQ-vue && npx vue-tsc --noEmit

# 前端构建
cd Oline-AQ-vue && npx vite build
```

确认编译无错误、构建成功。

## 9. 风险与后续优化

- **编辑时密码处理**：编辑账号时，前端弹出密码字段为空（留空表示不修改），后端 `updateAccount` 需判断密码非空时才更新。当前实现中，Edit 操作传递的 payload 仅在有密码值时包含 `password` 字段。
- **密码存储**：当前为明文存储，后续可考虑加密（如 Spring Cloud Config 的 encrypt/decrypt）或使用 AOP 加解密。
- **后续可优化**：在邮箱注册发送邮件时自动按轮询或最低发送量切换可用账号（智能路由），而非手动切换。
