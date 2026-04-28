-- ============================================================
-- V3: Payment Engine Tables — EMI Schedule & Payments
-- ============================================================

-- ── EMI Schedules ─────────────────────────────────────────────
-- Generated once at disbursement. IMMUTABLE after creation.
CREATE TABLE emi_schedules (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id                     UUID          NOT NULL REFERENCES loans(id),
    installment_number          INTEGER       NOT NULL CHECK (installment_number > 0),
    due_date                    DATE          NOT NULL,
    principal_component         NUMERIC(15,2) NOT NULL CHECK (principal_component >= 0),
    interest_component          NUMERIC(15,2) NOT NULL CHECK (interest_component >= 0),
    total_emi_amount            NUMERIC(15,2) NOT NULL CHECK (total_emi_amount > 0),
    outstanding_principal_after NUMERIC(15,2) NOT NULL CHECK (outstanding_principal_after >= 0),
    status                      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    version                     BIGINT        NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255),

    CONSTRAINT uq_loan_installment UNIQUE (loan_id, installment_number),
    CONSTRAINT chk_emi_status CHECK (
        status IN ('PENDING','PAID','PARTIALLY_PAID','OVERDUE','WAIVED')
    )
);

-- ── Payments ──────────────────────────────────────────────────
-- idempotency_key prevents duplicate recording on retried requests.
CREATE TABLE payments (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id           UUID          NOT NULL REFERENCES loans(id),
    idempotency_key   VARCHAR(255)  NOT NULL UNIQUE,
    amount_paid       NUMERIC(15,2) NOT NULL CHECK (amount_paid > 0),
    principal_portion NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (principal_portion >= 0),
    interest_portion  NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (interest_portion >= 0),
    penalty_portion   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (penalty_portion >= 0),
    payment_date      DATE          NOT NULL,
    payment_mode      VARCHAR(20)   NOT NULL,
    reference_number  VARCHAR(255),
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),

    CONSTRAINT chk_payment_mode CHECK (
        payment_mode IN ('CASH','BANK_TRANSFER','CHEQUE','UPI','NEFT','RTGS')
    )
);

-- ── Indexes ──────────────────────────────────────────────────
CREATE INDEX idx_emi_loan_id        ON emi_schedules(loan_id);
CREATE INDEX idx_emi_due_date       ON emi_schedules(due_date);
CREATE INDEX idx_emi_status         ON emi_schedules(status);
CREATE INDEX idx_payments_loan_id   ON payments(loan_id);
CREATE INDEX idx_payments_idem_key  ON payments(idempotency_key);
CREATE INDEX idx_payments_date      ON payments(payment_date);

-- ── Triggers ─────────────────────────────────────────────────
CREATE TRIGGER trg_emi_schedules_updated_at
    BEFORE UPDATE ON emi_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
