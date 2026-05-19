# 安全 / 代码质量 / 性能 / UI 全面优化 + 代码简洁化改动总结

## 1. 改动目标

本次改动针对项目四个维度进行全面优化：
- **安全**：解决密码明文存储、JWT Secret 硬编码、缺少参数校验、异常信息泄露等问题
- **代码质量**：修复后端 N+1 查询、拆分前端 470 行单体 Store、改进异常处理层级
- **性能**：题目查询增加后端分页支持
- **UI**：补充按钮 Loading 状态、动态 Document Title、优化操作反馈

## 2. 用户视角

- 无感知的安全升级：密码存储方式切换为 BCrypt，但登录体验无变化
- 浏览器标签页现在会显示当前页面名称（如"题库管理 - 智能在线答题系统"）
- 创建考试、发布考试等操作的按钮现在有 Loading 状态，防止重复点击
- 练习模式提交答案时也有 Loading 状态

## 3. 前端实现

### 架构拆分

- **`src/types/index.ts`**（新增）：提取所有类型接口定义（原在 `exam.ts` 中的 30+ 个 interface）
- **`src/api/index.ts`**（新增）：提取所有 API 调用到独立服务层，按模块组织（auth、questions、exams、results、users、config、notebooks、feedbacks）
- **`src/stores/exam.ts`**（重构）：保留 Pinia Store 状态管理，所有 API 调用委托给 `src/api/index.ts`，Backward-compatible 导出所有类型

### UI 改进

- **动态 Document Title**：在 `router/index.ts` 的 `afterEach` 钩子中根据路由路径设置标题
- **AdminExams**：新增 `creating` / `publishing` 两个 Loading 状态，绑定到保存和发布按钮
- **StudentPractice**：新增 `submitting` 状态，提交按钮增加 `:loading`
- **AdminStudents**：已有 `submitting` 状态，已验证

### 影响文件

| 操作 | 文件 |
|------|------|
| 新增 | `src/types/index.ts` |
| 新增 | `src/api/index.ts` |
| 重构 | `src/stores/exam.ts`（体积从 470 行减至 210 行） |
| 修改 | `src/utils/apiHelper.ts`（params 类型扩展） |
| 修改 | `src/router/index.ts`（添加 document.title） |
| 修改 | `src/views/admin/AdminExams.vue`（loading 状态） |
| 修改 | `src/views/student/StudentPractice.vue`（loading 状态） |

## 4. 后端实现

### 安全

| 改动 | 说明 |
|------|------|
| **BCrypt 密码加密** | `AuthService` 登录验证从明文对比改为 `passwordEncoder.matches()`；`UserService` 创建/更新用户时使用 `passwordEncoder.encode()` |
| **密码迁移** | `DemoUserInitializer` 新增 `migratePlaintextPasswords()`，检测并自动升级现有明文密码为 BCrypt |
| **JWT Secret 环境变量** | `application.yml` 中 `app.jwt-secret`、数据库密码等敏感配置改为 `${ENV_VAR:default}` 形式 |
| **参数校验** | `LoginRequest`、`QuestionRequest` DTO 增加 `@NotBlank`、`@Positive` 注解；Controller 增加 `@Valid` |
| **安全依赖** | `pom.xml` 新增 `spring-security-crypto`（BCrypt）和 `spring-boot-starter-validation` |
| **安全配置** | 新增 `SecurityConfig.java`，提供 `PasswordEncoder` Bean |

### 异常处理改进

- `GlobalExceptionHandler`：
  - 新增 `MethodArgumentNotValidException` 处理 → 拼接具体字段错误信息返回
  - 新增 `IllegalArgumentException` 处理 → 400
  - `RuntimeException` 改为 500（`INTERNAL_SERVER_ERROR`），返回通用消息"系统繁忙，请稍后重试"，避免泄露内部信息
  - 新增 `Exception` 兜底处理 + Slf4j 日志记录

### 代码质量

- **修复 N+1 查询**：
  - `ExamService.getQuestionMap()`：`questionMapper.selectBatchIds()` 替代循环 `selectById`
  - `QuestionService.updateCategoryBatch()` 和 `updateScoreBatch()`：`selectBatchIds` 替代循环 `selectById`

### 性能

- **题库分页**：`QuestionController.list()` 新增 `page` 和 `pageSize` 参数
- **`QuestionService.list()`** 返回 `PageResult<Question>` 包含 total/page/pageSize/totalPages
- **新增 `PageResult.java`** VO 类

