# 代码优化改动总结

## 1. 改动目标

系统性减少前后端代码重复冗余，消除重复模式、抽取共享工具方法，同时修复因类型推断暴露的预存类型错误。

## 2. 用户视角

无视觉变化。所有改动为代码层面的重构和优化，用户操作流程和界面表现完全一致。

## 3. 后端实现

### 3.1 WrongNotebookService — 抽取 `checkOwner()` 消除 5 处重复 + N+1 优化

- **问题**：`updateNotebook`、`deleteNotebook`、`addItem`、`removeItem`、`getNotebookDetail` 中均有完全相同的"错题本不存在"+"无权操作"检查，共约 40 行重复代码。
- **方案**：抽取 `checkOwner(notebookId, studentId)` 方法返回 `WrongNotebook` 实体，各方法首行调用此方法即可。
- **N+1 优化**：`getNotebookItemCounts` 原使用 for 循环逐条 `selectCount`，改为一次 `selectList` + `groupingBy` 聚合。
- **文件**：`Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/WrongNotebookService.java`

### 3.2 FeedbackService — 合并 resolve/reject

- **问题**：`resolve()` 和 `reject()` 高度相似，仅 status 和 resolveType 不同。
- **方案**：抽取 `updateFeedbackStatus(id, status, resolveType, rejectReason)`，同时抽取 `findFeedback(id)` 统一"反馈不存在"检查。
- **文件**：`FeedbackService.java`

### 3.3 ExamService — shuffleQuestionOptions 用 Spring BeanUtils

- **问题**：手动复制 7 个字段到新 Question 对象，字段增减易遗漏。
- **方案**：使用 `org.springframework.beans.BeanUtils.copyProperties(original, q, "correctAnswer")`。
- **文件**：`ExamService.java`

### 3.4 QuestionService — 抽取 buildOrderedWrapper

- **问题**：`listAll()` 和 `list()` 两处构造相同的 `LambdaQueryWrapper`。
- **方案**：抽取 `buildOrderedWrapper(category)` 私有方法。
- **文件**：`QuestionService.java`

### 3.5 RegisterController — 抽取 validateEmailRegistration

- **问题**：`sendCode()` 和 `register()` 方法均检查 `register.email.enabled` 和邮箱唯一性。
- **方案**：抽取 `validateEmailRegistration(email)` 抛 `RuntimeException`（由全局异常处理器统一转换）。
- **文件**：`RegisterController.java`

### 3.6 FileService — 魔法字符串常量抽取

- **问题**：`"parsed"` 和 `"r2://"` 魔法字符串散落各处。
- **方案**：抽取 `STATUS_PARSED` 和 `R2_PREFIX` 静态常量。
- **文件**：`FileService.java`

## 4. 前端实现

### 4.1 Store 透传 Action 消除

- **问题**：`stores/exam.ts` 中 32 个 action 是纯 1:1 透传调用 `api.xxxApi()`，零价值中间层。
- **方案**：从 store 中删除这 32 个 action，10 个组件改为直接 `import * as api from '@/api'` 并调用 `api.xxxApi()`。
- **保留的 action**：有状态变更或连锁刷新的 action（login/logout/register/loadXxx/createXxx/publishExam/submitExam 等）以及 helper 类（hasSubmittedExam/isExamAccessible）。
- **涉及组件**：
  - `AdminConfig.vue` — 所有 13 个配置 action 改为 api 直接调用，移除 `SmtpAccountData` 类型导入（由 `api.SmtpAccountData` 替代）
  - `AdminFeedbacks.vue` — 4 个反馈 action 改为 api 直接调用
  - `AdminQuestions.vue` — loadUploadFiles/deleteFile 改为 api 直接调用
  - `AdminUpload.vue` — loadUploadFiles/deleteFile 改为 api 直接调用
  - `AdminExams.vue` — getExamDetail/loadExamHistory 改为 api 直接调用
  - `AdminResults.vue` — getResultDetail 改为 api 直接调用
  - `StudentExamDetail.vue` — getExamDetail 改为 api 直接调用
  - `StudentResultDetail.vue` — submitFeedback/getResultDetail/myFeedbackQuestionIds 改为 api 直接调用
  - `StudentWrongBook.vue` — 7 个错题本 action 全部改为 api 直接调用
  - `StudentWrongBookDetail.vue` — 6 个错题本 action 全部改为 api 直接调用
