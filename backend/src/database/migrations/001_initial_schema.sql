-- BUZTRACK PRODUCTION DATABASE SCHEMA
-- Version: 1.0.0
-- Database Engine: PostgreSQL 14+
-- Design Principles: Multi-tenant tenant-aware schema, strict referential integrity, decimal representation for currency.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    pin_hash VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. BUSINESSES (Tenants)
CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    phone VARCHAR(20),
    address TEXT,
    gstin VARCHAR(20),
    logo_url TEXT,
    business_type VARCHAR(50) DEFAULT 'RETAIL',
    currency_code VARCHAR(10) DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. BUSINESS MEMBERS (Staff & Role-based Access)
CREATE TABLE IF NOT EXISTS business_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL DEFAULT 'STAFF', -- 'OWNER', 'ADMIN', 'STAFF'
    permissions JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, user_id)
);

-- 4. SUBSCRIPTION PLANS
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL, -- 'FREE', 'PRO_MONTHLY', 'PRO_YEARLY'
    name VARCHAR(100) NOT NULL,
    price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY', -- 'MONTHLY', 'YEARLY', 'LIFETIME'
    max_staff_members INT NOT NULL DEFAULT 1,
    max_scanned_bills_per_month INT NOT NULL DEFAULT 10,
    is_active BOOLEAN DEFAULT true,
    features JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. SUBSCRIPTIONS
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'EXPIRED', 'CANCELLED', 'PAST_DUE'
    start_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. SUBSCRIPTION PAYMENTS
CREATE TABLE IF NOT EXISTS subscription_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_gateway VARCHAR(50),
    payment_reference_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'SUCCESS', 'FAILED', 'PENDING'
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. ACCOUNTS (Money Sources: Cash, Bank, UPI, Card)
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL, -- 'CASH', 'UPI', 'BANK', 'CARD'
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. CUSTOMERS (Udhari / Credit)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    gstin VARCHAR(20),
    total_credit_given NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_payments_received NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    pending_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    last_transaction_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. SUPPLIERS
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    gstin VARCHAR(20),
    total_purchases NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_paid NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    pending_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    last_purchase_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. BILLS & RECEIPTS
CREATE TABLE IF NOT EXISTS bills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    supplier_name VARCHAR(150),
    invoice_number VARCHAR(100),
    invoice_date DATE,
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    image_url TEXT,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED', -- 'PENDING_VERIFICATION', 'VERIFIED'
    is_duplicate BOOLEAN DEFAULT false,
    ocr_raw_data JSONB,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. BILL ITEMS
CREATE TABLE IF NOT EXISTS bill_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_id UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL DEFAULT 1.00,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00
);

-- 12. TRANSACTIONS
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL, -- 'INCOME', 'EXPENSE', 'CUSTOMER_CREDIT', 'CUSTOMER_PAYMENT', 'SUPPLIER_PURCHASE', 'SUPPLIER_PAYMENT', 'TRANSFER_IN', 'TRANSFER_OUT'
    amount NUMERIC(15, 2) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- 'CASH', 'UPI', 'BANK', 'CARD'
    transaction_date DATE NOT NULL,
    transaction_time VARCHAR(20) DEFAULT '10:30 AM',
    note TEXT,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    bill_id UUID REFERENCES bills(id) ON DELETE SET NULL,
    transfer_target_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. RECURRING TRANSACTIONS
CREATE TABLE IF NOT EXISTS recurring_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL, -- 'INCOME', 'EXPENSE'
    category_name VARCHAR(100) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY', -- 'DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'
    next_due_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. REMINDERS
CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL, -- 'CUSTOMER_DUE', 'SUPPLIER_DUE', 'BILL_DUE', 'RECURRING'
    reference_id UUID,
    title VARCHAR(150) NOT NULL,
    message TEXT,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'COMPLETED', 'CANCELLED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. DAILY CLOSINGS (Cash Book)
CREATE TABLE IF NOT EXISTS daily_closings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    closing_date DATE NOT NULL,
    expected_income NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    expected_expense NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    expected_cash NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    actual_cash NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    difference NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    closed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    closed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, closing_date)
);

-- 16. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'ALERT',
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 17. ONLINE PAYMENT LINKS
CREATE TABLE IF NOT EXISTS payment_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    amount NUMERIC(15, 2) NOT NULL,
    description TEXT,
    payment_url TEXT,
    gateway_reference_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'EXPIRED', 'FAILED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ
);

-- INDEXES FOR MULTI-TENANT ISOLATION AND PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_business_members_business_id ON business_members(business_id);
CREATE INDEX IF NOT EXISTS idx_business_members_user_id ON business_members(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_business_id ON accounts(business_id);
CREATE INDEX IF NOT EXISTS idx_customers_business_id ON customers(business_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_business_id ON suppliers(business_id);
CREATE INDEX IF NOT EXISTS idx_bills_business_id ON bills(business_id);
CREATE INDEX IF NOT EXISTS idx_bills_supplier_id ON bills(supplier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_business_id ON transactions(business_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(business_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_customer ON transactions(business_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_supplier ON transactions(business_id, supplier_id);
CREATE INDEX IF NOT EXISTS idx_recurring_business ON recurring_transactions(business_id, is_active);
CREATE INDEX IF NOT EXISTS idx_reminders_business ON reminders(business_id, due_date, status);
CREATE INDEX IF NOT EXISTS idx_daily_closings_business ON daily_closings(business_id, closing_date);

-- DEFAULT SEED DATA FOR SUBSCRIPTION PLANS
INSERT INTO subscription_plans (code, name, price, billing_cycle, max_staff_members, max_scanned_bills_per_month, features)
VALUES
('FREE', 'Free Plan', 0.00, 'LIFETIME', 1, 10, '{"multi_device": true, "cloud_backup": true, "ocr_scans": 10}'::jsonb),
('PRO_MONTHLY', 'Pro Plan Monthly', 299.00, 'MONTHLY', 5, 500, '{"multi_device": true, "cloud_backup": true, "ocr_scans": 500, "unlimited_reports": true, "whatsapp_reminders": true}'::jsonb),
('PRO_YEARLY', 'Pro Plan Yearly', 2999.00, 'YEARLY', 10, 5000, '{"multi_device": true, "cloud_backup": true, "ocr_scans": 5000, "unlimited_reports": true, "whatsapp_reminders": true}'::jsonb)
ON CONFLICT (code) DO NOTHING;
