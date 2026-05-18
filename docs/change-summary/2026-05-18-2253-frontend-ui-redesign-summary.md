# 前端 UI 风格重构总结

## 1. 改动目标

将整个在线考试系统的前端 UI 从"科技感绿 + 毛玻璃"风格升级为**Editorial Paper-Ink（学术期刊纸墨）风格**，提升视觉质感和专业感。

## 2. 用户视角

- 登录页变为左右分栏布局：左侧品牌宣传区（深墨绿 + Georgia 衬线字体），右侧表单卡片
- 教师端/学生端侧边栏从白底毛玻璃改为**深墨绿**固体背景，白色文字，现代感强
- 所有按钮、标签、表格使用更克制的圆角和衬线数字，整体更像专业编辑器而非玩具 UI

## 3. 前端实现

### 样式体系（style.css）

| 维度 | 旧风格 | 新风格 |
|---|---|---|
| 主色 | Teal `#0d9488` + 毛玻璃 | 墨绿 `#1a3d2e` / `#2d5a47` |
| 背景 | `#f8fafc` + 渐变光晕 | `#faf9f6`（暖白纸感）|
| 字体 | Outfit（现代无衬线） | Georgia / Helvetica Neue 混合 |
| 侧边栏 | 毛玻璃半透明 | 墨绿实色 + 白色文字 |
| 圆角 | `10px` / `16px`（较夸张）| `2px`-`10px`（克制）|
| 动效 | 复杂弹簧曲线 | 简洁 ease-out 二次曲线 |

### 关键样式重写

- **登录页**：双栏网格（1fr + 440px），左侧带顶部绿色横条的品牌区，右侧白底表单卡片
- **侧边栏**：墨绿背景 + 白色半透明菜单项 + 用户卡片 + 底部版本号
- **按钮**：墨绿主按钮，取消渐变背景，添加 hover 阴影
- **标签**：极小圆角（2px）+ 小写字母间距
- **统计数字**：Georgia 衬线字体，数字感更强

### 文件视角

| 文件 | 改动 |
|---|---|
| `src/style.css` | 完全重写，968 行 → ~700 行 |
| `src/views/login/LoginView.vue` | 重构为 Editorial 分栏布局 |
| `src/views/admin/AdminLayout.vue` | 侧边栏改为墨绿实底，用户卡片重构 |
| `src/views/admin/AdminDashboard.vue` | Dashboard 保持结构，样式由全局覆盖 |
| `src/views/student/StudentLayout.vue` | 同 AdminLayout，学生端风格一致 |

## 4. 验证方式

```sh
cd Oline-AQ-vue && npm run build
# ✓ built in 565ms — 无错误
```

## 5. 风险与后续优化

- Element Plus 默认样式通过 CSS 变量深度覆盖，确保与新配色一致
- 登录页响应式（`@media (max-width: 1100px)`）自动切换为单栏堆叠
- 侧边栏响应式（`@media (max-width: 980px)`）自动折叠为横条
- 后续可进一步优化：深色模式切换、SVG 图标替换为 Heroicons、字体加载优化