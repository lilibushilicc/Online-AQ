# 错题本多本支持与导航优化改动总结

## 1. 改动目标

本次改动实现三个核心目标：
1. **支持多种错题本**：学生可以创建多个自定义错题本，将不同错题分类整理，而非所有错题混在一起
2. **优化内容布局**：错题本首页改为卡片式网格布局，清晰展示每个错题本的名称、描述和题量
3. **优化导航栏**：取消"成绩与错题"二级子菜单，将"我的成绩"和"错题本"提升为顶级菜单项，减少点击层级

## 2. 用户视角

### 错题本列表页（`/student/wrong-book`）
- 顶部显示三个统计卡片：错题本数量、全部错题数、已分类错题数
- "全部错题"卡片固定在首位，点击查看所有错题（原错题本功能保留）
- 每个自定义错题本以卡片形式展示，包含名称、描述、题量
- 每个卡片可点击查看详情、编辑名称/描述、删除
- 点击"新建错题本"按钮，弹窗输入名称和描述即可创建

### 错题本详情页（`/student/wrong-book/:notebookId`）
- 头部显示错题本名称和描述，可返回列表或删除整个错题本
- 统计卡片显示错题数量和涉及考试数量
- 错题按考试分组展示，每题显示题干、选项、答案对比
- 每题有"移出"按钮，可将错题从本中移除

### "全部错题"视图（`/student/wrong-book/all`）
- 展示所有错题（原错题本功能）
- 每题新增"加入错题本"按钮，可选择目标错题本将题目添加进去

### 导航栏变化
- 原来"成绩与错题"子菜单下的"我的成绩"和"错题本"提升为顶级菜单
- 图标更换：错题本使用 `Notebook` 图标

## 3. 前端实现

### 页面

| 文件 | 作用 |
|---|---|
| `StudentWrongBook.vue` | 错题本列表页，卡片网格布局，支持创建/编辑/删除错题本 |
| `StudentWrongBookDetail.vue` | 错题本详情页，展示错题本内题目，支持移出和加入操作 |

### 组件
- `StatCards.vue`：扩展支持 `columns=3` 的 3 列布局

### 路由
```typescript
{ path: '/student/wrong-book', component: StudentWrongBook, ... }
{ path: '/student/wrong-book/:notebookId', component: StudentWrongBookDetail, ... }
```

### 状态管理（`stores/exam.ts`）
新增类型：
- `WrongNotebook`：错题本（id, 名称, 描述, 创建时间, 题数）
- `NotebookDetail`：错题本详情（笔记本信息 + 题目分组）

新增 API 方法：
- `loadNotebooks()`：获取错题本列表
- `createNotebook(name, desc)`：创建错题本
- `updateNotebook(id, name, desc)`：更新错题本
- `deleteNotebook(id)`：删除错题本
- `getNotebookDetail(id)`：获取错题本详情
- `addToNotebook(notebookId, answerId)`：添加错题到错题本
- `removeNotebookItem(notebookId, itemId)`：从错题本移除题目

### 导航栏优化（`StudentLayout.vue`）
```diff
- <el-sub-menu index="performance">成绩与错题
-   <el-menu-item>我的成绩</el-menu-item>
-   <el-menu-item>错题本</el-menu-item>
- </el-sub-menu>
+ <el-menu-item index="/student/results">我的成绩</el-menu-item>
+ <el-menu-item index="/student/wrong-book">错题本</el-menu-item>
```

## 4. 后端实现

