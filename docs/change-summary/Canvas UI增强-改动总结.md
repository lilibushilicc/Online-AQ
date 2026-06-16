# Canvas UI 增强改动总结

## 1. 改动目标

用 Canvas 原生绘图替换部分静态/第三方组件，提升视觉效果与性能，减少外部依赖。

## 2. 用户视角

- 登录页：背景为交互式粒子动画，底部有流动波浪
- 管理后台：仪表盘分数分布使用 Canvas 环形图（更流畅的动画）
- 管理后台/学生端：背景有柔和的光晕粒子氛围

## 3. 前端实现

### 新增组件

| 组件 | 用途 | 使用位置 |
|---|---|---|
| `CanvasParticles.vue` | 登录页交互式粒子背景 | `LoginView.vue` |
| `CanvasWaves.vue` | 登录页底部流动波浪 | `LoginView.vue` |
| `CanvasDoughnut.vue` | 仪表盘分数分布环形图 | `AdminDashboard.vue` |
| `CanvasAmbient.vue` | 后台/学生端氛围光晕粒子 | `AdminLayout.vue`, `StudentLayout.vue` |

### 技术要点

- 所有组件使用 `<canvas>` + `requestAnimationFrame` 驱动
- CanvasParticles：鼠标悬浮粒子产生吸引/排斥交互，粒子带发光效果
- CanvasWaves：多正弦波叠加，随时间流动
- CanvasDoughnut：弧形分段 + 发光阴影动画
- CanvasAmbient：随机光晕粒子缓慢飘动

## 4. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/views/components/CanvasParticles.vue` | 粒子动画组件 |
| `Oline-AQ-vue/src/views/components/CanvasWaves.vue` | 波浪动画组件 |
| `Oline-AQ-vue/src/views/components/CanvasDoughnut.vue` | 环形图组件 |
| `Oline-AQ-vue/src/views/components/CanvasAmbient.vue` | 氛围光晕组件 |

## 5. 风险与后续

- Canvas 在高 DPI 设备上需处理 `devicePixelRatio` 缩放，否则模糊
- 大量粒子可能影响低端设备性能，可考虑粒子数量自适应
