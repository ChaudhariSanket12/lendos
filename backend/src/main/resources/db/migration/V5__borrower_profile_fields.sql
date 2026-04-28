-- ============================================================
-- V5: Borrower Profile Fields for Borrower Management Module
-- ============================================================

-- Add new profile fields expected by the Borrower module.
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS first_name VARCHAR(120);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS last_name VARCHAR(120);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS address TEXT;

-- Backfill first/last name from legacy full_name where needed.
UPDATE borrowers
SET first_name = COALESCE(
        first_name,
        NULLIF(SPLIT_PART(COALESCE(full_name, ''), ' ', 1), ''),
        'Unknown'
    ),
    last_name = COALESCE(
        last_name,
        CASE
            WHEN POSITION(' ' IN COALESCE(full_name, '')) > 0
                THEN NULLIF(BTRIM(SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)), '')
            ELSE 'Borrower'
        END,
        'Borrower'
    )
WHERE first_name IS NULL OR last_name IS NULL;

ALTER TABLE borrowers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE borrowers ALTER COLUMN last_name SET NOT NULL;

-- Remove legacy columns no longer used by Borrower module.
DROP INDEX IF EXISTS idx_borrowers_pan;
ALTER TABLE borrowers DROP COLUMN IF EXISTS pan_number;
ALTER TABLE borrowers DROP COLUMN IF EXISTS monthly_income;
ALTER TABLE borrowers DROP COLUMN IF EXISTS full_name;

-- Helpful list query indexes (tenant-scoped + newest first).
CREATE INDEX IF NOT EXISTS idx_borrowers_tenant_created_at
    ON borrowers(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_borrowers_tenant_status_created_at
    ON borrowers(tenant_id, status, created_at DESC);
