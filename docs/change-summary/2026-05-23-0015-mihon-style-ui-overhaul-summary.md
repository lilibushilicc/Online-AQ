# Mihon 风格 UI 全面改造总结

## 1. 改动目标

参考开源项目 Mihon（https://github.com/mihonapp/mihon）的 UI 设计风格，对 Android 学生端 App 进行全面视觉与交互改造。包括 Catppuccin 配色系统、Material 3 主题、底部导航栏、网格卡片布局、阅读器风格单题答题模式、Edge-to-edge 沉浸显示。

## 2. 用户视角

### 登录页
- 底色跟随系统/用户选择的主题模式（亮色/暗色/AMOLED）
- 渐变条从 Catppuccin Mauve → Blue

### 主界面
- 底部三个 Tab：考试（列表网格）、成绩（列表）、更多
- 更多页包含：用户信息卡片、主题切换（系统/亮色/暗色 + AMOLED 开关）、服务器设置、退出登录

### 考试列表
- 双列网格卡片，每张卡片顶部渐变色条
- 卡片显示考试名称、状态标签（带颜色圆角）、描述、时长、总分、时间范围

### 答题页面
- 单题显示，左右滑动或点击"上一题/下一题"按钮翻页
- 顶部：渐变色工具栏 + 倒计时 + 已答数量
- 中间：题目卡片（选项为圆角卡片样式，选中高亮边框）
- 底部：题目进度条 + 页码 + 翻页按钮，最后一题"下一题"变为"提交"

### 成绩详情
- 跨三个指标列展示：总分（主色）、正确数（绿色）、错误数（红色）
- 题目列表显示正确/错误标签

## 3. 前端实现

### 主题系统
- `values/colors.xml`：Catppuccin Latte（亮色）全色板 + M3 颜色映射
- `values-night/colors.xml`：Catppuccin Mocha（暗色）全色板 + M3 颜色映射
- `values/themes.xml`：Theme.OnlineAQ + Theme.OnlineAQ.Amoled 两个主题
- `values-night/themes.xml`：暗色 M3 主题
- `ThemeConfig.kt`：主题模式切换管理（系统/亮色/暗色），写入 SharedPreferences
- `OnlineAQApp.kt`：启动时读取主题配置并应用

### 导航系统
- 旧 DrawerLayout + NavigationView → 移除（activity_exam_list.xml 删除）
- 新 `activity_main.xml`：FrameLayout（Fragment 容器）+ BottomNavigationView
- `MainActivity.kt`：管理三个 Fragment 切换
- `navigation/bottom_nav_menu.xml`：底部菜单定义
- `color/bottom_nav_color.xml`：选中/未选中颜色选择器

### Fragment 页
- `ExamListFragment`：双列 GridLayoutManager + 自定义 GradientDrawable 状态标签
- `ResultsFragment`：单列 LinearLayoutManager
- `MoreFragment`：用户信息、主题切换（ToggleGroup + Switch）、服务器设置、退出登录

### 答题页面
- `activity_exam_detail.xml`：Toolbar + 计时 + ViewPager2 + 底部导航栏 + 提交按钮
- `item_question.xml`：单选/判断选项为 MaterialCardView 圆角卡片，选中高亮描边
- `ExamDetailActivity.kt`：ViewPager2 + FragmentStateAdapter（QuestionPagerAdapter）
- `QuestionPageFragment.kt`：单题渲染，管理 RadioGroup/输入框状态
- `QuestionPagerAdapter.kt`：FragmentStateAdapter，按需创建 Fragment

### 其他页面
- `activity_result_detail.xml`：三列统计 + RecyclerView
- `item_exam.xml`：渐变色顶条 + 内容区 + 分隔线 + 时长/分数
- `item_result.xml`：水平布局（进度环 + 考试信息 + 分数）
- `item_answer_detail.xml`：题目索引 + 对错标签 + 内容 + 答案对比
- `item_question.xml`：ScrollView 包裹的圆角卡片 + 选项 MaterialCardView

### Edge-to-edge
- 所有 Activity 调用 `enableEdgeToEdge()`（login、main、exam_detail、result_detail）
- 状态栏/导航栏透明，内容延伸到系统栏区域
- `themes.xml`：`android:statusBarColor` 和 `android:navigationBarColor` 设为透明

### 图标
- `ic_nav_exams.xml`：文档图标
- `ic_nav_results.xml`：柱状图图标
- `ic_nav_more.xml`：三点菜单图标

## 4. 后端实现

无后端改动。APP 使用 RetrofitClient 调用与改动前完全一致的 API 接口：
- `POST /api/auth/login`
- `GET /api/exams/student`
- `GET /api/exams/{examId}`
- `POST /api/exams/{examId}/submit`
- `GET /api/results/my`
- `GET /api/results/{resultId}`

## 5. 数据库与数据流

无数据库改动。

## 6. 接口说明

