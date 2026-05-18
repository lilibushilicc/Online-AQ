# Timer History Lazyload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为学生答题页增加倒计时与超时自动交卷，为考试历史记录补充操作人姓名，并通过路由懒加载和手动分包降低前端首包体积。

**Architecture:** 前端倒计时逻辑集中在学生答题页，通过考试时长与结束时间共同计算剩余秒数，并在超时后直接调用现有交卷接口。后端在查询考试历史时基于 `operatorId` 反查用户真实姓名并补充到返回结果。前端路由改为按页面动态导入，Vite 构建配置增加手动分包，将 Vue、Element Plus 与业务代码拆开。

**Tech Stack:** Vue 3、Vue Router、Pinia、Vite、Spring Boot、MyBatis-Plus、PostgreSQL

---

### Task 1: 答题倒计时与自动交卷

**Files:**
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\views\student\StudentExamDetail.vue`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\stores\exam.ts`

- [ ] 在学生答题页增加剩余秒数、倒计时格式化、已用时计算和自动交卷保护状态。
- [ ] 使用考试时长和 `endTime` 计算允许作答的剩余秒数，组件卸载时清理定时器。
- [ ] 在剩余秒数归零时直接触发交卷，不再弹确认框，并为未作答题保留空答案。
- [ ] 手动交卷时将实际用时传给后端，避免成绩记录始终为 0 秒。

### Task 2: 历史记录补操作人姓名

**Files:**
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-spring\src\main\java\com\example\olineaqspring\entity\ExamHistory.java`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-spring\src\main\java\com\example\olineaqspring\service\ExamService.java`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\stores\exam.ts`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\views\admin\AdminExams.vue`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\views\admin\AdminResults.vue`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\views\student\StudentResultDetail.vue`

- [ ] 在考试历史实体中增加非持久化字段 `operatorName`。
- [ ] 后端查询历史记录时根据 `operatorId` 批量或逐条补充 `realName`。
- [ ] 前端历史记录展示中同时显示动作类型、操作人姓名和时间。

### Task 3: 路由懒加载与构建分包

**Files:**
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\src\router\index.ts`
- Modify: `D:\study\Systemlocation\Online-AQ\Oline-AQ-vue\vite.config.ts`

- [ ] 将管理端、学生端和登录页组件改为动态导入。
- [ ] 为 Vue、Element Plus 和通用依赖配置手动分包。
- [ ] 重新执行前端构建，确认 chunk 警告消失或明显改善。

### Task 4: 验证与文档

**Files:**
- Modify: `D:\study\Systemlocation\Online-AQ\README.md`
- Create: `D:\study\Systemlocation\Online-AQ\docs\change-summary\2026-05-17-xxxx-timer-history-lazyload-summary.md`

- [ ] 执行 `npm run build` 和 `.\mvnw.cmd test`。
- [ ] 更新 README 中关于考试历史、答题体验和构建方式的说明。
- [ ] 生成符合项目规范的改动总结文档。
