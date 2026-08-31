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

-- Migra os 5 parceiros existentes em partner_credit
INSERT INTO partners (partner_uuid, name) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Parceiro 1'),
    ('a0000000-0000-0000-0000-000000000002', 'Parceiro 2'),
    ('a0000000-0000-0000-0000-000000000003', 'Parceiro 3'),
    ('a0000000-0000-0000-0000-000000000004', 'Parceiro 4'),
    ('a0000000-0000-0000-0000-000000000005', 'Parceiro 5');
