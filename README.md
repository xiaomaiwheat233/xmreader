# NovelHub

NovelHub 是一个本地优先开发的多源小说聚合阅读平台，采用 React、Spring Boot 和 MySQL。当前项目处于基础脚手架阶段，so-novel 尚未接入。

## 当前能力

- React + TypeScript + Vite 前端基础应用
- Spring Boot 3 + Java 21 后端基础应用
- MySQL 8 本地开发容器
- Flyway 数据库版本管理基础配置
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
