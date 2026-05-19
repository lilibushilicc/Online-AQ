# 在线题库接口修复改动总结

## 1. 改动目标

修复"在线题库不对"问题。分页重构后，前端向 `/api/questions` 发送请求时，`category` 参数值为 `undefined`，导致 axios 序列化后后端可能收到空字符串 `category=`，使分类过滤异常。

## 2. 用户视角

无界面变化。修复后题库管理页、练习页能正确加载全部题目，按分类筛选功能恢复正常。

## 3. 前端实现

- **apiHelper.ts**: `apiGet` 在发送前过滤掉值为 `undefined` 的 params，避免 axios 误序列化。
- **api/index.ts**: `loadQuestionsApi` 改为仅在 `category` 有值时加入 params 对象，不再携带 `category: undefined`。

## 4. 文件更改

| 文件 | 作用 |
|---|---|
| `Oline-AQ-vue/src/utils/apiHelper.ts` | `apiGet` 新增 Object.entries 过滤 undefined params |
| `Oline-AQ-vue/src/api/index.ts` | `loadQuestionsApi` 条件传递 category 参数 |

## 5. 验证方式

```sh
npm run build    # 前端构建通过
mvn compile      # 后端编译通过
```

## 6. 风险与后续

当前修复为防御性写法。后续可统一在 axios 请求拦截器中过滤 undefined 参数，避免每个调用处单独处理。