### Controller：`WrongNotebookController`

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/wrong-notebooks` | GET | 获取错题本列表（含题目计数） | 无（自动获取当前用户） | `[{notebookId, notebookName, description, createTime, itemCount}]` |
| `/api/wrong-notebooks` | POST | 创建错题本 | `{notebookName, description}` | `WrongNotebook` |
| `/api/wrong-notebooks/{id}` | PUT | 更新错题本 | `{notebookName, description}` | `WrongNotebook` |
| `/api/wrong-notebooks/{id}` | DELETE | 删除错题本及关联 | 无 | `null` |
| `/api/wrong-notebooks/{id}` | GET | 获取错题本详情 | 无 | `{notebook, groups, count}` |
| `/api/wrong-notebooks/{id}/items` | POST | 添加错题到错题本 | `{answerId}` | `{id, added}` |
| `/api/wrong-notebooks/{id}/items/{itemId}` | DELETE | 移出错题 | 无 | `null` |

### Service：`WrongNotebookService`
- `listNotebooks(studentId)`：按学生查询所有错题本
- `createNotebook(studentId, name, desc)`：创建新错题本
- `updateNotebook(id, studentId, name, desc)`：更新（含权限校验）
- `deleteNotebook(id, studentId)`：删除错题本及其下所有关联题目
- `addItem(notebookId, studentId, answerId)`：添加错题（含去重校验）
- `removeItem(notebookId, itemId, studentId)`：移除单条错题关联
- `getNotebookDetail(notebookId, studentId)`：查询错题本内所有题目，按考试分组
- `getNotebookItemCounts(studentId)`：获取错题本列表含题目计数

### Entity：`WrongNotebook`
```java
@TableName("wrong_notebook")
public class WrongNotebook {
    @TableId(type = IdType.AUTO) private Integer notebookId;
    private Integer studentId;
    private String notebookName;
    private String description;
    private LocalDateTime createTime;
}
```

### Entity：`WrongNotebookItem`
```java
@TableName("wrong_notebook_item")
public class WrongNotebookItem {
    @TableId(type = IdType.AUTO) private Integer id;
    private Integer notebookId;
    private Integer answerId;
    private LocalDateTime createTime;
}
```

## 5. 数据库与数据流

### 新增表

**`wrong_notebook`（错题本表）**

| 字段 | 类型 | 说明 |
|---|---|---|
| notebook_id | SERIAL PK | 错题本 ID |
| student_id | INTEGER | 所属学生 |
| notebook_name | VARCHAR(100) | 名称 |
| description | TEXT | 描述 |
| create_time | TIMESTAMP | 创建时间 |

**`wrong_notebook_item`（错题本-题目关联表）**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | SERIAL PK | 关联 ID |
| notebook_id | INTEGER | 错题本 ID |
| answer_id | INTEGER | student_answer 表中的答题记录 ID |
| create_time | TIMESTAMP | 添加时间 |

### 数据流

```
学生创建错题本
  → POST /api/wrong-notebooks
  → WrongNotebookController.createNotebook()
  → WrongNotebookService.createNotebook()
  → INSERT INTO wrong_notebook

学生从"全部错题"添加题目到错题本
  → POST /api/wrong-notebooks/{id}/items {answerId}
  → WrongNotebookService.addItem()
  → 校验权限 + 去重检查
  → INSERT INTO wrong_notebook_item

查看错题本详情
  → GET /api/wrong-notebooks/{id}
  → 查询 wrong_notebook_item 获取 answer_id 列表
  → 批量查询 student_answer、question、exam
  → 按 exam 分组返回
```

## 6. 关键文件

| 文件 | 作用 | 状态 |
|---|---|---|
| `.../config/SchemaInitializer.java` | 自动创建 wrong_notebook / wrong_notebook_item 表 | 修改 |
| `.../entity/WrongNotebook.java` | 错题本实体 | 新增 |
| `.../entity/WrongNotebookItem.java` | 错题本-题目关联实体 | 新增 |
| `.../mapper/WrongNotebookMapper.java` | 错题本 Mapper | 新增 |
| `.../mapper/WrongNotebookItemMapper.java` | 关联 Mapper | 新增 |
| `.../service/WrongNotebookService.java` | 错题本业务逻辑 | 新增 |
| `.../controller/WrongNotebookController.java` | 错题本接口 | 新增 |
| `Oline-AQ-vue/src/stores/exam.ts` | 新增类型定义和 API 方法 | 修改 |
| `Oline-AQ-vue/src/router/index.ts` | 新增 `/student/wrong-book/:notebookId` 路由 | 修改 |
| `Oline-AQ-vue/src/views/student/StudentLayout.vue` | 导航栏顶级菜单优化 | 修改 |
| `Oline-AQ-vue/src/views/student/StudentWrongBook.vue` | 错题本列表页（卡片网格） | 重写 |
| `Oline-AQ-vue/src/views/student/StudentWrongBookDetail.vue` | 错题本详情页 | 新增 |
| `Oline-AQ-vue/src/views/components/StatCards.vue` | 扩展支持 3 列布局 | 修改 |

## 7. 验证方式

```sh
# 前端构建验证
cd Oline-AQ-vue && npm run build

# 后端编译验证
cd Oline-AQ-spring && mvnw compile -q
```

结果：前端 Vite 构建成功，仅剩一个预存的 `AdminExams.vue` 类型错误（非本次改动引入）；后端 Maven 编译成功，无错误。

## 8. 风险与后续优化

### 当前限制
- "全部错题"中的题目被添加到某个错题本后，该题目仍在"全部错题"中显示（这是预期行为，因为"全部错题"展示所有错题）
- 同一道错题可以被添加到多个不同的错题本中（支持多对多关联）
- 删除错题本时，关联的 `wrong_notebook_item` 记录会被删除，但原始答题记录保留

### 后续优化方向
- 支持错题本内题目排序/拖拽排序
- 支持错题本导出（PDF/打印）
- 支持将错题本分享给其他学生
- 错题复习模式：按错题本逐题复习，支持标记"已掌握"
- 错题复习统计：每道错题的复习次数、掌握状态
