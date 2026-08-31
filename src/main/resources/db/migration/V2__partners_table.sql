-- ============================================================
-- V2 — Tabela de parceiros com ID sequencial e nome
-- ============================================================

CREATE TABLE partners (
    id           SERIAL        PRIMARY KEY,
    partner_uuid UUID          NOT NULL UNIQUE,
    name         VARCHAR(255)  NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_partners_uuid ON partners (partner_uuid);
CREATE INDEX idx_partners_name ON partners (name);
