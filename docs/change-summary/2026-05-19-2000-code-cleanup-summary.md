# 代码清理总结

## 1. 改动目标

去除项目中冗余、未使用、不必要的代码和文件，提升可维护性和整洁度。

## 2. 改动内容

### `main.ts` — Element Plus 全量导入
- 删除 40+ 组件的手工 import 和 `app.component()` 注册（减少 67 行）
- 改为 `import ElementPlus from 'element-plus'` + `app.use(ElementPlus)`
- 保留 `import 'element-plus/dist/index.css'`

### 未使用 import 清理
| 文件 | 删除的 import | 理由 |
|---|---|---|
| `AdminConfig.vue` | `ElMessageBox` | 未使用 |
| `FeedbackService.java` | `HashMap` | 未使用 |
| `ConfigService.java` | `LambdaQueryWrapper` | 未使用 |
| `BeanUtils.java` | `BigDecimal` | 未使用 |

### 根目录清理
- 删除 `Online-AQ/`（嵌套的重复仓库副本）
- 删除 `test-new-types.txt`、`test-questions.txt`（测试数据）
- 删除 `Oline-AQ-spring/HELP.md`（Spring Initializr 默认文档）
- `task_plan.md` 移至 `WillPlan/`

## 3. 关键文件

| 文件 | 改动 |
|---|---|
| `Oline-AQ-vue/src/main.ts` | 67 行 → 9 行，Element Plus 全量导入 |
| `.../admin/AdminConfig.vue` | 删除未用 `ElMessageBox` |
| `.../service/FeedbackService.java` | 删除未用 `HashMap` |
| `.../service/ConfigService.java` | 删除未用 `LambdaQueryWrapper` |
| `.../utils/BeanUtils.java` | 删除未用 `BigDecimal` |

## 4. 验证方式

```sh
cd Oline-AQ-vue && npx vite build --logLevel error   # 前端构建成功
cd Oline-AQ-spring && .\mvnw.cmd compile -q           # 后端编译成功
```
