-- V15: Normalize metadata fields from JSON/TEXT to relational tables

-- ============================================================================
-- EVENT METADATA
-- ============================================================================

CREATE TABLE IF NOT EXISTS event_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    metadata_type VARCHAR(20) DEFAULT 'STRING',

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_event_metadata_event FOREIGN KEY (event_id)
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT uk_event_metadata UNIQUE (event_id, metadata_key)
);

CREATE INDEX idx_event_metadata_event ON event_metadata(event_id);
CREATE INDEX idx_event_metadata_key ON event_metadata(metadata_key);
CREATE INDEX idx_event_metadata_deleted ON event_metadata(deleted_at);

COMMENT ON TABLE event_metadata IS 'Normalized event metadata - replaces events.metadata JSON';
COMMENT ON COLUMN event_metadata.metadata_type IS 'Data type hint: STRING, NUMBER, BOOLEAN, JSON, DATE';

-- ============================================================================
-- TICKET METADATA
-- ============================================================================

CREATE TABLE IF NOT EXISTS ticket_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_ticket_metadata_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_metadata UNIQUE (ticket_id, metadata_key)
);

CREATE INDEX idx_ticket_metadata_ticket ON ticket_metadata(ticket_id);
CREATE INDEX idx_ticket_metadata_key ON ticket_metadata(metadata_key);
CREATE INDEX idx_ticket_metadata_deleted ON ticket_metadata(deleted_at);

COMMENT ON TABLE ticket_metadata IS 'Normalized ticket metadata - replaces tickets.metadata TEXT';

-- ============================================================================
-- TICKET TYPE METADATA
-- ============================================================================

CREATE TABLE IF NOT EXISTS ticket_type_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_type_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_ticket_type_metadata_type FOREIGN KEY (ticket_type_id)
        REFERENCES ticket_types(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_type_metadata UNIQUE (ticket_type_id, metadata_key)
);

CREATE INDEX idx_ticket_type_metadata_type ON ticket_type_metadata(ticket_type_id);
CREATE INDEX idx_ticket_type_metadata_key ON ticket_type_metadata(metadata_key);
CREATE INDEX idx_ticket_type_metadata_deleted ON ticket_type_metadata(deleted_at);

COMMENT ON TABLE ticket_type_metadata IS 'Normalized ticket type metadata - replaces ticket_types.metadata TEXT';

-- Note: Old metadata TEXT/JSON columns remain for backward compatibility
-- Will be deprecated after data migration and verification
