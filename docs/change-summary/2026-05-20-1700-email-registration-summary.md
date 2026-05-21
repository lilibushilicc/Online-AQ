# 邮箱学生注册功能改动总结

## 1. 改动目标

支持学生通过邮箱自行注册账号，管理员可在后台配置页开启/关闭邮箱注册并设置 SMTP 参数，无需教师手动创建学生账号。

## 2. 用户视角

### 学生
- 登录页底部新增 **"没有账号？邮箱注册"** 链接
- 点击后切换到注册表单，填写：真实姓名、邮箱、验证码、密码、确认密码
- 点击"获取验证码"，系统会向邮箱发送 6 位数字验证码（5 分钟内有效）
- 填写完整后点击"注册"，注册成功自动登录并跳转到学生端

### 教师
- 系统配置页新增 **"邮箱注册设置"** 卡片
- 可开启/关闭邮箱注册开关（默认关闭）
- 填写 SMTP 服务器信息：主机、端口、账号、密码、发件人地址
- 支持"保存并测试 SMTP 连接"按钮验证配置是否正确

## 3. 前端实现

### 页面
- **LoginView.vue**：新增 `showRegister` 状态控制登录/注册表单切换；新增 `regForm` 响应式对象、`sendCode` 和 `doRegister` 方法；60 秒验证码倒计时
- **AdminConfig.vue**：新增"邮箱注册设置"卡片，含注册开关（el-switch）、SMTP 配置表单、测试连接按钮

### API 新增（`api/index.ts`）
- `sendCodeApi(email)` → POST `/api/auth/register/send-code`
- `registerApi(email, code, password, realName)` → POST `/api/auth/register`
- `testEmailApi()` → POST `/api/config/test-email`

### Store 新增（`stores/exam.ts`）
- `sendCode(email)`：发送验证码
- `register(email, code, password, realName)`：注册并自动登录（存 token/user 到 localStorage）
- `testEmail()`：测试 SMTP 连接

## 4. 后端实现

### 新增文件

| 文件 | 说明 |
|---|---|
| `service/EmailService.java` | 邮件发送核心服务：生成 6 位验证码、发送 HTML 邮件、保存/校验验证码、创建 SMTP 连接、测试连接 |
| `controller/RegisterController.java` | 注册相关两个接口：`/api/auth/register/send-code` 和 `/api/auth/register` |
| `entity/EmailVerification.java` | 邮箱验证码实体 |
| `mapper/EmailVerificationMapper.java` | 验证码 Mapper |
| `dto/SendCodeRequest.java` | 发送验证码请求 DTO（含 @Email 校验） |
| `dto/RegisterRequest.java` | 注册请求 DTO（含 email/code/password/realName 校验） |

### 修改文件

| 文件 | 改动 |
|---|---|
| `pom.xml` | 新增 `spring-boot-starter-mail` 依赖 |
| `config/SchemaInitializer.java` | 新增 `sys_user` 表 `email` 列、`email_verification` 表、SMTP 和注册配置默认值 |
| `entity/SysUser.java` | 新增 `email` 字段 |
| `config/WebConfig.java` | `/api/auth/register/**` 排除 JWT 拦截 |
| `controller/ConfigController.java` | 注入 `EmailService`，新增 `POST /api/config/test-email` 测试 SMTP 连接 |

### 注册流程

```
POST /api/auth/register/send-code
  1. 检查 register.email.enabled 是否为 true
  2. 检查邮箱是否已被注册
  3. 生成 6 位随机码
  4. 写入 email_verification 表（有效期 5 分钟）
  5. 发送 HTML 邮件到用户邮箱

POST /api/auth/register
  1. 检查 register.email.enabled 是否为 true
  2. 检查邮箱是否已被注册
  3. 校验验证码（匹配邮箱+code+未使用+未过期）
  4. 标记验证码已使用
  5. 生成 username（邮箱前缀 + _ + 时间戳后 4 位）
  6. BCrypt 加密密码
  7. 插入 sys_user（role=student, email=邮箱）
  8. 生成 JWT Token，返回用户信息
```

## 5. 数据库与数据流

### sys_user 表变更
```
新增：email VARCHAR(255) UNIQUE（可空）
```

