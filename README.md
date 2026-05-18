# 智能在线答题系统

本项目是一个前后端分离的在线答题系统，覆盖教师出题、考试发布、学生答题、自动评分、成绩明细和历史记录查看等流程。

- 前端：`Oline-AQ-vue/`
  - 技术栈：Vue 3、Vite、Element Plus、Pinia、Axios
- 后端：`Oline-AQ-spring/`
  - 技术栈：Spring Boot、MyBatis-Plus、JWT、PostgreSQL

## 当前已支持的核心功能

- 教师上传题库并解析试题
- 题库按关键词、题型、来源筛选
- 题库批量删除、批量设置分值
- 创建考试时设置开始时间、结束时间、是否允许重考
- 发布、关闭考试，并记录考试历史
- 学生仅在开放时间内参加考试
- 学生答题页显示倒计时，并在超时后自动交卷
- 防止“仅允许一次提交”的考试重复交卷
- 考试历史记录展示操作人姓名，而不只显示 `operatorId`
- 学生查看成绩历史、单次答题明细和考试历史记录
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
