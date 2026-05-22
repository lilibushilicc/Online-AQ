# 代码改动总结目录

本目录用于保存每次代码改动后的 Markdown 总结文档。

## 命名规范

```text
YYYY-MM-DD-HHMM-功能名称-改动总结.md
```

示例：`2026-05-21-1000-exam-create-fix-and-login-update-summary.md`

## 内容要求

每份总结文档需覆盖以下维度：

- **改动目标**：为什么改
- **用户视角**：用户看到什么、操作流变化
- **前端实现**：页面、组件、路由、状态管理、请求封装
- **后端实现**：Controller、Service、Mapper、Entity、配置
- **数据库与数据流**：表结构变化、数据链路
- **接口说明**：请求参数、返回数据、调用关系
- **关键文件**：新增/修改的文件及职责
- **验证方式**：启动命令、测试步骤
- **风险与后续优化**：限制、潜在问题、可优化方向

## 文档列表（按日期倒序）

| 文件名 | 日期 | 功能 |
|---|---|---|
| `2026-05-21-1000-exam-create-fix-and-login-update-summary.md` | 05-21 | 修复试卷创建 NPE、登录页调整 |
| `2026-05-20-2236-announcement-feature-summary.md` | 05-20 | 系统公告功能 |
| `2026-05-20-2201-export-word-excel-fix-summary.md` | 05-20 | 修复 Word/Excel 导出 |
| `2026-05-20-1700-email-registration-summary.md` | 05-20 | 邮箱学生注册 |
| `2026-05-20-1510-user-profile-summary.md` | 05-20 | 个人中心 |
| `2026-05-20-1435-code-optimization-summary.md` | 05-20 | 代码优化 |
| `2026-05-20-1430-smtp-send-limits-summary.md` | 05-20 | SMTP 发送限制 |
| `2026-05-20-1400-smtp-accounts-summary.md` | 05-20 | 多 SMTP 账号管理 |
| `2026-05-20-1210-考试管理去重改动总结.md` | 05-20 | 考试管理与试卷管理职责分离 |
| `2026-05-19-2005-admin-upload-ui-refactor-summary.md` | 05-19 | 上传试题页 UI 重构 |
| `2026-05-19-2000-code-cleanup-summary.md` | 05-19 | 代码清理 |
| `2026-05-19-1945-ai-upload-parsing-fix-summary.md` | 05-19 | AI 上传解析优化 |
| `2026-05-19-1930-batch-improvements-summary.md` | 05-19 | 批量优化 |
| `2026-05-19-1745-online-question-bank-fix-summary.md` | 05-19 | 题库分类过滤修复 |
| `2026-05-19-1700-shuffle-questions-answers-summary.md` | 05-19 | 题目选项乱序 |
| `2026-05-19-1630-student-export-features-summary.md` | 05-19 | 学生导出功能 |
| `2026-05-19-1630-security-quality-performance-ui-improvements-summary.md` | 05-19 | 安全/性能/UI 优化 |
| `2026-05-19-1615-ai-question-parsing-summary.md` | 05-19 | AI 试题解析 |
| `2026-05-19-1600-custom-score-and-wrong-book-summary.md` | 05-19 | 自定义分值与错题本 |
| `2026-05-19-1044-wrong-notebook-multi-and-nav-optimization.md` | 05-19 | 错题本多本与导航优化 |
| `2026-05-18-2259-question-feedback-feature-summary.md` | 05-18 | 题目纠错反馈 |
| `2026-05-18-2253-frontend-ui-redesign-summary.md` | 05-18 | 前端 UI 重构 |
| `2026-05-18-2104-android-student-app-summary.md` | 05-18 | Android 学生端 App |
| `2026-05-18-1730-ui-optimization-summary.md` | 05-18 | UI 优化 |
| `2026-05-18-1725-question-category-feature-summary.md` | 05-18 | 题目分类功能 |
| `2026-05-17-2341-登录修复改动总结.md` | 05-17 | 登录修复 |
| `2026-05-17-2320-login-routing-fix-summary.md` | 05-17 | 路由配置修复 |
| `2026-05-17-2309-login-failure-fix-summary.md` | 05-17 | 测试账号补齐 |
| `2026-05-17-2307-login-error-fix-summary.md` | 05-17 | 错误处理统一 |
| `2026-05-17-2155-first-priority-features-and-history-summary.md` | 05-17 | 考试时间窗口与历史 |
| `2026-05-17-2139-code-optimization-summary.md` | 05-17 | 首次代码优化 |
| `2026-05-17-0013-project-document-rule-summary.md` | 05-17 | 文档规则建立 |
