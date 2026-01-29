-- Migration V17: Remove event series feature
-- Drops the event_series table and removes series-related columns from the events table.

-- First drop the foreign key and series columns from events
ALTER TABLE events DROP COLUMN IF EXISTS parent_series_id;
ALTER TABLE events DROP COLUMN IF EXISTS series_occurrence_number;
ALTER TABLE events DROP COLUMN IF EXISTS is_series_master;
ALTER TABLE events DROP COLUMN IF EXISTS is_series_exception;
ALTER TABLE events DROP COLUMN IF EXISTS original_start_date_time;

-- Drop the event_series table
DROP TABLE IF EXISTS event_series CASCADE;
