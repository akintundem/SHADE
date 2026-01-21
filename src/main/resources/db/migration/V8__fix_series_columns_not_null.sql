-- Migration V8: Fix NOT NULL constraints for event series columns
-- This migration ensures is_series_master and is_series_exception have NOT NULL constraints
-- It updates any remaining NULL values and adds the constraint if needed

DO $$
BEGIN
    -- Fix is_series_master column
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'events' AND column_name = 'is_series_master') THEN
        -- Update any remaining NULL values to false
        UPDATE events SET is_series_master = false WHERE is_series_master IS NULL;
        
        -- Add NOT NULL constraint if column is currently nullable
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'events' 
            AND column_name = 'is_series_master'
            AND is_nullable = 'YES'
        ) THEN
            ALTER TABLE events ALTER COLUMN is_series_master SET NOT NULL;
        END IF;
    END IF;

    -- Fix is_series_exception column
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'events' AND column_name = 'is_series_exception') THEN
        -- Update any remaining NULL values to false
        UPDATE events SET is_series_exception = false WHERE is_series_exception IS NULL;
        
        -- Add NOT NULL constraint if column is currently nullable
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'events' 
            AND column_name = 'is_series_exception'
            AND is_nullable = 'YES'
        ) THEN
            ALTER TABLE events ALTER COLUMN is_series_exception SET NOT NULL;
        END IF;
    END IF;
END $$;
