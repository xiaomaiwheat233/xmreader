CREATE TABLE users (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(2048),
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

CREATE INDEX idx_users_status_created ON users (status, created_at);

CREATE TABLE user_sessions (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token_hash BINARY(32) NOT NULL UNIQUE,
    expires_at TIMESTAMP(3) NOT NULL,
    revoked_at TIMESTAMP(3),
    last_used_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_user_active ON user_sessions (user_id, revoked_at, expires_at);
