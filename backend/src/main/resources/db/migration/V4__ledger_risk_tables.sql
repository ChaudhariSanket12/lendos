-- ============================================================
-- V4: Ledger & Risk Assessment Tables
-- ============================================================

-- ── Ledger Entries ────────────────────────────────────────────
-- APPEND-ONLY TABLE. No UPDATE or DELETE permitted. Ever.
-- Double-entry: every financial event → exactly 2 rows (DEBIT + CREDIT)
-- transaction_group_id links the paired entries together.
CREATE TABLE ledger_entries (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID          NOT NULL REFERENCES tenants(id),
    transaction_group_id  UUID          NOT NULL,
    reference_id          UUID          NOT NULL,
    reference_type        VARCHAR(50)   NOT NULL,
    entry_type            VARCHAR(10)   NOT NULL,
    account_type          VARCHAR(30)   NOT NULL,
    amount                NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    value_date            DATE          NOT NULL,
    narration             TEXT,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),

    CONSTRAINT chk_entry_type   CHECK (entry_type   IN ('DEBIT','CREDIT')),
    CONSTRAINT chk_account_type CHECK (account_type IN (
        'LOAN_PRINCIPAL','LOAN_INTEREST','CASH',
        'PENALTY_INCOME','INTEREST_INCOME','SUSPENSE'
    ))
);

-- ── Risk Assessments ──────────────────────────────────────────
CREATE TABLE risk_assessments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id         UUID          NOT NULL REFERENCES loans(id) UNIQUE,
    risk_score      NUMERIC(5,2)  NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    decision        VARCHAR(20)   NOT NULL,
    reason_codes    TEXT,
    rules_evaluated TEXT,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),

    CONSTRAINT chk_risk_decision CHECK (
        decision IN ('APPROVED','REJECTED','NEEDS_REVIEW')
    )
);

-- ── Indexes ──────────────────────────────────────────────────
CREATE INDEX idx_ledger_tenant          ON ledger_entries(tenant_id);
CREATE INDEX idx_ledger_txn_group       ON ledger_entries(transaction_group_id);
CREATE INDEX idx_ledger_reference       ON ledger_entries(reference_id, reference_type);
CREATE INDEX idx_ledger_value_date      ON ledger_entries(value_date);
CREATE INDEX idx_ledger_account_type    ON ledger_entries(account_type);
CREATE INDEX idx_risk_loan_id           ON risk_assessments(loan_id);

-- NOTE: No UPDATE/DELETE triggers on ledger_entries by design.
-- The append-only constraint is enforced at the application layer (LedgerService).

CREATE TRIGGER trg_risk_assessments_updated_at
    BEFORE UPDATE ON risk_assessments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
