# 功能优化 P-A~P-C 改动总结

## 1. 改动目标

提升成绩详情和错题本之间的联动体验，以及多次重考时的信息清晰度。

## 2. 用户视角

- **P-A**：查看成绩详情时，每道错题旁出现「加入错题本」按钮 → 弹窗选择错题本 → 确认后加入，不再需要退出去错题本页手动找题
- **P-B**：提交试卷后，若有错题则弹窗提示「您有 X 道错题，是否立即查看并加入错题本？」
- **P-C**：成绩列表每行显示「第 N 次提交」，考试卡片的历史标签也显示「第 1 次 / 第 2 次…」

## 3. 前端实现

### P-A：成绩详情「加入错题本」

- `StudentResultDetail.vue`：
  - 新增 `notebookDialogVisible`、`notebookTarget`、`selectedNotebookId` 响应式状态
  - 错题行增加「加入错题本」按钮，仅对 `isCorrect=false` 且未添加过时显示
  - 点击打开弹窗 → `el-select` 选择目标错题本 → 调用 `api.addToNotebookApi()` → 标记 `addedToNotebook`
  - `onMounted` 同时加载错题本列表（`api.loadNotebooksApi()`）
- `types/index.ts`：`ResultAnswerDetail` 新增 `answerId` 字段
- `AnswerMapHelper.java`：`toAnswerMap()` 不再条件判断，始终返回 `answerId`

### P-B：提交后弹窗提示错题

- `StudentExamDetail.vue`：
  - `handleSubmit()` 中提交成功后，若 `result.wrongCount > 0`，调用 `ElMessageBox.confirm` 弹窗
  - 确认 → 跳转到成绩详情页（可操作添加错题本）
  - 取消 → 跳转到考试列表

### P-C：第 N 次提交标识

- `StudentResults.vue`：
  - 新增 `attemptMap` computed，按 `examId` 分组 → `submitTime` 升序 → 生成 `resultId → 第 N 次` 映射
  - 考试名称旁显示「第 N 次提交 · 时间」
- `StudentExams.vue`：
  - `getExamHistory()` 改为按 `submitTime` 升序返回
  - 新增 `attemptLabel()`，多个提交时显示「第 N 次」
  - 可参加考试的最近 3 次历史标签 + 历史试卷的全部标签均显示「第 N 次」

## 4. 后端实现

- `AnswerMapHelper.java`：`toAnswerMap()` 始终包含 `answerId` 字段

## 5. 数据库与数据流

无数据库变更。

## 6. 接口说明

无接口变更。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/views/student/StudentResultDetail.vue` | P-A：加入错题本按钮 + 弹窗 |
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | P-B：提交后错题弹窗 |
| `Oline-AQ-vue/src/views/student/StudentResults.vue` | P-C：第 N 次序号 |
| `Oline-AQ-vue/src/views/student/StudentExams.vue` | P-C：第 N 次序号 |
| `Oline-AQ-vue/src/types/index.ts` | ResultAnswerDetail 加 answerId |
| `Oline-AQ-spring/.../utils/AnswerMapHelper.java` | 始终返回 answerId |

## 8. 验证方式

```sh
cd Oline-AQ-spring && mvnw compile -q
cd Oline-AQ-vue && npm run type-check
```

后端编译 + 前端类型检查通过。

## 9. 风险与后续优化

- 错题本弹窗需要用户已创建过错题本，若用户无错题本则 select 为空，后续可增加「新建错题本」行内入口
- P-B 弹窗目前只有「查看错题」和「稍后再说」两个选项，后续可加「全部自动加入默认错题本」
