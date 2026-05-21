# Word/Excel 导出功能修复总结

## 1. 改动目标

修复项目中所有 Word 和 Excel 导出功能无法正常下载的问题。问题涉及题库导出、成绩导出、成绩详情导出、试卷导出和错题本导出等多个功能点。

## 2. 用户视角

- 之前：点击导出按钮后，可能出现下载失败、文件无法打开、下载无反应或只看到"导出失败"提示
- 修复后：点击导出按钮可正常下载对应格式的文件，文件能正常打开查看内容，文件名正确显示中文

## 3. 根因分析

### 3.1 后端：Content-Disposition 头部未对中文文件名编码

所有 Controller 的导出接口使用以下方式设置文件名：

```
Content-Disposition: attachment; filename=成绩导出.xlsx
```

该值包含中文（非 ASCII 字符），未按 RFC 5987 标准进行编码。某些浏览器（如新版 Chrome/Edge）可能直接拒绝下载或使用乱码文件名。

### 3.2 前端：Blob URL 过早释放

`download.ts` 中 `URL.revokeObjectURL()` 在 `link.click()` 后立即执行，但浏览器下载任务是异步的，可能在 URL 释放后才开始读取 Blob 数据，导致下载静默失败。

### 3.3 前端：Axios 拦截器未正确处理 Blob 错误响应

当后端返回 JSON 错误时，`responseType: 'blob'` 使 Axios 将 JSON 错误体当作 Blob 处理，拦截器内尝试读取 `.message` 属性失败，最终用户无法看到真实错误信息。

### 3.4 后端：ExportService 部分场景缺少 null 安全防护

`exportExamPaperExcel`、`exportExamPaperWord`、`exportResultDetail` 等方法中直接对 `questions`、`answers` 等列表调用 `.size()`，若数据为空或 null 可能导致 NPE。

## 4. 后端实现

### Controller 层改动

四个 Controller 统一使用 `ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8)` 构建符合 RFC 5987 标准的 Content-Disposition 头部，确保中文文件名正确传输：

| Controller | 导出接口 |
|---|---|
| `ResultController.java` | `//export/{examId}`、`//export/my`、`//export/wrong`、`//export/{resultId}/detail`、`//export/{resultId}/detail/word` |
| `ExamController.java` | `//{examId}/export/excel`、`//{examId}/export/word` |
| `QuestionController.java` | `//export` |
| `WrongNotebookController.java` | `//{notebookId}/export` |

### Service 层改动

`ExportService.java` 中所有导出方法增加 null 安全检查：

- `buildWorkbook` / `exportExamPaperExcel` / `exportExamPaperWord`：`questions` 为 null 时降级为空列表，每个 `Question` 对象判空跳过
- `exportResultDetail` / `exportResultDetailWord`：`answers` 为 null 时降级为空列表，每个 `Map` 对象判空跳过
- `setCellValue`：处理 `q.getScore()` 为 null 的情况

### 新增/修改的导入

所有 Controller 新增：
```java
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
```

## 5. 前端实现

### `download.ts`

- 新增 `document.body.appendChild(link)` 确保链接元素在 DOM 中
- `URL.revokeObjectURL()` 延迟 5 秒执行，确保浏览器有足够时间读取 Blob
- 优化 catch 错误处理：识别 Blob 类型错误响应，展示可读错误信息

### `request.ts`

- 错误拦截器新增 `data instanceof Blob` 判断：Blob 响应不尝试解析 `.message`，直接透传

## 6. 关键文件

| 文件 | 改动 |
|---|---|
| `Oline-AQ-spring/.../controller/ResultController.java` | Content-Disposition 编码 + 新增工具方法 |
| `Oline-AQ-spring/.../controller/ExamController.java` | Content-Disposition 编码 + 新增工具方法 |
| `Oline-AQ-spring/.../controller/QuestionController.java` | Content-Disposition 编码 |
| `Oline-AQ-spring/.../controller/WrongNotebookController.java` | Content-Disposition 编码 |
| `Oline-AQ-spring/.../service/ExportService.java` | null 安全检查 |
| `Oline-AQ-vue/src/utils/download.ts` | Blob URL 释放时序 + 错误处理优化 |
| `Oline-AQ-vue/src/utils/request.ts` | Blob 错误响应跳过 message 解析 |

## 7. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring && .\mvnw.cmd compile

# 前端构建
cd Oline-AQ-vue && npm run build
```

构建均通过，无报错。

## 8. 风险与后续优化

- 文件名编码使用 Spring 的 `ContentDisposition` 工具类，兼容所有现代浏览器
- Blob URL 延迟 5 秒释放是安全阈值，若网络极端缓慢可考虑增加重试机制
- 后续可考虑统一抽取导出工具方法，减少控制器间重复代码
