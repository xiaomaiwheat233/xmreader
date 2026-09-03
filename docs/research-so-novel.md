# so-novel 集成可行性研究

> Phase 0 调研结论。调研日期：2026-09-03。当前阶段只确定边界和方案，不编写 xmreader 业务代码。

## 1. 结论先行

so-novel 可以作为 xmreader 的采集能力来源，但**不能直接当作一个稳定的、结构化的小说采集 REST API 使用**。

推荐采用“xmreader 主系统 + 独立 Crawler Adapter”的低耦合架构：

- xmreader 后端只依赖自有的 `CrawlerGateway` 契约，不依赖 so-novel 的 Java 类、规则模型或目录结构。
- Crawler Adapter 作为独立进程运行，负责适配固定版本的 so-novel，并向主系统输出规范化的书籍、目录、章节和错误结果。
- 本地 MVP 先使用内置测试适配器打通阅读闭环；随后再实现 so-novel 适配器的小规模导入（一本书、前 5 章），验证成功后才开放整本和增量更新。
- 不把 so-novel 源码复制进 xmreader 后端，也不让 HTTP 请求线程直接等待整本下载。
- so-novel 及其衍生适配代码必须保留 AGPL-3.0 合规边界。上线前需要再次完成许可证和内容来源审查。

现成 CLI 只能按详情页 URL 导出整本 `txt/epub/html/pdf` 文件；现成 WebUI 的下载接口同样执行整本文件导出。二者都不能满足 xmreader 所需的逐层结构化契约。因此，直接调用现成 Web API 不是推荐方案；CLI 文件桥接只能作为一次性技术验证或降级方案。

## 2. 调研基线

本次基于上游仓库 `main` 分支提交：

- commit：`76150dbd2827b4de97cde83cffb3ee367bc9be3a`
- commit 时间：2026-07-27T00:07:24+08:00
- `pom.xml` 版本：1.11.0
- Java：21
- 构建：Maven，最终生成包含依赖的 `app.jar`
- Web 容器：Jetty 12
- HTTP：OkHttp 5
- HTML 解析：jsoup
- CLI：picocli
- JavaScript 规则运行时：Javet/V8，按操作系统和 CPU 架构选择 native 依赖
- 许可证：GNU AGPL-3.0

权威来源：上游 [README](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/README.md)、[pom.xml](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/pom.xml)、[LICENSE](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/LICENSE)。

## 3. 当前项目结构和能力

so-novel 是单 Maven 模块应用，不是 Spring Boot 服务，也没有独立发布的 Java SDK。主要结构如下：

```text
so-novel
├─ src/main/java/com/pcdd/sonovel
│  ├─ Main.java                  # tui / cli / web 三种启动模式
│  ├─ actions/                   # 搜索、下载、批量下载等用例
│  ├─ core/                      # Crawler、Source、配置加载、内容提取
│  ├─ parser/                    # Search / Book / Toc / Chapter 解析器
│  ├─ dsl/                       # @js、@java 后处理 DSL
│  ├─ handler/                   # TXT/EPUB/HTML/PDF 后处理
│  ├─ model/                     # Rule、SearchResult、Chapter 等内部模型
│  ├─ utils/                     # HTTP、规则加载、限流等
│  └─ web/                       # Jetty、Servlet、静态 WebUI
├─ src/main/resources/static/    # 内置 WebUI
├─ bundle/config.ini
├─ bundle/rules/                 # 书源规则集和模板
├─ Dockerfile
└─ pom.xml
```

核心解析链路为：

```text
SearchParser -> BookParser -> TocParser -> ChapterParser
                                      -> Crawler
                                      -> 本地章节缓存文件
                                      -> TXT/EPUB/HTML/PDF 合并器
```

内部解析器确实暴露了可调用的 public 方法：

- `SearchParser.parse(keyword)` 返回搜索结果。
- `BookParser.parse(url)` 返回详情模型。
- `TocParser.parseAll(url)` 返回章节列表。
- `ChapterParser.parse(chapter)` 返回正文。

但是它们不是稳定 SDK：会依赖全局配置、全局 HTTP 上下文、当前激活规则、线程本地 `BookContext`、文件输出和内部模型。直接把上游 JAR 加入 Spring Boot classpath 会形成高耦合，也会把 AGPL 兼容性问题带入主应用，因此不采用。

