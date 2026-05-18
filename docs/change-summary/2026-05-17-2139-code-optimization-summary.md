# 代码优化改动总结

## 1. 改动目标

本次改动聚焦“在不改变现有功能行为的前提下优化代码可维护性”，主要解决以下问题：

- 前端请求层和状态管理存在重复的本地存储读写逻辑。
- 前端路由权限控制依赖路径前缀判断，扩展性较弱。
- 后端考试与交卷服务存在重复查库、重复组装对象和重复状态更新逻辑。
- 根目录 `README.md` 存在编码不可读问题，不满足日常维护与交接要求。

## 2. 用户视角

- 登录、路由跳转、考试创建、考试发布、学生交卷、成绩查询等功能的使用方式不变。
- 页面表现和接口行为没有新增功能变化。
- 项目说明文档恢复为可读状态，开发者可以正常按 README 启动和验证项目。

## 3. 前端实现

- 页面：
  - 未新增页面，也未调整页面交互流程。
- 组件：
  - 未新增组件。
- 路由：
  - 将原本基于路径前缀的权限控制整理为基于 `meta.requiresAuth` 与 `meta.role` 的守卫判断。
  - 提取默认跳转逻辑，避免管理员和学生角色回退路径散落在守卫中。
- 状态管理：
  - 在 `exam` store 中统一当前用户读取、持久化和清理逻辑。
  - 对 `localStorage` 中用户信息增加解析兜底，避免脏数据导致运行时异常。
- 接口请求：
  - 统一使用解构后的 `data`，减少多层 `response.data` 重复访问。
  - 请求拦截器中补充 `headers` 初始化，避免赋值时依赖隐式对象状态。
- 样式与布局：
  - 本次未改动样式。

## 4. 后端实现

- Controller：
  - 未改动。
- Service：
  - `ExamService` 将考试详情和创建考试中的重复题目查询合并为题目映射读取。
  - `ExamService` 提取总分计算和状态更新辅助方法，减少重复控制流。
  - `ResultService` 提取答案映射构建与学生答题记录构建逻辑，简化提交流程。
  - `ResultService` 在交卷处理中集中构造题目映射，避免循环中重复查库代码散落。
- Mapper：
  - 未改动。
- Entity / DTO / VO：
  - 未改动。
- 配置：
  - 未改动。

## 5. 数据库与数据流

本次没有新增表、字段或迁移脚本，也没有改变数据库写入链路：

- 登录后仍由前端保存 token 和用户信息。
- 创建考试时仍按题目列表计算总分并写入 `exam`、`exam_question`。
- 学生提交考试时仍写入 `student_answer` 和 `exam_result`。
- 数据流只做了代码层面的组织优化，没有改变存储结构和返回格式。

## 6. 接口说明

本次未新增或删除接口，也未修改请求参数和响应结构。

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/auth/login` | POST | 用户登录 | `username`、`password`、`role` | 用户信息与 token |
| `/api/exams` | GET | 获取考试列表 | 无 | 考试列表 |
| `/api/exams` | POST | 创建考试 | `examName`、`description`、`duration`、`questionIds` | 创建后的考试信息 |
| `/api/exams/{examId}` | GET | 获取考试详情 | `examId` | 考试信息与题目列表 |
| `/api/exams/{examId}/publish` | PUT | 发布考试 | `examId` | 通用成功响应 |
| `/api/exams/{examId}/close` | PUT | 关闭考试 | `examId` | 通用成功响应 |
| `/api/exams/{examId}/submit` | POST | 提交考试 | `studentId`、`useTime`、`answers` | 总分、对错题统计 |
| `/api/results/my` | GET | 查询学生自己的成绩 | 无 | 成绩列表 |
| `/api/results/exam/{examId}` | GET | 查询考试成绩 | `examId` | 成绩列表 |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/utils/request.ts` | 统一 token 存取与 401 清理逻辑 |
| `Oline-AQ-vue/src/stores/exam.ts` | 收敛用户持久化和接口响应解构逻辑 |
| `Oline-AQ-vue/src/router/index.ts` | 使用路由元信息统一权限控制 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/ExamService.java` | 合并考试题目查询与状态更新逻辑 |
| `Oline-AQ-spring/src/main/java/com/example/olineaqspring/service/ResultService.java` | 简化交卷评分与答题记录组装流程 |
| `README.md` | 修复根目录项目说明文档编码并同步当前结构说明 |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm run build

cd Oline-AQ-spring
.\mvnw.cmd test
```

验证结果：

- 前端 `npm run build` 成功。
- 后端 `.\mvnw.cmd test` 成功。
- 前端构建仍提示单个 chunk 超过 500kB，这是现有打包体积告警，不是本次整理引入的问题。

## 9. 风险与后续优化

- 当前 `Oline-AQ-spring/sql/init.sql` 中仍存在历史中文编码异常，影响初始化种子数据的可读性，本次未改动以避免连带修改演示数据。
- `ExamService` 和 `ResultService` 目前仍通过多次 `selectById` 组装题目映射，虽然代码已收敛，但性能上仍可继续优化为批量查询。
- 前端打包体积偏大，后续可考虑按管理端/学生端做路由懒加载与分包。
- 如后续继续做“代码优化”专项，建议下一步集中处理 SQL 初始化脚本编码、前端视图文案编码和接口类型定义复用。
