CREATE TABLE content_sources (
    id BIGINT UNSIGNED NOT NULL,
    source_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    adapter_type VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    rate_limit_per_minute SMALLINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_content_sources PRIMARY KEY (id),
    CONSTRAINT uk_sources_key UNIQUE (source_key),
    CONSTRAINT uk_sources_base_url UNIQUE (base_url),
    CONSTRAINT ck_sources_adapter CHECK (adapter_type IN ('FIXTURE', 'SONOVEL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_sources_enabled ON content_sources (enabled);

CREATE TABLE books (
    id BIGINT UNSIGNED NOT NULL,
    source_id BIGINT UNSIGNED NOT NULL,
    source_book_id VARCHAR(255) NULL,
    source_url VARCHAR(2048) NOT NULL,
    source_url_hash BINARY(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(128) NOT NULL,
    cover_url VARCHAR(2048) NULL,
    description TEXT NULL,
    category VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    visibility VARCHAR(16) NOT NULL,
    word_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    chapter_count INT UNSIGNED NOT NULL DEFAULT 0,
    latest_chapter_id BIGINT UNSIGNED NULL,
    latest_chapter_title VARCHAR(255) NULL,
    last_crawled_at DATETIME(3) NULL,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT uk_books_source_book UNIQUE (source_id, source_book_id),
    CONSTRAINT uk_books_source_url_hash UNIQUE (source_id, source_url_hash),
    CONSTRAINT fk_books_source FOREIGN KEY (source_id) REFERENCES content_sources (id) ON DELETE RESTRICT,
    CONSTRAINT ck_books_status CHECK (status IN ('ONGOING', 'COMPLETED', 'PAUSED', 'UNKNOWN')),
    CONSTRAINT ck_books_visibility CHECK (visibility IN ('VISIBLE', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
CREATE INDEX idx_books_updated ON books (visibility, deleted_at, updated_at);
CREATE INDEX idx_books_latest_chapter ON books (latest_chapter_id);

CREATE TABLE chapters (
    id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,
    chapter_index INT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash BINARY(32) NOT NULL,
    source_url VARCHAR(2048) NULL,
    source_url_hash BINARY(32) NULL,
    word_count INT UNSIGNED NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_chapters PRIMARY KEY (id),
    CONSTRAINT uk_chapters_book_index UNIQUE (book_id, chapter_index),
    CONSTRAINT uk_chapters_book_url_hash UNIQUE (book_id, source_url_hash),
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT,
    CONSTRAINT ck_chapters_index CHECK (chapter_index >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_chapters_book_updated ON chapters (book_id, updated_at);
