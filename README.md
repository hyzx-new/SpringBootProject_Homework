# 学生在线答题系统

基于 Spring Boot 3、MySQL、Redis、Vue 3 和 Vite 的在线答题系统，包含管理员端、教师端、学生端。

## 功能

- 登录、教师注册、Redis token 登录态
- BCrypt 哈希密码存储，管理员新增账号/学生时初始密码默认 `123456`
- 管理员端：账号管理、学生管理、试卷题库、成绩查看导出、资料文件、讨论区
- 教师端：试卷题库、成绩查看导出、学生信息只读查看、资料文件、讨论区
- 学生端：在线答题、成绩查看、考试前三天提醒、资料文件、讨论区
- 学生账号只能由管理员在学生管理中添加
- 学号支持自动生成 10 位数字格式，例如 `2052443662`
- 题型支持单选、多选、判断、填空、简答
- 多选题自动集合判分，简答题支持教师/管理员在成绩详情中人工改分
- 文件上传、下载、删除
- 讨论区发帖、回复、关联试卷
- 成绩导出为 Excel
- 个人中心支持头像切换、头像上传、姓名修改和密码修改

## 目录

```text
student-answer-system
├── backend/      Spring Boot 3 后端
├── frontend/     Vue 3 + Vite 前端
├── database/     MySQL 初始化 SQL
└── uploads/      运行后保存上传文件
```

## 环境

- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8
- Redis

默认配置：

```yaml
MySQL: localhost:3306
数据库: student_answer_system
用户名: root
密码: 123456
Redis: localhost:6379
后端端口: 8080
前端端口: 5173
```

配置文件：

```text
backend/src/main/resources/application.yml
```

## 初始化数据库

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content -Raw -Encoding UTF8 E:\student-answer-system\database\init.sql | mysql --default-character-set=utf8mb4 -uroot -p123456
```

## 默认账号

密码均为 `123456`，数据库保存的是 BCrypt 哈希值。

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| 学生 | student | 123456 |

示例学生学号：`2052443662`。

## 启动后端

```powershell
cd E:\student-answer-system\backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

## 启动前端

```powershell
cd E:\student-answer-system\frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到 `http://localhost:8080`。

## 常用接口

- `POST /api/auth/login` 登录
- `POST /api/auth/register` 教师注册
- `GET /api/profile/me` 当前个人信息
- `PUT /api/profile/me` 修改个人信息/头像
- `PUT /api/profile/password` 修改密码
- `GET /api/students?page=1&size=10` 学生列表
- `POST /api/students` 管理员新增学生
- `GET /api/users?page=1&size=10` 账号列表
- `GET /api/papers?page=1&size=10` 试卷列表
- `GET /api/papers/{paperId}/questions` 题目列表
- `POST /api/submissions/papers/{paperId}` 学生提交答案
- `PUT /api/submissions/{submissionId}/answers/{answerId}/score` 人工批改
- `GET /api/submissions/export` 导出成绩表
- `GET /api/reminders/my` 学生考试提醒
- `POST /api/reminders/generate` 手动生成三天前提醒
- `GET /api/files` 文件列表
- `POST /api/files` 上传文件
- `GET /api/discussions` 讨论列表
- `POST /api/discussions` 发起讨论

分页接口统一返回：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10,
  "pages": 0
}
```
