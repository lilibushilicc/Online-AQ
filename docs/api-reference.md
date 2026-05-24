# 接口参考文档

当前系统共 83 个接口，按模块分类如下。

## 认证与注册

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` | 登录 |
| `POST /api/auth/register/send-code` | 发送邮箱注册验证码 |
| `POST /api/auth/register` | 邮箱注册 |

## 用户管理

| 接口 | 说明 |
|---|---|
| `GET /api/users` | 管理员：学生列表 |
| `GET /api/users/me` | 获取当前用户信息 |
| `PUT /api/users/me` | 修改个人信息 |
| `GET /api/users/{userId}` | 查询单个用户 |
| `POST /api/users` | 新增用户 |
| `PUT /api/users/{userId}` | 修改用户 |
| `DELETE /api/users/{userId}` | 删除用户 |

## 题库管理

| 接口 | 说明 |
|---|---|
| `GET /api/questions` | 题目列表（支持关键词/题型/分类筛选） |
| `GET /api/questions/categories` | 获取所有分类 |
| `POST /api/questions` | 新增题目 |
| `PUT /api/questions/{questionId}` | 修改题目 |
| `DELETE /api/questions/{questionId}` | 删除题目 |
| `POST /api/questions/batch-delete` | 批量删除 |
| `POST /api/questions/batch-score` | 批量设置分值 |
| `POST /api/questions/batch-category` | 批量设置分类 |
| `GET /api/questions/export` | 导出题目 |

## 考试管理

| 接口 | 说明 |
|---|---|
| `POST /api/exams` | 创建考试 |
| `GET /api/exams` | 考试列表（管理员） |
| `GET /api/exams/student` | 学生端考试列表 |
| `GET /api/exams/{examId}` | 考试详情（学生端脱敏正确答案，支持 `attemptId` 会话） |
| `PUT /api/exams/{examId}` | 编辑考试 |
| `PUT /api/exams/{examId}/settings` | 更新考试设置（乱序等） |
| `PUT /api/exams/{examId}/publish` | 发布考试 |
| `PUT /api/exams/{examId}/close` | 关闭考试 |
| `DELETE /api/exams/{examId}` | 删除考试 |
| `GET /api/exams/{examId}/history` | 考试操作历史 |
| `GET /api/exams/{examId}/export/excel` | 导出成绩 Excel |
| `GET /api/exams/{examId}/export/word` | 导出试卷 Word |
| `PUT /api/exams/{examId}/draft` | 保存当前作答草稿 |
| `GET /api/exams/{examId}/draft` | 读取当前作答草稿 |
| `DELETE /api/exams/{examId}/draft` | 清除当前作答草稿 |
| `POST /api/exams/{examId}/submit` | 提交答卷 |

## 成绩管理

| 接口 | 说明 |
|---|---|
| `GET /api/results/my` | 我的成绩列表 |
| `GET /api/results/exam/{examId}` | 某场考试所有成绩（仅管理员） |
| `GET /api/results/{resultId}` | 成绩详情（含答题明细） |
| `GET /api/results/wrong-questions` | 学生错题本 |
| `GET /api/results/export/{examId}` | 导出考试全部成绩 Excel |
| `GET /api/results/export/my` | 导出我的成绩 Excel |
| `GET /api/results/export/wrong` | 导出错题本 Excel |
| `GET /api/results/export/{resultId}/detail` | 导出答题明细 Excel |
| `GET /api/results/export/{resultId}/detail/word` | 导出答题明细 Word |

## 题目反馈

| 接口 | 说明 |
|---|---|
| `POST /api/feedbacks` | 提交反馈 |
| `GET /api/feedbacks` | 反馈列表（管理员） |
| `GET /api/feedbacks/{id}` | 反馈详情 |
| `PUT /api/feedbacks/{id}/resolve` | 采纳并修改题目 |
| `PUT /api/feedbacks/{id}/reject` | 驳回反馈 |
| `GET /api/feedbacks/my` | 查询当前学生已反馈题目 |

## 错题本

| 接口 | 说明 |
|---|---|
| `GET /api/wrong-notebooks` | 错题本列表 |
| `POST /api/wrong-notebooks` | 新建错题本 |
| `GET /api/wrong-notebooks/{notebookId}` | 错题本详情 |
| `PUT /api/wrong-notebooks/{notebookId}` | 修改错题本 |
| `DELETE /api/wrong-notebooks/{notebookId}` | 删除错题本 |
| `GET /api/wrong-notebooks/{notebookId}/export` | 导出错题本 Excel |
| `POST /api/wrong-notebooks/{notebookId}/items` | 添加错题 |
| `DELETE /api/wrong-notebooks/{notebookId}/items/{itemId}` | 移除错题 |

## 公告管理

| 接口 | 说明 |
|---|---|
| `GET /api/announcements` | 公告列表 |
| `GET /api/announcements/{id}` | 公告详情 |
| `POST /api/announcements` | 发布公告 |
| `PUT /api/announcements/{id}` | 修改公告 |
| `DELETE /api/announcements/{id}` | 删除公告 |
| `GET /api/announcements/unread` | 未读公告数 |
| `POST /api/announcements/{id}/read` | 标记已读 |
| `POST /api/announcements/read-all` | 全部标记已读 |

## 文件管理

| 接口 | 说明 |
|---|---|
| `GET /api/files` | 文件列表 |
| `POST /api/files/upload` | 上传文件（题库导入） |
| `POST /api/files/{fileId}/parse` | 解析文件中的试题 |
| `DELETE /api/files/{fileId}` | 删除文件 |

## 系统配置

| 接口 | 说明 |
|---|---|
| `GET /api/config` | 获取全部配置（仅管理员） |
| `PUT /api/config` | 更新配置 |
| `GET /api/config/public/login` | 获取登录页公开配置 |
| `POST /api/config/test-r2` | 测试 R2 连接 |
| `POST /api/config/test-email` | 测试邮箱发送 |
| `POST /api/config/test-ai` | 测试 AI 接口连接 |
| `POST /api/config/migrate-exam-shuffle` | 迁移考试乱序设置 |
| `GET /api/config/smtp-accounts` | SMTP 账号列表 |
| `POST /api/config/smtp-accounts` | 新增 SMTP 账号 |
| `PUT /api/config/smtp-accounts/{id}` | 修改 SMTP 账号 |
| `DELETE /api/config/smtp-accounts/{id}` | 删除 SMTP 账号 |
| `PUT /api/config/smtp-accounts/{id}/activate` | 激活 SMTP 账号 |
| `PUT /api/config/smtp-accounts/{id}/toggle-enabled` | 启用/禁用 SMTP 账号 |
| `GET /api/config/smtp-accounts/{id}/stats` | SMTP 发送统计 |
| `POST /api/config/smtp-accounts/test` | 测试指定 SMTP 账号 |
| `GET /api/config/email-stats` | 全局邮件发送统计 |

## App 版本管理

| 接口 | 说明 |
|---|---|
| `GET /api/app/version/latest` | 获取最新 App 版本信息（versionCode/versionName/downloadUrl/releaseNotes/forceUpdate） |
