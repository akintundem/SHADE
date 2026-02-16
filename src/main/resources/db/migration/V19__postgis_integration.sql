-- V19: PostGIS integration — add geometry columns, backfill, spatial indexes
-- Replaces bounding-box approximations with true geospatial queries.

-- ============================================================================
-- 1. ENABLE POSTGIS EXTENSION
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================================
-- 2. VENUES TABLE — add geometry(Point, 4326) column
-- ============================================================================
ALTER TABLE venues ADD COLUMN IF NOT EXISTS location geometry(Point, 4326);

-- Backfill from existing lat/lng
UPDATE venues
SET location = ST_SetSRID(ST_MakePoint(
    CAST(longitude AS double precision),
    CAST(latitude  AS double precision)
), 4326)
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL
  AND location IS NULL;

-- Spatial index (GiST)
CREATE INDEX IF NOT EXISTS idx_venues_location_gist ON venues USING GIST (location);

-- ============================================================================
-- 3. EVENTS TABLE — add geometry column for embedded venue
-- ============================================================================
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_location geometry(Point, 4326);

-- Backfill from existing embedded venue lat/lng
UPDATE events
SET venue_location = ST_SetSRID(ST_MakePoint(
    CAST(venue_longitude AS double precision),
    CAST(venue_latitude  AS double precision)
), 4326)
WHERE venue_latitude IS NOT NULL
  AND venue_longitude IS NOT NULL
  AND venue_location IS NULL;

-- Spatial index
CREATE INDEX IF NOT EXISTS idx_events_venue_location_gist ON events USING GIST (venue_location);

-- ============================================================================
-- 4. LOCATIONS TABLE — add geometry column
-- ============================================================================
ALTER TABLE locations ADD COLUMN IF NOT EXISTS location geometry(Point, 4326);

-- Backfill from existing lat/lng
UPDATE locations
SET location = ST_SetSRID(ST_MakePoint(
    CAST(longitude AS double precision),
    CAST(latitude  AS double precision)
), 4326)
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL
  AND location IS NULL;

-- Spatial index
CREATE INDEX IF NOT EXISTS idx_locations_location_gist ON locations USING GIST (location);

-- ============================================================================
-- 5. COMMENTS
-- ============================================================================
COMMENT ON COLUMN venues.location    IS 'PostGIS point (SRID 4326) — single source of truth for spatial queries';
COMMENT ON COLUMN events.venue_location IS 'PostGIS point (SRID 4326) for embedded venue — spatial queries on events';
COMMENT ON COLUMN locations.location IS 'PostGIS point (SRID 4326) — spatial queries on city locations';