无新增或修改接口。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-app/app/src/main/res/values/colors.xml` | Catppuccin Latte 亮色配色 + M3 映射 |
| `Oline-AQ-app/app/src/main/res/values-night/colors.xml` | Catppuccin Mocha 暗色配色 + M3 映射 |
| `Oline-AQ-app/app/src/main/res/values/themes.xml` | 亮色 + AMOLED 主题定义 |
| `Oline-AQ-app/app/src/main/res/values-night/themes.xml` | 暗色主题定义 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/utils/ThemeConfig.kt` | 主题管理工具 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/OnlineAQApp.kt` | 应用入口，初始化主题 |
| `Oline-AQ-app/app/src/main/res/layout/activity_main.xml` | 底部导航主界面布局 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/main/MainActivity.kt` | 主 Activity，Fragment 切换 |
| `Oline-AQ-app/app/src/main/res/layout/fragment_exam_list.xml` | 考试列表 Fragment 布局 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/examlist/ExamListFragment.kt` | 考试列表 Fragment + GridAdapter |
| `Oline-AQ-app/app/src/main/res/layout/fragment_results.xml` | 成绩列表 Fragment 布局 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/results/ResultsFragment.kt` | 成绩列表 Fragment + Adapter |
| `Oline-AQ-app/app/src/main/res/layout/fragment_more.xml` | 更多页 Fragment 布局 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/more/MoreFragment.kt` | 更多页 Fragment（主题/服务器/注销） |
| `Oline-AQ-app/app/src/main/res/navigation/bottom_nav_menu.xml` | 底部导航菜单定义 |
| `Oline-AQ-app/app/src/main/res/color/bottom_nav_color.xml` | 底部导航颜色选择器 |
| `Oline-AQ-app/app/src/main/res/layout/activity_exam_detail.xml` | ViewPager2 答题页布局 |
| `Oline-AQ-app/app/src/main/res/layout/item_question.xml` | 单题卡片（选项为 MaterialCardView） |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/examdetail/ExamDetailActivity.kt` | 答题页 Activity |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/examdetail/QuestionPagerAdapter.kt` | ViewPager2 FragmentStateAdapter |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/examdetail/QuestionPageFragment.kt` | 单题 Fragment（选择题/填空/简答） |
| `Oline-AQ-app/app/src/main/res/layout/item_exam.xml` | 网格卡片（渐变色顶条） |
| `Oline-AQ-app/app/src/main/res/layout/item_result.xml` | 结果列表卡片 |
| `Oline-AQ-app/app/src/main/res/layout/item_answer_detail.xml` | 答题详情卡片 |
| `Oline-AQ-app/app/src/main/res/layout/activity_result_detail.xml` | 成绩详情页布局 |
| `Oline-AQ-app/app/src/main/java/com/onlineaq/student/ui/resultdetail/ResultDetailActivity.kt` | 成绩详情 Activity |
| `Oline-AQ-app/app/src/main/res/drawable/ic_nav_exams.xml` | 考试 Tab 图标 |
| `Oline-AQ-app/app/src/main/res/drawable/ic_nav_results.xml` | 成绩 Tab 图标 |
| `Oline-AQ-app/app/src/main/res/drawable/ic_nav_more.xml` | 更多 Tab 图标 |
| `Oline-AQ-app/app/build.gradle.kts` | 添加 ViewPager2 依赖 |
| `Oline-AQ-app/app/src/main/AndroidManifest.xml` | 注册 MainActivity，移除旧 Activity |

## 8. 验证方式

```sh
cd Oline-AQ-app
.\gradlew.bat assembleDebug
```

构建成功。APK 产物位于 `Oline-AQ-app/app/build/outputs/apk/debug/app-debug.apk`。

验证步骤：
1. 安装 APK → 看到登录页（Catppuccin 亮色/跟随系统）
2. 登录 → 进入底部导航主界面（默认考试 Tab）
3. 考试列表以双列网格展示，卡片有渐变色顶条
4. 点击考试 → 单题阅读器模式，滑动或点击按钮翻页
5. 切换到成绩 Tab → 成绩列表
6. 切换到更多 Tab → 切换主题模式 → 应用立即重建
7. 打开 AMOLED 开关 → 底色变为纯黑
8. 修改服务器地址 → 重新登录生效

## 9. 风险与后续优化

### 当前限制
- 主题切换后需要 `recreate()` 整个 Activity，体验略生硬
- AMOLED 模式仅覆盖核心 surface/background 色，个别组件可能未适配
- 旧 ExamListActivity 和 ResultsActivity 已删除，无法回退
- 答题页 ViewPager2 使用 FragmentStateAdapter，Fragment 重建时输入状态可能丢失
- `nav_graph.xml` 被删除（未使用 Navigation Component）

### 后续优化方向
- 使用 Jetpack Compose 彻底重写 UI，实现动态度量取色（Monet）
- 添加更多 Catppuccin 风味的页面过渡动画
- 为答题页增加"交卷确认"的 Mihon 风格对话框
- 实现更多 Mihon 主题变体（Nord、Catppuccin 不同口味等）
