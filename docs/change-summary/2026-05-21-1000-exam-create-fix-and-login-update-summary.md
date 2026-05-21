# 试卷创建修复与登录页更新总结

## 1. 改动目标

- 修复试卷创建失败问题（服务端 null score 导致 NPE）
- 修复编辑试卷时自定义分值未正确加载的 bug
- 删除登录页管理员入口，改为点击左上角 logo 三次弹出管理员登录对话框
- 优化试卷创建时的前端错误提示

## 2. 用户视角

### 试卷创建
之前：选择题目后点击"保存试卷"可能无反应或静默失败（服务端 NPE 导致事务回滚）。
现在：创建成功或失败均有明确消息提示。

### 登录页
之前：登录页有"教师"和"学生"两个身份选项，教师可直接选择登录。
现在：登录页默认只显示学生登录表单，左上角新增品牌 logo，连续点击 logo 三次弹出管理员登录对话框。

## 3. 前端实现

- **LoginView.vue**：移除 `el-segmented` 角色选择器，`form.role` 默认值改为 `'student'`；新增 `handleLogoClick` 函数跟踪点击次数（2 秒内连续点击 3 次触发），弹出 `el-dialog` 管理员登录表单
- **AdminPapers.vue**：`savePaper` 新增 try-catch 错误处理；`editPaper` 修复 NaN 判断 bug，改为从 `detail.relationScores` 加载自定义分值
- **types/index.ts**：`ExamDetail` 新增 `serverTime` 字段

## 4. 后端实现

- **ExamService.java**：`calculateTotalScore` 中 `score` 为 null 时兜底为 `BigDecimal.ZERO`，避免 NPE；`create` 和 `update` 中写入 `exam_question.score` 时同样做 null 防护

## 5. 数据库与数据流

无表结构变更。`question.score` 列允许 NULL，之前未设分值的题目在创建试卷时会导致 `calculateTotalScore` 抛出 NPE，从而回滚整个事务。现在 null 分值按 0 处理。

## 6. 接口说明

无接口变更。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../service/ExamService.java` | 修复 null score NPE |
| `Oline-AQ-vue/src/views/admin/AdminPapers.vue` | 修复编辑时自定义分值加载、保存时错误处理 |
| `Oline-AQ-vue/src/views/login/LoginView.vue` | 重构登录页，移除教师入口，添加 logo 三次点击 |
| `Oline-AQ-vue/src/types/index.ts` | ExamDetail 补充 serverTime 字段 |
| `Oline-AQ-vue/src/style.css` | 添加 logo 样式 |
| `README.md` | 追加更新记录 |

## 8. 验证方式

```sh
# 1. 试卷创建
向 question 表插入一条 score=NULL 的题目，在前端创建试卷时选择该题，应成功创建。
# 2. 登录页
打开登录页，左上角应显示 "O◈AQ" logo，点击三次弹出管理员登录对话框。
# 3. 编辑试卷
创建含自定义分值的试卷草稿后编辑，自定义分值应正确回显。
```

## 9. 风险与后续优化

- logo 暂时使用文本 `O◈AQ`，后续可替换为实际图片
- 三次点击检测窗口设为 2 秒，可根据需要调整
- 管理员隐藏入口仅前端隐藏，后端 API 仍接受 `role: admin` 的登录请求
