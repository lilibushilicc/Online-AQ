# 题目纠错反馈功能 开发文档

## 1. 改动目标

上传试题时解析可能不准确（选项错位、答案错误、题干缺失等），学生做题目时如果发现错误，可以标记题目并发起反馈，管理员收到反馈后核实并更正题库。

## 2. 需求概述

**角色**：学生（发起反馈）、管理员（审核处理）

### 学生端
- 提交试卷后，在成绩详情页面（`/student/results/:resultId`）每道题的答题卡片旁增加一个 **「反馈纠错」** 按钮
- 点击后弹出对话框，填写反馈类型（答案错误/题干错误/选项错误/其他）和详细描述
- 提交反馈后，该题目的反馈按钮变为 **「已反馈」** 状态（禁用）

### 管理员端
- 侧边栏新增 **「反馈管理」** 菜单项，路由 `/admin/feedbacks`
- 页面展示一个表格：反馈列表（题目内容、反馈学生、反馈类型、描述、状态、时间）
- 每条反馈可执行：
  - **查看详情**：Drawer 中展示完整反馈 + 题目当前信息
  - **驳回**：填写驳回理由，状态变为「已驳回」
  - **修改题目并采纳**：弹出修改题目的表单（复用现有编辑题目逻辑），保存后状态变为「已修正」

## 3. 数据库设计

新建 `question_feedback` 表：

```sql
CREATE TABLE IF NOT EXISTS question_feedback (
    feedback_id   SERIAL PRIMARY KEY,
    question_id   INTEGER NOT NULL REFERENCES question(question_id),
    student_id    INTEGER NOT NULL REFERENCES sys_user(user_id),
    exam_id       INTEGER,                              -- 来自哪场考试（可选）
    feedback_type VARCHAR(50) NOT NULL,                  -- answer_error / content_error / option_error / other
    description   TEXT NOT NULL,                         -- 反馈描述
    status        VARCHAR(50) DEFAULT 'pending',          -- pending / resolved / rejected
    reject_reason TEXT,                                   -- 驳回理由
    resolve_type  VARCHAR(50),                            -- 管理员处理方式：modified / rejected
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `feedback_id` | INTEGER (PK) | 反馈ID |
| `question_id` | INTEGER (FK) | 关联的题目ID |
| `student_id` | INTEGER (FK) | 发起反馈的学生ID |
| `exam_id` | INTEGER (FK, nullable) | 来源考试ID（可在考试环境中回溯） |
| `feedback_type` | VARCHAR(50) | 反馈类型枚举 |
| `description` | TEXT | 详细的错误描述 |
| `status` | VARCHAR(50) | `pending` 待处理 / `resolved` 已采纳 / `rejected` 已驳回 |
| `reject_reason` | TEXT (nullable) | 驳回理由 |
| `resolve_type` | VARCHAR(50) (nullable) | `modified` 已修改 / `rejected` 已驳回 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |

### 数据流

```
学生提交考试
    → 查看成绩详情
    → 点击「反馈纠错」
    → 填写表单 → POST /api/feedbacks
    → 数据库插入 question_feedback (status = pending)
    → 管理员登录
    → 加载 GET /api/feedbacks (status = pending)
    → 审查后：
        → 点击「修改」→ PUT /api/feedbacks/{id}/resolve (修改question + 更新status)
        → 点击「驳回」→ PUT /api/feedbacks/{id}/reject (更新status + reject_reason)
