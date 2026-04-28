-- ============================================================
-- V6: Seed Sample Borrowers for Default Tenant
-- ============================================================

WITH default_tenant AS (
    SELECT id
    FROM tenants
    ORDER BY created_at ASC
    LIMIT 1
)
INSERT INTO borrowers (
    tenant_id,
    first_name,
    last_name,
    email,
    phone,
    status,
    date_of_birth,
    address,
    created_by,
    updated_by
)
SELECT
    dt.id,
    sample.first_name,
    sample.last_name,
    sample.email,
    sample.phone,
    sample.status,
    sample.date_of_birth,
    sample.address,
    'SYSTEM_SEED',
    'SYSTEM_SEED'
FROM default_tenant dt
CROSS JOIN (
    VALUES
        ('Rohan', 'Sharma', 'rohan.sharma@sample.com', '+91-9876543210', 'DRAFT', DATE '1994-02-10', '12 MG Road, Bengaluru'),
        ('Priya', 'Mehta', 'priya.mehta@sample.com', '+91-9123456780', 'UNDER_REVIEW', DATE '1990-07-18', '44 Park Street, Kolkata'),
        ('Amit', 'Verma', 'amit.verma@sample.com', '+91-9988776655', 'VERIFIED', DATE '1988-11-03', '8 Civil Lines, Jaipur'),
        ('Neha', 'Kapoor', 'neha.kapoor@sample.com', '+91-9011223344', 'ACTIVE', DATE '1992-05-27', '72 Sector 21, Chandigarh'),
        ('Suresh', 'Iyer', 'suresh.iyer@sample.com', '+91-9090909090', 'BLACKLISTED', DATE '1985-09-14', '5 Marina Road, Chennai')
) AS sample(first_name, last_name, email, phone, status, date_of_birth, address)
WHERE NOT EXISTS (
    SELECT 1
    FROM borrowers b
    WHERE b.tenant_id = dt.id
);
