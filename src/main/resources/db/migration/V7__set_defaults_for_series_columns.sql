-- Migration V7: Set default values for event series columns
-- This migration sets default values for is_series_master and is_series_exception
-- for existing rows before making them NOT NULL

-- Update existing events to have default values for series columns
UPDATE events
SET is_series_master = false
WHERE is_series_master IS NULL;

UPDATE events
SET is_series_exception = false
WHERE is_series_exception IS NULL;
