# Online-AQ Android 学生端

智能在线答题系统的 Android 原生学生客户端，面向移动场景的考试、练习、成绩复盘与错题沉淀。

## 系统架构概览

本项目是 **Online-AQ 智能在线答题系统** 的 Android 前端部分，整个系统由四部分组成：

```
┌───────────────┐     ┌───────────────┐     ┌────────────────┐
│  Web 前端      │     │  Android 学生端 │     │  第三方服务     │
│  (Vue3+Vite)   │     │  (本仓库)      │     │  R2/AI/SMTP    │
└───────┬───────┘     └───────┬───────┘     └────────┬───────┘
        │                     │                       │
        └────────────┬────────┘───────────────────────┘
                     │ HTTP/JSON (REST API)
                     ▼
            ┌────────────────┐
            │  Spring Boot    │
            │  后端服务       │
            │  (Java 17)      │
            └───────┬────────┘
                    │
                    ▼
            ┌────────────────┐
            │  PostgreSQL     │
            │  数据库         │
            └────────────────┘
```

**App 本身不包含任何业务数据**，所有数据（考试、题目、成绩、用户信息等）均通过 REST API 从后端服务获取。因此使用本 App 前必须部署配套的后端服务。

---

## 依赖的前置系统

### 1. 后端服务 (Oline-AQ-spring)

- **技术栈**: Spring Boot 3.3.5 + MyBatis-Plus + PostgreSQL
- **必须部署**: App 的所有接口调用均依赖此服务
- 部署方式详见 [Oline-AQ-spring/README.md](../Oline-AQ-spring/README.md)（如无，见根目录 README）

### 2. 数据库 (PostgreSQL)

- **位置**: 后端服务连接的后端数据库
- **初始化**: 使用根目录 `init.sql` 初始化所有表结构和默认数据
- **配置**: 通过后端的 `application.yml` 中的 `spring.datasource` 配置连接

### 3. Web 前端（可选）

- **技术栈**: Vue 3 + Vite + Element Plus + Vant
- 管理端功能（题库管理、考试创建、成绩导出、系统配置等）需通过 Web 前端操作
- 部署方式详见 [Oline-AQ-vue/README.md](../Oline-AQ-vue/README.md)

---

## 功能列表

| 模块 | 功能 | 说明 |
|---|---|---|
| **登录** | 账号密码登录 | 自动保存 Token，支持自动登录 |
| **考试列表** | 查看已发布的考试 | 按状态（未开始/进行中/已结束）展示 |
| **在线考试** | 进入考试作答 | 支持单选题、判断题、简答题；题目和选项可乱序 |
| | 题号导航 | 底部题号栏快速跳转，已答/未答/当前高亮 |
| | 草稿保存 | 退出考试时自动保存草稿，下次进入可恢复 |
| | 退出确认 | 退出时弹窗确认，防止误操作丢失作答 |
| | 自动交卷 | 倒计时到期自动提交答卷 |
| | 未答提交 | 允许未完成全部题目时提交，会弹窗提示 |
| **成绩列表** | 按考试分组查看历史成绩 | 展示得分、用时、提交时间 |
| **成绩详情** | 查看每道题详情 | 题干、A/B/C/D 选项、你的答案、正确答案、得分 |
| **练习模式** | 随机抽题练习 | 从题库中按分类抽题，提交后即时反馈正误与正确答案 |
| **错题本** | 创建和管理错题本 | 将考试/练习中的错题加入错题本归类管理 |
| | 错题筛选 | 按题型（单选/简答）和关键词搜索筛选 |
| **个人中心** | 个人信息编辑 | 修改真实姓名、邮箱 |
| | 修改密码 | 修改登录密码 |
| | 公告查看 | 查看系统公告，标记已读 |
| | 服务器设置 | 动态切换后端 API 地址 |
| | 主题设置 | 跟随系统/浅色/深色/AMOLED 深色模式 |
| | 退出登录 | 清除 Token 并跳转登录页 |

