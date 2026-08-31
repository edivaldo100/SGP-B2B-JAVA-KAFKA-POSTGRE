-- ============================================================
-- V1 — Schema inicial do SGP-B2B
-- ============================================================

CREATE TABLE orders (
    id           UUID        PRIMARY KEY,
    partner_id   UUID        NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL
);

CREATE INDEX idx_orders_partner_id ON orders (partner_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- ────────────────────────────────────────────────────────────

CREATE TABLE order_items (
    id          UUID          PRIMARY KEY,
    order_id    UUID          NOT NULL REFERENCES orders (id),
    product_id  VARCHAR(255)  NOT NULL,
    quantity    INT           NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ────────────────────────────────────────────────────────────

CREATE TABLE partner_credit (
    id               UUID          PRIMARY KEY,
    partner_id       UUID          NOT NULL UNIQUE,
    credit_limit     NUMERIC(19,2) NOT NULL CHECK (credit_limit >= 0),
    available_credit NUMERIC(19,2) NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_partner_credit_partner_id ON partner_credit (partner_id);

-- ────────────────────────────────────────────────────────────

CREATE TABLE idempotency_keys (
    id              UUID         PRIMARY KEY,
    partner_id      UUID         NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    order_id        UUID,
    response_status INT,
    response_body   TEXT,
    created_at      TIMESTAMP    NOT NULL,
    completed_at    TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency_partner_key UNIQUE (partner_id, idempotency_key)
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at) WHERE status = 'PROCESSING';

-- ────────────────────────────────────────────────────────────

CREATE TABLE outbox_events (
    id              UUID        PRIMARY KEY,
    aggregate_id    UUID        NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    retry_count     INT         NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP   NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMP   NOT NULL,
    published_at    TIMESTAMP
);

CREATE INDEX idx_outbox_status_next ON outbox_events (status, next_attempt_at)
    WHERE status = 'PENDING';
