-- V10: Collaboration permissions system
-- Adds event_user_permissions table for granular permission overrides

-- Create event_user_permissions table if not exists
CREATE TABLE IF NOT EXISTS event_user_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_user_id UUID NOT NULL,
    permission VARCHAR(120) NOT NULL,

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Foreign key
    CONSTRAINT fk_event_user_permissions_user FOREIGN KEY (event_user_id)
        REFERENCES event_users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_event_user_permissions_user ON event_user_permissions(event_user_id);
CREATE INDEX IF NOT EXISTS idx_event_user_permissions_permission ON event_user_permissions(permission);
CREATE INDEX IF NOT EXISTS idx_event_user_permissions_deleted ON event_user_permissions(deleted_at);

-- Ensure event_users table has the permissions relationship ready
-- Add any missing columns to event_users if needed (idempotent)
DO $$
BEGIN
    -- No structural changes needed, just ensuring compatibility
    -- event_users table should already exist with proper structure
END $$;

COMMENT ON TABLE event_user_permissions IS 'Granular permission overrides for event collaborators';
COMMENT ON COLUMN event_user_permissions.event_user_id IS 'Foreign key to event_users table';
COMMENT ON COLUMN event_user_permissions.permission IS 'Permission type: VIEW_EVENT, EDIT_EVENT_DETAILS, MANAGE_COLLABORATORS, MANAGE_INVITES, MANAGE_SCHEDULE, MANAGE_BUDGET, MANAGE_TICKETS, MANAGE_CONTENT';
