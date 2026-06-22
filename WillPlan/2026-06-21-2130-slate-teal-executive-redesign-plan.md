# Slate & Teal Executive 高端后台系统整合实施方案

## 目标

将 Online-AQ 考试系统全面升级为 **Slate & Teal Executive** 设计系统：深海午夜灰基底 + 青绿色点缀 + 玻璃拟态 + 品牌开场动画 + 极致高效页面切换。

## 实施阶段

### Phase 1: 基础设计系统 (style.css 核心重写)
- [x] ~~备份原始 style.css~~
- [ ] 重写 CSS 变量体系：Slate & Teal 暗色系色彩变量
- [ ] 更新字体：引入 Hanken Grotesk Google Font
- [ ] 更新圆角/间距规范
- [ ] 更新 Element Plus 覆盖变量
- [ ] 更新 Vant 移动端覆盖变量
- [ ] 保留所有现有布局类名和结构（.shell, .sidebar, .bento-grid 等）

### Phase 2: 品牌开场动画
- [x] ~~创建 BrandSplash.vue 组件~~
- [ ] 在 index.html 中注入 Hanken Grotesk 字体
- [ ] 在 App.vue 中集成 BrandSplash 到路由根路径('/')
- [ ] 实现登录页从中心缩放展开的转场动画
- [ ] 添加 localStorage 标志控制开场动画只显示一次

### Phase 3: 路由切换动画 + 智能防误触
- [ ] 在 router/index.ts 中实现全局页面过渡动画（中心缩放+透明渐变）
- [ ] 实现路由守卫防重复点击拦截
- [ ] 实现滚动位置记忆
- [ ] 实现页面状态保留

### Phase 4: 全局组件样式统一
- [ ] 按钮样式：主按钮 teal 渐变，幽灵按钮 slate 边框
- [ ] 输入框：内嵌凹陷质感 + teal 聚焦描边
- [ ] 卡片：无外部投影，45°渐变 + 浅色内顶边
- [ ] 标签 Chip：无填充 + 细灰边框
- [ ] 表格：slate 灰交替底色
- [ ] 弹窗/抽屉：毛玻璃遮罩 + teal 外发光

### Phase 5: 侧边栏导航重构
- [ ] 磨砂渐变高亮 hover 状态
- [ ] 选中项仅 teal 细侧边标识
- [ ] 折叠/展开动画优化

### Phase 6: Canvas 背景优化
- [ ] 更新 CanvasParticles 颜色为 Slate & Teal 色系
- [ ] 更新 CanvasAmbient 颜色为 Slate & Teal 色系

### Phase 7: 验证与测试
- [ ] npm run build 构建通过
- [ ] 暗色模式正常
- [ ] 亮色模式正常
- [ ] 移动端布局正常
- [ ] 开场动画流程正常
- [ ] 路由切换流畅

## 关键技术决策

1. **CSS 变量渐进替换**：保留所有现有 class 名称，只修改变量值
2. **开场动画只播放一次**：使用 localStorage 标记，避免每次刷新都播放
3. **暗色优先**：默认主题设为暗色，亮色作为可选
4. **字体回退**：Hanken Grotesk 加载失败时使用 Inter 回退
5. **动画性能**：所有动画使用 transform + opacity GPU 加速