```

## 4. 接口设计

### 4.1 提交反馈

| 项目 | 说明 |
|---|---|
| 接口 | `POST /api/feedbacks` |
| 权限 | Student |
| 请求体 | `{ questionId, examId?, feedbackType, description }` |
| 返回 | `ApiResponse<QuestionFeedback>` |

### 4.2 查询反馈列表（管理员）

| 项目 | 说明 |
|---|---|
| 接口 | `GET /api/feedbacks` |
| 权限 | Admin |
| 参数 | `status?` (pending / resolved / rejected) |
| 返回 | `ApiResponse<QuestionFeedback[]>` （含学生姓名、题目内容） |

### 4.3 查询反馈详情

| 项目 | 说明 |
|---|---|
| 接口 | `GET /api/feedbacks/{id}` |
| 权限 | Admin |
| 返回 | `ApiResponse<FeedbackDetail>` （反馈 + 题目完整信息 + 学生姓名） |

### 4.4 采纳反馈并修改题目

| 项目 | 说明 |
|---|---|
| 接口 | `PUT /api/feedbacks/{id}/resolve` |
| 权限 | Admin |
| 请求体 | `QuestionRequest` （与修改题目接口一致） |
| 逻辑 | 1. 更新 `question` 表 2. 更新 `question_feedback` 表 `status = resolved`, `resolve_type = modified` |
| 返回 | `ApiResponse<Void>` |

### 4.5 驳回反馈

| 项目 | 说明 |
|---|---|
| 接口 | `PUT /api/feedbacks/{id}/reject` |
| 权限 | Admin |
| 请求体 | `{ rejectReason: string }` |
| 逻辑 | 更新 `question_feedback` 表 `status = rejected`, `reject_reason = ...` |
| 返回 | `ApiResponse<Void>` |

### 4.6 查询当前用户已反馈的题目列表（学生端判断状态用）

| 项目 | 说明 |
|---|---|
| 接口 | `GET /api/feedbacks/my?questionIds=1,2,3` |
| 权限 | Student |
| 返回 | `ApiResponse<number[]>` — 已反馈的 questionId 列表 |

## 5. 后端实现

### 5.1 Entity & VO

**QuestionFeedback.java**（entity）
```java
@Data
@TableName("question_feedback")
public class QuestionFeedback {
    @TableId(type = IdType.AUTO)
    private Integer feedbackId;
    private Integer questionId;
    private Integer studentId;
    private Integer examId;
    private String feedbackType;
    private String description;
    private String status;
    private String rejectReason;
    private String resolveType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**FeedbackListVO.java**（列表视图）
```java
@Data
public class FeedbackListVO {
    private Integer feedbackId;
    private Integer questionId;
    private String questionContent;     // 联表查询
    private String studentName;         // 联表查询
    private String feedbackType;
    private String description;
    private String status;
    private String rejectReason;
    private LocalDateTime createTime;
}
```

**FeedbackDetailVO.java**（详情视图）
```java
@Data
public class FeedbackDetailVO {
    private QuestionFeedback feedback;
    private Question question;          // 完整的题目信息
    private String studentName;
}
```

### 5.2 Mapper

```java
public interface FeedbackMapper extends BaseMapper<QuestionFeedback> {
    List<FeedbackListVO> selectFeedbackList(@Param("status") String status);
    FeedbackDetailVO selectFeedbackDetail(@Param("id") Integer id);
}
```

XML 映射参考：

```xml
<select id="selectFeedbackList" resultType="com.example.olineaqspring.vo.FeedbackListVO">
    SELECT
        f.feedback_id,
        f.question_id,
        q.question_content,
        u.real_name AS studentName,
        f.feedback_type,
        f.description,
        f.status,
        f.reject_reason,
        f.create_time
    FROM question_feedback f
    LEFT JOIN question q ON f.question_id = q.question_id
    LEFT JOIN sys_user u ON f.student_id = u.user_id
    <where>
        <if test="status != null and status != ''">
            f.status = #{status}
        </if>
    </where>
    ORDER BY f.create_time DESC
</select>
```

### 5.3 Service

```java
@Service
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;
    private final QuestionService questionService;

    // 提交反馈
    public QuestionFeedback create(FeedbackCreateRequest request, Integer studentId) {
        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setQuestionId(request.getQuestionId());
        feedback.setStudentId(studentId);
        feedback.setExamId(request.getExamId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setDescription(request.getDescription());
        feedback.setStatus("pending");
        feedbackMapper.insert(feedback);
        return feedback;
    }

    // 管理员列表查询
    public List<FeedbackListVO> list(String status) {
        return feedbackMapper.selectFeedbackList(status);
    }

    // 详情
    public FeedbackDetailVO detail(Integer id) {
        return feedbackMapper.selectFeedbackDetail(id);
    }

    // 采纳并修改题目
    @Transactional
    public void resolve(Integer id, QuestionRequest request) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        // 1. 修改题目
        questionService.update(feedback.getQuestionId(), request);
        // 2. 更新反馈状态
        feedback.setStatus("resolved");
        feedback.setResolveType("modified");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
    }

    // 驳回
    public void reject(Integer id, String reason) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        feedback.setStatus("rejected");
        feedback.setRejectReason(reason);
        feedback.setResolveType("rejected");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
    }

    // 学生已反馈的题目ID列表
    public List<Integer> myFeedbackQuestionIds(Integer studentId, List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return List.of();
        return feedbackMapper.selectQuestionIdsByStudent(studentId, questionIds);
    }
}
```

### 5.4 Controller

```java
@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<QuestionFeedback> create(
            @RequestBody FeedbackCreateRequest request,
            HttpServletRequest httpRequest) {
        Integer studentId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("反馈已提交", feedbackService.create(request, studentId));
    }

    @GetMapping
    public ApiResponse<List<FeedbackListVO>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(feedbackService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeedbackDetailVO> detail(@PathVariable Integer id) {
        return ApiResponse.ok(feedbackService.detail(id));
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Integer id, @RequestBody QuestionRequest request) {
        feedbackService.resolve(id, request);
        return ApiResponse.ok("题目已修改，反馈已采纳");
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String reason = body.get("rejectReason");
        feedbackService.reject(id, reason);
        return ApiResponse.ok("反馈已驳回");
    }

    @GetMapping("/my")
    public ApiResponse<List<Integer>> myFeedbacks(
            @RequestParam List<Integer> questionIds,
            HttpServletRequest httpRequest) {
        Integer studentId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok(feedbackService.myFeedbackQuestionIds(studentId, questionIds));
    }
}
```

## 6. 前端实现

### 6.1 新增变量与接口类型

在 `src/stores/exam.ts` 新增：

```typescript
export interface QuestionFeedback {
  feedbackId: number
  questionId: number
  studentId: number
  examId?: number
  feedbackType: string
  description: string
  status: string
  rejectReason?: string
  resolveType?: string
  createTime: string
  updateTime: string
}

export interface FeedbackListVO {
  feedbackId: number
  questionId: number
  questionContent: string
  studentName: string
  feedbackType: string
  description: string
  status: string
  rejectReason?: string
  createTime: string
}

export interface FeedbackCreatePayload {
  questionId: number
  examId?: number
  feedbackType: string
  description: string
}
```

### 6.2 Store 新增 actions

```typescript
// 提交反馈
async submitFeedback(payload: FeedbackCreatePayload) {
  const { data } = await request.post<unknown, ApiResponse<QuestionFeedback>>('/feedbacks', payload)
  return data
},

// 查询已反馈题目（学生端）
async myFeedbackQuestionIds(questionIds: number[]) {
  const { data } = await request.get<unknown, ApiResponse<number[]>>('/feedbacks/my', {
    params: { questionIds: questionIds.join(',') },
  })
  return data
},

// 管理员：获取反馈列表
async loadFeedbacks(status?: string) {
  const params: Record<string, string> = {}
  if (status) params.status = status
  const { data } = await request.get<unknown, ApiResponse<FeedbackListVO[]>>('/feedbacks', { params })
  return data
},

// 管理员：采纳
async resolveFeedback(feedbackId: number, questionRequest: Record<string, unknown>) {
  await request.put(`/feedbacks/${feedbackId}/resolve`, questionRequest)
},

// 管理员：驳回
async rejectFeedback(feedbackId: number, rejectReason: string) {
  await request.put(`/feedbacks/${feedbackId}/reject`, { rejectReason })
},
```

### 6.3 学生端 — 成绩详情页改造

文件：`src/views/student/StudentResultDetail.vue`

- 在 `onMounted` 中，获取所有 questionId 后，调用 `store.myFeedbackQuestionIds(questionIds)` 获取已反馈列表
- 每道题的答题卡片右上角添加反馈按钮：

```html
<el-button
  v-if="!isFeedbackSubmitted(answer.questionId)"
  size="small"
  type="warning"
  plain
  @click="openFeedbackDialog(answer)"
>反馈纠错</el-button>
<el-tag v-else size="small" type="info">已反馈</el-tag>
```

- 反馈对话框：

```html
<el-dialog v-model="feedbackVisible" title="题目纠错反馈" width="500px">
  <p style="margin-bottom:12px"><strong>{{ feedbackTarget?.questionContent }}</strong></p>
  <el-form label-position="top">
    <el-form-item label="反馈类型">
      <el-select v-model="feedbackForm.feedbackType">
        <el-option label="答案错误" value="answer_error" />
        <el-option label="题干错误" value="content_error" />
        <el-option label="选项错误" value="option_error" />
        <el-option label="其他问题" value="other" />
      </el-select>
    </el-form-item>
    <el-form-item label="详细说明">
      <el-input v-model="feedbackForm.description" type="textarea" :rows="4" placeholder="请描述题目哪里有问题" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="feedbackVisible = false">取消</el-button>
    <el-button type="primary" @click="submitFeedback">提交反馈</el-button>
  </template>
</el-dialog>
```

### 6.4 管理员端 — 反馈管理页面

新建文件：`src/views/admin/AdminFeedbacks.vue`

- 路由：`/admin/feedbacks`
- 顶部标签切换：全部 / 待处理 / 已采纳 / 已驳回（用 `el-tabs`）
- 表格列：反馈ID、题目内容（可点击预览）、反馈学生、反馈类型、状态、提交时间、操作
- 操作列按钮：
  - **查看** → Drawer 显示详情
  - **修改并采纳** → 弹出修改题目对话框（复用类似 AdminExams 的编辑逻辑，调整 `questionContent`、`optionA-D`、`correctAnswer`、`score`），确认后调 `PUT /api/feedbacks/{id}/resolve`
  - **驳回** → 弹出输入驳回理由的对话框，确认后调 `PUT /api/feedbacks/{id}/reject`

侧边栏新增菜单项（`AdminLayout.vue`）：

```html
<el-menu-item index="/admin/feedbacks">
  <el-icon><ChatDotRound /></el-icon>
  <span>反馈管理</span>
</el-menu-item>
```

需要引入 `ChatDotRound` 图标（从 `@element-plus/icons-vue` 导入）。

## 7. 路由更新

### 前端路由

文件：`src/router/index.ts`，新增管理员路由：

```typescript
{
  path: '/admin/feedbacks',
  name: 'AdminFeedbacks',
  component: () => import('@/views/admin/AdminFeedbacks.vue'),
}
```

### 后端无需新增路由前缀 `/api/feedbacks` 已在 JWT 拦截生效范围内。

## 8. 文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `sql/init.sql` | 修改 | 新增 `CREATE TABLE question_feedback` |
| `.../entity/QuestionFeedback.java` | 新增 | 反馈实体 |
| `.../vo/FeedbackListVO.java` | 新增 | 列表视图 |
| `.../vo/FeedbackDetailVO.java` | 新增 | 详情视图 |
| `.../dto/FeedbackCreateRequest.java` | 新增 | 创建反馈请求体 |
| `.../dto/FeedbackRejectRequest.java` | 新增 | 驳回请求体（可选） |
| `.../mapper/QuestionFeedbackMapper.java` | 新增 | Mapper 接口 |
| `.../resources/mapper/QuestionFeedbackMapper.xml` | 新增 | XML 映射（可选） |
| `.../service/FeedbackService.java` | 新增 | 服务层 |
| `.../controller/FeedbackController.java` | 新增 | 控制器 |
| `src/stores/exam.ts` | 修改 | 新增类型定义和 actions |
| `src/router/index.ts` | 修改 | 新增 `/admin/feedbacks` 路由 |
| `src/views/admin/AdminFeedbacks.vue` | 新增 | 管理员反馈管理页面 |
| `src/views/admin/AdminLayout.vue` | 修改 | 侧边栏新增菜单 |
| `src/views/student/StudentResultDetail.vue` | 修改 | 每道题新增反馈按钮 |
| `docs/change-summary/...` | 新增 | 本功能总结文档 |

## 9. 验证方式

1. 启动后端 + 前端
2. 学生账号登录 → 参加考试 → 成绩详情页 → 点击「反馈纠错」→ 填写并提交
3. 切换至管理员账号 → 侧边栏「反馈管理」→ 看到新反馈
4. 点击「修改并采纳」→ 修改题目内容或答案 → 保存
5. 确认题库题目已更新，反馈状态变为「已采纳」
6. 或点击「驳回」→ 填写理由 → 反馈状态变为「已驳回」

## 10. 风险与后续优化

- 反馈建议加**通知机制**（管理员登录时未读反馈计数 badge）
- 同一个题同一个学生**只能反馈一次**（通过 `myFeedbackQuestionIds` 限制）
- 管理员修改题目后，受影响的考试答案可能需要重新评分（后续优化）
- 可以增加**反馈处理统计**：今日新增、平均处理时长等
