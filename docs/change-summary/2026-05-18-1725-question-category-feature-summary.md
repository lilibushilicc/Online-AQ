# 题目分类功能完善改动总结

## 1. 改动目标

完善"上传题目分类功能"，使分类不仅在上传时可设置，还能在全流程中被有效利用：

- 教师在考试创建时可按分类筛选题目，快速组卷
- 教师可对已有题目批量设置/修改分类
- 学生练习时可按分类筛选题目，分类练习
- 后端提供独立分类查询接口和服务端筛选能力

## 2. 用户视角

- **考试创建页**：新增分类下拉筛选，选择分类后只展示该分类下的题目，每道题旁显示分类标签；切换分类时自动全选当前分类下的所有题目。
- **题库管理页**：新增"批量设分类"按钮和分类名称输入框，选中题目→输入分类名→点击按钮即可批量分类。
- **学生练习页**：新增分类下拉筛选，选择分类后只展示该分类的题目；切换分类自动重置答题状态。

## 3. 前端实现

### 页面

- **AdminExams.vue**：
  - 新增 `formCategory` 响应式变量和 `categoryOptions` 计算属性（从全量题目推导分类列表）
  - 新增 `filteredQuestions` 计算属性，按 `formCategory` 过滤题目
  - 表单区域增加分类 `<el-select>` 下拉框 + 显示 "显示 X / Y 道题"
  - 题目复选框列表改为遍历 `filteredQuestions`
  - 每道题旁增加分类 `<el-tag>` 显示
  - 切换分类时自动将 `form.questionIds` 重置为过滤后题目的 ID 列表
- **AdminQuestions.vue**：
  - 新增 `batchCategoryValue` 响应式变量
  - 新增 `updateSelectedCategory` 方法，调用 store 的批量分类 action
  - 工具栏新增分类名称输入框和"批量设分类"按钮
- **StudentPractice.vue**：
  - 新增 `practiceCategory` 响应式变量、`categoryOptions` 和按分类过滤的 `questions` 计算属性
  - 新增 `handleCategoryChange` 切换分类时重置答题/提交状态
  - 页面顶部增加分类 `<el-select>` 下拉框 + 显示 "显示 X / Y 道题"
  - 空状态区分"题库无题"和"当前分类无题"

### 状态管理

- **exam.ts store**：
  - `state` 新增 `categories: string[]`
  - 新增 `loadQuestions(category?)` 支持传分类参数进行服务端筛选
  - 新增 `loadCategories()` 调用 `GET /api/questions/categories`
  - 新增 `updateQuestionCategories()` 调用 `POST /api/questions/batch-category`

### 接口变化

| 接口 | 方法 | 变化 |
|---|---|---|
| `GET /api/questions` | GET | 新增可选参数 `?category=`，服务端按分类筛选 |
| `GET /api/questions/categories` | GET | 新增，返回去重非空分类列表 |
| `POST /api/questions/batch-category` | POST | 新增，批量设置题目分类 |

## 4. 后端实现

- **QuestionService.java**：
  - `list()` → `list(category)`：增加 `LambdaQueryWrapper` 条件查询，支持按 `category` 字段筛选
  - 新增 `categories()`：查询 `question` 表去重、非空的 `category` 字段，返回 `List<String>`
  - 新增 `updateCategoryBatch(questionIds, category)`：循环更新题目分类
- **QuestionController.java**：
  - `list()` 增加 `@RequestParam(required = false) String category` 参数
  - 新增 `GET /api/questions/categories` 端点
  - 新增 `POST /api/questions/batch-category` 端点
- **QuestionBatchCategoryRequest.java**（新增 DTO）：
  - 包含 `questionIds: List<Integer>` 和 `category: String`

## 5. 数据库与数据流

无数据库结构变化，仅利用已有的 `question.category` 字段（VARCHAR(100)）。

数据流：

1. 上传题目时输入分类 → 解析时写入 `question.category`
2. `GET /api/questions/categories` 从 `question` 表聚合去重分类列表返回前端
3. 前端各页面按分类筛选时，可走客户端过滤或服务端 `?category=` 参数
4. 批量设置分类：前端选中题目 ID → `POST /api/questions/batch-category` → 后端循环更新

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `GET /api/questions` | GET | 查询题目列表 | `?category=Java`（可选） | 题目列表 |
| `GET /api/questions/categories` | GET | 获取全部分类 | 无 | 字符串数组 |
| `POST /api/questions/batch-category` | POST | 批量设置分类 | `questionIds`、`category` | 通用成功响应 |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../controller/QuestionController.java` | 新增分类查询和批量分类端点 |
| `Oline-AQ-spring/.../service/QuestionService.java` | 分类筛选、分类列表聚合、批量分类更新逻辑 |
| `Oline-AQ-spring/.../dto/QuestionBatchCategoryRequest.java` | 批量分类请求 DTO |
| `Oline-AQ-vue/src/stores/exam.ts` | 新增 `loadCategories`、`loadQuestions(category)`、`updateQuestionCategories` |
| `Oline-AQ-vue/src/views/admin/AdminExams.vue` | 考试创建页增加分类筛选 |
| `Oline-AQ-vue/src/views/admin/AdminQuestions.vue` | 题库管理页增加批量设分类 |
| `Oline-AQ-vue/src/views/student/StudentPractice.vue` | 学生练习页增加分类筛选 |

## 8. 验证方式

```sh
cd Oline-AQ-vue
npm run type-check        # 通过
npm run build-only        # 构建成功（423ms）

cd Oline-AQ-spring
.\mvnw.cmd compile -q     # 编译通过
.\mvnw.cmd test -q        # 测试通过
```

## 9. 风险与后续优化

- 批量分类/设分目前仍基于逐条 `updateById`，题库量大时可优化为批量 SQL
- 分类为自由文本，没有受控词表，后续可增加分类管理页面（CRUD）
- 考试创建页切换分类时自动全选，对于跨分类组卷需手动取消/勾选
- 后续可考虑将 `upload_file` 表也增加 `category` 字段，方便同一文件重解析时复用分类