---

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Material Design 3 (Catppuccin 配色), ConstraintLayout, DataBinding + ViewBinding |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson 2.10.1 |
| 架构 | Activity + Fragment, 生命周期感知组件 |
| 主题 | 日间/夜间双主题，支持 System/Light/Dark/AMOLED 模式 |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 34 (Android 14) |
| 构建 | Gradle 8.14 + Kotlin 2.0.21 + AGP 8.7.3 |

---

## 前置环境

| 工具 | 版本要求 |
|---|---|
| Android Studio | Hedgehog (2023.1.1) 或更高 |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.14.3（自动下载） |
| 后端服务 | 必须已部署并运行（详见根目录 README） |

---

## 快速开始

### 1. 导入项目

用 Android Studio 打开 `Oline-AQ-app/` 目录，等待 Gradle 同步完成。

### 2. 配置后端地址

默认连接地址：`http://39.98.69.153:8084/api/`

如需修改：

**方式一** — 代码级默认值（编译前修改）：
打开 `app/src/main/java/com/onlineaq/student/utils/ServerConfig.kt`，修改 `DEFAULT_URL` 常量。

**方式二** — 运行时修改：
App 内「个人中心 → 服务器设置」中输入新的 API 地址，App 会自动重建 Retrofit 实例。

### 3. 构建 APK

```sh
# Debug 版（用于开发测试）
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# Release 版（用于发布，需先配置签名）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

---

## 配置发布签名

Android 应用发布必须使用独立的签名密钥，不可使用默认的 Debug 签名。

### 生成签名密钥

```sh
keytool -genkey -v -keystore app/release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key
```

按提示输入：
- 密钥库密码（`storePassword`）
- 密钥密码（`keyPassword`，可与密钥库密码相同）
- 个人信息（姓名、组织、城市、省份、国家代码等）

### 配置签名信息

在 `app/` 目录下创建 `keystore.properties`：

```properties
storePassword=你的密钥库密码
keyPassword=你的密钥密码
keyAlias=release-key
storeFile=release-key.jks
```

> **安全说明**：`keystore.properties` 和 `*.jks` 文件已加入 `.gitignore`，不会被提交到 Git 仓库。请妥善保管密钥文件和密码，**密钥丢失将无法更新已发布的应用**。

### 验证签名

```sh
# 查看 APK 签名信息
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

---

## API 接口清单

App 通过 Retrofit 调用后端 REST API，完整接口列表如下：

| 方法 | 接口路径 | 说明 | Token |
|------|---------|------|-------|
| POST | `auth/login` | 登录 | 否 |
| GET | `exams/student` | 获取学生考试列表 | 是 |
| GET | `exams/{examId}` | 获取考试详情（含题目） | 是 |
| POST | `exams/{examId}/submit` | 提交答卷 | 是 |
| GET | `results/my` | 获取我的成绩列表 | 是 |
| GET | `results/{resultId}` | 获取成绩详情（含答题明细） | 是 |
| GET | `results/wrong-questions` | 获取错题分组 | 是 |
| GET | `questions` | 获取题库题目（分页） | 是 |
| GET | `questions/categories` | 获取题目分类列表 | 是 |
| GET | `users/me` | 获取个人信息 | 是 |
| PUT | `users/me` | 更新个人信息 | 是 |
| GET | `announcements/unread` | 获取未读公告 | 是 |
| POST | `announcements/{id}/read` | 标记公告已读 | 是 |
| POST | `announcements/read-all` | 标记所有公告已读 | 是 |
| GET | `wrong-notebooks` | 获取错题本列表 | 是 |
| POST | `wrong-notebooks` | 创建错题本 | 是 |
| PUT | `wrong-notebooks/{notebookId}` | 更新错题本 | 是 |
| DELETE | `wrong-notebooks/{notebookId}` | 删除错题本 | 是 |
| GET | `wrong-notebooks/{notebookId}` | 获取错题本详情 | 是 |
| POST | `wrong-notebooks/{notebookId}/items` | 添加错题到错题本 | 是 |
| DELETE | `wrong-notebooks/{notebookId}/items/{itemId}` | 从错题本移除题目 | 是 |
| GET | `feedbacks/my` | 获取我的反馈题目 ID | 是 |
| POST | `feedbacks` | 提交题目反馈 | 是 |

