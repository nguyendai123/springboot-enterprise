-- V1__init_schema.sql
-- Initial schema for enterprise backend

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- trigram search

-- ─── Users ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(100) NOT NULL UNIQUE,
    password              TEXT         NOT NULL,
    first_name            VARCHAR(50),
    last_name             VARCHAR(50),
    phone_number          VARCHAR(20),
    avatar_url            TEXT,
    status                VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    two_factor_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    last_login_at         TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMP
);

CREATE INDEX idx_users_email        ON users(email)        WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username     ON users(username)     WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status       ON users(status)       WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at   ON users(created_at);
CREATE INDEX idx_users_email_trgm   ON users USING gin (email gin_trgm_ops);

-- User roles (element collection)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roles   VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, roles)
);

-- ─── Products ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock       INT           NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category    VARCHAR(100),
    sku         VARCHAR(100) UNIQUE,
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_products_category  ON products(category) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_status    ON products(status)   WHERE deleted_at IS NULL;
CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);

-- ─── Orders ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id           UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID          NOT NULL REFERENCES users(id),
    status       VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL DEFAULT 'USD',
    notes        TEXT,
    paid_at      TIMESTAMP,
    shipped_at   TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id    ON orders(user_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- Order items
CREATE TABLE IF NOT EXISTS order_items (
    id          UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id    UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES products(id),
    quantity    INT           NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(12,2) NOT NULL,
    total_price NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

-- ─── Audit Log ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(100) NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    actor_id     VARCHAR(100),
    actor_name   VARCHAR(100),
    before_state JSONB,
    after_state  JSONB,
    ip_address   VARCHAR(50),
    occurred_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_actor       ON audit_logs(actor_id);
CREATE INDEX idx_audit_occurred_at ON audit_logs(occurred_at DESC);

-- ─── Failed Messages ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS failed_messages (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    topic       VARCHAR(200) NOT NULL,
    key         VARCHAR(200),
    payload     JSONB        NOT NULL,
    error       TEXT,
    retry_count INT          NOT NULL DEFAULT 0,
    status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_failed_msgs_status ON failed_messages(status);

-- ─── Refresh Tokens ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);

-- ─── Default seed data ────────────────────────────────────────────────────
INSERT INTO users (id, username, email, password, first_name, last_name, status, email_verified)
VALUES (
    uuid_generate_v4(),
    'admin',
    'admin@enterprise.com',
    -- bcrypt of 'Admin@123' (12 rounds)
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2yEmr3ukMi',
    'System',
    'Admin',
    'ACTIVE',
    TRUE
) ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, roles)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;