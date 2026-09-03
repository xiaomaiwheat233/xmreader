# Flyway migrations

生产数据库迁移脚本存放在本目录，并按照 `docs/database.md` 中的版本规划维护。

已实现：

- `V1__create_users_and_sessions.sql`：用户与可撤销刷新会话。
- `V2__create_sources_books_and_chapters.sql`：内容来源、书籍与章节。
- `V3__create_bookshelves_progress_and_history.sql`：用户书架、阅读进度与最近阅读。

已经在任何共享数据库执行过的迁移禁止修改；后续结构变化必须新增迁移版本。
