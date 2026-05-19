# 改进计划

## 目标
完成三类改进：核心体验、代码质量、运维

## 阶段

### 阶段一：核心体验
- [ ] 1.1 考试作答自动保存草稿（localStorage）
- [ ] 1.2 服务端时间校验（倒计时改为服务端驱动）
- [ ] 1.3 前端页面统一添加 Loading 状态

### 阶段二：代码质量
- [ ] 2.1 `Map<String, Object>` 返回改为明确 VO
- [ ] 2.2 Element Plus 改为自动按需导入（unplugin-vue-components）
- [ ] 2.3 SQL 手动拼接改为 MyBatis-Plus Page 插件

### 阶段三：运维
- [ ] 3.1 拆分 application-dev.yml / application-prod.yml
- [ ] 3.2 Vue DevTools 仅开发模式启用
- [ ] 3.3 添加 `.env.production`
