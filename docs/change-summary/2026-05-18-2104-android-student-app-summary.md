# Android 学生端 App 实现总结

## 1. 改动目标

为在线答题系统生成 Android 原生学生端 App，覆盖学生核心功能：登录、查看考试列表、参加考试（含倒计时自动提交）、查看成绩列表和成绩详情。

## 2. 用户视角

1. 打开 App → 进入登录页，输入学号和密码，点击登录
2. 登录成功 → 进入考试列表页（DrawerLayout 侧边栏导航）
3. 考试列表页展示所有可参加的考试（卡片列表，下拉刷新），点击卡片进入作答
4. 作答页：顶部显示倒计时和已答进度，逐题作答（单选/判断→RadioGroup，填空→输入框，简答→多行文本），底部提交按钮
5. 时间到自动提交，或手动提交（未答完提示确认）
6. 提交后跳转到成绩详情页，展示总分、正确数、错误数、逐题对错和正确答案
7. 侧边栏可切换至"我的成绩"列表，查看历史成绩记录

## 3. 前端（Android）实现

- **项目结构**：Kotlin + Android + Material Design 3 + ViewBinding
- **网络层**：Retrofit2 + OkHttp4 + Gson，请求拦截器自动注入 `Authorization: Bearer token`
- **状态管理**：`SharedPreferences` 持久化 token 和用户信息（TokenManager）
- **UI 组件**：MaterialCardView 卡片列表、CountDownTimer 倒计时、SwipeRefreshLayout 下拉刷新、DrawerLayout 侧边栏导航

### 页面与路由（Activity 跳转）

| 页面 | Activity | 功能 |
|---|---|---|
| 登录 | LoginActivity | 账号密码登录，角色固定为 student |
| 考试列表 | ExamListActivity | 下拉刷新考试列表，侧边栏导航（成绩/退出），点击考试卡片进入作答 |
| 在线作答 | ExamDetailActivity | 加载题目列表，倒计时，逐题作答，手动或自动提交 |
| 成绩列表 | ResultsActivity | 历史成绩列表，展示分数/正确数/错误数 |
| 成绩详情 | ResultDetailActivity | 总分/正确/错误统计，逐题分析对错和正确答案 |

### 核心代码

- `ApiService.kt`：定义所有 API 接口（登录、考试列表、考试详情、提交、成绩查询）
- `RetrofitClient.kt`：OkHttp + Retrofit 单例，自动添加 JWT Token
- `TokenManager.kt`：SharedPreferences 保存/读取登录态
- `LoginActivity.kt`：登录逻辑，成功保存 token 并跳转
- `ExamDetailActivity.kt`：考试作答核心逻辑，CountDownTimer 倒计时、问题类型自适配控件、提交逻辑
- `ResultDetailActivity.kt`：成绩详情逐题展示

## 4. 后端实现

**无后端改动。** 完全复用现有 Spring Boot 后端的 REST API：

- `POST /api/auth/login` — 登录
- `GET /api/exams/student` — 学生获取考试列表
- `GET /api/exams/{examId}` — 获取考试详情（含题目）
- `POST /api/exams/{examId}/submit` — 提交答卷
- `GET /api/results/my` — 我的成绩列表
- `GET /api/results/{resultId}` — 成绩详情

## 5. 数据库与数据流

数据流与现有 Web 前端一致：
```
Android App → Retrofit → HTTP API → Spring Boot → MyBatis-Plus → PostgreSQL
```

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/auth/login` | POST | 登录 | `{username, password, role:"student"}` | `{token, userId, username, realName, role}` |
| `/api/exams/student` | GET | 学生考试列表 | Header: Authorization | `[{examId, examName, duration, ...}]` |
| `/api/exams/{examId}` | GET | 考试详情+题目 | Header: Authorization | `{exam: {...}, questions: [...]}` |
| `/api/exams/{examId}/submit` | POST | 提交答卷 | `{studentId, useTime, answers: [{questionId, studentAnswer}]}` | `{resultId, totalScore, correctCount, wrongCount}` |
| `/api/results/my` | GET | 我的成绩 | Header: Authorization | `[{resultId, totalScore, correctCount, ...}]` |
| `/api/results/{resultId}` | GET | 成绩详情 | Header: Authorization | `{result, exam, answers: [...], history: [...]}` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `app/build.gradle.kts` | Android 构建配置、依赖声明 |
| `app/src/main/res/layout/activity_login.xml` | 登录页布局 |
| `app/src/main/res/layout/activity_exam_list.xml` | 考试列表布局（DrawerLayout） |
| `app/src/main/res/layout/item_exam.xml` | 考试卡片布局 |
| `app/src/main/res/layout/activity_exam_detail.xml` | 作答页布局 |
| `app/src/main/res/layout/item_question.xml` | 题目卡片布局（含题型自适配控件） |
| `app/src/main/res/layout/activity_results.xml` | 成绩列表页布局 |
| `app/src/main/res/layout/activity_result_detail.xml` | 成绩详情页布局 |
| `app/src/main/res/layout/item_result.xml` | 成绩卡片布局 |
| `app/src/main/res/layout/item_answer_detail.xml` | 逐题分析卡片布局 |
| `app/src/main/java/.../data/api/ApiService.kt` | API 接口定义 |
| `app/src/main/java/.../data/api/RetrofitClient.kt` | Retrofit 单例 |
| `app/src/main/java/.../utils/TokenManager.kt` | Token 持久化管理 |
| `app/src/main/java/.../ui/login/LoginActivity.kt` | 登录页面逻辑 |
| `app/src/main/java/.../ui/examlist/ExamListActivity.kt` | 考试列表逻辑 |
| `app/src/main/java/.../ui/examdetail/ExamDetailActivity.kt` | 在线作答逻辑（含倒计时和提交） |
| `app/src/main/java/.../ui/results/ResultsActivity.kt` | 成绩列表逻辑 |
| `app/src/main/java/.../ui/resultdetail/ResultDetailActivity.kt` | 成绩详情逻辑 |
| `app/src/main/AndroidManifest.xml` | 组件注册、权限 |
| `settings.gradle.kts` | Gradle 项目设置 |
| `build.gradle.kts` | 根构建脚本 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 配置 |
| `gradlew.bat` | Windows 构建脚本 |

## 8. 验证方式

```sh
cd Oline-AQ-app
set JAVA_HOME=D:\software\java version\Java17.0.12
gradlew.bat assembleDebug
```

验证要点：
1. 确保后端 Spring Boot 已启动（localhost:8080）
2. 第一次构建需联网下载 Gradle 和 Android 依赖
3. 构建产物：`app/build/outputs/apk/debug/app-debug.apk`
4. 安装 APK 到模拟器或真机后，使用学生账号登录测试

## 9. 风险与后续优化

**当前限制**：
- 初次构建需联网下载 Gradle 8.7 和所有 Maven/Google 依赖
- 后端 API 地址硬编码为 `http://10.0.2.2:8080/api/`（模拟器访问宿主机），真机需改为局域网或公网 IP
- 当前使用明文 HTTP，生产环境应配置 HTTPS
- 没有实现离线缓存和错误重试机制

**后续可优化**：
1. 添加 Retrofit 拦截器实现网络异常统一处理
2. 添加本地数据库（Room）支持离线查看已缓存的考试和成绩
3. 优化倒计时精度（结合服务端时间戳校准）
4. 添加推送通知（考试即将开始、成绩通知）
5. 支持自适应布局和深色主题
6. 添加单元测试和 UI 测试
7. 使用 MVVM 模式 + ViewModel 优化架构
8. 用 Gradle Kotlin DSL 替代 Groovy
