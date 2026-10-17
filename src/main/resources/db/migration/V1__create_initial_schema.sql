CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name    VARCHAR(80)  NOT NULL,
    UNIQUE (user_id, name)
);

CREATE TABLE expenses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id BIGINT         REFERENCES categories (id) ON DELETE SET NULL,
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(255),
    spent_on    DATE           NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_user_date ON expenses (user_id, spent_on DESC);
CREATE INDEX idx_categories_user ON categories (user_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
