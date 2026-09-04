ALTER TABLE books
    ADD COLUMN owner_user_id BIGINT UNSIGNED NULL AFTER source_id,
    ADD COLUMN access_scope VARCHAR(16) NOT NULL DEFAULT 'PUBLIC' AFTER visibility,
    ADD COLUMN origin_type VARCHAR(16) NOT NULL DEFAULT 'CRAWLER' AFTER access_scope,
    ADD CONSTRAINT fk_books_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_books_access_scope CHECK (access_scope IN ('PUBLIC', 'PRIVATE')),
    ADD CONSTRAINT ck_books_origin_type CHECK (origin_type IN ('CRAWLER', 'UPLOAD', 'FIXTURE'));

CREATE INDEX idx_books_source_owner_url ON books (source_id, owner_user_id, source_url_hash);
ALTER TABLE books DROP INDEX uk_books_source_book;
ALTER TABLE books DROP INDEX uk_books_source_url_hash;
CREATE INDEX idx_books_access ON books (access_scope, owner_user_id, visibility, deleted_at);

CREATE TABLE remote_bookshelves (
    id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    source_id INT NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    source_url_hash BINARY(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(128) NOT NULL,
    description TEXT NULL,
    cover_url VARCHAR(2048) NULL,
    category VARCHAR(64) NULL,
    latest_chapter_title VARCHAR(255) NULL,
    status_text VARCHAR(64) NULL,
    import_supported TINYINT(1) NOT NULL DEFAULT 1,
    imported_book_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_remote_bookshelves PRIMARY KEY (id),
    CONSTRAINT uk_remote_bookshelves_user_url UNIQUE (user_id, source_url_hash),
    CONSTRAINT fk_remote_bookshelves_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_remote_bookshelves_imported_book FOREIGN KEY (imported_book_id) REFERENCES books (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_remote_bookshelves_user_created ON remote_bookshelves (user_id, created_at);
