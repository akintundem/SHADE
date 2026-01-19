-- Add ticket type pricing metadata, dependencies, and templates.
-- Postgres dialect.

ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS early_bird_price_minor BIGINT,
    ADD COLUMN IF NOT EXISTS early_bird_end_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS group_discount_min_qty INTEGER,
    ADD COLUMN IF NOT EXISTS group_discount_percent_bps INTEGER;

CREATE TABLE IF NOT EXISTS ticket_price_tiers (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    name VARCHAR(120),
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    price_minor BIGINT NOT NULL,
    priority INTEGER
);

CREATE INDEX IF NOT EXISTS idx_ticket_price_tier_type ON ticket_price_tiers (ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_ticket_price_tier_dates ON ticket_price_tiers (starts_at, ends_at);

CREATE TABLE IF NOT EXISTS ticket_type_dependencies (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    required_ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    min_quantity INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_dependency_type ON ticket_type_dependencies (ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_ticket_dependency_required ON ticket_type_dependencies (required_ticket_type_id);

CREATE TABLE IF NOT EXISTS ticket_type_templates (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    created_by UUID REFERENCES auth_users(id),
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    price_minor BIGINT,
    currency VARCHAR(3) NOT NULL,
    quantity_available INTEGER NOT NULL,
    sale_start_date TIMESTAMP,
    sale_end_date TIMESTAMP,
    max_tickets_per_person INTEGER,
    requires_approval BOOLEAN NOT NULL,
    early_bird_price_minor BIGINT,
    early_bird_end_date TIMESTAMP,
    group_discount_min_qty INTEGER,
    group_discount_percent_bps INTEGER,
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS idx_ticket_type_templates_created_by ON ticket_type_templates (created_by);
