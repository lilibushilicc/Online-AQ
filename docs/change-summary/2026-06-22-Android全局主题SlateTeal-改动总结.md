# Android 全局主题 Slate & Teal 改动总结

## 1. 改动目标

将 Android 学生端（Oline-AQ-app）从 Catppuccin 主题全面切换为 Slate & Teal Executive 深色风格，统一登录页、底部导航、全局色彩、渐变背景、输入框等视觉元素，形成一致的暗色玻璃拟态设计体系。

## 2. 用户视角

- 所有页面变为深色背景（`#051424`），主色调为 Teal `#57F1DB`
- 登录页：深色毛玻璃卡片、暗色输入框、Teal 主按钮、移除底部导航
- 底部导航：半透明悬浮条、选中态 Teal 高亮
- 整体视觉更统一、更具品牌感

## 3. 前端实现（Android）

- `values/colors.xml` 和 `values-night/colors.xml`：Catppuccin → Slate & Teal Executive 色板
- `values/themes.xml` 和 `values-night/themes.xml`：适配新 Material3 色板
- `color/bottom_nav_color.xml`：选中态 Teal `#57F1DB`
- 所有 drawable 渐变/背景资源重写（`bg_gradient_top`、`bg_login_btn`、`bg_bottom_nav`、`bg_profile_avatar`、`bg_option_*`、`bg_question_nav_*` 等）
- `activity_login.xml`：完全重写为深色玻璃态登录页
- 新增 `drawable/bg_input_login.xml` 输入框 selector
- 新增 `drawable/bg_btn_ghost.xml` ghost 按钮背景
- 登录页输入框从 `TextInputEditText` 改为 `EditText`

## 4. 后端实现

无后端改动。

## 5. 数据库与数据流

无数据库改动。

## 6. 接口说明

无接口改动。

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `app/src/main/res/values/colors.xml` | 亮色调色板（Slate & Teal） |
| `app/src/main/res/values-night/colors.xml` | 暗色调色板（Slate & Teal） |
| `app/src/main/res/values/themes.xml` | 亮色 Material3 主题 |
| `app/src/main/res/values-night/themes.xml` | 暗色 Material3 主题 |
| `app/src/main/res/layout/activity_login.xml` | 登录页布局（重写） |
| `app/src/main/res/drawable/bg_gradient_top.xml` | 顶部渐变背景 |
| `app/src/main/res/drawable/bg_login_btn.xml` | 登录按钮渐变 |
| `app/src/main/res/drawable/bg_bottom_nav.xml` | 底部导航背景 |
| `app/src/main/res/drawable/bg_input_login.xml` | 登录输入框背景 (新增) |
| `app/src/main/res/drawable/bg_btn_ghost.xml` | Ghost 按钮背景 (新增) |
| `app/src/main/java/.../login/LoginActivity.kt` | 登录页 Activity（适配 EditText） |

## 8. 验证方式

```sh
cd Oline-AQ-app
.\gradlew.bat assembleDebug
```

构建成功，无编译/链接错误。

## 9. 风险与后续优化

- 登录页使用硬编码颜色（不随系统主题变化），这是设计意图（纯暗色风格）
- 后续可统一通过 `DESIGN.md` 维护设计 Token
- 需要验证在真机上的视觉效果是否与 HTML 原型一致
