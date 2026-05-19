# 批量改进总结

## 1. 改动目标

在已有功能基础上，从核心体验、代码质量、运维三个维度进行系统性改进，提升系统可靠性、用户体验和可维护性。

## 2. 用户视角

- **考试作答草稿自动保存**：答题过程中答案每隔 800ms 自动保存到浏览器本地存储，意外刷新、关闭标签页后重新进入考试时可恢复上次作答，并提示"已恢复上次作答草稿"。提交成功后自动清除草稿。
- **倒计时防篡改**：剩余时间基于服务端返回的时间戳计算，修改本地时钟不再影响倒计时显示。
- **页面加载状态**：考试列表、成绩列表、管理后台等页面首次加载时显示 `v-loading` 遮罩，避免白屏等待。
- **运维无感知**：启动方式不变，默认使用 dev 配置。

## 3. 前端实现

- **自动保存草稿**（`StudentExamDetail.vue`）：
  - 新增 `DRAFT_KEY_PREFIX` 常量，以 `exam_draft_{examId}` 为 key 存入 `localStorage`
  - `watch(answers, ..., { deep: true })` 配合 800ms 防抖定时器自动保存
  - `loadDraft()` 在 `onMounted` 中加载试卷后从 `localStorage` 恢复答案
  - `clearDraft()` 在提交成功后清除草稿
  - `beforeunload` 事件同步保存一次，防止关闭页面时丢失最后修改
  - 提交按钮上方增加"草稿已自动保存"文字提示
- **服务端时间校准**（`StudentExamDetail.vue`）：
  - 记录 `serverTime`（后端返回的 `System.currentTimeMillis()`）
  - `resolveRemainingSeconds` 中计算 `serverOffset = Date.now() - serverTime`，用于校准 `endTime` 计算
  - 倒计时不再直接依赖客户端 `Date.now()`
- **页面 Loading 状态**（`StudentExams.vue`, `StudentResults.vue`, `AdminDashboard.vue`, `AdminQuestions.vue`）：
  - 每个页面新增 `loading` ref 变量
  - `onMounted` 中 `try...finally` 确保 `loading = false`
  - 根元素添加 `v-loading="loading"`

## 4. 后端实现

- **MyBatis-Plus 分页**（`QuestionService.java`）：
  - `list()` 方法改用 `questionMapper.selectPage(new Page<>(page, pageSize), wrapper)`
  - 删除手动拼接的 `"LIMIT " + pageSize + " OFFSET " + offset`
- **分页插件配置**（`MybatisPlusConfig.java`，新增）：
  - 注册 `MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.POSTGRE_SQL)`
- **服务端时间戳**（`ExamService.java`）：
  - `detail()` 返回的 Map 增加 `data.put("serverTime", System.currentTimeMillis())`

## 5. 运维实现

- **Profile 配置拆分**：
  - `application.yml`：仅保留通用配置（端口、数据源驱动、multipart、mybatis-plus），激活 `dev` profile
  - `application-dev.yml`：开发环境默认值（DB 密码、JWT 密钥含演示默认值），DEBUG 日志
  - `application-prod.yml`：生产环境**无默认密钥和密码**（必须通过环境变量注入），WARN 日志
- **Vue DevTools 条件启用**（`vite.config.ts`）：
  - `process.env.NODE_ENV === 'development' && vueDevTools()`，非开发构建自动排除
- **新增 `.env.production`**：配置 `VITE_API_BASE_URL=/api`

## 6. 数据库视角

无数据库变更。

## 7. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/exams/{examId}` | GET | 考试详情（新增 serverTime） | `examId` | 原返回值 + `serverTime` 字段 |

## 8. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | 草稿自动保存 + 服务端时间校准 |
| `Oline-AQ-vue/src/views/student/StudentExams.vue` | 新增 `v-loading` 加载态 |
| `Oline-AQ-vue/src/views/student/StudentResults.vue` | 新增 `v-loading` 加载态 |
| `Oline-AQ-vue/src/views/admin/AdminQuestions.vue` | 新增 `v-loading` 加载态 |
| `Oline-AQ-vue/src/views/admin/AdminDashboard.vue` | 新增 `v-loading` 加载态 |
| `Oline-AQ-vue/vite.config.ts` | Vue DevTools 条件启用 |
| `Oline-AQ-vue/.env.production` | 新增生产环境构建变量 |
| `Oline-AQ-spring/src/main/resources/application.yml` | 通用配置 + `spring.profiles.active: dev` |
| `Oline-AQ-spring/src/main/resources/application-dev.yml` | 新建开发环境配置 |
| `Oline-AQ-spring/src/main/resources/application-prod.yml` | 新建生产环境配置（无默认密钥） |
| `Oline-AQ-spring/.../service/QuestionService.java` | MyBatis-Plus Page 替代手动 SQL |
| `Oline-AQ-spring/.../config/MybatisPlusConfig.java` | 新增分页插件 |
| `Oline-AQ-spring/.../service/ExamService.java` | `detail()` 返回 `serverTime` |

## 9. 验证方式

```sh
cd Oline-AQ-vue && npm run build
cd ../Oline-AQ-spring && mvn clean compile
```

验证内容：
1. 考试作答页：选择答案后刷新页面，应恢复已选答案并显示"已恢复上次作答草稿"提示
2. 提交后草稿自动清除，再次进入同一考试无恢复提示
3. 倒计时基于服务端时间显示
4. 所有页面加载时显示 `v-loading` 遮罩
5. `npm run build` 构建产物不包含 Vue DevTools
6. 后端以 `--spring.profiles.active=prod` 启动时，未设置环境变量应报错而不是使用默认值

## 10. 风险与后续优化

- **草稿存储限制**：`localStorage` 容量约 5MB，极端大文本作答（如多篇千字简答）可能溢出。后续可在接近上限时压缩或提示用户。
- **服务端时间校准仅一次**：`serverTime` 在考试加载时采样，未考虑长时间考试过程中客户端与服务器时钟漂移。后续可在倒计时中定期同步 `NTP` 或通过心跳接口校准。
- **生产配置无默认值**：`application-prod.yml` 移除了所有演示默认值，部署时需确保环境变量 `DB_URL`、`DB_USER`、`DB_PASSWORD`、`JWT_SECRET` 已正确设置，否则启动失败。
- **`Map<String, Object>` 返回**：`ExamService.detail()`、`ResultService.submit()`、`ResultService.resultDetail()` 等仍使用无类型 Map 返回。后续可抽取为明确 VO 类以获得编译时类型安全。
