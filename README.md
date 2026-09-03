# xmreader（小麦中文网）

xmreader 是“小麦中文网”的工程名称。这是一个本地优先开发的多源小说聚合阅读平台，采用 React、Spring Boot 和 MySQL。当前版本已经具备本地书库和完整阅读链路，so-novel 尚未接入。

## 当前能力

- React + TypeScript + Vite 前端应用，包含首页、搜索、书籍详情、章节目录、阅读器与认证页面
- Spring Boot 3 + Java 21 后端 API
- MySQL 8 本地开发容器
- Flyway 用户、刷新会话、内容来源、书籍和章节表迁移
- Spring Security + JWT + BCrypt 用户认证
- 可轮换、可撤销的 HttpOnly refresh cookie
- 书籍检索、分页、章节正文和前后章导航 API
- 三部原创模拟小说和九个章节，支持关闭本地初始化数据
- 阅读器字号、行距、版心宽度和主题偏好本地持久化
- 登录用户书架、阅读进度和最近阅读跨设备同步
- 详情页继续阅读，阅读器节流保存并在切章时补存进度
- `/api/health` 数据库连通性健康检查
- `/actuator/health` 应用健康检查

## 本地前置条件

- JDK 21
- Maven 3.9+
- Node.js 22.12+
- npm 11+
- Docker Desktop（只用于启动 MySQL；也可使用本机 MySQL 8）

如果系统默认仍是 Java 8，可在当前 PowerShell 会话切换到 JDK 21：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
```

本机本次验证使用的是用户目录下的 Temurin 21，无需修改系统级 Java 配置。

## 启动

复制本地环境变量（不要提交 `.env`）：

```powershell
Copy-Item .env.example .env
```

启动 MySQL：

```powershell
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml ps
```

启动后端：

```powershell
Set-Location backend
mvn spring-boot:run
```

启动前端（另一个终端）：

```powershell
Set-Location frontend
npm ci
npm run dev
```

访问 <http://localhost:5173>。前端通过 Vite 代理访问后端，不需要配置 CORS。

内容入口：

- 首页：<http://localhost:5173>
- 搜索：<http://localhost:5173/search>
- 我的阅读：<http://localhost:5173/library>（需要登录）
- 书籍、目录和阅读地址由页面内链接进入

默认 `FIXTURES_ENABLED=true`，后端会幂等写入“小麦原创测试书库”。如需使用空书库，在 `.env` 中设置 `FIXTURES_ENABLED=false`。

认证入口：

- <http://localhost:5173/register>
- <http://localhost:5173/login>
- <http://localhost:5173/profile>

## 验证

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:5173/api/health

Set-Location backend
mvn test

Set-Location ..\frontend
npm run lint
npm run test:run
npm run build
```

## 文档

- [系统架构](docs/architecture.md)
- [数据库设计](docs/database.md)
- [API 设计](docs/api.md)
- [so-novel 调研](docs/research-so-novel.md)
