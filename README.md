# 智能在线答题系统

> 2026-05-23 更新：Android 学生端 App 全面 Mihon 风格 UI 改造。迁移至 Catppuccin Mocha/Latte 配色系统 + Material 3 主题，支持亮色/暗色/AMOLED 纯黑模式切换。Drawer 导航替换为底部导航栏（考试/成绩/更多）。考试列表改用双列网格卡片布局（顶部渐变色条）。答题页面重构为单题 ViewPager2 阅读器模式（滑动/按钮翻页 + 进度条 + 计时器）。所有页面启用 Edge-to-edge 沉浸显示。
> 2026-05-22 更新：Android 学生端 App 完善。修改 BASE_URL 指向部署地址 `http://39.98.69.153:8084/`，所有 Activity 使用 lifecycleScope 防止内存泄漏，考试列表工具栏点击打开侧边抽屉，成绩列表新增下拉刷新，列表空状态展示，数据模型修复可空字段容错。
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
> 2026-05-16 更新：前端已抽取共享导航与会话持久化工具，统一复用路由跳转、登出清理和 `localStorage` 读写逻辑，减少重复代码。

本项目是一个前后端分离的在线答题系统，覆盖教师出题、考试发布、学生答题、自动评分、成绩明细和历史记录查看等流程。

- 前端：`Oline-AQ-vue/`
  - 技术栈：Vue 3、Vite、Element Plus、Pinia、Axios
- 后端：`Oline-AQ-spring/`
  - 技术栈：Spring Boot、MyBatis-Plus、JWT、PostgreSQL

## 当前已支持的核心功能

### 教师/管理员端

- 上传题库文件（TXT/DOCX），支持正则解析和 **AI 智能解析**（兼容 OpenAI/DeepSeek），可动态切换，兼容章节式无编号文本
- **系统设置**：配置存储方式（本地/R2）、AI 接口参数、登录方式、邮箱 SMTP（支持多账号管理）
- **题库管理**：按关键词/题型/分类/文件筛选，批量删除、批量设置分值、批量设置分类
- **试卷管理**：组卷草稿，自定义每道题分值
- **考试管理**：创建、编辑、发布、关闭考试，设置时间窗口/重考策略/题目选项乱序
- **成绩查看**：查看某场考试全部学生成绩、单次提交详情、考试操作历史
- **学生管理**：学生账号 CRUD，显示邮箱和注册时间
- **公告管理**：发布、编辑、删除系统公告
- **反馈管理**：查看学生提交的题目纠错反馈，采纳修改或驳回
- **个人中心**：修改个人资料和密码
- **数据导出**：成绩列表、试卷、答题明细导出 Excel/Word

### 学生端

- **在线考试**：在开放时间内参加考试，倒计时基于服务端时间校准，超时自动交卷，防止重复提交
- **草稿自动保存**：作答自动保存到 localStorage，刷新/关闭页面可恢复
- **题目/选项乱序**：同一考试不同乱序版本，有效防作弊
- **在线随机练习**：随机抽题练习模式
- **成绩历史**：查看历史考试成绩和单次答题明细
- **错题本**：多本管理，卡片网格布局，按考试/题目分类展示
- **题目纠错反馈**：在成绩详情页标记题目并发起纠错反馈
- **邮箱自助注册**：通过邮箱验证码完成注册后自动登录
- **个人中心**：查看和编辑个人资料、修改密码
- **数据导出**：错题本、成绩、答题明细导出 Excel/Word

### 系统特性

- 路由懒加载 + Element Plus 按需注册 + 模块分包，构建无 500kB chunk 告警
- 前后端分离，JWT 认证，角色权限控制（admin / student）

## 目录结构

```text
Online-AQ
├─ docs/                   项目文档
│  ├─ api-reference.md     接口参考文档（83 个接口）
│  ├─ change-summary/      代码改动总结文档
│  ├─ question-feedback-feature-design.md  题目纠错反馈功能设计文档
│  └─ superpowers/plans/   实施计划文档
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

系统共 83 个接口，按功能模块分类。完整列表见 [`docs/api-reference.md`](docs/api-reference.md)。

主要模块：认证注册、用户管理、题库管理、考试管理、成绩管理、题目反馈、错题本、公告管理、文件管理、系统配置。

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
