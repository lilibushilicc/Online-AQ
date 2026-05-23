# 学生端手机页面 Vant 适配改动总结

## 1. 改动目标

本次改动的目标是只调整 Web 前端学生端的手机访问体验，不改变桌面端页面结构和现有业务逻辑。

原有学生端页面主要基于 Element Plus 的桌面布局，虽然有部分响应式处理，但手机访问时仍然存在导航层级偏深、标题区占用空间大、公告入口不集中、底部操作距离手指热区较远等问题。此次改动引入 Vant，并在手机宽度下切换到移动端专用壳布局，使学生端在手机上具备更符合手持场景的导航与信息组织方式，同时继续复用原有考试、成绩、练习、错题本、个人中心等功能逻辑。

## 2. 用户视角

- 学生使用桌面浏览器访问时，页面保持原有桌面版样式和结构，不受影响。
- 学生使用手机或窄屏访问时，学生端顶部会切换为移动导航栏，并显示当前页面标题。
- 手机端学生页面新增底部标签导航，直接进入考试、成绩、练习、错题、我的五个核心区域。
- 手机端学生页面新增公告按钮与底部抽屉，学生可在移动端集中查看未读公告并标记已读。
- 学生在手机端打开考试、成绩、练习、错题本、个人中心时，功能与网页端保持一致，仍使用同一路由、同一接口和同一数据。

## 3. 前端实现

- 页面：
  - 新增 `src/views/student/StudentMobileLayout.vue` 作为学生端手机壳布局。
  - 保留原 `StudentLayout.vue` 作为桌面端布局。
  - `StudentLayoutWrapper.vue` 根据屏幕宽度动态切换手机壳或桌面壳。
- 组件：
  - 手机壳使用 `Vant` 的 `NavBar`、`Tabbar`、`Popup`、`Badge`、`Button`、`Tag` 组织移动端导航与公告入口。
  - 学生业务页面本身未拆散业务逻辑，继续复用原有页面组件和内部接口调用。
- 路由：
  - 学生端路由地址不变，手机端和桌面端共用 `/student/*` 路由。
  - 明细页与详情页在手机壳中自动启用返回箭头。
- 状态管理：
  - 继续复用 `Pinia` 中的 `useExamStore`。
  - 手机端公告未读数通过现有公告接口直接获取，不新增全局 store。
- 接口请求：
  - 继续复用 `src/api/index.ts` 中已有学生端接口。
  - 公告读取沿用 `getUnreadAnnouncementsApi`、`markAnnouncementReadApi`、`markAllAnnouncementsReadApi`。
- 样式与布局：
  - `src/style.css` 新增 Vant 主题变量映射。
  - 新增 `.student-mobile-shell` 相关样式，仅在移动端学生壳下生效。
  - 保持桌面端原 `.page`、`.shell`、`.sidebar` 体系不变。

## 4. 后端实现

- Controller：无改动。
- Service：无改动。
- Mapper：无改动。
- Entity / DTO / VO：无改动。
- 配置：无后端配置改动。

本次改动仅发生在前端展示层与前端依赖层，没有调整 Spring Boot 后端逻辑。

## 5. 数据库与数据流

本次没有新增表、字段或迁移脚本。

数据流仍沿用原有链路：

1. 学生在手机端进入 `student` 路由。
2. 前端根据屏幕宽度切换到移动端壳布局。
3. 业务页面继续调用原有考试、成绩、错题本、个人资料、公告等接口。
4. 后端返回的数据结构保持不变。
5. 前端仅改变导航组织方式和移动端样式呈现。

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/announcements/unread` | GET | 获取学生未读公告及公告列表 | 无 | `UnreadInfo` |
| `/api/announcements/{id}/read` | POST | 标记单条公告已读 | `id` | 成功状态 |
| `/api/announcements/read-all` | POST | 标记全部公告已读 | 无 | 成功状态 |
| `/api/exams/student` | GET | 获取学生考试列表 | 无 | `Exam[]` |
| `/api/results/my` | GET | 获取学生成绩记录 | 无 | `ExamResult[]` |
| `/api/questions` | GET | 获取练习题库 | `page`、`pageSize`、`category` | `PageResult<Question>` |
| `/api/wrong-notebooks` | GET/POST | 获取或创建错题本 | 名称、描述 | `WrongNotebook[]` / `WrongNotebook` |
| `/api/users/me` | GET/PUT | 获取或更新当前学生资料 | 姓名、邮箱、密码字段 | `User` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/main.ts` | 注册 Vant，并保留 Element Plus 与现有应用初始化流程 |
| `Oline-AQ-vue/src/composables/useMobile.ts` | 统一检测当前是否为手机宽度 |
| `Oline-AQ-vue/src/views/student/StudentMobileLayout.vue` | 学生端手机壳布局，负责顶部导航、公告抽屉、底部 Tab 导航 |
| `Oline-AQ-vue/src/views/student/StudentLayoutWrapper.vue` | 在手机端与桌面端布局之间切换 |
| `Oline-AQ-vue/src/style.css` | 增加 Vant 主题变量和手机端学生壳样式覆盖 |
| `Oline-AQ-vue/package.json` | 新增 `vant` 前端依赖 |
| `README.md` | 补充本次更新记录，并同步前端技术栈与学生端能力说明 |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm install
npm run build
```

验证结果：

- `npm install` 成功，已加入 `vant` 依赖。
- `npm run build` 成功，包含 `vue-tsc --build` 和 `vite build`。
- 构建过程中仍有大体积 chunk 警告，但不是本次改动引入的编译错误，产物已正常生成。

## 9. 风险与后续优化

- 当前手机端改造以“移动壳 + 现有业务页复用”为主，优点是对桌面端零侵入，但手机端内部个别页面仍保留部分 Element Plus 组件结构，视觉上是 Vant 风格主壳与原业务内容的组合。
- 学生端业务功能已与网页端保持一致，但若后续希望进一步提升手机端体验，可以继续把考试详情、成绩详情、错题本详情等页面细化为专门的移动端组件。
- 当前前端同时使用 Element Plus 和 Vant，两套 UI 库并存会增加一点打包体积与样式维护成本。
- 构建输出中依然存在较大的 vendor chunk，后续可继续做依赖拆包与更细粒度的懒加载优化。