所有需要 Token 的接口在请求头中自动注入 `Authorization: Bearer <token>`（由 `RetrofitClient` 中的 OkHttp Interceptor 实现）。

---

## 项目结构

```
Oline-AQ-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/onlineaq/student/
│   │   │   │   ├── OnlineAQApp.kt              # Application 入口，初始化主题
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── ApiService.kt        # Retrofit 接口定义（全部 API）
│   │   │   │   │   │   └── RetrofitClient.kt    # Retrofit 单例，自动注入 Token
│   │   │   │   │   └── model/
│   │   │   │   │       ├── Exam.kt              # 考试列表 / 考试详情模型
│   │   │   │   │       ├── SubmitRequest.kt     # 提交答卷请求模型
│   │   │   │   │       ├── ResultDetail.kt      # 成绩详情与答题明细
│   │   │   │   │       ├── LoginRequest.kt      # 登录请求/响应
│   │   │   │   │       ├── UserProfile.kt       # 用户信息
│   │   │   │   │       ├── AnnouncementItem.kt  # 公告
│   │   │   │   │       ├── Question.kt          # 题目
│   │   │   │   │       ├── ApiResponse.kt       # 通用 API 响应包装
│   │   │   │   │       ├── PageResult.kt        # 分页结果
│   │   │   │   │       ├── WrongNotebook.kt     # 错题本
│   │   │   │   │       ├── QuestionFeedback.kt  # 题目反馈
│   │   │   │   │       └── StudentFeatureModels.kt # 练习/考试历史相关模型
│   │   │   │   ├── ui/
│   │   │   │   │   ├── login/                   # 登录页
│   │   │   │   │   ├── main/                    # 主页面（底部导航框架）
│   │   │   │   │   ├── examlist/                # 考试列表页
│   │   │   │   │   ├── examdetail/              # 考试作答页（核心）
│   │   │   │   │   ├── examhistory/             # 考试历史
│   │   │   │   │   ├── results/                 # 成绩列表
│   │   │   │   │   ├── resultdetail/            # 成绩详情
│   │   │   │   │   ├── practice/                # 练习模式
│   │   │   │   │   ├── wrongbook/               # 错题本
│   │   │   │   │   ├── profile/                 # 个人中心
│   │   │   │   │   └── more/                    # 更多设置
│   │   │   │   └── utils/
│   │   │   │       ├── ServerConfig.kt          # 服务器地址管理
│   │   │   │       ├── TokenManager.kt          # Token 与用户信息管理
│   │   │   │       └── ThemeConfig.kt           # 主题模式管理
│   │   │   ├── res/
│   │   │   │   ├── layout/                      # XML 布局文件
│   │   │   │   ├── drawable/                    # 图标和背景 drawable
│   │   │   │   ├── values/                      # 字符串、颜色、主题（日间）
│   │   │   │   ├── values-night/                # 主题（夜间）
│   │   │   │   ├── color/                       # 颜色资源
│   │   │   │   ├── navigation/                  # 底部导航菜单
│   │   │   │   └── mipmap/                      # 应用图标
│   │   │   └── AndroidManifest.xml
│   │   └── test/                                # 单元测试
│   ├── build.gradle.kts                         # 模块级构建配置
│   └── proguard-rules.pro                       # 混淆规则
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                             # 项目级构建配置
├── settings.gradle.kts                          # 项目设置
├── gradle.properties                            # Gradle 属性
├── gradlew                                      # Gradle Wrapper (Unix)
├── gradlew.bat                                  # Gradle Wrapper (Windows)
└── local.properties                             # 本地 SDK 路径（不提交）
```

