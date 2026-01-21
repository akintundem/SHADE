-- Migration V7: Set default values for event series columns
-- This migration sets default values for is_series_master and is_series_exception
-- for existing rows that have NULL values

DO $$
BEGIN
    -- Only update if the column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'events' AND column_name = 'is_series_master') THEN
        UPDATE events SET is_series_master = false WHERE is_series_master IS NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'events' AND column_name = 'is_series_exception') THEN
        UPDATE events SET is_series_exception = false WHERE is_series_exception IS NULL;
    END IF;
END $$;
