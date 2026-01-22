-- V14: Normalize currency codes to reference table

-- ============================================================================
-- CURRENCIES REFERENCE TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INT NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE currencies IS 'ISO 4217 currency reference table';
COMMENT ON COLUMN currencies.code IS 'ISO 4217 3-letter currency code (USD, EUR, GBP, etc.)';
COMMENT ON COLUMN currencies.decimal_places IS 'Number of decimal places (2 for most, 0 for JPY)';

-- ============================================================================
-- SEED COMMON CURRENCIES
-- ============================================================================

INSERT INTO currencies (code, name, symbol, decimal_places, is_active) VALUES
('USD', 'US Dollar', '$', 2, true),
('EUR', 'Euro', '€', 2, true),
('GBP', 'British Pound', '£', 2, true),
('JPY', 'Japanese Yen', '¥', 0, true),
('CAD', 'Canadian Dollar', 'CA$', 2, true),
('AUD', 'Australian Dollar', 'A$', 2, true),
('NGN', 'Nigerian Naira', '₦', 2, true),
('ZAR', 'South African Rand', 'R', 2, true),
('INR', 'Indian Rupee', '₹', 2, true),
('CNY', 'Chinese Yuan', '¥', 2, true),
('BRL', 'Brazilian Real', 'R$', 2, true),
('MXN', 'Mexican Peso', '$', 2, true),
('CHF', 'Swiss Franc', 'CHF', 2, true),
('SEK', 'Swedish Krona', 'kr', 2, true),
('NZD', 'New Zealand Dollar', 'NZ$', 2, true)
ON CONFLICT (code) DO NOTHING;

-- Note: Existing currency fields in ticket_types, budgets, etc. remain as VARCHAR
-- Future: Add foreign key constraints after validating all existing currency codes are in this table
-- For now, this table serves as a reference for validation and metadata lookup
