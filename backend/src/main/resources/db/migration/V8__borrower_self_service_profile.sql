-- ============================================================
-- V8: Borrower Self-Service Profile Fields + User Link
-- ============================================================

-- Allow BORROWER role in users table.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_user_role;
ALTER TABLE users
    ADD CONSTRAINT chk_user_role
    CHECK (role IN ('ADMIN', 'CREDIT_OFFICER', 'AUDITOR', 'BORROWER'));

-- Borrower financial + identity fields for self-service onboarding.
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS monthly_income NUMERIC(15,2);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS employment_type VARCHAR(30);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS years_in_current_job NUMERIC(4,1);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS existing_monthly_obligations NUMERIC(15,2) DEFAULT 0;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS pan_number VARCHAR(10);
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS credit_score INTEGER;
ALTER TABLE borrowers ADD COLUMN IF NOT EXISTS user_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_borrowers_user_id'
    ) THEN
        ALTER TABLE borrowers
            ADD CONSTRAINT fk_borrowers_user_id
            FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_borrowers_user_id
    ON borrowers(user_id)
    WHERE user_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_borrowers_employment_type'
    ) THEN
        ALTER TABLE borrowers
            ADD CONSTRAINT chk_borrowers_employment_type
            CHECK (
                employment_type IS NULL
                OR employment_type IN ('SALARIED', 'SELF_EMPLOYED', 'GOVERNMENT', 'BUSINESS')
            );
    END IF;
END $$;

-- Seed one borrower login/profile for testing borrower portal.
WITH default_tenant AS (
    SELECT id
    FROM tenants
    ORDER BY created_at ASC
    LIMIT 1
),
existing_user AS (
    SELECT u.id, u.tenant_id
    FROM users u
    JOIN default_tenant dt ON dt.id = u.tenant_id
    WHERE LOWER(u.email) = LOWER('borrower.seed@lendos.com')
    LIMIT 1
),
inserted_user AS (
    INSERT INTO users (
        id, full_name, email, password, role, status, tenant_id,
        version, created_at, updated_at, created_by, updated_by
    )
    SELECT
        uuid_generate_v4(),
        'Seed Borrower',
        'borrower.seed@lendos.com',
        '$2b$12$B.XTTJ4ByD5BzzbK926P3Opgs82DvsgqQawhSYRQUEOtucSpqK/My',
        'BORROWER',
        'ACTIVE',
        dt.id,
        0,
        NOW(),
        NOW(),
        'SYSTEM_SEED',
        'SYSTEM_SEED'
    FROM default_tenant dt
    WHERE NOT EXISTS (SELECT 1 FROM existing_user)
    RETURNING id, tenant_id
),
seed_user AS (
    SELECT id, tenant_id FROM existing_user
    UNION ALL
    SELECT id, tenant_id FROM inserted_user
)
INSERT INTO borrowers (
    id, tenant_id, first_name, last_name, email, phone, status, date_of_birth, address,
    monthly_income, employment_type, years_in_current_job, existing_monthly_obligations,
    pan_number, credit_score, user_id, version, created_at, updated_at, created_by, updated_by
)
SELECT
    uuid_generate_v4(),
    su.tenant_id,
    'Seed',
    'Borrower',
    'borrower.seed@lendos.com',
    '9876543211',
    'UNDER_REVIEW',
    DATE '1993-04-15',
    '101 Seed Street, Pune',
    75000.00,
    'SALARIED',
    5.5,
    12000.00,
    'ABCDE1234F',
    735,
    su.id,
    0,
    NOW(),
    NOW(),
    'SYSTEM_SEED',
    'SYSTEM_SEED'
FROM seed_user su
WHERE NOT EXISTS (
    SELECT 1
    FROM borrowers b
    WHERE b.user_id = su.id
);
