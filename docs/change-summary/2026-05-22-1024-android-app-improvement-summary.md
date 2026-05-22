# Android 学生端 App 完善改动总结

## 1. 改动目标

完善 Android 学生端 App，修复数据模型兼容性问题，优化代码健壮性，提升用户体验，并将 API 地址切换至部署环境。

## 2. 用户视角

- App 连接部署服务器 `http://39.98.69.153:8084/`，学生可在手机上完成在线答题
- 考试列表侧边栏点击左上角菜单图标即可打开
- 成绩列表支持下拉刷新，列表为空时显示"暂无考试成绩"提示
- 考试列表为空时显示"暂无考试安排"提示
- 成绩详情页每道题显示题型标签（单选题/判断题/填空题/简答题）
- 网络请求更稳定，不会因页面销毁导致内存泄漏

## 3. 前端实现

### 3.1 数据模型修复

**`Exam.kt`** - `allowRetake` 字段从 `Boolean` 改为 `Boolean?`，避免后端返回 null 时 Gson 解析崩溃

**`ExamResult.kt`** - `ResultDetail.history` 字段从 `List<ExamHistory>` 改为 `List<ExamHistory>?`，匹配后端仅为管理员返回该字段的行为

### 3.2 网络层改进

**`OnlineAQApp.kt`** - `BASE_URL` 从 `http://10.0.2.2:8080/api/` 改为 `http://39.98.69.153:8084/api/`

### 3.3 Activity 生命周期改进

所有 Activity 替换了原始 `CoroutineScope(Dispatchers.IO).launch` 为 `lifecycleScope.launch(Dispatchers.IO)`：
- `LoginActivity.kt`
- `ExamListActivity.kt`
- `ExamDetailActivity.kt`
- `ResultsActivity.kt`
- `ResultDetailActivity.kt`

该改进可防止协程在 Activity 销毁后继续运行导致的内存泄漏。

### 3.4 考试列表页改进

**`ExamListActivity.kt`**
- 新增 Toolbar 菜单按钮点击事件：打开侧边抽屉
- 新增空状态 TextView（id: `tv_empty`），列表为空时显示"暂无考试安排"
- 导航菜单中"考试列表"项点击时关闭抽屉（不跳转）

### 3.5 成绩列表页改进

**`ResultsActivity.kt`**
- 新增 `SwipeRefreshLayout` 支持下拉刷新
- 新增空状态 TextView，成绩为空时显示"暂无考试成绩"
- 成绩卡片标题改为显示考试 ID（`考试 #${examId}`）

### 3.6 成绩详情页改进

**`ResultDetailActivity.kt`**
- 每道题的标题增加题型标签（单选题/判断题/填空题/简答题）
- 统一错误处理与用户提示

### 3.7 布局文件改进

**`activity_exam_list.xml`**
- 在 SwipeRefreshLayout 内用 FrameLayout 包裹 RecyclerView，新增空状态 TextView

**`activity_results.xml`**
- 新增 SwipeRefreshLayout 包裹 RecyclerView
- 新增空状态 TextView

## 4. 依赖更新

**`app/build.gradle.kts`**
- 新增 `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` 依赖，用于 `lifecycleScope`

## 5. 关键文件

| 文件 | 作用 |
|---|---|
| `app/src/main/java/.../OnlineAQApp.kt` | 应用入口，修改 API 基础地址 |
| `app/src/main/java/.../data/model/Exam.kt` | 修复 allowRetake 类型为可空 |
| `app/src/main/java/.../data/model/ExamResult.kt` | 修复 history 类型为可空 |
| `app/src/main/java/.../ui/login/LoginActivity.kt` | 使用 lifecycleScope |
| `app/src/main/java/.../ui/examlist/ExamListActivity.kt` | 工具栏打开抽屉、空状态 |
| `app/src/main/java/.../ui/examdetail/ExamDetailActivity.kt` | 使用 lifecycleScope |
| `app/src/main/java/.../ui/results/ResultsActivity.kt` | 下拉刷新、空状态 |
| `app/src/main/java/.../ui/resultdetail/ResultDetailActivity.kt` | 题型标签、错误处理 |
| `app/build.gradle.kts` | 新增 lifecycle-runtime-ktx 依赖 |
| `app/src/main/res/layout/activity_exam_list.xml` | 新增空状态布局 |
| `app/src/main/res/layout/activity_results.xml` | 新增下拉刷新和空状态布局 |

## 6. 验证方式

```bash
# 使用 Android Studio 打开 Oline-AQ-app/ 目录
# 点击 Run 或使用 Gradle 构建
cd Oline-AQ-app
./gradlew assembleDebug
```

安装 APK 后验证：
1. 登录页 - 输入学生账号密码登录
2. 考试列表 - 下拉刷新，点击菜单打开侧边栏
3. 答题页 - 作答并提交
4. 成绩列表 - 下拉刷新，点击查看详情
5. 成绩详情 - 查看每道题的对错和正确答案

## 7. 风险与后续优化

- **当前限制**：成绩列表无法显示考试名称（后端 `myResults` 接口未返回 examName），当前显示考试 ID
- **后续可优化**：
  - 后端 `myResults` 接口增加 examName 字段（通过 JOIN `exam` 表）
  - 新增在线随机练习功能
  - 新增错题本功能
  - 增加离线缓存机制
  - 使用 ViewBinding 替代 findViewById
  - 增加加载状态骨架屏
