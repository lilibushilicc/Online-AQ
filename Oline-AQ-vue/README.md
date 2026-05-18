# 智能在线答题系统前端原型

这是一个 Vue 3 + Vite + Element Plus 的本地演示版，用内存数据模拟后端接口和数据库，先跑通初版核心流程：

- 教师登录
- 粘贴 TXT 格式试题并解析入库
- 查看题库
- 创建、发布、关闭考试
- 学生登录并参加考试
- 提交后自动评分
- 学生和教师查看成绩

## 启动

```sh
npm install
npm run dev
```

浏览器访问 Vite 输出的本地地址，默认通常是：

```text
http://127.0.0.1:5173
```

## 测试账号

```text
教师账号：admin
教师密码：123456

学生账号：2023001
学生密码：123456
```

## 构建检查

```sh
npm run build
```

当前版本已预留 `src/utils/request.ts` 和 `src/api/auth.ts`，后续接 Spring Boot 后端时可以继续补齐真实接口。
