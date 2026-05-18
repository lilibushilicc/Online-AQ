# 第一优先级功能与历史记录改动总结

## 1. 改动目标

本次改动用于落实上一轮功能推荐中的第一优先级内容，并补充历史记录能力，重点解决以下问题：

- 考试缺少开始时间和结束时间控制，无法按时间窗口开放。
- 学生提交后仍可能重复交卷，缺少“仅允许一次提交”控制。
- 题库管理只有基础筛选和单条删除，缺少批量处理能力。
- 成绩页面只能看总分，无法查看单次答题明细和考试历史记录。
- 教师侧缺少考试历史轨迹，不利于排查“何时创建、何时发布、谁提交过”。

## 2. 用户视角

- 教师创建考试时可以设置开始时间、结束时间和是否允许重考。
- 学生只能在考试开放时间内进入考试；若考试设置为“仅允许一次提交”，交卷后不能再次进入作答。
- 教师在题库管理页可以按题型筛选，并可批量删除题目、批量设置题目分值。
- 学生成绩页新增“查看详情”，可以看到每道题的答案、正确与否、得分，以及该场考试的历史记录。
- 教师成绩页新增“查看详情”，可查看每次提交的答题明细与对应考试的历史轨迹。
- 教师考试管理页新增“历史记录”抽屉，可直接查看考试创建、发布、关闭、学生提交等事件。

## 3. 前端实现

- 页面：
  - `AdminExams.vue` 新增考试开始/结束时间、是否允许重考、考试历史记录查看。
  - `AdminQuestions.vue` 新增题型筛选、批量删除、批量设置分值。
  - `AdminResults.vue` 新增成绩详情抽屉。
  - `StudentExams.vue` 新增开放时间、重考策略和历史提交提示。
  - `StudentExamDetail.vue` 新增进入考试前的时间窗口与重复提交校验。
  - `StudentResults.vue` 新增历史成绩详情入口。
  - `StudentResultDetail.vue` 为本次新增页面，用于展示单次答题明细和考试历史。
- 组件：
  - 沿用现有布局组件 `AdminLayout.vue`、`StudentLayout.vue`，未新增通用布局组件。
- 路由：
  - 新增学生成绩详情路由 `/student/results/:resultId`。
- 状态管理：
  - `src/stores/exam.ts` 新增考试历史、成绩详情、题库批量操作、考试可访问性判断等能力。
  - 将考试开放时间与是否允许提交的前端判断集中到 store，避免页面各自重复写逻辑。
- 接口请求：
  - 新增对 `/api/questions/batch-delete`、`/api/questions/batch-score`、`/api/exams/{examId}/history`、`/api/results/{resultId}` 的调用。
- 样式与布局：
  - 复用现有全局样式结构，未单独增加新的样式文件。

## 4. 后端实现

- Controller：
  - `ExamController` 增加考试历史查询，并在创建、发布、关闭时读取登录用户 ID 记录操作。
  - `ResultController` 新增成绩详情查询接口。
  - `QuestionController` 新增批量删除和批量设置分值接口。
- Service：
  - `ExamService` 新增考试时间范围校验、重考策略保存、考试历史记录写入与查询。
  - `ResultService` 新增交卷前的考试状态/时间窗口/重复提交校验，并支持返回成绩详情。
  - `QuestionService` 新增批量删除和批量分值设置逻辑。
- Mapper：
  - 新增 `ExamHistoryMapper`。
- Entity / DTO / VO：
  - `Exam` 新增 `allowRetake` 字段。
  - 新增 `ExamHistory` 实体。
  - `ExamCreateRequest` 新增 `startTime`、`endTime`、`allowRetake`。
  - 新增 `QuestionBatchDeleteRequest`、`QuestionBatchScoreRequest`。
- 配置：
  - 没有改动 Spring 配置文件。

## 5. 数据库与数据流

本次数据库层新增一张历史表，并扩展了考试配置字段：