---

## 数据模型说明

### 通用 API 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 核心业务模型

**ExamDetail** — 考试详情（进入考试时获取）：
```json
{
  "examId": 1,
  "examName": "期中考试",
  "description": "考试说明",
  "duration": 60,
  "totalScore": 100,
  "startTime": "2026-05-20T19:00:00",
  "endTime": "2026-05-20T21:00:00",
  "shuffleQuestions": true,
  "shuffleAnswers": true,
  "questions": [
    {
      "questionId": 1,
      "questionContent": "题干",
      "questionType": "single",
      "optionA": "...",
      "optionB": "...",
      "optionC": "...",
      "optionD": "...",
      "sortOrder": 1,
      "score": 5
    }
  ],
  "attemptId": "uuid-string",
  "draftAnswers": { "1": "A" },
  "useTime": 120
}
```

**SubmitExamRequest** — 提交答卷请求：
```json
{
  "attemptId": "uuid-string",
  "answers": { "1": "A", "2": "B" },
  "useTime": 600,
  "autoSubmit": false
}
```

**ResultDetail** — 成绩详情：
```json
{
  "resultId": 1,
  "examId": 1,
  "examName": "期中考试",
  "totalScore": 85,
  "correctCount": 17,
  "wrongCount": 3,
  "useTime": 3600,
  "answers": [
    {
      "questionId": 1,
      "questionContent": "题干",
      "questionType": "single",
      "optionA": "...",
      "optionB": "...",
      "optionC": "...",
      "optionD": "...",
      "studentAnswer": "A",
      "correctAnswer": "A",
      "isCorrect": true,
      "score": 5
    }
  ]
}
```

---

## 通信与安全

### 身份认证流程

```
App                             Backend
  │                                │
  │  POST auth/login               │
  │  { username, password }        │
  │ ──────────────────────────→    │
  │  ← { token, userId, role }     │
  │                                │
  │  GET exams/student             │
  │  Authorization: Bearer <token> │
  │ ──────────────────────────→    │
  │  ← { exams }                   │
```

- Token 存储在 `SharedPreferences`（`online_aq_prefs`）
- 每次请求由 `OkHttp Interceptor` 自动注入 `Authorization` 请求头
- Token 过期或无效时，后端返回 401，App 应跳转到登录页

### 网络要求

| 项目 | 说明 |
|---|---|
| 协议 | HTTP/HTTPS |
| 内容类型 | application/json |
| 超时 | 连接 30s / 读取 30s / 写入 30s |
| 调试 | OkHttp LoggingInterceptor 打印完整请求/响应日志 |

---

## 开发说明

### 调试模式

`RetrofitClient` 默认开启 `HttpLoggingInterceptor.Level.BODY`，所有 API 请求和响应会在 Logcat 中输出。如需关闭，可在 `RetrofitClient.kt` 中修改日志级别。

### 主题定制

配色方案基于 [Catppuccin Mocha/Macchiato](https://github.com/catppuccin/catppuccin)，定义在 `res/values/colors.xml`。支持：

- **跟随系统** — 默认，自动切换日间/夜间
- **浅色模式** — 强制日间主题
- **深色模式** — 强制夜间主题
- **AMOLED 深色** — 使用纯黑背景，适合 OLED 屏幕

### 常见问题

**Q: 登录后看不到考试列表？**
A: 确认后端已启动且考试已发布并为当前学生可见；检查服务器地址是否正确。

**Q: 提交考试时提示错误？**
A: 查看 Logcat 中 Retrofit 的请求/响应日志，确认后端返回的具体错误信息。

**Q: 如何更换后端服务器？**
A: App 内「个人中心 → 服务器设置」中输入新地址，例如 `http://your-server:8084/api/`。

---

## 许可证

MIT
