# xmreader（小麦中文网）API 设计

> Phase 1 契约。基础路径 `/api`，请求和响应编码 UTF-8，除文件资源外统一使用 `application/json`。

## 1. 通用约定

### 1.1 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "01J..."
}
```

分页 `data`：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 100
}
```

- 页码从 1 开始；`pageSize` 默认 20，最大 100。
- Java `Long` ID 在 JSON 中输出字符串，避免 JavaScript 超过安全整数范围。
- 时间使用 ISO-8601 UTC，例如 `2026-09-03T08:30:00.123Z`。
- 空集合返回 `[]`，不返回 `null`。

### 1.2 错误响应

```json
{
  "code": 40001,
  "message": "用户名格式不正确",
  "data": null,
  "error": {
    "type": "VALIDATION_ERROR",
    "fields": {
      "username": "长度必须为 3 到 32"
    }
  },
  "requestId": "01J..."
}
```

HTTP 状态表达协议结果，`code` 表达稳定业务分类：

| HTTP | 场景 |
|---:|---|
| 200 | 查询、更新、删除成功 |
| 201 | 用户等同步资源创建成功 |
| 202 | 异步采集任务已接受 |
| 400 | 格式、参数或状态转换错误 |
| 401 | 未登录、token 无效/过期 |
| 403 | 已登录但权限不足 |
| 404 | 资源不存在或对当前用户不可见 |
| 409 | 唯一约束、活跃任务等冲突 |
| 429 | 速率限制 |
| 500 | 未预期内部错误 |
| 503 | Adapter 或必要依赖暂不可用 |

异常消息不包含 SQL、堆栈、内部路径、JWT、Cookie 或来源页面正文。

### 1.3 请求约定

- Access JWT：`Authorization: Bearer <token>`。
- Refresh token：仅通过 HttpOnly cookie 传输。
- `Content-Type: application/json` 是写请求的默认格式。
- 未声明的 JSON 字段默认拒绝，尽早发现客户端和服务端契约漂移。
- `X-Request-Id` 可由客户端提供；缺失或不合法时由服务端生成并回传。
- 列表排序只接受文档列出的枚举值，禁止将字段名直接拼入 SQL。

## 2. 认证 API

### `POST /api/auth/register`

匿名。创建普通用户，不允许客户端提交 role/status。

```json
{
  "username": "reader_01",
  "password": "correct horse battery staple",
  "confirmPassword": "correct horse battery staple"
}
```

规则：username 3–32 位 ASCII 字母、数字或下划线；password 8–72 字符；两次密码必须一致。昵称不再作为注册项，系统暂以规范化后的用户名作为初始昵称。成功返回 `201` 和安全用户资料，不回传 token；客户端随后显式登录。

### `POST /api/auth/login`

匿名但限速。

```json
{
  "username": "reader_01",
  "password": "correct horse battery staple"
}
```

