# 用户个人信息查看与修改功能总结

## 1. 改动目标

完善用户信息管理，支持：
- 管理员可查看所有用户的完整信息（用户名、姓名、邮箱、角色、注册时间）
- 用户可查看自己的个人信息
- 用户可修改自己的姓名、邮箱和密码

## 2. 用户视角

### 学生端
- 侧边栏新增「个人中心」入口
- 进入后展示用户名、姓名、邮箱、身份、注册时间等完整信息
- 点击「编辑资料」可修改姓名和邮箱
- 点击「修改密码」可设置新密码

### 管理端
- 侧边栏新增「个人中心」入口，功能与学生端一致
- 「学生管理」页的表格新增"邮箱"和"注册时间"列，管理员可以查看所有学生的完整注册信息

## 3. 前端实现

### 页面
- `src/views/student/StudentProfile.vue` — 个人中心页面，包含个人信息展示卡片、编辑资料对话框、修改密码对话框
- 布局使用 `el-descriptions` + `el-dialog`，最大宽度 640px 居中

### 路由
- 学生端：`/student/profile`
- 管理端：`/admin/profile`（复用同一个组件）
- 两侧边栏均新增「个人中心」菜单项

### 类型
- `User` 接口新增 `email?: string` 和 `createTime?: string` 字段

### API
- `getMyProfileApi()` — `GET /api/users/me`
- `updateMyProfileApi(payload)` — `PUT /api/users/me`

## 4. 后端实现

### Controller
- `UserController.getMyProfile(@CurrentUser)` — `GET /api/users/me`，返回当前用户完整信息（不含密码）
- `UserController.updateMyProfile(@CurrentUser, body)` — `PUT /api/users/me`，当前用户修改自己的 realName/email/password
- `UserController.update` — `PUT /api/users/{userId}` 补充 email 参数

### Service
- `UserService.getById(userId)` — 按 ID 查询用户
- `UserService.update(userId, realName, email, password)` — 扩展支持 email 更新

## 5. 数据库视角

无数据库变更。`sys_user` 表已有 `email` 和 `create_time` 字段。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/users/me` | GET | 获取当前用户完整信息 | 无（从 JWT 取用户 ID） | `SysUser`（不含 password） |
| `/api/users/me` | PUT | 修改当前用户个人资料 | `{ realName?, email?, password? }` | 更新后的 `SysUser` |
| `/api/users/{userId}` | PUT | 管理员修改用户信息 | `{ realName?, email?, password? }` | 更新后的 `SysUser` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `service/UserService.java` | 新增 `getById()`，`update()` 扩展 email 参数 |
| `controller/UserController.java` | 新增 `/me` 两个端点，`/{userId}` 扩展 email |
| `types/index.ts` | User 类型补充 email/createTime |
| `api/index.ts` | 新增 getMyProfileApi/updateMyProfileApi |
| `views/student/StudentProfile.vue` | **新增** 个人中心页面 |
| `router/index.ts` | 新增 `/student/profile` 和 `/admin/profile` 路由 |
| `views/student/StudentLayout.vue` | 侧边栏新增"个人中心" |
| `views/admin/AdminLayout.vue` | 侧边栏新增"个人中心" |
| `views/admin/AdminStudents.vue` | 表格补充邮箱和注册时间列 |

## 8. 验证方式

```powershell
# 后端
cd Oline-AQ-spring
.\mvnw.cmd compile -q

# 前端
cd Oline-AQ-vue
npm run build
```

前后端编译通过，无错误。

## 9. 风险与后续优化

- 当前邮箱修改无需验证，后续可增加邮箱修改验证码流程。
- 密码修改未要求输入旧密码验证，后续可补充。
- 管理员侧个人中心与编辑学生共用同一个组件，打开顺序和 tab 切换可能不符合预期。
