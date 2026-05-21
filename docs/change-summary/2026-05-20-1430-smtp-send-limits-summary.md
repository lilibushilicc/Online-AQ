# SMTP 发送限制与单账号统计改动总结

## 1. 改动目标

- 管理员需要看到每个 SMTP 账号的已发送数量（本小时/本日）
- 可自由配置每小时/每日发送上限
- 达到阈值时自动停止发送并提示切换账号

## 2. 用户视角

- 管理后台 → 邮箱注册设置 → SMTP 账号列表新增"发送限制"列
- 每行显示 `已发 / 上限`（本小时 + 本日），数字超过上限时显示红色警告
- 点击"编辑"后可在底部设置"每小时上限"和"每日上限"（0 或留空表示不限制）
- 新增账号时默认为 100 封/小时、1000 封/天
- 达到上限时注册页面发送验证码会报错"当前账号已达到 xx 上限"，引导管理员切换账号或等待重置
- 每小时/每日计数器在服务启动后自动按时间窗口重置（不依赖定时任务）

## 3. 前端实现

- **SmtpAccountData 类型**：新增 `hourlyLimit`、`dailyLimit`、`hourlySent`、`dailySent` 字段
- **el-table 列**：新增"发送限制"列，用 `hourlySent / hourlyLimit` 和 `dailySent / dailyLimit` 展示，超限时字体变红
- **对话框**：新增"每小时上限"和"每日上限"两个 `el-input-number` 字段，最小值为 0
- **editingAccount 初始值**：包含 `hourlyLimit: 100, dailyLimit: 1000`

## 4. 后端实现

- **SmtpAccount 实体**：新增 `hourlyLimit`、`dailyLimit`、`hourlySent`、`dailySent`、`lastHourlyReset`、`lastDailyReset` 六个字段
- **EmailSendLog 实体**：新增 `accountId` 字段
- **EmailService**：
  - `checkAndResetLimits(account)` — 检查并重置小时/日计数器（按时间窗口判断，无需定时器）
  - `sendVerificationCode` — 发送前检查 `hourlySent >= hourlyLimit` 和 `dailySent >= dailyLimit`，超限直接抛异常
  - `incrementCounts(account)` — 发送成功后自增计数器并写库
  - `logSend` — 记录 `accountId`，关联到对应账号
  - `saveAccount` — 设置默认 `hourlyLimit=100, dailyLimit=1000, hourlySent=0, dailySent=0`
  - `updateAccount` — 支持更新 `hourlyLimit/dailyLimit`
  - `getAccountStats(id)` — 从 `email_send_log` 查询指定账号本小时/本日发送量
- **ConfigController**：`GET /api/config/smtp-accounts/{id}/stats` 返回实时统计 + 限制值
- **SchemaInitializer**：
  - `smtp_account` 新增列：`hourly_limit`、`daily_limit`、`hourly_sent`、`daily_sent`、`last_hourly_reset`、`last_daily_reset`
  - `email_send_log` 新增列 `account_id`
  - 创建 `idx_email_send_log_account` 索引

## 5. 数据库与数据流

- `smtp_account` 表新增列：
  - `hourly_limit INTEGER DEFAULT 100`
  - `daily_limit INTEGER DEFAULT 1000`
  - `hourly_sent INTEGER DEFAULT 0`
  - `daily_sent INTEGER DEFAULT 0`
  - `last_hourly_reset TIMESTAMP`
  - `last_daily_reset TIMESTAMP`
- `email_send_log` 表新增列：
  - `account_id INTEGER` — 关联 `smtp_account.id`
- 限速数据流：
  1. 发送前读取 account 当前 `hourlySent/dailySent` 及 `lastHourlyReset/lastDailyReset`
  2. 如果重置时间超过 1 小时/1 天 → 重置对应计数器为 0
  3. 如果 `hourlySent >= hourlyLimit` 且 limit > 0 → 抛异常
  4. 如果 `dailySent >= dailyLimit` 且 limit > 0 → 抛异常
  5. 发送成功后 `hourlySent++`、`dailySent++`，更新数据库

## 6. 接口说明

| 接口 | 方法 | 作用 | 返回结果 |
|---|---|---|---|
| `/api/config/smtp-accounts/{id}/stats` | GET | 查询单账号发送统计+限制 | `{ hourlySend, dailySend, hourlyLimit, dailyLimit }` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `.../entity/SmtpAccount.java` | 新增 limit/sent/reset 六个字段 |
| `.../entity/EmailSendLog.java` | 新增 accountId 字段 |
| `.../config/SchemaInitializer.java` | 新增列 ALTER TABLE + account_id 索引 |
| `.../service/EmailService.java` | 限速检查 + 计数器自增 + 重置 + 单账号统计 |
| `.../controller/ConfigController.java` | 新增 GET /smtp-accounts/{id}/stats |
| `.../api/index.ts` | SmtpAccountData 新增限制字段 |
| `.../AdminConfig.vue` | 发送限制列 + 对话框编辑上限 |

## 8. 验证方式

```sh
cd Oline-AQ-spring && .\mvnw.cmd compile -q
cd Oline-AQ-vue && npx vue-tsc --noEmit && npx vite build
```

编译通过，构建成功。

## 9. 风险与后续优化

- **计数器重置机制**：基于 `last_HourlyReset` / `last_DailyReset` 判断，服务重启后计数器归零重新开始，但已发送日志仍在 `email_send_log` 中可查。
- **limit = 0/null**：表示不限制，跳过检查逻辑。
- **后续优化**：可添加定时任务（@Scheduled）更精确地每小时/每天零点重置计数器，而非用惰性检查；可添加自动切换账号链路（当一个账号达到上限时自动尝试下一个可用账号）。
