# Mihon 风格 UI 改造计划

## 目标
将 Android 学生端 App 改造为 Mihon 风格的 Material 3 设计，包含 Catppuccin 配色、底部导航、网格布局、阅读器式答题页面、Edge-to-edge。

## 阶段

### 阶段 1: Catppuccin 主题 + 夜间模式 + AMOLED
- [ ] 替换 `colors.xml` 为 Catppuccin Mocha（暗色）+ Catppuccin Latte（亮色）全色板
- [ ] 创建 `colors_night.xml` / `themes_night.xml`
- [ ] 创建 `colors_amoled.xml` / AMOLED 暗色模式
- [ ] 更新 `themes.xml` 支持亮/暗/AMOLED 切换
- [ ] 添加主题切换 UI（设置页面或底部弹窗）
- [ ] 更新所有 toolbar 渐变色调为 Catppuccin 主色

### 阶段 2: Edge-to-edge 沉浸显示
- [ ] 在各 Activity 中添加 `enableEdgeToEdge()`
- [ ] 调整 window insets 使内容不被导航栏遮挡
- [ ] 透明状态栏 + 导航栏

### 阶段 3: 底部导航栏（替换 Drawer）
- [ ] 创建 `activity_main.xml` 包含 BottomNavigationView + Fragment 容器
- [ ] 创建 ExamListFragment、ResultsFragment、MoreFragment
- [ ] 重构 ExamListActivity → ExamListFragment
- [ ] 重构 ResultsActivity → ResultsFragment
- [ ] MoreFragment 包含：用户信息、服务器设置、切换主题、退出登录
- [ ] `navigation/bottom_nav_menu.xml`
- [ ] 移除旧 DrawerLayout / NavigationView / nav_header / nav_menu

### 阶段 4: 网格卡片布局（考试列表）
- [ ] 修改 `item_exam.xml` 为正方形卡片，顶部渐变色条
- [ ] 添加考试状态图标指示器
- [ ] 改用 GridLayoutManager(2) 双列网格

### 阶段 5: 阅读器风格答题页
- [ ] 重构 ExamDetailActivity 为单题模式（ViewPager2）
- [ ] 底部：上一题 / 下一题按钮 + 答题进度
- [ ] 顶部：题目索引 + 计时器 + 状态标签
- [ ] 提交按钮固定在底部
- [ ] 题目切换动画

## 决策日志
| 日期 | 决策 |
|------|-------|
| 2026-05-23 | 选择 Catppuccin Mocha/Latte 作为配色方案 |
| 2026-05-23 | 使用 BottomNavigationView + Fragment 替代 Drawer |
| 2026-05-23 | 考试列表采用 GridLayoutManager(2) 双列网格 |
| 2026-05-23 | 答题页面采用单题 ViewPager2 模式（Mihon reader 风格） |
| 2026-05-23 | 主题切换存储在 SharedPreferences 中 |

## 材料
- Catppuccin 配色系统：https://github.com/catppuccin/catppuccin
- Mihon 主题系统参考：14 色主题 + Material 3 + AMOLED
