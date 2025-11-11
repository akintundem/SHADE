-- Migration: Add owner_id column to budgets table
-- Date: 2025-11-11
-- Description: Add owner_id field to track budget ownership for proper RBAC enforcement

-- Add owner_id column to budgets table
ALTER TABLE budgets 
ADD COLUMN owner_id UUID;

-- For existing budgets, set owner_id to the event owner
-- This assumes budgets should be owned by the event owner
UPDATE budgets b
SET owner_id = e.owner_id
FROM events e
WHERE b.event_id = e.id
AND b.owner_id IS NULL;

-- Now make owner_id NOT NULL
ALTER TABLE budgets 
ALTER COLUMN owner_id SET NOT NULL;

-- Add index on owner_id for better query performance
CREATE INDEX idx_budgets_owner_id ON budgets(owner_id);

-- Add comment
COMMENT ON COLUMN budgets.owner_id IS 'User ID of the budget owner, used for ownership validation and RBAC';

