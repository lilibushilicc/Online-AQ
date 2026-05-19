# 题目与答案乱序功能改动总结

## 1. 改动目标

学生在线答题时，题目始终按创建顺序、选项始终按 A/B/C/D 固定顺序排列，防作弊和考核区分度不足。本次新增"题目乱序"和"选项乱序"两个配置项，教师可在创建考试时灵活选择。

## 2. 用户视角

### 教师端
- **发布考试弹窗**中新增乱序配置区域：
  - **题目乱序**：打乱题目顺序 / 题目按创建顺序
  - **选项乱序**：打乱选项顺序 / 选项按原始顺序
- 考试列表卡片中显示当前乱序配置状态
- 同一考试可先发布一个版本（如题目乱序），关闭后重新发布不同版本（如同时题目+选项乱序）

### 学生端
- 如果考试启用了"题目乱序"，学生看到题目顺序随机排列，每次刷新都可能不同
- 如果考试启用了"选项乱序"，单选题/判断题的选项 A/B/C/D 内容随机排列，对应的正确答案自动映射
- 学生作答时看到的选项顺序与提交时映射到原始答案，评分不受影响

## 3. 前端实现

### `AdminExams.vue`
- `form` 新增 `shuffleQuestions` 和 `shuffleAnswers` 两个布尔字段，默认 false
- 表单新增两个 `el-switch` 开关
- 考试描述和列表卡片中显示乱序配置
- `createExam()` 调用时传入乱序字段

### `StudentExamDetail.vue`
- 新增 `shuffleMap` 响应式变量，存储后端返回的选项映射
- 新增 `toOriginalAnswer()` 函数：将学生在乱序后选择的字母（如 "A"）映射回原始字母
- `handleSubmit()` 提交前遍历所有答案，通过 `toOriginalAnswer()` 转换后再提交

### `types/index.ts`
- `Exam` 接口增加 `shuffleQuestions` 和 `shuffleAnswers` 可选字段
- `ExamDetail` 接口增加 `shuffleMap` 可选字段
- `CreateExamPayload` 接口增加对应字段

## 4. 后端实现

### `Exam.java`
- 新增字段：`shuffleQuestions` (Boolean)、`shuffleAnswers` (Boolean)

### `ExamCreateRequest.java`
- 新增字段：`shuffleQuestions`、`shuffleAnswers`

### `ExamService.java`
- `create()`: 保存时将乱序配置写入 Exam 实体
- `detail()`: 重载为接受 `role` 参数，学生角色时根据配置：
  - **题目乱序**：`Collections.shuffle(questions)` 打乱题目列表
  - **选项乱序**：`shuffleQuestionOptions()` 为每道题生成随机排列，重新排列 optionA~D 并转换 correctAnswer
- 新增 `shuffleQuestionOptions(Question, Map<Integer, int[]>)`：生成排列数组，重排选项字段，同时将排列存入 `shuffleMap`（`newPosition -> originalIdx`）
- 响应中包含 `shuffleMap`，供前端提交时还原答案

### `ExamController.java`
- `detail` 方法新增 `HttpServletRequest` 参数，传入 `role` 给 service

### `init.sql`
- 新增两行 ALTER TABLE 添加字段

## 5. 数据库与数据流

### 新增字段
```sql
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_answers BOOLEAN DEFAULT FALSE;
```

### 完整答题数据流（乱序场景）
```
Admin 创建考试（设置 shuffleQuestions/shuffleAnswers）
  → Student 获取考试详情 GET /api/exams/{id}
    → 后端按角色判断：学生且启用了乱序 → 打乱题目/选项
    → 返回 shuffleMap 用于答案还原
  → Student 答题（看到的选项已乱序）
  → Student 提交答案（前端用 shuffleMap 还原字母后再提交）
  → 后端用原始 correctAnswer 正常评分
```

## 6. 接口说明

接口无变化，`GET /api/exams/{examId}` 响应中新增 `shuffleMap` 字段（仅学生角色且启用选项乱序时存在）。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../entity/Exam.java` | 新增 shuffleQuestions / shuffleAnswers 字段 |
| `Oline-AQ-spring/.../dto/ExamCreateRequest.java` | 新增乱序请求字段 |
| `Oline-AQ-spring/.../service/ExamService.java` | `detail()` 按角色乱序逻辑、`shuffleQuestionOptions()` 工具方法 |
| `Oline-AQ-spring/.../controller/ExamController.java` | `detail` 传入 request 获取角色 |
| `Oline-AQ-spring/sql/init.sql` | 新增字段 DDL |
| `Oline-AQ-vue/src/types/index.ts` | 类型定义更新 |
| `Oline-AQ-spring/.../dto/PublishExamRequest.java` | 新增 shuffleQuestions / shuffleAnswers 字段 |
| `Oline-AQ-vue/src/views/admin/AdminExams.vue` | 发布弹窗添加乱序配置，创建表单移除乱序 |
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | `shuffleMap` 存储、`toOriginalAnswer()` 答案还原 |

## 8. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring && .\mvnw.cmd compile -q

# 前端类型检查
cd Oline-AQ-vue && npm run type-check
```

两个命令均通过，无错误。

## 9. 风险与后续优化

- 非 `single`/`judge` 题型（填空、简答）的答案不变，不影响评分
- 乱序仅对学生角色生效，教师预览时始终显示原始顺序
- 后续可考虑：查看成绩详情时也记录当时的乱序排列，便于事后核对
