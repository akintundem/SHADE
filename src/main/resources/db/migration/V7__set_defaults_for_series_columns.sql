-- Migration V7: Set default values for event series columns
-- This migration sets default values for is_series_master and is_series_exception
-- for existing rows before making them NOT NULL
-- Note: This migration will be handled by Hibernate ddl-auto=update instead

-- Column creation and default value setting will be handled by Hibernate
-- when it creates/updates the events table schema based on the Event entity
