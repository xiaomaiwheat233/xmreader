CREATE TABLE bookshelves (
    id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_bookshelves PRIMARY KEY (id),
    CONSTRAINT uk_bookshelves_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_bookshelves_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookshelves_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_bookshelves_user_created ON bookshelves (user_id, created_at);

CREATE TABLE reading_progress (
    id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,
    chapter_id BIGINT UNSIGNED NOT NULL,
    position INT UNSIGNED NOT NULL DEFAULT 0,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_reading_progress PRIMARY KEY (id),
    CONSTRAINT uk_progress_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE,
    CONSTRAINT ck_progress_percent CHECK (progress_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_progress_user_updated ON reading_progress (user_id, updated_at);

CREATE TABLE reading_history (
    id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,
    chapter_id BIGINT UNSIGNED NOT NULL,
    visited_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_reading_history PRIMARY KEY (id),
    CONSTRAINT uk_history_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_history_user_visited ON reading_history (user_id, visited_at);
