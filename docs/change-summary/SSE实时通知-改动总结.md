# SSE 实时通知改动总结

## 1. 改动目标
考试发布和公告创建时，在线学生能实时收到通知，无需手动刷新或轮询。

## 2. 用户视角
- 教师发布考试后，所有已登录的学生浏览器会弹出系统通知"新考试发布：xxx"
- 教师创建公告后，所有已登录的学生浏览器会弹出系统通知"新公告：xxx"
- 学生登录后自动建立 SSE 长连接，断开后 10 秒自动重连

## 3. 前端实现
- **composable**：新增 `src/composables/useNotification.ts`，使用 `EventSource` 连接 SSE，通过 `ElNotification` 展示通知
- **StudentLayout.vue**：`onMounted` 时调用 `connectSSE()`，`onUnmounted` 时自动断开
- 通知 URL 使用与 API 相同的 base URL，token 通过 query 参数传递

## 4. 后端实现
- **NotificationService**：管理 `ConcurrentHashMap<Integer, SseEmitter>`，提供 `subscribe(token)`、`notifyExamPublished()`、`notifyAnnouncement()` 方法
- **NotificationController**：`GET /api/notifications/subscribe?token=xxx` 返回 `SseEmitter`
- **ExamService.publish()**：发布成功后调用 `notificationService.notifyExamPublished()`
- **AnnouncementService.create()**：创建成功后若 `active=true` 则调用 `notificationService.notifyAnnouncement()`
- **WebConfig**：排除 `/api/notifications/**` 路径（SSE 通过 query param 传 token 手动验证）

## 5. 数据流
```
教师发布考试/创建公告
  → ExamService.publish() / AnnouncementService.create()
    → NotificationService.broadcast()
      → 遍历所有 SseEmitter → 发送 SSE event
        → 前端 EventSource 监听 → ElNotification 弹出
```

## 6. 接口说明

| 接口 | 方法 | 作用 | 主要参数 | 返回结果 |
|---|---|---|---|---|
| `/api/notifications/subscribe` | GET | SSE 订阅 | `token`(query) | `SseEmitter` |

## 7. 关键文件

| 文件 | 作用 |
|---|---|
| `Oline-AQ-spring/.../service/NotificationService.java` | **新增** 通知发布与 SSE 管理 |
| `Oline-AQ-spring/.../controller/NotificationController.java` | **新增** SSE 订阅端点 |
| `Oline-AQ-spring/.../service/ExamService.java` | 发布时调用通知 |
| `Oline-AQ-spring/.../service/AnnouncementService.java` | 创建公告时调用通知 |
| `Oline-AQ-spring/.../config/WebConfig.java` | 排除 SSE 路径 |
| `Oline-AQ-vue/src/composables/useNotification.ts` | **新增** SSE 连接与通知展示 |
| `Oline-AQ-vue/src/views/student/StudentLayout.vue` | 挂载时连接 SSE |

## 8. 验证方式
```sh
cd Oline-AQ-spring && .\mvnw compile -q
cd Oline-AQ-vue && npx vue-tsc --noEmit
```
后端编译通过，前端类型检查通过，无报错。

## 9. 风险与后续优化
- 使用 SSE 而非 WebSocket，仅支持服务端→客户端单向推送（满足当前需求）
- 浏览器对同一域名的 SSE 连接数有限制（6 个），当前每页面只开一个连接，不影响
- token 通过 URL query 参数传递，理论上会写入服务器日志，生产环境建议使用 `EventSource` + 自定义 polyfill 支持 header
- 后续可增加心跳保活机制，防止代理层断连
- 未在学生移动端布局（StudentMobileLayout.vue）中添加 SSE 支持