### 新增 email_verification 表
| 字段 | 类型 | 说明 |
|---|---|---|
| id | SERIAL PK | 自增 |
| email | VARCHAR(255) NOT NULL | 邮箱 |
| code | VARCHAR(6) NOT NULL | 6 位验证码 |
| expire_time | TIMESTAMP NOT NULL | 过期时间 |
| used | BOOLEAN DEFAULT FALSE | 是否已使用 |
| create_time | TIMESTAMP | 创建时间 |

### sys_config 新增配置项

| Key | 默认值 | 说明 |
|---|---|---|
| `register.email.enabled` | `false` | 开启邮箱注册 |
| `smtp.host` | 空 | SMTP 服务器 |
| `smtp.port` | `587` | SMTP 端口 |
| `smtp.username` | 空 | SMTP 账号 |
| `smtp.password` | 空 | SMTP 密码 |
| `smtp.from` | 空 | 发件人地址 |

### 数据流
```
前端注册表单 → sendCode: POST /api/auth/register/send-code
  → 后端校验 → 生成验证码 → 存入 email_verification → 发送邮件
前端注册表单 → register: POST /api/auth/register
  → 后端校验验证码 → BCrypt 加密密码 → 插入 sys_user → 生成 JWT → 返回前端
前端自动保存 token/user → localStorage → 路由跳转学生端
```

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回 |
|---|---|---|---|---|
| `/api/auth/register/send-code` | POST | 发送验证码 | `{email}` | - |
| `/api/auth/register` | POST | 邮箱注册 | `{email, code, password, realName}` | token + 用户信息 |
| `/api/config/test-email` | POST | 测试 SMTP 连接 | - | `{result: "ok"}` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/pom.xml` | 加 mail 依赖 |
| `.../service/EmailService.java` | 邮件发送核心 |
| `.../controller/RegisterController.java` | 注册接口 |
| `.../controller/ConfigController.java` | 加 test-email |
| `.../config/SchemaInitializer.java` | 数据库自动变更 |
| `.../entity/SysUser.java` | 加 email 字段 |
| `.../entity/EmailVerification.java` | 验证码实体 |
| `.../mapper/EmailVerificationMapper.java` | 验证码 Mapper |
| `.../dto/SendCodeRequest.java` | 发送验证码 DTO |
| `.../dto/RegisterRequest.java` | 注册 DTO |
| `.../config/WebConfig.java` | 排除注册路径 |
| `Oline-AQ-vue/src/api/index.ts` | 加注册 API |
| `Oline-AQ-vue/src/stores/exam.ts` | 加注册/测试 action |
| `Oline-AQ-vue/src/views/login/LoginView.vue` | 注册表单 |
| `Oline-AQ-vue/src/views/admin/AdminConfig.vue` | 邮箱配置卡片 |

## 8. 验证方式

```bash
# 后端编译
cd Oline-AQ-spring && ./mvnw compile -q

# 前端构建
cd Oline-AQ-vue && npx vite build

# 启动后端
cd Oline-AQ-spring && ./mvnw spring-boot:run

# 登录后台配置 SMTP 并开启邮箱注册
# 打开 http://localhost:5173/login 使用邮箱注册
```

验证步骤：
1. 启动后端和前端
2. 用管理员账号登录，进入系统配置页面
3. 填写 SMTP 配置（如 QQ 邮箱的 SMTP），点击保存并测试连接
4. 开启"邮箱注册"开关并保存
5. 退出登录，在登录页点击"没有账号？邮箱注册"
6. 输入邮箱，获取验证码，填写密码和姓名完成注册
7. 注册成功后自动跳转到学生端

## 9. 风险与后续优化

- **风险**：SMTP 密码以明文存储在 `sys_config` 表中，后续可考虑 AES 加密存储
- **风险**：验证码发送无频率限制，后续可加入 IP/邮箱维度的发送间隔限制（如 60 秒内只能发一次）
- **后续**：可支持邮箱登录（通过 email 查找用户）
- **后续**：可支持密码找回功能（通过邮箱重置密码）
- **后续**：可限制同一 IP 注册频率，防止恶意注册
