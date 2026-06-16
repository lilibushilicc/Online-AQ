# 智能在线答题系统

> 2026-06-16 更新：重新打包前后端；修复 TypeScript 类型错误；前端包管理器切换为 pnpm
> 2026-05-23 更新：打包前后端（Vue + Spring Boot）；Android 个人中心新增「检查版本更新」按钮
> 2026-05-23 更新：管理后台新增「版本更新」配置页面，支持在线配置 App 版本更新信息
> 2026-05-23 更新：修复在线练习学生无法打开的问题（QuestionController @AdminOnly 类级别权限误拦）
> 2026-05-23 更新：[查看完整更新日志](docs/CHANGELOG.md)

`Online-AQ` 是一个面向教学与练习场景的智能在线答题系统，提供从题目录入、组卷发布、学生作答、自动评分/人工评分，到成绩复盘、错题沉淀、题目反馈修正的完整闭环。

## 系统架构

```
┌──────────────────────────────────────────────────┐
│                    用户访问层                      │
│                                                    │
│  Web 管理端 (Vue3)     Web 学生端 (Vue3)           │
│  :5173 (dev)           :5173 (dev)                 │
│  ┌──────────────┐     ┌──────────────┐            │
│  │ Element Plus  │     │  Vant (移动) │            │
│  │ Pinia / Axios │     │  Pinia/Axios│            │
│  └──────┬───────┘     └──────┬───────┘            │
│         │                    │                    │
│         └────────┬───────────┘                    │
│                  │ HTTP /api 代理                  │
│                  ▼                                 │
│  Android 学生端 (Kotlin)                           │
│  ├─ Retrofit2 + OkHttp                            │
│  ├─ JWT Bearer 自动注入                            │
│  └─ 运行时动态切换服务器地址                         │
│         │                                          │
│         │ HTTP/JSON                                │
└─────────┼──────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────┐
│              后端服务 (Spring Boot 3.3.5)          │
│                                                    │
│  ├─ 端口 :8880                                     │
│  ├─ JWT 鉴权                                       │
│  ├─ 管理员注解 @AdminOnly                           │
│  ├─ 草稿 & 会话 attemptId                           │
│  ├─ 简答题人工评分                                  │
│  ├─ 并发幂等与限流 AOP                              │
│  ├─ LocalCache (Caffeine)                           │
│  └─ 启动时自动 DDL 迁移 (SchemaInitializer)         │
│                                                    │
├────────────────────┬───────────────────────────────┤
│ 对象存储 (R2/S3)   │  AI 批改 (DeepSeek)           │
│  ├─ 题目文件上传    │  ├─ 简答题智能评分             │
│  └─ 文件管理        │  └─ 配置化模型切换             │
└────────────────────┴───────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────┐
│             PostgreSQL 数据库                       │
│                                                    │
│  数据库名: exam_system                             │
│  默认端口: 5432                                    │
│  21 张表 / 44 个索引                               │
│  自动迁移: SchemaInitializer.java                   │
└──────────────────────────────────────────────────┘
```

## 前置环境要求

| 组件 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 运行后端服务 |
| Maven | 3.8+（可用 `mvnw` wrapper） | 构建后端 |
| Node.js | ^20.19.0 或 >=22.12.0 | 构建 Web 前端 |
| pnpm | 10+ | 前端依赖管理 |
| PostgreSQL | 12+ | 业务数据库 |
| Android Studio | Hedgehog+ (2023.1.1) | 构建 Android 端（可选） |
| Gradle | 8.14.3（自动下载） | 构建 Android 端（可选） |

## 项目结构

```
Online-AQ/
│
├── Oline-AQ-vue/              Web 前端（Vue 3 + Vite + Element Plus + Vant）
│   └── README.md              前端详细说明
│
├── Oline-AQ-spring/           Spring Boot 后端（Java 17 + MyBatis-Plus）
│   └── sql/                   SQL 迁移脚本
│   └── README.md              后端详细说明
│
├── Oline-AQ-app/              Android 学生端（Kotlin + Material 3 + Retrofit）
│   └── README.md              Android 端详细说明（含签名配置）
│
├── init.sql                   数据库完整初始化脚本
│
├── docs/                      项目文档
│   ├── 项目整体说明.md         全局功能与设计说明
│   ├── 考试模块.md             考试模块详细设计
│   ├── 学生端移动应用.md        Android 端设计说明
│   ├── api-reference.md        接口参考
│   ├── CHANGELOG.md            版本更新日志
│   └── change-summary/        每次代码改动后的总结文档
│
├── release/                   打包产物与部署文件
│   ├── package/                最新构建产物：后端 JAR + 前端 dist + 配置 + SQL
│   └── docker/                 Docker 相关文件（待完善）
│
├── log/                       运行日志 / 构建日志 / 排查记录
│
└── WillPlan/                  规划文档 / 待办清单 / 后续建设方案
```

## 主要功能

### 管理员/教师端（Web）

