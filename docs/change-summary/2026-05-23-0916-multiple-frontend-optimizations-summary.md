# 前端多处优化改动总结

## 1. 改动目标

本次改动包含三个独立任务：优化登录页面视觉效果、调整考试列表卡片布局、修复学生端无法作答的 Bug。

## 2. 用户视角

- **登录页**：登录卡片顶部新增圆形渐变头像图标，标题改为"欢迎回来"，新增学生/教师身份切换 Segmented Control，账号输入框添加前缀图标。用户可直接在登录卡片切换角色，不再需要点击 Logo 打开管理员弹窗。
- **考试列表**：试卷卡片从两列布局改为单列满宽排列，每个卡片占据整行宽度，视觉信息更清晰易读。
- **答题页**：选择题选项点击可正常选中并记录答案，填空/简答输入框可正常输入内容，草稿自动保存功能正常运转。

## 3. 前端实现

### 登录页优化 (`LoginView.vue`)

- 新增 `.login-card-header` 容器包裹头像图标与标题
- 新增 `.login-card-avatar` 圆形渐变头像图标（使用 `User` 图标）
- 移除硬编码的"学生登录"标题，改为动态 "欢迎回来"
- 新增 `el-segmented` 组件实现学生/教师身份切换，通过 `form.role` 双向绑定
- 新增 `subtitleText` 计算属性，动态显示副标题说明
- 账号输入框新增 `School` 图标前缀，密码输入框新增 `User` 图标前缀
- placeholder 根据角色动态切换："学号 / 邮箱" 或 "管理员账号"
- scoped CSS 使用 `:deep()` 穿透到 Element Plus 子组件样式，确保前缀图标内边距

### 考试列表布局调整 (`StudentExams.vue`)

- 可参加考试区域 `el-col` 从 `:xs="24" :md="12"` 改为 `:span="24"`
- 历史试卷区域 `el-col` 同样改为 `:span="24"`
- 每个卡片通过已有 `height: 100%` 样式自动撑满整列高度

### 答题 Bug 修复 (`StudentExamDetail.vue`)

- 新增 `selectAnswer(questionId, value)` 方法，直接操作 `answers.value[questionId] = value`
- `el-radio-group` 从 `v-model="answers[question.questionId]"` 改为 `:model-value` + `@change` 回调
- `el-input`（填空/简答）从 `v-model` 改为 `:model-value` + `@update:model-value` 回调
- 使用 `String(val)` 安全转换传入值，避免类型不匹配

## 4. 后端实现

本次未改动后端代码。

## 5. 数据库与数据流

无变化。

## 6. 接口说明

无新增接口。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/views/login/LoginView.vue` | 登录页重构，新增角色切换、头像图标、前缀图标 |
| `Oline-AQ-vue/src/views/student/StudentExams.vue` | 卡片布局改为单列满宽 |
| `Oline-AQ-vue/src/views/student/StudentExamDetail.vue` | 修复 v-model 动态键绑定导致的答题不可用 Bug |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npx vue-tsc --build --noEmit    # 类型检查通过
npx vite build                   # 构建成功
```

验证结果：TypeScript 类型检查无错误，Vite 构建成功。

## 9. 风险与后续优化

- 登录页 Segmented Control 切换角色后，如果输入框已有内容不会自动清空，后续可考虑加角色切换清空表单
- 答题页使用 `String(val)` 做类型转换，可考虑进一步提取统一类型守卫
