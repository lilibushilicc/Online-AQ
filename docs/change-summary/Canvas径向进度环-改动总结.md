# Canvas 径向进度环改动总结

## 1. 改动目标

新增 CanvasRadialGauge 组件，替换仪表盘和练习页面中通过率、答题进度、练习进度的纯文本显示，以更直观的环形进度条展示。

## 2. 用户视角

- 管理后台仪表盘：通过率、答题进度、练习进度从纯文字改为带动画的径向环形进度环
- 学生练习页：练习完成进度改为环形进度环
- 学生考试详情：答题进度改为环形进度环

## 3. 前端实现

### 组件：`CanvasRadialGauge.vue`

- 使用 `<canvas>` + `requestAnimationFrame` 驱动
- 支持参数：`value`（进度值）、`size`（尺寸）、`strokeWidth`（环宽）、`color`（颜色）、`label`（标签文字）
- 动画效果：从 0 到目标值平滑过渡
- 脉冲呼吸光效：环形末端有发光光晕

### Props

| Prop | 类型 | 默认值 | 说明 |
|---|---|---|---|
| value | number | 0 | 进度值 0-100 |
| size | number | 120 | 画布尺寸 |
| strokeWidth | number | 8 | 环宽 |
| color | string | '#8b5cf6' | 主色 |
| label | string | '' | 中心标签文字 |

## 4. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/views/components/CanvasRadialGauge.vue` | 径向进度环组件 |
| `Oline-AQ-vue/src/views/admin/AdminDashboard.vue` | 使用径向环展示通过率/答题进度/练习进度 |
| `Oline-AQ-vue/src/views/student/StudentPractice.vue` | 使用径向环展示练习完成进度 |
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | 使用径向环展示考试答题进度 |

## 5. 风险与后续

- 高 DPI 缩放已在 `onMounted` 中处理
- 后续可增加多环叠加、渐变色彩配置