| 模块 | 功能 |
|------|------|
| 文件导入 | 上传 TXT/DOCX 解析题目入库 |
| 题库管理 | 筛选、批量设分、批量分类、单题编辑、导出 |
| 考试管理 | 创建/发布/关闭考试，设置时间/乱序/重考策略 |
| 成绩管理 | 查看成绩、详情、待评分简答题、Excel 导出 |
| 反馈处理 | 查看学生纠错反馈，采纳/驳回 |
| 系统配置 | 公告管理、用户管理、SMTP/AI/R2 存储配置 |
| 数据统计 | 仪表盘图表概览 |

### 学生端（Web + Android）

| 模块 | 功能 |
|------|------|
| 考试 | 查看可参加考试、在线作答、草稿自动保存恢复 |
| 成绩 | 查看历史成绩、每道题详情与正确答案 |
| 练习 | 随机抽题、即时评分 |
| 错题本 | 创建/管理错题本、按题型/关键词筛选 |
| 公告 | 查看系统公告 |
| 反馈 | 对题目提交纠错反馈 |
| 个人中心 | 修改资料、密码、服务器地址（Android） |

## 快速启动（开发环境）

### 1. 初始化数据库

```sh
# 创建数据库
psql -U postgres -c "CREATE DATABASE exam_system;"

# 执行初始化脚本
psql -U postgres -d exam_system -f init.sql
```

### 2. 启动后端

后端默认配置连接 `localhost:5432` 的 PostgreSQL，数据库 `exam_system`。

```sh
cd Oline-AQ-spring

# 直接启动（使用 Maven Wrapper）
.\mvnw.cmd spring-boot:run

# 或先编译后启动
.\mvnw.cmd -DskipTests package
java -jar target\oline-aq-spring-0.0.1-SNAPSHOT.jar
```

**默认地址**：`http://localhost:8880`

**默认数据库配置**（通过 `application-dev.yml` 的环境变量覆盖）：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/exam_system` | 数据库连接 |
| `DB_USER` | `postgres` | 数据库用户 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `JWT_SECRET` | `oline-aq-demo-secret-key-...` | JWT 签名密钥 |
| `JWT_EXPIRE_HOURS` | `24` | Token 过期小时数 |

### 3. 启动 Web 前端

```sh
cd Oline-AQ-vue
pnpm install
pnpm run dev
```

**默认地址**：`http://127.0.0.1:5173`

开发环境通过 Vite proxy 将 `/api` 请求代理到后端 `http://localhost:8880`，配置在 `.env.development`：

```env
VITE_API_BASE_URL=/api
API_PROXY_TARGET=http://localhost:8880
```

### 4. 构建 Android 学生端

```sh
cd Oline-AQ-app

# Debug APK（开发测试用）
.\gradlew.bat assembleDebug

# Release APK（发布用，需先配置签名）
.\gradlew.bat assembleRelease
```

APK 产物位置：
- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

> **Android 发布签名配置详见**：[Oline-AQ-app/README.md](Oline-AQ-app/README.md#配置发布签名)

## 默认测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin` | `123456` |
| 学生 | `2023001` | `123456` |

## 生产部署

### 后端（jar 包方式）

```sh
cd Oline-AQ-spring

# 构建生产包
.\mvnw.cmd clean package -DskipTests

# 运行（使用 prod profile，强制通过环境变量注入配置）
java -jar target\oline-aq-spring-0.0.1-SNAPSHOT.jar ^
  --spring.profiles.active=prod ^
  --DB_URL=jdbc:postgresql://<数据库地址>:5432/exam_system ^
  --DB_USER=<数据库用户> ^
  --DB_PASSWORD=<数据库密码> ^
  --JWT_SECRET=<替换为安全随机字符串> ^
  --JWT_EXPIRE_HOURS=24
```

生产环境配置严格区分开发和测试，所有敏感信息必须通过环境变量注入（`application-prod.yml` 中无默认值）。

### 前端（Nginx 部署）

```sh
cd Oline-AQ-vue
pnpm run build
# 产物在 dist/ 目录，部署到 Nginx
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /path/to/dist;
    index index.html;

    location /api/ {
        proxy_pass http://localhost:8880;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### Docker（待完善）

`release/docker/` 目录已预留，Docker 化部署方案即将补充。

---

## 常用命令速查

```sh
# 后端编译
cd Oline-AQ-spring && .\mvnw.cmd -DskipTests compile

# 后端测试
cd Oline-AQ-spring && .\mvnw.cmd test

# 前端类型检查
cd Oline-AQ-vue && pnpm run type-check

# 前端构建
cd Oline-AQ-vue && pnpm run build

# Android Debug APK
cd Oline-AQ-app && .\gradlew.bat assembleDebug

# Android Release APK
cd Oline-AQ-app && .\gradlew.bat assembleRelease
```

## 文档导航

| 文档 | 说明 |
|------|------|
| [docs/项目整体说明.md](docs/项目整体说明.md) | 全局功能与设计说明 |
| [docs/考试模块.md](docs/考试模块.md) | 考试模块详细设计 |
| [docs/学生端移动应用.md](docs/学生端移动应用.md) | Android App 设计说明 |
| [docs/api-reference.md](docs/api-reference.md) | 后端 API 接口参考 |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 版本更新日志 |
| [docs/change-summary/](docs/change-summary/) | 每次代码改动总结 |
| [AGENTS.md](AGENTS.md) | 项目协作与文档规范 |
