# AI 智能解析试题功能改动总结

## 1. 改动目标

原系统仅支持正则解析试题文件，格式要求严格（必须包含题号、A-D 选项），遇到段落换行、格式不规范的试题文件时解析失败或解析不全。

本次引入 AI 智能解析，管理员配置 AI 接口后，在上传试题页可动态切换使用 AI 还是正则解析，兼容任意排版格式，提升解析率和准确性。

## 2. 用户视角

- 管理员进入"系统设置"页，可在原有存储设置下方看到新增的"AI 智能解析"区块，填写 API Endpoint、API Key、模型名称后保存。
- 管理员可点击"保存并测试连接"验证 AI 接口是否可用。
- 管理员回到"上传试题"页，在分类输入框右侧看到一个新的开关，默认"正则解析"，可切换为"AI 智能解析"。
- 开启 AI 后点击"上传并解析"，按钮图标变为魔法棒样式，解析成功后提示 `[AI] 上传并解析成功，共新增 N 道题`。
- 如果 AI 接口未配置或解析失败，前端提示错误，不阻塞后续操作。

## 3. 前端实现

### 页面
- `AdminConfig.vue`：新增 AI 配置区块，包含 endpoint、api_key、model 三个输入框，以及"保存并测试连接"按钮。
- `AdminUpload.vue`：新增 `el-switch` 开关绑定 `useAi` 状态，控制 AI/正则解析切换。

### 组件
- 无新增组件。使用了 Element Plus 的 `el-switch`、`el-icon（MagicStick）`。

### 路由
- 无变化。

### 状态管理
- `stores/exam.ts`：新增 `testAi` action，调用 `api.testAiApi()`。

### 接口请求
- `api/index.ts`：
  - `uploadAndParseApi` 新增 `useAi` 参数，调用 parse 时传递 `{ category, useAi }`。
  - 新增 `testAiApi()`，调用 `POST /config/test-ai`。

### 样式与布局
- AdminUpload 的操作栏改为 `flex-wrap: wrap` 以适应开关和按钮。

## 4. 后端实现

### Controller
- `FileController.java`：`/api/files/{fileId}/parse` 新增 `@RequestParam(defaultValue = "false") boolean useAi` 参数，传递给 `fileService.parse()`。
- `ConfigController.java`：新增 `POST /api/config/test-ai` 端点，读取 `sys_config` 中的 ai 配置，调用 `AiQuestionParseService.testConnection()`。

### Service
- `FileService.java`：`parse()` 方法签名新增 `boolean useAi`，根据该参数决定调用 `regexParse()` 还是 `aiQuestionParseService.parse()`。
- `AiQuestionParseService.java`（新增）：
  - 读取 `sys_config` 获取 endpoint、api_key、model。
  - `parse(text, category)`：构建 prompt 调用 LLM，解析返回的 JSON 数组，转换为 `Question` 对象列表。
  - `testConnection()`：调用 LLM 回复 "ok" 以验证连接。
  - `callLlm()`：使用 RestTemplate 调用 OpenAI 兼容接口，处理流式/非流式响应，剥离 markdown 代码块。

### Entity / DTO / VO
- 无新增实体。Question 解析直接构造为已有 `Question` 实体。

### 配置
- `RestTemplateConfig.java`（新增）：创建 `RestTemplate` bean，供 `AiQuestionParseService` 调用 LLM API。

## 5. 数据库与数据流

### 数据流
1. 管理员在系统设置页填写 AI 配置 → 保存到 `sys_config` 表（key: `ai.endpoint`、`ai.api_key`、`ai.model`）。
2. 管理员在上传页选择文件，开启 AI 开关 → 上传文件到文件服务 → 调用 `/api/files/{fileId}/parse?useAi=true`。
3. 后端读取 `sys_config` 获取 AI 参数 → 构建 prompt（包含文件文本）→ 调用 LLM API → 接收 JSON 响应 → 解析为 `Question` 列表 → 批量插入 `question` 表。
4. 后端会清洗 LLM 返回结果，跳过章节标题；如果本地结构化解析结果比 AI 结果更完整，则使用本地解析结果兜底。
5. 如果 LLM 调用失败或返回格式无效，抛出 `RuntimeException`，前端捕获后显示错误。

### 涉及表
- `sys_config`：新增三个配置键（`ai.endpoint`、`ai.api_key`、`ai.model`），无 DDL 变更。
- `question`：无变化。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `POST /api/files/{fileId}/parse` | POST | 解析试题文件 | `category`（可选）、`useAi`（默认 false） | `ApiResponse<{ questionCount: number }>` |
| `POST /api/config/test-ai` | POST | 测试 AI 连接 | 无（从数据库读取配置） | `ApiResponse<{ result: string }>` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../config/RestTemplateConfig.java` | 新增 RestTemplate Bean |
| `Oline-AQ-spring/.../service/AiQuestionParseService.java` | 新增 AI 解析核心服务，调用 LLM、解析 JSON |
| `Oline-AQ-spring/.../service/FileService.java` | 修改 parse() 支持 useAi 分发 |
| `Oline-AQ-spring/.../controller/FileController.java` | 修改 parse 接口接收 useAi 参数 |
| `Oline-AQ-spring/.../controller/ConfigController.java` | 新增 test-ai 端点 |
| `Oline-AQ-vue/src/api/index.ts` | 修改 uploadAndParseApi、新增 testAiApi |
| `Oline-AQ-vue/src/stores/exam.ts` | 新增 testAi action |
| `Oline-AQ-vue/src/views/admin/AdminConfig.vue` | 新增 AI 配置区块 |
| `Oline-AQ-vue/src/views/admin/AdminUpload.vue` | 新增 AI 解析开关 |

## 8. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring
.\mvnw.cmd compile -q

# 前端类型检查
cd Oline-AQ-vue
npx vue-tsc --noEmit

# 前端构建
npm run build
```

编译与类型检查均通过。

验证步骤：
1. 启动前后端服务
2. 使用 admin/123456 登录，进入系统设置
3. 填写 AI Endpoint（如 `https://api.deepseek.com/v1/chat/completions`）和 API Key
4. 点击"保存并测试连接"，应提示成功
5. 进入上传试题页，上传一个试题文件
6. 切换到"AI 智能解析"模式，点击上传并解析，应提示成功并显示新增题目数
7. 可切回"正则解析"对比解析结果

## 9. 风险与后续优化

### 当前限制
- AI 解析依赖外部 API 的可用性和响应速度，网络不佳时可能超时。
- AI 返回格式需为严格 JSON 数组，prompt 已要求；若返回格式不符合要求仍会失败，但当 AI 漏题且本地解析更完整时会自动回退到本地结果。
- API Key 存储在 `sys_config` 表中明文保存，建议后续加密存储。
- `testConnection` 会保存所有配置后再测试，可能误保存未完成的配置。

### 后续优化方向
- 增加 AI 解析超时配置和重试机制。
- 前端增加 AI 解析进度提示或 poll 轮询。
- 支持更多 LLM 提供商（Claude、Gemini 等）和自定义 prompt 模板。
- API Key 加密存储（如使用 Jasypt 或 Spring Cloud Config 的加密功能）。
- 上传预览页支持解析后人工确认和修正，再批量入库。
