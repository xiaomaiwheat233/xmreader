# xmreader（小麦中文网）

xmreader 是“小麦中文网”的工程名称。这是一个本地优先开发的多源小说聚合阅读平台，采用 React、Spring Boot 和 MySQL。当前版本已经具备本地书库和完整阅读链路，并且接入了 so-novel 用于爬取小说。

## 当前能力

- React + TypeScript + Vite 前端应用，包含首页、搜索、书籍详情、章节目录、阅读器与认证页面
- Spring Boot 3 + Java 25 后端 API
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

- JDK 25
- Maven 3.9+
- Node.js 22.12+
- npm 11+
- Docker Desktop（只用于启动 MySQL；也可使用本机 MySQL 8）

如果系统默认仍是 Java 8，可在当前 PowerShell 会话切换到 JDK 25：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
```

当前 `backend/pom.xml` 声明使用 Java 25。只需要在运行 xmreader 的终端中设置
`JAVA_HOME`，不必修改系统级 Java 配置。

## 本地启动

### 一键启动

项目可以通过start-local.cmd一键启动

### 手动启动

项目需要同时运行 MySQL、后端和前端。建议准备三个 PowerShell 终端，并按下面的顺序启动。

### 1. 初始化本地配置

在项目根目录执行：

复制本地环境变量（不要提交 `.env`）：

```powershell
Set-Location C:\path\to\xmreader
Copy-Item .env.example .env
```

此操作只需在第一次启动时执行。如果 `.env` 已存在，请保留已有配置，不要覆盖。

Docker Compose 会自动读取根目录的 `.env`。Spring Boot 不会自动读取该文件，但后端的默认配置与
`.env.example` 中的本地数据库配置一致，因此使用默认值时无需额外操作。

### 2. 启动 MySQL

在第一个终端、项目根目录执行：

```powershell
Set-Location C:\path\to\xmreader
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml ps
```

首次启动需要下载 MySQL 镜像。等待 `xmreader-mysql` 显示为 `healthy` 后再启动后端。

### 3. 启动后端

在第二个终端执行：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Set-Location C:\path\to\xmreader\backend
mvn spring-boot:run
```

看到后端在 `8080` 端口启动后，可以在另一个终端检查：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

如果暂时没有安装 JDK 25，当前代码也可以临时使用 JDK 21 编译运行：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Set-Location C:\path\to\xmreader\backend
mvn '-Djava.version=21' spring-boot:run
```

该参数只覆盖当前 Maven 命令，不会修改 `pom.xml`；完成 Java 25 环境升级后应恢复使用正常启动命令。

### 4. 启动前端

在第三个终端执行：

```powershell
Set-Location C:\path\to\xmreader\frontend
npm ci
npm run dev
```

`npm ci` 在第一次启动或 `package-lock.json` 发生变化后执行即可。依赖已经安装时，后续直接执行
`npm run dev`。

### 5. 访问项目

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

### 6. 停止项目

前端和后端终端分别按 `Ctrl+C`，然后在项目根目录停止 MySQL：

```powershell
Set-Location C:\path\to\xmreader
docker compose -f compose.local.yml stop
```

不要使用 `docker compose -f compose.local.yml down -v`，除非明确需要删除本地 MySQL 数据。

### 自定义后端环境变量

如果修改了 `.env` 中的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET_BASE64` 或
`FIXTURES_ENABLED`，还需要在启动后端的 PowerShell 中导入对应变量，例如：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/xmreader?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME = 'xmreader'
$env:DB_PASSWORD = 'xmreader_dev'
$env:FIXTURES_ENABLED = 'true'

Set-Location C:\path\to\xmreader\backend
mvn spring-boot:run
```

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
