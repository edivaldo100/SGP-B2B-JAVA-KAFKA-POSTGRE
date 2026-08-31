-- ============================================================
-- V3 — Tabela de transações de crédito (auditoria append-only)
-- ============================================================

CREATE TABLE credit_transaction (
    id          UUID           PRIMARY KEY,
    partner_id  UUID           NOT NULL,
    order_id    UUID           NOT NULL,
    type        VARCHAR(10)    NOT NULL CHECK (type IN ('DEBIT', 'RELEASE')),
    amount      DECIMAL(19,2)  NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- fonte de verdade: partner_credit.available_credit
-- esta tabela é somente auditoria / histórico de movimentações
CREATE INDEX idx_credit_transaction_partner_created
    ON credit_transaction (partner_id, created_at DESC);
