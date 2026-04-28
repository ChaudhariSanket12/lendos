-- ============================================================
-- V2: Borrower & Loan Tables — Placeholder
-- Full columns will be added when Module 2 & 3 are implemented.
-- Adding minimal structure so entities map cleanly.
-- ============================================================

-- ── Borrowers ────────────────────────────────────────────────
CREATE TABLE borrowers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    pan_number      VARCHAR(20)  UNIQUE,
    monthly_income  NUMERIC(15,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),

    CONSTRAINT chk_borrower_status CHECK (
        status IN ('DRAFT','UNDER_REVIEW','VERIFIED','ACTIVE','BLACKLISTED')
    )
);

-- ── Loans ─────────────────────────────────────────────────────
CREATE TABLE loans (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID           NOT NULL REFERENCES tenants(id),
    borrower_id           UUID           NOT NULL REFERENCES borrowers(id),
    principal_amount      NUMERIC(15,2)  NOT NULL CHECK (principal_amount > 0),
    annual_interest_rate  NUMERIC(5,2)   NOT NULL CHECK (annual_interest_rate > 0),
    tenure_months         INTEGER        NOT NULL CHECK (tenure_months > 0),
    status                VARCHAR(20)    NOT NULL DEFAULT 'APPLIED',
    disbursement_date     DATE,
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),

    CONSTRAINT chk_loan_status CHECK (
        status IN ('APPLIED','UNDER_ASSESSMENT','APPROVED','REJECTED',
                   'DISBURSED','ACTIVE','CLOSED','DEFAULTED')
    )
);

-- ── Indexes ──────────────────────────────────────────────────
CREATE INDEX idx_borrowers_tenant     ON borrowers(tenant_id);
CREATE INDEX idx_borrowers_email      ON borrowers(email);
CREATE INDEX idx_borrowers_pan        ON borrowers(pan_number);
CREATE INDEX idx_loans_tenant         ON loans(tenant_id);
CREATE INDEX idx_loans_borrower       ON loans(borrower_id);
CREATE INDEX idx_loans_status         ON loans(status);

-- ── Triggers ─────────────────────────────────────────────────
CREATE TRIGGER trg_borrowers_updated_at
    BEFORE UPDATE ON borrowers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_loans_updated_at
    BEFORE UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
