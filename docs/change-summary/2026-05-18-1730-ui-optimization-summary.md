# UI 全面优化改动总结

## 1. 改动目标

基于 High-Agency Frontend Design 规范，对项目前端进行全面 UI 优化。核心目标：
- 替换旧的蓝色渐变主色（AI Blue 风格）为翡翠/板岩高级配色
- 替换 Inter 字体为 Outfit 字体（更独特、更精致）
- 去除渐变文字、霓虹发光等 AI 设计陈词滥调
- 采用非对称布局和 Bento 网格替代通用卡片网格
- 加入 Liquid Glass 毛玻璃折射效果
- 增加交错加载动画和弹性过渡
- 优化侧边栏导航（含用户信息区域）

## 2. 用户视角

- **登录页**：品牌徽章 + 左对齐英雄区域 + 四个功能特性卡片（带图标）+ 登录卡片带内边框折射效果。整体感觉更干净、高级，去除了之前的渐变文字和步骤条。
- **侧边栏**：教师/学生端均新增了用户头像首字母、用户角色标签和版本号，品牌区域带图标 Logo。
- **仪表盘**：从 4 个独立 el-card + 3 个独立 el-card 的布局变为 Bento 网格布局。前 4 个指标卡变为紧凑数据卡片，资产总览和快捷操作合并为 2 列宽 + 1 列高的非对称布局，工作流程和最近考试为全宽卡片。每项带有交错入场动画。
- **全局**：页面切换过渡从 `translateY(12px)` 改为 `translateY(12px) scale(0.98)`，更柔和。退出登录按钮缩小为 `size="small"`。

## 3. 前端实现

### 页面
- **LoginView.vue**：重写英雄区域布局，徽章标签 + h1 换行（"智能"+"在线答题系统"绿色强调）+ 功能特性网格替代 el-steps。登录卡片增加 `::before` 伪元素的渐变内边框折射效果。图标使用 `@element-plus/icons-vue`。
- **AdminLayout.vue**：品牌区域改为 Logo 图标（首字母 "A"）+ 标题组合。新增用户信息卡片（首字母头像 + 用户名 + "管理员"）。底部固定版本号 "AQ System v1.0"。
- **StudentLayout.vue**：与 AdminLayout 对称设计，用户信息卡片显示 "学生" 角色标签。
- **AdminDashboard.vue**：完全重写为 Bento 网格布局。4 个统计指标使用 `.bento-cell` + `.fade-in` + `.stagger-*` 类实现交错入场。资产总览占 2 列宽，快捷操作占 1 列。工作流程和最近考试为全宽。
- **App.vue**：退出按钮改为 `size="small"`。

### 状态管理
- 无变化，依赖现有的 Pinia store。

### 样式
- **style.css**：完全重写。
- 新 CSS 变量：`--primary: #0d9488`（翡翠绿）、`--ink: #0f172a`（石板黑）、`--muted: #64748b` 等全部来自 Tailwind Slate/Teal 调色板。
- 新增：`--spring` 弹性贝塞尔曲线、`--shadow-diffusion` 扩散阴影、`--radius-xl`。
- 新增：`.glass-card-panel`、`.bento-grid`、`.bento-cell`、`.bento-cell-wide`、`.bento-cell-full`、`.bento-label`、`.bento-value`、`.bento-trend` 系列类。
- 新增：`@keyframes fadeSlideUp`、`@keyframes float`、`@keyframes pulse`、`@keyframes countUp`。
- 更新：所有 Element Plus 组件覆盖从蓝色改为翡翠色。
- 更新：背景从蓝色网格线 + 蓝色径向渐变改为翡翠色径向渐变 + 纯色背景。
- 更新：`fade-in` 类从 `fadeUp` 改为 `fadeSlideUp` 使用弹性曲线。

### 字体
- `index.html`：新增 Google Fonts 预连接和 Outfit 字体加载。
- `style.css`：`--font-sans` 将 Inter 替换为 Outfit（英文使用 Outfit，中文回退到系统字体）。

## 4. 后端实现

无后端改动。

## 5. 数据库与数据流

无数据库变动。

## 6. 接口说明

无接口变动。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `index.html` | 新增 Outfit 字体加载 (Google Fonts) |
| `src/style.css` | 完全重写：全新配色、弹性动画、Bento 网格、Liquid Glass、Element Plus 覆盖 |
| `src/App.vue` | 退出按钮改为 small 尺寸 |
| `src/views/login/LoginView.vue` | 重写：徽标 + 非对称布局 + 功能特性卡片 + 内边框折射 |
| `src/views/admin/AdminLayout.vue` | 重写：Logo 图标 + 用户信息卡片 + 版本号 |
| `src/views/student/StudentLayout.vue` | 重写：对称设计，学生角色标签 |
| `src/views/admin/AdminDashboard.vue` | 重写：Bento 网格布局 + 交错入场动画 |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm run type-check     # TypeScript 类型检查通过
npm run build-only     # Vite 构建成功（401ms）
```

## 9. 风险与后续优化

- **风险**：Outfit 字体通过 Google Fonts 加载，断网或 GFW 环境下会回退到系统字体，不影响功能。
- **风险**：Bento 网格在屏幕宽度 < 980px 时自动折叠为单列，已验证响应式兼容。
- **后续**：可考虑将 Outfit 字体下载到本地静态资源，减少外部依赖。
- **后续**：可进一步优化 Element Plus 组件的主题变量（通过 Element Plus 的 ConfigProvider），而非仅通过 CSS 覆盖。
- **后续**：可添加更多微交互（计数动画、鼠标跟踪等）。
