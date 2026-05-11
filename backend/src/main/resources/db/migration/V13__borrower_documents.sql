-- ============================================================
-- V13: Borrower document metadata table
-- ============================================================

CREATE TABLE IF NOT EXISTS borrower_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    borrower_id UUID NOT NULL REFERENCES borrowers(id) ON DELETE CASCADE,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('PAN', 'AADHAAR')),
    document_url TEXT NOT NULL,
    storage_path VARCHAR(500),
    original_size BIGINT,
    compressed_size BIGINT,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    ocr_text TEXT,
    verified_at TIMESTAMP,
    verified_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_borrower_docs_borrower
    ON borrower_documents(borrower_id);
CREATE INDEX IF NOT EXISTS idx_borrower_docs_type
    ON borrower_documents(borrower_id, document_type);
CREATE UNIQUE INDEX IF NOT EXISTS idx_borrower_docs_one_per_type
    ON borrower_documents(borrower_id, document_type)
    WHERE verification_status = 'PENDING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_borrower_documents_updated_at'
    ) THEN
        CREATE TRIGGER trg_borrower_documents_updated_at
            BEFORE UPDATE ON borrower_documents
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
