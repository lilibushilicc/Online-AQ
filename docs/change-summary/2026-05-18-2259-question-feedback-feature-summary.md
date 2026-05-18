# 题目纠错反馈功能 改动总结

## 1. 改动目标

上传智能解析的题目可能存在错误（选项错位、答案错误等），学生发现后可对具体题目发起纠错反馈，管理员收到反馈后核实并修改题库，形成闭环。

## 2. 用户视角

### 学生端
- 提交试卷后，进入 `/student/results/:resultId` 成绩详情页
- 每道题右上角增加 **「反馈纠错」** 按钮
- 点击后弹出对话框，选择反馈类型（答案错误/题干错误/选项错误/其他）+ 填写说明
- 提交后按钮变为 **「已反馈」** 标签（同题不可重复提交）

### 管理员端
- 侧边栏新增 **「反馈管理」** 菜单（`/admin/feedbacks`）
- 页面顶部标签切换：全部 / 待处理 / 已采纳 / 已驳回
- 反馈列表含：题目内容、反馈学生、类型、描述、状态、时间
- 操作列：**查看**（Drawer 展示反馈+题目详情）| **修改并采纳**（弹窗编辑题目内容/选项/答案）| **驳回**（填写驳回理由）
- 采纳后题库自动更新，反馈状态变为「已采纳」

## 3. 数据库

新建 `question_feedback` 表，通过 `SchemaInitializer.java` 自动创建：

```sql
CREATE TABLE IF NOT EXISTS question_feedback (
    feedback_id   SERIAL PRIMARY KEY,
    question_id   INTEGER NOT NULL,
    student_id    INTEGER NOT NULL,
    exam_id       INTEGER,
    feedback_type VARCHAR(50) NOT NULL,   -- answer_error / content_error / option_error / other
    description   TEXT NOT NULL,
    status        VARCHAR(50) DEFAULT 'pending',  -- pending / resolved / rejected
    reject_reason TEXT,
    resolve_type  VARCHAR(50),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 4. 接口设计

| 接口 | 方法 | 权限 | 说明 | 请求参数 | 返回 |
|---|---|---|---|---|---|
| `/api/feedbacks` | POST | Student | 提交反馈 | `{ questionId, examId?, feedbackType, description }` | `QuestionFeedback` |
| `/api/feedbacks` | GET | Admin | 查询反馈列表 | `status?` | `FeedbackListVO[]` |
| `/api/feedbacks/{id}` | GET | Admin | 查看反馈详情 | — | `FeedbackDetailVO` |
| `/api/feedbacks/{id}/resolve` | PUT | Admin | 采纳并修改题目 | `QuestionRequest` | Void |
| `/api/feedbacks/{id}/reject` | PUT | Admin | 驳回 | `{ rejectReason }` | Void |
| `/api/feedbacks/my` | GET | Student | 查询已反馈题目ID | `questionIds=1,2,3` | `number[]` |

## 5. 后端实现

| 文件 | 说明 |
|---|---|
| `config/SchemaInitializer.java` | 新增 `question_feedback` 建表 |
| `entity/QuestionFeedback.java` | 反馈实体 |
| `dto/FeedbackCreateRequest.java` | 创建反馈 DTO |
| `vo/FeedbackListVO.java` | 列表视图（联表学生名+题干） |
| `vo/FeedbackDetailVO.java` | 详情视图（反馈+题目+学生名） |
| `mapper/QuestionFeedbackMapper.java` | MyBatis-Plus Mapper + 自定义 @Select 联表查询 |
| `service/FeedbackService.java` | 创建/列表/详情/采纳/驳回业务逻辑 |
| `controller/FeedbackController.java` | 6 个 REST 接口 |

## 6. 前端实现

| 文件 | 改动 |
|---|---|
| `stores/exam.ts` | 新增 `QuestionFeedback`、`FeedbackListVO`、`FeedbackCreatePayload` 类型 + 6 个 actions |
| `router/index.ts` | 新增 `/admin/feedbacks` 路由 |
| `views/admin/AdminLayout.vue` | 侧边栏新增「反馈管理」菜单项，导入 `ChatDotRound` 图标 |
| `views/student/StudentResultDetail.vue` | 每道题增加反馈按钮+对话框，加载时查询已反馈状态 |
| `views/admin/AdminFeedbacks.vue` | **新增**完整管理页面：标签筛选、表格、详情 Drawer、采纳/驳回弹窗 |

## 7. 验证方式

```sh
# 前端
cd Oline-AQ-vue && npm run build   # ✓ built in 585ms

# 后端
cd Oline-AQ-spring && .\mvnw compile -q   # ✓ 编译成功
```

启动后：
1. 学生登录 → 提交考试 → 成绩详情页 → 点击「反馈纠错」→ 提交
2. 管理员登录 → 侧边栏「反馈管理」→ 查看待处理反馈
3. 点击「修改并采纳」→ 修改题干/选项/答案 → 保存
4. 确认题库题目已更新，反馈状态变为「已采纳」

## 8. 风险与后续优化

- **同题同生反馈限制**：通过 `myFeedbackQuestionIds` 接口查询已反馈题目，前端禁用重复提交
- **修改仅覆盖输入字段**：管理员提交时留空的字段不覆盖原题目（`FeedbackService.copy` 判空）
- **通知机制**：后续可增加管理员未读反馈 badge 计数
- **评分影响**：修改题目后不会重新计算已有考试成绩，后续可增加「重新评分」功能
