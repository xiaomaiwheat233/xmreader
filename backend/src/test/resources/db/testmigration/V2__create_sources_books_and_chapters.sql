CREATE TABLE content_sources (
    id BIGINT NOT NULL PRIMARY KEY,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(512) NOT NULL UNIQUE,
    adapter_type VARCHAR(32) NOT NULL CHECK (adapter_type IN ('FIXTURE', 'SONOVEL')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute SMALLINT,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

CREATE INDEX idx_sources_enabled ON content_sources (enabled);

CREATE TABLE books (
    id BIGINT NOT NULL PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_book_id VARCHAR(255),
    source_url VARCHAR(2048) NOT NULL,
    source_url_hash BINARY(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(128) NOT NULL,
    cover_url VARCHAR(2048),
    description CLOB,
    category VARCHAR(64),
    status VARCHAR(32) NOT NULL CHECK (status IN ('ONGOING', 'COMPLETED', 'PAUSED', 'UNKNOWN')),
    visibility VARCHAR(16) NOT NULL CHECK (visibility IN ('VISIBLE', 'HIDDEN')),
    word_count BIGINT NOT NULL DEFAULT 0,
    chapter_count INT NOT NULL DEFAULT 0,
    latest_chapter_id BIGINT,
    latest_chapter_title VARCHAR(255),
    last_crawled_at TIMESTAMP(3),
    deleted_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uk_books_source_book UNIQUE (source_id, source_book_id),
    CONSTRAINT uk_books_source_url_hash UNIQUE (source_id, source_url_hash),
    CONSTRAINT fk_books_source FOREIGN KEY (source_id) REFERENCES content_sources (id)
);

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
CREATE INDEX idx_books_updated ON books (visibility, deleted_at, updated_at);
CREATE INDEX idx_books_latest_chapter ON books (latest_chapter_id);

CREATE TABLE chapters (
    id BIGINT NOT NULL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    chapter_index INT NOT NULL CHECK (chapter_index >= 1),
    title VARCHAR(255) NOT NULL,
    content CLOB NOT NULL,
    content_hash BINARY(32) NOT NULL,
    source_url VARCHAR(2048),
    source_url_hash BINARY(32),
    word_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uk_chapters_book_index UNIQUE (book_id, chapter_index),
    CONSTRAINT uk_chapters_book_url_hash UNIQUE (book_id, source_url_hash),
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE INDEX idx_chapters_book_updated ON chapters (book_id, updated_at);
