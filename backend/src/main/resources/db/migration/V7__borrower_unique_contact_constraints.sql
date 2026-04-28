-- ============================================================
-- V7: Borrower Contact Uniqueness Constraints (Per Tenant)
-- ============================================================

-- Normalize existing data before applying unique constraints.
UPDATE borrowers
SET email = LOWER(BTRIM(email))
WHERE email IS NOT NULL;

UPDATE borrowers
SET phone = REGEXP_REPLACE(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL;

-- Email uniqueness should be case-insensitive inside the same tenant.
CREATE UNIQUE INDEX IF NOT EXISTS uq_borrowers_tenant_email_ci
    ON borrowers (tenant_id, LOWER(email));

-- Phone uniqueness inside the same tenant after normalization.
CREATE UNIQUE INDEX IF NOT EXISTS uq_borrowers_tenant_phone
    ON borrowers (tenant_id, phone)
    WHERE phone IS NOT NULL;
