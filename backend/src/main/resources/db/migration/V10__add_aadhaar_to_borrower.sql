-- ============================================================
-- V10: Add Aadhaar field for borrower KYC profile completion
-- ============================================================

ALTER TABLE borrowers
    ADD COLUMN IF NOT EXISTS aadhaar_number VARCHAR(12);
