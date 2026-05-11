-- ============================================================
-- V12: Enhanced financial profile fields for loan application
-- ============================================================

ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS employer_name VARCHAR(255);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS industry_type VARCHAR(50);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS salary_payment_mode VARCHAR(20);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS rent_expense NUMERIC(15,2) DEFAULT 0;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS existing_loan_emis NUMERIC(15,2) DEFAULT 0;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS credit_card_payments NUMERIC(15,2) DEFAULT 0;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS other_fixed_expenses NUMERIC(15,2) DEFAULT 0;

ALTER TABLE borrowers DROP CONSTRAINT IF EXISTS chk_borrowers_salary_payment_mode;
ALTER TABLE borrowers
    ADD CONSTRAINT chk_borrowers_salary_payment_mode
    CHECK (
        salary_payment_mode IS NULL
        OR salary_payment_mode IN ('BANK_TRANSFER', 'CHEQUE', 'CASH')
    );

ALTER TABLE borrowers DROP CONSTRAINT IF EXISTS chk_borrowers_industry_type;
ALTER TABLE borrowers
    ADD CONSTRAINT chk_borrowers_industry_type
    CHECK (
        industry_type IS NULL
        OR industry_type IN (
            'INFORMATION_TECHNOLOGY',
            'MANUFACTURING',
            'BANKING',
            'HEALTHCARE',
            'EDUCATION',
            'RETAIL',
            'CONSTRUCTION',
            'OTHER'
        )
    );
