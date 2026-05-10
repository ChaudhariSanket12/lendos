-- ============================================================
-- V11: Loan application fields and tenant-scoped loan workflow
-- ============================================================

-- Borrower financial profile extensions captured during loan application.
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS total_work_experience NUMERIC(4,1);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS residence_type VARCHAR(30);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS years_at_current_residence NUMERIC(4,1);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS cibil_score INTEGER;

ALTER TABLE borrowers DROP CONSTRAINT IF EXISTS chk_borrowers_employment_type;
ALTER TABLE borrowers
    ADD CONSTRAINT chk_borrowers_employment_type
    CHECK (
        employment_type IS NULL
        OR employment_type IN (
            'SALARIED',
            'GOVERNMENT',
            'SELF_EMPLOYED',
            'PROFESSIONAL',
            'RETIRED',
            'OTHER',
            'BUSINESS'
        )
    );

ALTER TABLE borrowers DROP CONSTRAINT IF EXISTS chk_borrowers_residence_type;
ALTER TABLE borrowers
    ADD CONSTRAINT chk_borrowers_residence_type
    CHECK (
        residence_type IS NULL
        OR residence_type IN ('OWNED', 'RENTED', 'WITH_FAMILY', 'COMPANY_PROVIDED')
    );

ALTER TABLE borrowers DROP CONSTRAINT IF EXISTS chk_borrowers_cibil_score;
ALTER TABLE borrowers
    ADD CONSTRAINT chk_borrowers_cibil_score
    CHECK (cibil_score IS NULL OR cibil_score BETWEEN 300 AND 900);

-- Ensure loans table exists for fresh installations.
CREATE TABLE IF NOT EXISTS loans (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID           NOT NULL REFERENCES tenants(id),
    borrower_id           UUID           NOT NULL REFERENCES borrowers(id),
    principal_amount      NUMERIC(15,2)  NOT NULL CHECK (principal_amount > 0),
    annual_interest_rate  NUMERIC(5,2)   NOT NULL DEFAULT 12.00 CHECK (annual_interest_rate > 0),
    tenure_months         INTEGER        NOT NULL CHECK (tenure_months > 0),
    status                VARCHAR(20)    NOT NULL DEFAULT 'APPLIED',
    disbursement_date     DATE,
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

ALTER TABLE loans ADD COLUMN IF NOT EXISTS loan_amount NUMERIC(15,2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS loan_purpose VARCHAR(40);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS status_notes TEXT;

UPDATE loans
SET loan_amount = COALESCE(loan_amount, principal_amount)
WHERE loan_amount IS NULL;

UPDATE loans
SET applied_at = COALESCE(applied_at, created_at, NOW())
WHERE applied_at IS NULL;

ALTER TABLE loans ALTER COLUMN loan_amount SET NOT NULL;
ALTER TABLE loans ALTER COLUMN applied_at SET NOT NULL;
ALTER TABLE loans ALTER COLUMN annual_interest_rate SET DEFAULT 12.00;

ALTER TABLE loans DROP CONSTRAINT IF EXISTS chk_loan_status;
ALTER TABLE loans
    ADD CONSTRAINT chk_loan_status CHECK (
        status IN ('APPLIED','UNDER_ASSESSMENT','APPROVED','REJECTED',
                   'DISBURSED','ACTIVE','CLOSED','DEFAULTED')
    );

ALTER TABLE loans DROP CONSTRAINT IF EXISTS chk_loan_purpose;
ALTER TABLE loans
    ADD CONSTRAINT chk_loan_purpose CHECK (
        loan_purpose IS NULL
        OR loan_purpose IN (
            'DEBT_CONSOLIDATION',
            'HOME_RENOVATION',
            'MEDICAL',
            'EDUCATION',
            'BUSINESS',
            'WEDDING',
            'TRAVEL',
            'VEHICLE',
            'OTHER'
        )
    );

ALTER TABLE loans DROP CONSTRAINT IF EXISTS chk_loan_tenure_months;
ALTER TABLE loans
    ADD CONSTRAINT chk_loan_tenure_months
    CHECK (tenure_months IN (3, 6, 12, 18, 24, 36, 48, 60));

CREATE INDEX IF NOT EXISTS idx_loans_tenant_applied_at
    ON loans(tenant_id, applied_at DESC);
CREATE INDEX IF NOT EXISTS idx_loans_borrower_applied_at
    ON loans(borrower_id, applied_at DESC);
CREATE INDEX IF NOT EXISTS idx_loans_tenant_status_applied_at
    ON loans(tenant_id, status, applied_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_loans_updated_at'
    ) THEN
        CREATE TRIGGER trg_loans_updated_at
            BEFORE UPDATE ON loans
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- Risk assessment extension for stub evaluation details.
ALTER TABLE risk_assessments ADD COLUMN IF NOT EXISTS foir NUMERIC(5,2);
