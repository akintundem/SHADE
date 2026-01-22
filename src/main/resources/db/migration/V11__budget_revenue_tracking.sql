-- V11: Budget revenue tracking and timeline integration
-- Adds revenue tracking fields and timeline task linkage

-- Add revenue tracking columns to budgets table
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS total_revenue NUMERIC(12, 2) DEFAULT 0;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS projected_revenue NUMERIC(12, 2) DEFAULT 0;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS net_position NUMERIC(12, 2) DEFAULT 0;

-- Add timeline task reference to budget line items
ALTER TABLE budget_line_items ADD COLUMN IF NOT EXISTS task_id UUID;

-- Add index for task_id lookups
CREATE INDEX IF NOT EXISTS idx_budget_line_items_task ON budget_line_items(task_id);

-- Add comments
COMMENT ON COLUMN budgets.total_revenue IS 'Total actual revenue from sold tickets';
COMMENT ON COLUMN budgets.projected_revenue IS 'Projected revenue if all tickets are sold';
COMMENT ON COLUMN budgets.net_position IS 'Net position: revenue minus expenses';
COMMENT ON COLUMN budget_line_items.task_id IS 'Optional reference to timeline task';
