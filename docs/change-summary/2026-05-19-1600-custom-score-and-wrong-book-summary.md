# 自定义题目分值 & 错题本改动总结

## 1. 改动目标

1. **试卷题目分数支持自定义**：原来创建考试时只能使用题库中预设的每题分值，无法针对某一套考试单独调整每道题的分数。本次改动允许管理员在选题面板中为每道题单独设置考试分值，不改变题库原始分值。
2. **学生端增加错题本**：学生之前只能在成绩详情中逐次查看错题，缺少一个集中展示所有历史错题的入口。本次新增错题本页面，按考试分组展示所有错题，方便学生复习巩固。

## 2. 用户视角

### 自定义分值
- 管理员打开创建考试的「选题面板」，选择题目后，在「已选题目列表」中每道题旁出现一个数字输入框
- 默认显示题库预设分值，管理员可修改为任意数值（0~100 分）
- 修改后，顶部「预计总分」实时更新
- 保存考试时，自定义分值会随创建请求一起提交，考试使用自定义分值而非题库原始分值

### 错题本
- 学生端侧边栏新增「错题本」导航项，位于「我的成绩」下方
- 点击进入后展示统计卡片（累计错题数、涉及考试数）
- 错题按考试分组展示，每组显示考试名称和「查看考试」按钮
- 每条错题展示题干、选项（正确答案高亮标记）、你的答案、正确答案、提交时间

## 3. 前端实现

- **AdminExams.vue**：新增 `customScores` Reactive Map 跟踪每题自定义分值；新增 `customScores` 计算属性；`getScore()`/`setScore()` 辅助函数；已选题目列表每道题增加 `el-input-number` 组件允许编辑分值；提交时若存在自定义分值则附加 `questionScores` 字段
- **StudentWrongBook.vue**：全新页面，调用 `GET /api/results/wrong-questions`；统计卡片 + 按考试分组的错题列表；正确答案选项高亮显示
- **StudentLayout.vue**：侧边栏新增「错题本」菜单项，使用 `WarningFilled` 图标
- **router/index.ts**：新增 `/student/wrong-book` 路由（懒加载）

## 4. 后端实现

- **ExamCreateRequest.java**：新增内部类 `QuestionScoreItem`（questionId + score），新增可选字段 `questionScores`
- **ExamService.java**：`create()` 方法解析 `request.questionScores` 构建分值映射；`detail()` 方法返回 `relationScores`（题目在本次考试中的实际分值）；`calculateTotalScore()` 接受自定义分值映射参数
- **ResultService.java**：新增 `wrongQuestions(studentId)` 方法，查询 `student_answer` 表中当前学生的所有错题，按考试分组并关联 `question` 和 `exam` 表获取完整信息
- **ResultController.java**：新增 `GET /api/results/wrong-questions` 接口

## 5. 数据库与数据流

### 自定义分值
- 数据流：管理员修改分值 → 存入 `exam_question.score`（覆盖原有的 `question.score` 复制值）
- 评分时 `ResultService.submit()` 已经从 `exam_question.score` 读取分值，无需修改
- 预览试卷时从 `detail()` 返回的 `relationScores` 读取实际分值

### 错题本
- 数据流：查询 `student_answer` 表 `student_id = ? AND is_correct = false` → 关联 `question` 表获取题干选项 → 关联 `exam` 表获取考试名称
- 按 `exam_id` 分组输出

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `POST /api/exams` | POST | 创建考试（支持自定义分值） | `questionScores: [{questionId, score}]` | Exam |
| `GET /api/exams/{examId}` | GET | 考试详情（含自定义分值） | 无 | `{exam, questions, relationScores}` |
| `GET /api/results/wrong-questions` | GET | 学生错题列表 | 无 | `[{examId, examName, questions}]` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/src/main/java/.../dto/ExamCreateRequest.java` | 新增 QuestionScoreItem 内部类和 questionScores 字段 |
| `Oline-AQ-spring/src/main/java/.../service/ExamService.java` | create 支持自定义分值；detail 返回 relationScores |
| `Oline-AQ-spring/src/main/java/.../service/ResultService.java` | 新增 wrongQuestions 方法 |
| `Oline-AQ-spring/src/main/java/.../controller/ResultController.java` | 新增 /wrong-questions 端点 |
| `Oline-AQ-vue/src/stores/exam.ts` | 新增 CreateExamPayload.questionScores、WrongQuestionItem、WrongQuestionGroup 类型；新增 getWrongQuestions API |
| `Oline-AQ-vue/src/views/admin/AdminExams.vue` | 选题面板支持自定义每道题分值 |
| `Oline-AQ-vue/src/views/student/StudentWrongBook.vue` | 错题本页面 |
| `Oline-AQ-vue/src/views/student/StudentLayout.vue` | 新增错题本导航项 |
| `Oline-AQ-vue/src/router/index.ts` | 新增 /student/wrong-book 路由 |

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

验证结果：后端编译通过，前端类型检查和构建均通过。

## 9. 风险与后续优化

- 自定义分值仅在创建考试时设定；后续可以考虑增加「编辑考试」功能，允许修改已创建考试的分值
- 错题本目前只展示已回答的记录；未来可增加错题重做功能、按分类/题型筛选、导出错题等功能
- 自定义分值与题库原始分值分离存储（exam_question.score vs question.score），修改题库分值不会影响已有考试
- 错题本接口中若同一道题在多个考试中都答错，会重复展示，这是预期行为（不同考试上下文不同）
