-- ============================================================
-- V9: Tenant Firm Code for Borrower Self-Registration
-- ============================================================

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS firm_code VARCHAR(20);

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC, id ASC) AS rn
    FROM tenants
)
UPDATE tenants t
SET firm_code = 'DEMO-LEN-AB12'
FROM ranked r
WHERE t.id = r.id
  AND r.rn = 1
  AND t.firm_code IS NULL;

UPDATE tenants
SET firm_code = CONCAT(
        COALESCE(NULLIF(SUBSTRING(REGEXP_REPLACE(UPPER(name), '[^A-Z0-9]', '', 'g') FROM 1 FOR 8), ''), 'TENANT'),
        '-',
        UPPER(SUBSTRING(MD5(id::TEXT) FROM 1 FOR 4))
    )
WHERE firm_code IS NULL;

ALTER TABLE tenants
    ALTER COLUMN firm_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_firm_code_ci
    ON tenants (LOWER(firm_code));
