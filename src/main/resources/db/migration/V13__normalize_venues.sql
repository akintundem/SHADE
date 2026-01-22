-- V13: Normalize venues from events.venue JSON to separate venues table

-- ============================================================================
-- VENUES TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    capacity INT,
    venue_type VARCHAR(50),
    accessibility_features TEXT,
    description TEXT,
    phone VARCHAR(40),
    email VARCHAR(180),
    website_url VARCHAR(500),
    parking_available BOOLEAN,
    public_transit_nearby BOOLEAN,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID
);

CREATE INDEX idx_venues_location ON venues(city, state, country);
CREATE INDEX idx_venues_coordinates ON venues(latitude, longitude);
CREATE INDEX idx_venues_name ON venues(name);
CREATE INDEX idx_venues_deleted ON venues(deleted_at) WHERE deleted_at IS NULL;

COMMENT ON TABLE venues IS 'Normalized venue data - reusable across events';
COMMENT ON COLUMN venues.latitude IS 'Latitude for geo-based searches';
COMMENT ON COLUMN venues.longitude IS 'Longitude for geo-based searches';
COMMENT ON COLUMN venues.capacity IS 'Maximum venue capacity';

-- ============================================================================
-- ADD VENUE_ID TO EVENTS
-- ============================================================================

-- Add venue_id foreign key to events table
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_id UUID;

-- Add foreign key constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_events_venue'
    ) THEN
        ALTER TABLE events
            ADD CONSTRAINT fk_events_venue
            FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_events_venue ON events(venue_id);

COMMENT ON COLUMN events.venue_id IS 'Foreign key to venues table (replaces venue JSON field)';

-- Note: events.venue JSON column will be deprecated in future migration
-- Data migration will parse JSON and create venue records, then link via venue_id