- `exam` 表新增：
  - `allow_retake`：是否允许重考
  - `start_time`：考试开始时间
  - `end_time`：考试结束时间
- 新增 `exam_history` 表：
  - `history_id`
  - `exam_id`
  - `operator_id`
  - `action_type`
  - `action_detail`
  - `create_time`

数据流变化如下：

1. 教师创建考试时，前端提交时间窗口、重考策略和题目列表。
2. 后端保存 `exam` 与 `exam_question` 后，写入一条 `exam_history` 创建记录。
3. 教师发布/关闭考试时，后端更新状态并写入 `exam_history`。
4. 学生交卷时，后端先校验考试状态、开放时间和重复提交规则，再写入 `student_answer`、`exam_result`，同时写入一条 `exam_history` 提交记录。
5. 学生成绩详情页和教师成绩详情页通过 `/api/results/{resultId}` 拉取 `exam`、`exam_result`、`student_answer` 和 `exam_history` 聚合数据。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/questions/batch-delete` | POST | 批量删除题目 | `questionIds` | 通用成功响应 |
| `/api/questions/batch-score` | POST | 批量设置题目分值 | `questionIds`、`score` | 通用成功响应 |
| `/api/exams` | POST | 创建考试并配置时间窗口/重考策略 | `examName`、`description`、`duration`、`startTime`、`endTime`、`allowRetake`、`questionIds` | 考试信息 |
| `/api/exams/{examId}/history` | GET | 查询考试历史记录 | `examId` | 历史记录列表 |
| `/api/exams/{examId}/submit` | POST | 提交考试 | `studentId`、`useTime`、`answers` | `resultId`、总分、对错题统计 |
| `/api/results/{resultId}` | GET | 查询某次提交的成绩详情 | `resultId` | 考试信息、成绩信息、答题明细、历史记录 |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/ExamService.java` | 管理考试时间窗口、重考策略和考试历史 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/ResultService.java` | 交卷校验、成绩详情聚合、提交历史写入 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/QuestionService.java` | 题库批量删除与批量分值设置 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/entity/ExamHistory.java` | 考试历史记录实体 |
| `Oline-AQ-spring/sql/init.sql` | 新增 `exam_history` 表与 `exam` 扩展字段 |
| `Oline-AQ-vue/src/stores/exam.ts` | 前端考试、成绩、历史与批量操作状态入口 |
| `Oline-AQ-vue/src/views/admin/AdminExams.vue` | 教师侧考试时间、重考策略、历史记录展示 |
| `Oline-AQ-vue/src/views/admin/AdminQuestions.vue` | 题库筛选与批量操作界面 |
| `Oline-AQ-vue/src/views/admin/AdminResults.vue` | 教师侧成绩详情抽屉 |
| `Oline-AQ-vue/src/views/student/StudentExams.vue` | 学生侧考试开放状态与历史提交展示 |
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | 学生考试进入与提交校验 |
| `Oline-AQ-vue/src/views/student/StudentResults.vue` | 学生成绩历史列表 |
| `Oline-AQ-vue/src/views/student/StudentResultDetail.vue` | 学生单次成绩详情与考试历史页面 |
| `README.md` | 更新项目能力与接口说明 |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm run build

cd Oline-AQ-spring
.\mvnw.cmd test
```

验证结果：

- 前端构建成功。
- 后端测试成功。
- 前端构建仍提示打包 chunk 超过 500kB，这是现有体积告警，不是本次功能改动导致的编译失败。

## 9. 风险与后续优化

- 目前题库批量操作仍基于多次逐条更新/删除，后续可以继续优化为更高效的批量 SQL。
- 交卷校验已在后端保证可靠，但前端仍主要做友好提示，后续可以加入倒计时和自动交卷。
- `exam_history` 目前记录 `operator_id`，未额外冗余用户名；如果后续要做完整审计页面，可增加展示用户名或做联表查询。
- `README.md` 已同步为当前功能说明，但仓库中部分旧文件仍有历史中文编码问题，本次未全面清洗。