- **文件**：`stores/exam.ts`、10 个 `.vue` 组件

### 4.2 useDataSource 组合函数

- **问题**：9 个组件有重复的 `const loading = ref(true); try { data = await xxx() } catch { ElMessage.error('...') } finally { loading.value = false }` 模式。
- **方案**：创建 `src/composables/useDataSource.ts`，提供 `data`/`loading`/`load` 三个返回值，统一 loading 和 error 处理。
- **文件**：`src/composables/useDataSource.ts`

### 4.3 cleanParams 共享函数

- **问题**：`apiHelper.ts` 和 `download.ts` 各自独立实现参数清洗，语义不一致。
- **方案**：在 `apiHelper.ts` 中导出 `cleanParams()`（过滤 `undefined` 和空串），`download.ts` 导入复用。
- **文件**：`src/utils/apiHelper.ts`、`src/utils/download.ts`

### 4.4 预存类型错误修复

- store 类型推断从 `any` 变为精确类型后，暴露了 AdminExams.vue 中未声明的模板 ref（`form`/`publishDialogVisible`/`previewVisible` 等 13 个变量），均已添加声明。
- `AdminQuestions.vue` 中 `QUESTION_TYPE_LABEL` 索引类型不兼容问题。
- `StudentExamDetail.vue` 中 `serverTime` 不在 ExamDetail 类型上的问题。

## 5. 数据库视角

无数据库变更。

## 6. 接口视角

无接口变更。所有 API 路径、参数、返回值不变。

## 7. 关键文件

### 后端

| 文件 | 改动 |
|---|---|
| `service/WrongNotebookService.java` | 抽取 `checkOwner()` + N+1 优化 |
| `service/FeedbackService.java` | 合并 resolve/reject + 抽取 `findFeedback() + updateFeedbackStatus()` |
| `service/ExamService.java` | shuffleQuestionOptions 用 Spring BeanUtils |
| `service/QuestionService.java` | 抽取 `buildOrderedWrapper()` |
| `service/FileService.java` | 魔法字符串常量 |
| `controller/RegisterController.java` | 抽取 `validateEmailRegistration()` |

### 前端

| 文件 | 改动 |
|---|---|
| `stores/exam.ts` | 删除 32 个透传 action |
| `composables/useDataSource.ts` | **新增** 组合函数 |
| `utils/apiHelper.ts` | 导出 `cleanParams()` |
| `utils/download.ts` | 导入 `cleanParams()` |
| 10 个 `.vue` 组件 | store 调用 → api 直接调用 |
| `admin/AdminExams.vue` | 补充 13 个未声明的 ref 变量 |

## 8. 验证方式

```powershell
# 前端
cd Oline-AQ-vue
npm run build

# 后端
cd Oline-AQ-spring
.\mvnw.cmd compile -q
```

前后端均编译通过，无错误。

## 9. 风险与后续优化

### 风险
- store 透传 action 删除后，需确认所有组件已更新为直接调用 api（已通过编译验证）。
- `AdminExams.vue` 中 13 个模板 ref 是临时补充的声明式修复，建议后续由熟悉该文件逻辑的人确认默认值和类型。

### 剩余可优化项目
- `ElMessageBox.confirm` 9 处统一封装为组合函数。
- `<ExamMetaDescriptions>` 组件抽取消除 AdminExams/StudentExams 中重复的 `<el-descriptions-item>` 标签。
- AdminUpload.vue（654 行）和 AdminConfig.vue（464 行）可拆分为子组件。
