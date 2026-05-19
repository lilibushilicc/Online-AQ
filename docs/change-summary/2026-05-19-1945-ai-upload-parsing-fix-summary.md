# AI 上传识别优化改动总结

## 1. 改动目标

本次改动用于修复上传 `cs.txt` 这类章节式试题文本时，系统把“一、选择题（5 题）”识别成单道题的问题。根因是原本地解析器只按 `1.` 这类数字编号切分题目，而该文件使用“章节标题 + 无编号题干 + 答案”的排版，导致整份选择题被当成一个解析块。

## 2. 用户视角

管理员上传类似 `cs.txt` 的试题文件后，题库中不再出现章节标题作为题干。系统会逐题识别选择题和简答题，选择题保存 A/B/C/D 选项与字母答案，简答题保存多行参考答案。

开启 AI 智能解析时，如果 AI 返回结果明显漏题，后端会使用本地结构化解析结果兜底，减少只入库 1 道题或章节标题入库的情况。

## 3. 前端实现

- 页面：无前端页面改动。
- 组件：无新增组件。
- 路由：无变化。
- 状态管理：无变化。
- 接口请求：仍使用现有上传解析接口。
- 样式与布局：无变化。

## 4. 后端实现

- Controller：无接口签名变化，继续使用 `POST /api/files/{fileId}/parse`。
- Service：
  - `QuestionParseService` 从数字编号切分改为按“题干、选项、答案、下一题”扫描。
  - 新增章节标题识别，跳过“一、选择题”“二、简答题”等行。
  - 支持无编号选择题、全角选项点、多行简答答案。
  - `AiQuestionParseService` 对 AI 返回题目做清洗，跳过章节标题；当本地解析出的题目数多于 AI 结果时，使用本地解析兜底。
- Mapper：无变化。
- Entity / DTO / VO：无变化。
- 配置：无变化。

## 5. 数据库与数据流

本次无表结构变化。数据流保持为：上传文件读取 `raw_text`，解析服务把文本转换为 `Question` 列表，随后写入 `question` 表。

变化点在解析阶段：章节标题不会进入 `question_content`；选择题选项会拆入 `option_a` 到 `option_d`；简答题多行答案会合并写入 `correct_answer`。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/files/{fileId}/parse` | POST | 解析上传文件并入库题目 | `category`、`useAi` | `questionCount` |

接口路径和参数未变化，解析准确性在后端服务内部增强。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/QuestionParseService.java` | 增强本地题目解析，支持章节式无编号文本和多行简答答案 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/AiQuestionParseService.java` | 清洗 AI 返回结果，并在 AI 漏题时回退到本地解析 |
| `Oline-AQ-spring/src/test/java/com/example/olineaqspring/service/QuestionParseServiceTest.java` | 新增解析单元测试，覆盖无编号章节式题库文本 |
| `README.md` | 追加本次更新记录，并补充上传解析能力说明 |

## 8. 验证方式

```sh
cd Oline-AQ-spring
.\mvnw.cmd test
.\mvnw.cmd clean test
```

验证结果：两次测试均通过；`clean test` 从零编译 72 个后端主类，并运行 2 个测试，结果为 `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。

## 9. 风险与后续优化

- 当前本地解析仍依赖“答案：”作为题目边界；如果文件没有答案标记，仍更适合交给 AI 解析。
- 简答题下一题识别基于常见问句特征，极端格式可能需要继续扩充规则。
- 后续可以在上传预览页增加解析结果确认与人工修正，再批量入库，进一步降低题库脏数据风险。