源码依据：[Main.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/Main.java)、[Crawler.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/core/Crawler.java)、[parser 目录](https://github.com/freeok/so-novel/tree/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/parser)。

## 4. CLI、Web 和 Docker 的真实行为

### 4.1 CLI

启动模式通过 JVM 参数 `-Dmode=cli` 选择。CLI 只有两个业务参数：

```text
-u, --url <详情页 URL>
-e, --ext <txt|epub|html|pdf>，默认 epub
```

CLI 会解析详情、目录和全部章节，最后写出电子书文件。它没有搜索子命令、结构化 JSON 输出、章节范围、任务 ID、进度查询、增量更新或稳定的机器可读错误码。

结论：可以做受控子进程 PoC，但不适合作为最终集成契约。

源码依据：[CliLauncher.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/launcher/CliLauncher.java)。

### 4.2 WebUI 和接口

Web 模式使用内嵌 Jetty，默认端口 7765。当前 servlet 路由包括：

| 路径 | 用途 | 对 xmreader 的适用性 |
|---|---|---|
| `GET /search/aggregated` | 聚合搜索，返回 JSON | 可参考，但不是承诺稳定的公共 API |
| `GET /book-fetch` | 抓取并导出整本文件 | 阻塞执行；不返回结构化章节，不适用 |
| `GET /download-progress` | 全局 SSE 下载进度 | 没有 xmreader 任务隔离，不适用 |
| `GET /local-books` | 已导出文件列表 | 文件管理语义，不适用 |
| `GET /book-download` | 下载导出文件 | 文件管理语义，不适用 |
| `GET /book-delete` | 删除导出文件 | 使用 GET 改变状态，不应暴露 |
| `GET /config` | 返回配置 | 不应直接暴露给业务前端 |
| `GET /sources` | 书源列表/检查 | 可作为适配器内部诊断参考 |

当前 Web 服务没有认证、授权、任务持久化和 xmreader 所需的书籍/目录/章节契约。`/book-fetch` 在 servlet 请求线程中同步调用 `Crawler.crawl()`。因此即使作为独立容器启动，也不能直接满足异步采集系统要求。

源码依据：[WebServer.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/web/WebServer.java)、[BookFetchServlet.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/web/servlet/BookFetchServlet.java)。

### 4.3 Docker

官方镜像为 `ghcr.io/freeok/sonovel:latest`，README 示例使用 `JAVA_OPTS=-Dmode=web` 并映射 7765。Dockerfile 基于 `eclipse-temurin:21-jre-jammy`，原因是 Javet 的 native 库依赖 glibc，不能直接使用常规 Alpine/musl 镜像。

本项目本地优先阶段不要求通过 Docker 启动前后端，但后续做 so-novel 适配时，独立容器仍是最清晰的运行边界。镜像必须固定到具体版本或 digest，不能在可重复环境中使用 `latest`。

来源：[Dockerfile](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/Dockerfile) 和 [README Docker 部分](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/README.md#-docker)。

## 5. rules 工作方式

规则位于 `rules/*.json`，`config.ini` 的 `source.active-rules` 指定当前规则文件，也支持绝对路径。加载时按文件顺序为规则动态分配 ID，所以 **source ID 不是跨规则集的稳定业务标识**；xmreader 应保存规范化的 `source_key` 和源站 URL，而不是只保存数字 ID。

单个规则包含：

- 书源元信息：`url`、`name`、`language`、`needProxy`、`disabled`。
- `search`：请求 URL/方法/参数、结果列表、书名、作者、分类、最新章节、更新时间、分页等。
- `book`：详情 URL、书名、作者、简介、封面、分类、状态、最新章节等。
- `toc`：目录 URL、列表/条目选择器、正倒序、分页。
- `chapter`：正文、过滤、段落、分页、下一章链接等。
- `crawl`：并发、请求间隔、重试次数和重试间隔，可覆盖全局配置。

选择器支持 CSS Selector 和 XPath；字段还可使用 `@js:`、`@java:` DSL 做后处理。这意味着规则文件不是纯数据，尤其 JavaScript 规则应当视为可执行代码，只能加载受信、固定版本、经过代码审查的规则，禁止管理员上传任意规则。

上游将规则分为默认、需代理、不支持搜索、限流、Cloudflare 等集合；源站可用性、限流策略和域名会变化，必须允许单书源熔断与禁用，不能假设所有书源长期可用。

来源：[规则模板](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/bundle/rules/rule-template.json5)、[SourceUtils.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/utils/SourceUtils.java)、[书源说明](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/BOOK_SOURCES.md)。

## 6. 稳定性、安全与隐私发现

- 默认抓取配置是 200–400ms 间隔、最多 3 次重试；并发未指定时，`Crawler` 最多可取 50，代码硬上限是 100。这对 xmreader 过高，适配器应覆盖为每来源 2–4 并发，并增加来源级速率限制。
- 部分章节失败时，上游主要写错误日志，整本任务仍可能继续；xmreader 必须自己记录每章状态，不能仅以进程退出或总耗时判断完整成功。
- 上游依据详情 URL 前缀匹配规则，但这不是 SSRF 防护。xmreader 必须在任务创建和每次重定向/实际连接前进行协议、端口、DNS 解析和私网地址校验，并优先只允许已启用书源域名。
- WebUI 没有认证边界，不应映射给终端用户或公网。
- 上游 `Main` 启动时无条件创建客户端报告线程；当前实现会向一个外部 Workers 地址发送本机用户名、主机名、MAC、本地 IP、操作系统和应用版本。虽然类注释写着“仅限代理用户”，调用处没有相应条件。xmreader 不应在未明确告知和同意的情况下运行这一行为。后续适配器必须通过源码审查后的构建移除该行为，或在隔离环境中实施严格的目的域名出站控制。
- 上游规则可执行 JavaScript，Javet 又带平台相关 native 库，需要把规则供应链、平台镜像和版本固定纳入测试。

源码依据：[Crawler.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/core/Crawler.java)、[ClientReportRepository.java](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/src/main/java/com/pcdd/sonovel/repository/ClientReportRepository.java)。

## 7. 集成方案比较

| 方案 | 可行性 | 优点 | 主要问题 | 决策 |
|---|---:|---|---|---|
| 直接调用现成 WebUI 接口 | 低 | 启动简单，有搜索和进度接口 | 同步整本导出、无结构化章节、无认证/任务隔离、契约不稳定 | 不采用 |
| 受控 CLI 子进程 + 解析缓存文件 | 中 | 不侵入源码、进程隔离、PoC 快 | 只能整本导出、输出/目录易变、进度和章节错误难映射、无法原生前 5 章 | 仅作 PoC/降级 |
| 主后端直接依赖 so-novel JAR/源码 | 技术上可行 | 能直接调用 Parser | 强耦合全局状态和内部 API，升级脆弱，AGPL 边界扩散 | 不采用 |
| 独立、版本固定的 SoNovel Adapter | 高 | 契约可控、任务隔离、可逐章返回、便于限流和审计 | 需要维护少量适配代码并履行 AGPL 义务 | 推荐 |
| 完全重写所有书源爬虫 | 中 | 主系统许可证和接口完全自主 | 重复投入大，偏离使用 so-novel 的目标 | 不作为首选 |

推荐方案不是直接复用上游现成 Web 端点，而是在 `crawler/` 中建立独立适配器边界。具体适配实现要等 Phase 5 的单书、前 5 章实验后再定；在实验前不承诺某个内部类接口长期稳定。

## 8. 推荐的 xmreader 本地架构

当前目标是本机可运行、核心流程可验证，采用模块化单体，不引入微服务、Kafka、Elasticsearch 或 Kubernetes。

```text
React + Vite (localhost:5173)
          |
          | /api，Vite 开发代理
          v
Spring Boot modular monolith (localhost:8080)
  ├─ auth/user
  ├─ catalog/book/chapter/search
  ├─ library/bookshelf/history/progress
  ├─ crawler-task
  └─ admin
          |
          ├─ MySQL 8（业务数据 + 任务状态）
          └─ CrawlerGateway
                 |
                 ├─ FixtureCrawlerAdapter（先打通本地闭环）
                 └─ SoNovelAdapter（Phase 5 再接入，独立进程）
```

本地阶段决策：

1. 前端使用 React、TypeScript、Vite、React Router、TanStack Query、Zustand 和 Ant Design。
2. 后端使用 Java 21、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、Flyway 和 MySQL 8。
3. 后端保持一个可执行应用，但按业务模块分包；Controller 不直接访问 Mapper，DTO 与持久化实体分离。
4. 采集任务先用 MySQL 表作为可靠状态源，用受限线程池异步执行。单机 MVP 不需要消息队列。
5. Redis 在本地第一版不是启动前提；先保证数据库版本正确，再在后续阶段为书籍详情和目录增加可关闭缓存。
6. 本地开发不要求 Nginx、TLS 或完整 Docker Compose。MySQL 可以使用本机服务或单独的开发容器；前后端直接用开发命令启动。
7. 部署文件、资源限制和生产网络拓扑推迟到核心功能通过测试后处理，但配置从一开始使用环境变量覆盖，避免代码内写死秘密。
8. 采集 URL 采用“已启用来源域名 allowlist + DNS/IP 双重检查 + 禁止私网/回环/link-local/metadata + 限制重定向”的 SSRF 策略。

## 9. Adapter 契约建议

主系统只认识以下语义，不认识 so-novel 的 `Rule` 或 `Chapter` 类：

```text
search(keyword) -> SourceBookCandidate[]
fetchBook(sourceBookUrl) -> CrawledBook
fetchChapterList(sourceBookUrl) -> CrawledChapterRef[]
fetchChapter(chapterUrl) -> CrawledChapter
health() -> AdapterHealth
```

每次调用都携带 `taskId`、`sourceKey`、超时、最大响应体和 trace ID。错误需要归一化为：

```text
UNSUPPORTED_SOURCE
INVALID_SOURCE_URL
SOURCE_UNAVAILABLE
SOURCE_RATE_LIMITED
PARSE_RULE_MISMATCH
CONTENT_EMPTY
TIMEOUT
INTERNAL_ADAPTER_ERROR
```

主系统负责业务幂等、书籍/章节唯一约束、任务状态、失败重试和增量更新；Adapter 只负责受控访问与解析，不直接写 xmreader 数据库。

## 10. 许可证与内容合规边界

so-novel 使用 AGPL-3.0。若修改、组合或通过网络提供其衍生程序，需要认真履行许可证要求，尤其是向网络用户提供相应源代码。把它拆成独立进程有助于工程隔离，但**不自动构成法律结论，也不自动免除 AGPL 义务**。

工程约束：

- 不复制整个上游仓库到 xmreader。
- 适配器固定上游 commit/version，并保留版权、许可证、修改说明和对应源码获取方式。
- 主系统与适配器通过明确的进程间 JSON 契约通信。
- 上线或公开分发前，由项目所有者再次核对 AGPL-3.0 和上游免责声明；必要时咨询专业法律意见。
- 功能验证只使用公共领域、自有、明确授权或专门测试内容；不绕过登录、付费墙、验证码、访问控制、DRM 或反爬机制，并尊重 robots.txt 和站点条款。

来源：[AGPL-3.0 LICENSE](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/LICENSE)、[上游免责声明](https://github.com/freeok/so-novel/blob/76150dbd2827b4de97cde83cffb3ee367bc9be3a/bundle/DISCLAIMER.md)。本文是工程风险说明，不是法律意见。

## 11. 本地开发路线调整

结合“先本地合理可用，后续再部署”的目标，阶段顺序调整为：

1. Phase 0：完成本调研和架构边界。
2. Phase 1：设计模块、数据库、API、任务状态机和本地运行约定。
3. Phase 2：创建前后端脚手架；本地 MySQL 可启动；前后端 health check 通过。
4. Phase 3–4：认证、书籍、章节、搜索；用合法 fixture 数据完成阅读器闭环。
5. Phase 5：实现隔离的 SoNovel Adapter，仅验证一本书前 5 章，并记录实际兼容性。
6. Phase 6–8：异步任务、书架/进度/历史、管理后台。
7. Phase 9：按压测结果引入可选 Redis，不让 Redis 成为本地启动硬依赖。
8. Phase 10 及以后：在本地功能和测试稳定后，再做 Compose、Nginx 和服务器部署。

## 12. 本机环境检查结果

调研时检测到：

- Node.js `v22.14.0`、npm `11.3.0`：满足后续前端开发。
- Maven `3.9.12`：满足后续后端构建。
- Docker `27.5.1`：可用于后续单独启动 MySQL。
- 当前默认 Java 为 `1.8.0_482`：**不满足** xmreader 和 so-novel 的 Java 21 要求。
- 当前工作区起初为空，且尚不是 Git 仓库。

进入 Phase 2 前必须安装 JDK 21，并确认 `java -version` 与 `mvn -version` 都指向同一个 JDK 21。Phase 0 没有 Java 项目，因而没有可执行的业务编译或测试；本阶段验证对象是上游源码、文档引用和本地工具链状态。

## 13. Phase 0 验收结果

- [x] 阅读当前项目目录（空目录）。
- [x] 阅读上游 README、pom.xml、LICENSE 和免责声明。
- [x] 阅读 CLI、Web、Crawler、Parser、rules、Docker 相关实现。
- [x] 确认没有满足 xmreader 需求的稳定结构化 REST API。
- [x] 确认 Java 21、Javet native、规则 DSL、并发和文件输出行为。
- [x] 确认 AGPL-3.0、内容合规和客户端报告风险。
- [x] 给出低耦合 Adapter 方案和本地优先架构。
- [ ] 业务代码、项目 scaffold、数据库/API 详细设计：按阶段要求留到 Phase 1/2。
