-- BUZTRACK MIGRATION 002: BILL SCANNER, ATTACHMENTS & EXPORT ENHANCEMENTS
-- Engine: PostgreSQL 14+

-- Ensure bills table columns
ALTER TABLE bills ADD COLUMN IF NOT EXISTS attachment_url TEXT;
ALTER TABLE bills ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE bills ADD COLUMN IF NOT EXISTS ocr_status VARCHAR(30) DEFAULT 'COMPLETED';

-- Ensure bill_items table columns
ALTER TABLE bill_items ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5, 2) DEFAULT 0.00;
ALTER TABLE bill_items ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(15, 2) DEFAULT 0.00;

-- Composite Index for Server-Side Duplicate Invoice Detection
CREATE INDEX IF NOT EXISTS idx_bills_duplicate_check ON bills(business_id, supplier_id, invoice_number);
