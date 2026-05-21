# 智能在线答题系统

> 2026-05-21 更新：修复试卷创建失败（question.score 为 null 时 calculateTotalScore NPE），编辑试卷时正确加载自定义分值。登录页移除教师身份选择入口，左上角添加品牌 logo，点击三次弹出管理员登录对话框。
> 2026-05-20 更新：新增系统公告功能。管理员可在后台发布和管理公告，学生登录后自动弹窗通知，右上角铃铛图标显示未读数量并支持查看全部公告。
> 2026-05-20 更新：修复 Word/Excel 导出功能无法正常下载的问题：后端所有导出接口使用 RFC 5987 标准编码中文文件名，前端修复 Blob URL 过早释放和 Axios 拦截器对 Blob 错误响应的处理，ExportService 增加 null 安全防护。
> 2026-05-20 更新：新增个人中心功能，支持学生和管理员查看/编辑个人资料（姓名、邮箱）和修改密码；管理员学生管理页补充邮箱和注册时间列。后端新增 GET/PUT /api/users/me 接口，前端新增 StudentProfile 页面和路由入口。
> 2026-05-20 更新：系统级代码优化，后端抽取 checkOwner/updateFeedbackStatus/buildOrderedWrapper/validateEmailRegistration 等共享方法消除 10+ 处重复代码，修复 N+1 查询；前端删除 store 32 个透传 action 改为组件直接调用 api，创建 useDataSource 组合函数统一加载/错误处理，cleanParams 函数合并参数清洗逻辑，修复因类型推断暴露的预存类型错误。
> 2026-05-20 更新：新增邮箱学生注册功能。管理员可在后台配置页开启/关闭邮箱注册，设置 SMTP 服务器用于发送验证码。学生在登录页可切换至注册表单，通过邮箱验证码完成注册后自动登录。
> 2026-05-19 更新：Element Plus 改为全量导入，去除 40+ 组件手工注册（减少 67 行）。清理 5 处未使用 import、删除嵌套仓库副本 Online-AQ/ 和测试文件、将 task_plan.md 移入 WillPlan/。
> 2026-05-19 更新：考试作答支持自动保存草稿到 localStorage，刷新/关闭页面可恢复；倒计时改为服务端时间校准，防止篡改客户端时钟。页面新增统一的 v-loading 加载状态。后端 MyBatis-Plus 分页替代手动 SQL 拼接，新增 PaginationInnerInterceptor。运维配置拆分为 dev/prod profile，Vue DevTools 仅开发模式启用，添加 .env.production。
> 2026-05-19 更新：重构上传试题页布局，拆分为导入文件、解析策略、最近导入记录和格式参考四个区域，并收拢页面内解析模式与文件状态逻辑，提升可读性与维护性。
> 2026-05-19 更新：优化上传试题解析，支持无数字编号的章节式题库文本，自动跳过"一、选择题"等章节标题，并在 AI 漏题时回退到本地结构化解析。
> 2026-05-19 更新：上传试题页新增"AI 智能解析"开关，管理员可在系统设置页配置 AI 接口后，在上传时动态切换使用 AI 解析还是正则解析，兼容 OpenAI/DeepSeek 接口。
> 2026-05-19 更新：发布考试时可设置"题目乱序"和"选项乱序"，支持对同一考试发布不同乱序版本；学生答题时题目和选项顺序随机排列，有效防作弊。
> 2026-05-19 更新：错题本详情和成绩详情页新增导出功能，支持将错题和答题明细导出为 Excel。
> 2026-05-18 更新：R2 配置页新增"恢复已保存配置"和保存前确认，降低误填、误保存导致上传异常的风险。
> 2026-05-18 更新：水电费率维护已归入"低碳规则配置"页，保存费率后会同步影响规则预览、看板碳排换算和水电扣费逻辑；奖励管理页只保留奖励与 R2 存储配置。
> 2026-05-17 更新：管理员后台已支持直接读取、测试并保存奖励图片上传使用的 R2 配置；上传服务优先使用后台保存的配置，未配置时回退到环境变量。
> 2026-05-16 更新：奖励图片上传已从服务器本地磁盘切换为 Cloudflare R2。后端不再写入 `storage/reward-images/`，也不再提供 `/uploads/rewards/**` 本地静态访问，数据库继续仅保存 `image_url`。
> 2026-05-16 更新：前端已抽取共享导航与会话持久化工具，统一复用路由跳转、登出清理和 `localStorage` 读写逻辑，减少重复代码。

本项目是一个前后端分离的在线答题系统，覆盖教师出题、考试发布、学生答题、自动评分、成绩明细和历史记录查看等流程。