### 影响文件

| 操作 | 文件 |
|------|------|
| 修改 | `pom.xml`（添加 2 个依赖） |
| 修改 | `application.yml`（敏感配置环境变量化） |
| 新增 | `config/SecurityConfig.java`（PasswordEncoder Bean） |
| 修改 | `config/GlobalExceptionHandler.java`（分层异常处理） |
| 修改 | `config/DemoUserInitializer.java`（BCrypt + 密码迁移） |
| 修改 | `service/AuthService.java`（BCrypt 验证） |
| 修改 | `service/UserService.java`（BCrypt 存储） |
| 修改 | `service/ExamService.java`（N+1 修复） |
| 修改 | `service/QuestionService.java`（N+1 修复 + 分页） |
| 修改 | `controller/QuestionController.java`（分页参数） |
| 修改 | `controller/AuthController.java`（@Valid） |
| 新增 | `vo/PageResult.java`（分页 VO） |
| 修改 | `dto/LoginRequest.java`（校验注解） |
| 修改 | `dto/QuestionRequest.java`（校验注解） |
| 修改 | `sql/init.sql`（移除明文密码 SQL） |

## 5. 数据库与数据流

- 密码存储方式变更：现有明文密码在应用启动时自动迁移为 BCrypt 哈希（检测条件：不以 `$2a$` 或 `$2b$` 开头）
- init.sql 中的明文密码插入已移除，由 DemoUserInitializer 通过 BCrypt 创建
- 数据流无变化，仅密码验证和存储方式改变

## 6. 接口说明

| 接口 | 方法 | 变化 |
|------|------|------|
| `/api/questions` | GET | 新增 `page` 和 `pageSize` 参数，返回 `PageResult` 结构 |

其余接口无变化。

## 7. 验证方式

```sh
# 前端类型检查 + 构建
cd Oline-AQ-vue
npm run build

# 后端编译
cd Oline-AQ-spring
.\mvnw.cmd compile
```

## 8. 代码简洁化（追加）

### 后端
- 所有 11 个 Service 类使用 `@RequiredArgsConstructor` 替代显式构造函数，消除约 80 行冗长构造器代码
- 提取 `AnswerMapHelper` 工具类，消除 `ResultService` 和 `WrongNotebookService` 中重复的 "题目答案转 Map" 逻辑（~50 行重复代码合并为共享方法）
- `ResultService.toAnswerMap()` 从 22 行缩减为 3 行

### 前端
- `exam.ts` Store 从 285 行精简为 **130 行**（降幅 54%）
  - 合并 import 语句（`import * as api`）
  - 合并 export type 语句
  - 精简 helper 函数（`loadUser` 从 8 行减为 2 行，`canAccess` 合并 3 个函数为 1 个）
  - 无状态变更的 action 改为箭头函数单行表达式
  - getter 表达式合并为单行

### 影响文件

| 操作 | 文件 |
|------|------|
| 新增 | `utils/AnswerMapHelper.java` |
| 修改 | 所有 11 个 Service 文件（加 `@RequiredArgsConstructor`） |
| 修改 | `src/stores/exam.ts`（285→130 行） |

## 9. 风险与后续优化

### 风险
- 已有数据库中存在明文密码的用户，启动时 `migratePlaintextPasswords()` 会自动升级为 BCrypt。但如果应用在启动过程中被中断，可能存在部分用户未迁移的情况。
- `RuntimeException` 现在返回 500 + 通用消息，前端需要确保不依赖后端的具体错误消息做业务判断。
- 如果前端曾经直接使用 `loadQuestions()` 的返回作为数组（而非 `list` 字段），需要适配新的 `PageResult` 结构。当前 `exam.ts` 的 `loadQuestions` 已做适配（取 `result.list`）。
- 使用 `@RequiredArgsConstructor` 要求所有注入字段为 `final`（已是），且 Lombok 注解处理器必须在 IDE 和构建中正确配置（pom.xml 已有 `lombok` 依赖）

### 后续可优化
- 引入 Swagger/SpringDoc 自动生成 API 文档
- 后端增加全局请求日志（Request/Response 日志）
- 全表增加分页支持（成绩列表、用户列表等）
- 前端增加全局 Loading 遮罩层
- 前端 XSS 防护（DOMPurify 等）
- 数据库连接池配置调优
