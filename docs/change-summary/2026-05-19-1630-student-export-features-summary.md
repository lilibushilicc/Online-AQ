# 学生端导出功能扩充改动总结

## 1. 改动目标

学生端的"错题本详情"和"成绩详情"页面缺少导出功能，学生无法将错题和单次考试答题明细导出为 Excel 文件。本次改动为这两个页面添加了导出按钮和后端导出接口。

## 2. 用户视角

### 错题本详情页
- 在页面顶部操作栏新增"导出错题"按钮
- 全部错题视图：导出当前学生的所有错题（按考试分组）
- 具体错题本视图：导出该错题本内的所有错题
- 下载的文件名为 `错题导出_<日期>.xlsx` 或 `错题本导出_<日期>.xlsx`

### 成绩详情页
- 在"答题明细"卡片头部新增"导出详情"按钮
- 导出当前成绩记录的所有答题明细
- 文件名为 `成绩详情_<日期>.xlsx`

## 3. 前端实现

### 错题本详情页 (`StudentWrongBookDetail.vue`)
- 新增导入：`Download` 图标、`downloadFile` 工具函数
- 新增 `handleExport()` 函数：根据 `isAllView` 判断调用 `/api/results/export/wrong` 或 `/api/wrong-notebooks/{notebookId}/export`

### 成绩详情页 (`StudentResultDetail.vue`)
- 新增导入：`downloadFile` 工具函数
- 在 `el-card` header 区域新增"导出详情"按钮，调用 `/api/results/export/{resultId}/detail`

## 4. 后端实现

### `ExportService.java`
- 新增 `exportWrongQuestions(List<Map<String, Object>> groups)`：将错题数据（按考试分组）展平为 Excel 行，包含考试、题目、题型、选项、学生答案、正确答案、分值、提交时间等列
- 新增 `exportResultDetail(Map<String, Object> detail)`：将成绩详情的答题明细导出为 Excel，首行显示考试概要信息，后续行为每题明细

### `ResultController.java`
- 新增 `GET /api/results/export/wrong`：调用 `resultService.wrongQuestions()` 获取当前学生的全部错题数据，调用 `exportService.exportWrongQuestions()` 生成 Excel
- 新增 `GET /api/results/export/{resultId}/detail`：调用 `resultService.resultDetail()` 获取成绩详情，调用 `exportService.exportResultDetail()` 生成 Excel

### `WrongNotebookController.java`
- 新增依赖：`ExportService`
- 新增 `GET /api/wrong-notebooks/{notebookId}/export`：调用 `wrongNotebookService.getNotebookDetail()` 获取指定错题本数据，调用 `exportService.exportWrongQuestions()` 生成 Excel

## 5. 数据库与数据流

无数据库变更。导出数据来自现有查询接口：
- 错题数据：`student_answer` 表按 `studentId + isCorrect=false` 查询
- 成绩详情数据：`exam_result` + `student_answer` + `question` 表联查

## 6. 接口说明

| 接口 | 方法 | 作用 | 权限 | 返回 |
|---|---|---|---|---|
| `/api/results/export/wrong` | GET | 导出当前学生的全部错题 | 学生（JWT） | .xlsx 文件 |
| `/api/results/export/{resultId}/detail` | GET | 导出单次成绩的答题明细 | 学生/管理员（JWT） | .xlsx 文件 |
| `/api/wrong-notebooks/{notebookId}/export` | GET | 导出指定错题本内的错题 | 学生（JWT） | .xlsx 文件 |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../service/ExportService.java` | 新增 `exportWrongQuestions` 和 `exportResultDetail` 方法 |
| `Oline-AQ-spring/.../controller/ResultController.java` | 新增 `/export/wrong` 和 `/export/{resultId}/detail` 接口 |
| `Oline-AQ-spring/.../controller/WrongNotebookController.java` | 新增 `/export` 接口，注入 ExportService |
| `Oline-AQ-vue/.../student/StudentWrongBookDetail.vue` | 新增"导出错题"按钮和 `handleExport` 函数 |
| `Oline-AQ-vue/.../student/StudentResultDetail.vue` | 新增"导出详情"按钮 |

## 8. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring
.\mvnw.cmd compile -q

# 前端类型检查
cd Oline-AQ-vue
npm run type-check
```

两个命令均通过，无报错。

## 9. 风险与后续优化

- 无已知风险，导出功能基于已有数据查询接口，不涉及数据写操作
- 后续可考虑：导出时支持选择导出列、支持 CSV 格式导出
