# 系统公告功能实现总结

## 1. 改动目标

实现系统公告功能，管理员可以发布和管理公告，学生登录后自动弹窗通知，右上角消息入口可随时查看。

## 2. 用户视角

### 管理员
- 侧边栏「系统设置」下新增「公告管理」入口
- 可查看所有公告列表（标题、内容预览、启用状态、创建时间）
- 支持发布新公告、编辑已有公告、删除公告
- 发布时可设置标题、内容、启用/关闭状态

### 学生
- 登录后如果有未读公告，自动弹出通知弹窗，点击「我知道了」关闭并标记全部已读
- 页面右上角新增铃铛图标，显示未读公告数量角标
- 点击铃铛打开公告列表弹窗，可逐条标记已读
- 关闭公告列表时自动标记所有为已读

## 3. 前端实现

### 页面
- `AdminAnnouncements.vue`：管理员公告管理页，表格展示 + 编辑弹窗

### 布局
- `AdminLayout.vue`：侧边栏「系统设置」子菜单新增「公告管理」项
- `StudentLayout.vue`：标题栏右侧新增铃铛图标 + 未读角标 + 公告列表弹窗 + 登录后弹窗

### 路由
- `/admin/announcements` → `AdminAnnouncements.vue`（管理员）

### 类型
- `types/index.ts`：新增 `Announcement`、`AnnouncementItem`、`UnreadInfo` 接口

### API
- `api/index.ts`：新增 `loadAnnouncementsApi`、`createAnnouncementApi`、`updateAnnouncementApi`、`deleteAnnouncementApi`、`getUnreadAnnouncementsApi`、`markAnnouncementReadApi`、`markAllAnnouncementsReadApi`

## 4. 后端实现

### Controller：`AnnouncementController.java`
| 接口 | 方法 | 作用 |
|---|---|---|
| `GET /api/announcements` | `list()` | 获取全部公告 |
| `GET /api/announcements/{id}` | `get()` | 获取单条公告 |
| `POST /api/announcements` | `create()` | 创建公告 |
| `PUT /api/announcements/{id}` | `update()` | 更新公告 |
| `DELETE /api/announcements/{id}` | `delete()` | 删除公告 |
| `GET /api/announcements/unread` | `unreadInfo()` | 获取当前用户未读公告信息 |
| `POST /api/announcements/{id}/read` | `markAsRead()` | 标记单条已读 |
| `POST /api/announcements/read-all` | `markAllAsRead()` | 标记全部已读 |

### Service：`AnnouncementService.java`
- `listAll()` / `listActive()`：查询公告
- `create()`：创建公告并记录创建时间
- `update()`：更新公告内容和状态
- `delete()`：删除公告同时清理关联的已读记录
- `getUnreadInfo()`：查询活跃公告及当前用户已读状态
- `markAsRead()` / `markAllAsRead()`：标记已读（幂等）

### Mapper：`AnnouncementMapper.java`、`AnnouncementReadMapper.java`
- 继承 `BaseMapper<Announcement>` / `BaseMapper<AnnouncementRead>`

### Entity：`Announcement.java`、`AnnouncementRead.java`
- `announcement`：announcementId, title, content, active, createTime, updateTime
- `announcement_read`：id, announcementId, userId, readTime

### DTO：`AnnouncementRequest.java`
- title, content, active

## 5. 数据库视角

### 新增表

**`announcement`**
| 字段 | 类型 | 说明 |
|---|---|---|
| `announcement_id` | SERIAL PK | 主键 |
| `title` | VARCHAR(200) NOT NULL | 公告标题 |
| `content` | TEXT NOT NULL | 公告内容 |
| `active` | BOOLEAN DEFAULT TRUE | 启用状态 |
| `create_time` | TIMESTAMP | 创建时间 |
| `update_time` | TIMESTAMP | 更新时间 |

**`announcement_read`**
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | SERIAL PK | 主键 |
| `announcement_id` | INTEGER NOT NULL | 公告ID |
| `user_id` | INTEGER NOT NULL | 用户ID |
| `read_time` | TIMESTAMP | 阅读时间 |

### 索引
- `idx_announcement_read_user` on `announcement_read(user_id)`
- `idx_announcement_read_announcement` on `announcement_read(announcement_id)`

### 数据流
1. 管理员创建公告 → 写入 `announcement` 表
2. 学生登录 → 前端调用 `GET /api/announcements/unread` → 查询 `announcement WHERE active=true` + `announcement_read WHERE user_id=?`
3. 学生点击「我知道了」→ 前端调用 `POST /api/announcements/read-all` → 批量插入 `announcement_read` 记录
4. 学生点击铃铛 → 查看公告列表 → 关闭时调用 `read-all`

## 6. 关键文件

| 文件 | 作用 |
|---|---|
| `entity/Announcement.java` | 公告实体 |
| `entity/AnnouncementRead.java` | 已读记录实体 |
| `dto/AnnouncementRequest.java` | 创建/更新请求体 |
| `mapper/AnnouncementMapper.java` | 公告 Mapper |
| `mapper/AnnouncementReadMapper.java` | 已读记录 Mapper |
| `service/AnnouncementService.java` | 公告业务逻辑 |
| `controller/AnnouncementController.java` | 公告 REST 接口 |
| `config/SchemaInitializer.java` | 建表与索引 |
| `views/admin/AdminAnnouncements.vue` | 管理员公告管理页 |
| `views/admin/AdminLayout.vue` | 侧边栏菜单项 |
| `views/student/StudentLayout.vue` | 学生端铃铛图标 + 公告弹窗 |
| `router/index.ts` | 路由注册 |
| `api/index.ts` | 前端 API 封装 |
| `types/index.ts` | 公告类型定义 |

## 7. 验证方式

```sh
# 后端编译
cd Oline-AQ-spring && .\mvnw.cmd compile

# 前端构建
cd Oline-AQ-vue && npm run build
```

## 8. 风险与后续优化

- 当前公告内容使用纯文本，后续可考虑支持富文本/HTML
- 公告列表弹窗点击「标记已读」后仅更新前端状态 + 调用后端接口，若接口失败则下次可能会再次显示
- 后续可支持公告置顶、定时发布、指定接收角色等功能