- 前端：`Oline-AQ-vue/`
  - 技术栈：Vue 3、Vite、Element Plus、Pinia、Axios
- 后端：`Oline-AQ-spring/`
  - 技术栈：Spring Boot、MyBatis-Plus、JWT、PostgreSQL

## 当前已支持的核心功能

- 教师上传题库并解析试题（支持正则解析和 AI 智能解析，可动态切换，兼容章节式无编号文本）
- 后台系统设置页可配置存储方式（本地/R2）和 AI 解析参数
- 题库按关键词、题型、来源筛选
- 题库批量删除、批量设置分值
- 创建考试时设置开始时间、结束时间、是否允许重考
- **创建考试支持自定义每道题的分值**
- 发布、关闭考试，并记录考试历史
- 学生仅在开放时间内参加考试
- 学生答题页显示倒计时，并在超时后自动交卷
- 防止"仅允许一次提交"的考试重复交卷
- 考试历史记录展示操作人姓名，而不只显示 `operatorId`
- 学生查看成绩历史、单次答题明细和考试历史记录
- **学生端错题本：按考试分组展示所有错题**
- 教师查看某场考试的成绩列表、单次提交详情和考试历史
- 前端页面已改为路由懒加载，Element Plus 按需注册并按模块分包

## 目录结构

```text
Online-AQ
├─ docs/                   项目说明文档
│  └─ change-summary/      每次代码改动总结
├─ docs/superpowers/plans/ 实施计划文档
├─ log/                    日志与排查记录
├─ release/
│  ├─ docker/              Docker 相关文件与镜像构建产物
│  └─ package/             打包产物
├─ WillPlan/               后续规划与预研资料
├─ Oline-AQ-vue/           前端源码
└─ Oline-AQ-spring/        后端源码
```

## 后端启动

1. 创建数据库：

```sql
CREATE DATABASE exam_system;
```

2. 执行建表脚本：

```text
Oline-AQ-spring/sql/init.sql
```

3. 修改数据库连接配置：

```text
Oline-AQ-spring/src/main/resources/application.yml
```

4. 启动后端服务：

```sh
cd Oline-AQ-spring
.\mvnw.cmd spring-boot:run
```

默认地址：`http://localhost:8080`

后端启动时会自动补齐默认测试账号；如果 `sys_user` 表中缺少 `admin` 或 `2023001`，系统会自动创建。

## 前端启动

```sh
cd Oline-AQ-vue
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5173`

前端已配置 `/api` 代理到 `http://localhost:8080`。
如果本机 `8080` 被其他项目占用，可在 `Oline-AQ-vue/.env.development` 中通过 `VITE_API_BASE_URL` 指向当前后端地址，例如 `http://localhost:8081/api`。

## 测试账号

```text
教师账号：admin
教师密码：123456

学生账号：2023001
学生密码：123456
```

如你已修改过初始化数据，请以 `Oline-AQ-spring/sql/init.sql` 中的实际种子数据为准。

## 已实现接口

认证与文件：

- `POST /api/auth/login`
- `POST /api/files/upload`
- `POST /api/files/{fileId}/parse`

题库管理：

- `GET /api/questions`
- `POST /api/questions`
- `PUT /api/questions/{questionId}`
- `DELETE /api/questions/{questionId}`
- `POST /api/questions/batch-delete`
- `POST /api/questions/batch-score`

考试管理：

- `POST /api/exams`
- `GET /api/exams`
- `GET /api/exams/{examId}`
- `GET /api/exams/{examId}/history`
- `PUT /api/exams/{examId}/publish`
- `PUT /api/exams/{examId}/close`
- `POST /api/exams/{examId}/submit`

成绩管理：

- `GET /api/results/my`
- `GET /api/results/exam/{examId}`
- `GET /api/results/{resultId}`
- `GET /api/results/wrong-questions` — 学生错题本

## 构建与验证

后端测试：

```sh
cd Oline-AQ-spring
.\mvnw.cmd test
```

前端构建：

```sh
cd Oline-AQ-vue
npm run build
```

当前构建结果已经完成：

- 路由懒加载拆分为独立页面 chunk
- Vue 生态依赖拆分到 `vue-vendor`
- Element Plus 改为按需注册并拆分为多个模块 chunk
- 当前 `npm run build` 已无 500kB chunk 告警

## 文档约束

- 项目规则见根目录 `AGENTS.md`
- 每次代码改动后，必须在 `docs/change-summary/` 下新增一份改动总结
- 说明文档统一沉淀在根目录 `docs/`
- 日志文件统一存放在根目录 `log/`