成功时设置 refresh cookie，并返回：

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "192000000000000001",
    "username": "reader_01",
    "nickname": "阅读者",
    "avatarUrl": null,
    "role": "USER"
  }
}
```

用户名不存在与密码错误统一返回 `INVALID_CREDENTIALS`，避免枚举用户。

### `POST /api/auth/reset-password`

匿名。个人测试模式下不进行短信、邮箱等二次验证，仅根据存在的用户名设置新密码：

```json
{
  "username": "reader_01",
  "newPassword": "new secure password",
  "confirmPassword": "new secure password"
}
```

两次密码必须一致。重置成功后撤销该用户已有 refresh session，需要使用新密码重新登录；用户名不存在返回 `404 USER_NOT_FOUND`。此模式仅适用于本地个人测试，不能直接用于公开部署。

### `POST /api/auth/refresh`

使用 refresh cookie，轮换 refresh token 并返回新的 Access JWT。无效、过期、撤销或重用均返回 401，并清理 cookie。

### `POST /api/auth/logout`

撤销当前 session、清理 refresh cookie。重复调用按幂等成功处理。

## 3. 用户 API

### `GET /api/users/me`

登录用户。返回当前安全资料。

### `PATCH /api/users/me`

```json
{
  "nickname": "新昵称",
  "avatarUrl": "https://example.test/avatar.png"
}
```

第一版只接受外部 HTTPS 头像 URL，不做文件上传。URL 需要长度和协议校验；前端显示时设置安全 referrer policy。

### `PUT /api/users/me/password`

```json
{
  "currentPassword": "old password",
  "newPassword": "new secure password"
}
```

修改成功后撤销该用户所有 refresh session，客户端重新登录。

## 4. 首页、书籍与搜索 API

### `GET /api/home`

匿名可用。返回首页所需的小型聚合结果：

```json
{
  "recommended": [],
  "recentlyUpdated": [],
  "popular": []
}
```

每组最多 12 项。第一版推荐按最近更新并适度打散；热门按有效书架数量计算。规则必须可解释，不能伪造推荐算法。

### `GET /api/books`

匿名可用。

查询参数：

| 参数 | 默认 | 说明 |
|---|---|---|
| `page` | 1 | 正整数 |
| `pageSize` | 20 | 1–100 |
| `category` | - | 完全匹配分类 |
| `status` | - | `ONGOING/COMPLETED/PAUSED/UNKNOWN` |
| `sort` | `UPDATED_DESC` | `UPDATED_DESC/CREATED_DESC/POPULAR_DESC` |

只返回可见且未软删除书籍。

### `GET /api/search?q={keyword}`

匿名可用，只搜索本地 MySQL。另支持 `page/pageSize`。`q` 去除首尾空白后长度 1–100，匹配书名或作者。

### `GET /api/crawler/search?q={keyword}`

匿名可用，通过独立 SoNovel Adapter 聚合联网书源。结果仍是尚未进入本地书库的候选项，包含
`sourceId/sourceName/sourceUrl/title/author/description/category/latestChapterTitle` 等字段。
Adapter 未启动或源站不可用时返回 `503 CRAWLER_UNAVAILABLE`，不影响本地搜索。

### `POST /api/crawler/imports`

匿名可用。把联网搜索结果中的来源地址提交给后端：

```json
{
  "sourceUrl": "https://enabled-source.example/book/123"
}
```

本地 MVP 同步抓取并导入前 5 章，成功返回新建或更新后的 `bookId/title/importedChapterCount`，
前端随后进入现有书籍详情和阅读链路。来源 URL 必须匹配 Adapter 内已启用规则的协议、主机和端口。
首版不绕过登录、付费、验证码、DRM 或其他访问控制，也不承诺每个第三方书源长期可用。

### `GET /api/books/{bookId}`

匿名可用。返回：

```json
{
  "id": "...",
  "title": "书名",
  "author": "作者",
  "coverUrl": null,
  "description": "纯文本简介",
  "category": "分类",
  "status": "ONGOING",
  "wordCount": 123456,
  "chapterCount": 100,
  "latestChapter": {
    "id": "...",
    "title": "第一百章",
    "chapterIndex": 100
  },
  "source": {
    "key": "fixture-public-domain",
    "name": "测试来源"
  },
  "inBookshelf": false,
  "lastCrawledAt": "...",
  "updatedAt": "..."
}
```

匿名访问时 `inBookshelf` 固定 false；登录时按当前用户计算。

### `GET /api/books/{bookId}/chapters`

匿名可用，支持 `page/pageSize`；`pageSize` 最大 200。列表项不返回正文：

```json
{
  "id": "...",
  "chapterIndex": 1,
  "title": "第一章",
  "wordCount": 2350,
  "publishedAt": null
}
```

### `GET /api/chapters/{chapterId}`

匿名可用。返回正文与阅读导航：

```json
{
  "id": "...",
  "book": { "id": "...", "title": "书名" },
  "chapterIndex": 1,
  "title": "第一章",
  "content": "第一段\n\n第二段",
  "wordCount": 2350,
  "previousChapterId": null,
  "nextChapterId": "...",
  "updatedAt": "..."
}
```

`content` 是规范化纯文本。前端按空行拆分段落并用文本节点渲染。

## 5. 书架、进度与历史 API

以下接口都要求 USER 或 ADMIN，并且始终从认证上下文取得 user ID，不接受请求传入 user ID。

### `GET /api/bookshelf`

支持 `page/pageSize`，默认按加入时间倒序。每项包含书籍摘要和该用户当前阅读进度。

### `PUT /api/bookshelf/{bookId}`

加入书架。没有请求体；重复加入返回幂等成功。

### `DELETE /api/bookshelf/{bookId}`

移出书架。书籍原本不在书架也返回幂等成功。为保持统一响应，返回 HTTP 200 JSON，而不是 204。

### `GET /api/reading-progress/{bookId}`

返回当前用户对该书的进度；没有记录时 `data` 为 `null`。

### `PUT /api/reading-progress`

```json
{
  "bookId": "192000000000000002",
  "chapterId": "192000000000000101",
  "position": 840,
  "progressPercent": 36.50
}
```

服务端验证章节属于该书。更新进度时同步 upsert 最近阅读历史。前端最多每 10 秒保存一次，并在切章或页面隐藏时补充保存，避免每次滚动都请求。

### `GET /api/reading-history`

支持分页，按 `visitedAt` 倒序。返回每本书最近一次访问，不返回每次滚动事件。

### `DELETE /api/reading-history/{bookId}`

删除当前用户该书的最近阅读记录，不删除阅读进度。重复删除幂等成功。

## 6. 管理端图书 API

全部要求 ADMIN。

### `GET /api/admin/books`

支持 `q/status/visibility/sourceKey/page/pageSize/sort`，包含隐藏和软删除书籍。

### `PATCH /api/admin/books/{bookId}`

第一版只允许修改管理字段：

```json
{
  "visibility": "HIDDEN"
}
```

抓取生成的书名、作者、章节等不通过这个接口任意修改，避免下一次更新覆盖产生歧义。

### `DELETE /api/admin/books/{bookId}`

软删除书籍。存在 RUNNING 采集任务时返回 409。重复删除幂等成功。第一版不提供物理删除 HTTP API。

## 7. 来源与采集任务 API

### `GET /api/admin/crawler/sources`

列出来源注册信息与 Adapter 健康状态。健康探测失败不修改 enabled。

### `PATCH /api/admin/crawler/sources/{sourceKey}`

```json
{
  "enabled": false,
  "rateLimitPerMinute": 20
}
```

不提供上传/编辑 so-novel 可执行规则的 API；规则只能通过版本控制和发布流程更新。

### `POST /api/admin/crawler/tasks`

创建导入任务：

```json
{
  "type": "IMPORT_BOOK",
  "sourceUrl": "https://allowed.example/books/123"
}
```

创建更新任务：

```json
{
  "type": "UPDATE_BOOK",
  "bookId": "192000000000000002"
}
```

校验：

- IMPORT 只允许 `sourceUrl`，UPDATE 只允许 `bookId`。
- URL 必须匹配已启用来源，并通过 SSRF 校验。
- 相同目标已存在 PENDING/RUNNING 任务时返回 `409 ACTIVE_TASK_EXISTS`，`data` 可携带现有 task ID。

成功返回 `202`：

```json
{
  "taskId": "01K4...",
  "status": "PENDING",
  "pollAfterMs": 2000
}
```

这是唯一的采集任务创建入口，避免 `/books/import` 和 `/crawler/tasks` 两套语义漂移。管理页面的“导入”和“更新”按钮都调用此接口。

### `GET /api/admin/crawler/tasks`

参数：`status/type/bookId/page/pageSize/sort`。默认按创建时间倒序。

### `GET /api/admin/crawler/tasks/{taskId}`

```json
{
  "taskId": "01K4...",
  "type": "IMPORT_BOOK",
  "status": "RUNNING",
  "bookId": "...",
  "sourceUrl": "https://allowed.example/books/123",
  "progress": 42,
  "totalChapters": 100,
  "completedChapters": 41,
  "failedChapters": 1,
  "attemptCount": 1,
  "maxAttempts": 3,
  "error": null,
  "createdAt": "...",
  "startedAt": "...",
  "finishedAt": null
}
```

前端仅在 `PENDING/RUNNING` 时每 2 秒轮询；页面隐藏时暂停，终态立即停止。

### `GET /api/admin/crawler/tasks/{taskId}/logs`

支持分页和 `level` 过滤。只返回脱敏日志，不返回源页面正文。

### `POST /api/admin/crawler/tasks/{taskId}/retry`

只允许 FAILED 且未达到 `maxAttempts` 的任务。重新执行 URL 和来源校验，状态转回 PENDING，返回 `202`。任务仍活跃、已经成功或超过次数时返回 409。

### `POST /api/admin/crawler/tasks/{taskId}/cancel`

允许取消 PENDING；RUNNING 任务发出协作式取消信号并最终进入 CANCELLED。SUCCESS/FAILED/CANCELLED 重复取消返回 409，不伪装为已取消。

## 8. 管理端 Dashboard 和用户 API

### `GET /api/admin/dashboard`

返回：

```json
{
  "bookCount": 20,
  "chapterCount": 3200,
  "userCount": 12,
  "todayTaskCount": 5,
  "successTaskCount": 4,
  "failedTaskCount": 1,
  "recentlyUpdatedBooks": [],
  "recentTasks": []
}
```

任务成功/失败计数默认指当天，字段实现时命名为 `todaySuccessTaskCount/todayFailedTaskCount`，避免统计周期含糊。

### `GET /api/admin/users`

支持 `q/role/status/page/pageSize`。

### `PATCH /api/admin/users/{userId}`

```json
{
  "status": "DISABLED"
}
```

第一版不允许通过普通接口修改角色，也不能禁用当前登录管理员自身。禁用用户时撤销其全部 session。

## 9. Adapter 内部契约

Crawler Adapter 不是浏览器 API，不暴露给普通用户。主后端通过 `CrawlerGateway` 使用以下规范化操作：

```text
health()
search(keyword, sourceKey)
fetchBook(sourceKey, sourceBookUrl)
fetchChapterList(sourceKey, sourceBookUrl)
fetchChapter(sourceKey, chapterUrl)
```

关键 DTO：

```json
{
  "sourceKey": "source-a",
  "sourceBookId": "123",
  "sourceUrl": "https://allowed.example/book/123",
  "title": "书名",
  "author": "作者",
  "description": "纯文本",
  "coverUrl": null,
  "category": null,
  "status": "UNKNOWN",
  "latestChapterTitle": "第十章"
}
```

```json
{
  "chapterIndex": 1,
  "title": "第一章",
  "sourceUrl": "https://allowed.example/chapter/1",
  "content": "纯文本正文",
  "publishedAt": null
}
```

Adapter 错误码：

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

主后端把 Adapter 错误保存到任务记录，但对浏览器只返回脱敏摘要。

## 10. 缓存与条件请求

- 启用 Redis 后，GET 书籍详情和章节目录使用 cache-aside。
- 管理端更新或采集落库后删除相关 cache key。
- 第一版正文不进入 Redis，避免大对象挤压缓存。
- 可对书籍详情/目录返回 `ETag`；这是优化项，不作为 Phase 2–4 的阻塞条件。

## 11. API 验收清单

- 注册、登录、刷新、登出和改密形成可撤销会话闭环。
- 匿名用户能搜索、查看详情/目录并阅读正文。
- 用户只能操作自己的书架、进度和历史。
- 所有管理端接口同时验证认证和 ADMIN 角色。
- 创建采集任务在 202 响应前不执行网络抓取。
- 重复任务、重复书架和重复进度均有确定的幂等/冲突语义。
- 正文 API 不返回未经清洗的 HTML。
- 分页、ID、时间和错误结构在所有端点一致。
- OpenAPI 实现必须与本文一致；若实现阶段调整契约，先更新本文和契约测试。
