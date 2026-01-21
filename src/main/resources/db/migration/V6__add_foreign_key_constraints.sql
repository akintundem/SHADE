-- Migration V6: Add Foreign Key Constraints for Data Integrity
-- This migration adds database-level foreign key constraints to ensure referential integrity
-- between events, tickets, ticket types, attendees, and related entities.

-- =============================================================================
-- CREATE EVENT WAITLIST ENTRIES TABLE (if not exists)
-- =============================================================================
CREATE TABLE IF NOT EXISTS event_waitlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    requester_user_id UUID,
    requester_email VARCHAR(180),
    requester_name VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    promoted_by UUID,
    promoted_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- TICKETS -> EVENT
-- =============================================================================
-- Add foreign key constraint for tickets to events
-- ON DELETE CASCADE: When an event is deleted, all associated tickets are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ticket_event' AND table_name = 'tickets'
    ) THEN
        ALTER TABLE tickets
        ADD CONSTRAINT fk_ticket_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKETS -> TICKET TYPE
-- =============================================================================
-- Add foreign key constraint for tickets to ticket types
-- ON DELETE RESTRICT: Prevent deletion of ticket type if tickets exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ticket_ticket_type' AND table_name = 'tickets'
    ) THEN
        ALTER TABLE tickets
        ADD CONSTRAINT fk_ticket_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- =============================================================================
-- TICKETS -> ATTENDEE
-- =============================================================================
-- Add foreign key constraint for tickets to attendees
-- ON DELETE SET NULL: When attendee is deleted, ticket's attendee_id is set to null
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ticket_attendee' AND table_name = 'tickets'
    ) THEN
        ALTER TABLE tickets
        ADD CONSTRAINT fk_ticket_attendee
        FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE SET NULL;
    END IF;
END $$;

-- =============================================================================
-- ATTENDEES -> EVENT
-- =============================================================================
-- Add foreign key constraint for attendees to events
-- ON DELETE CASCADE: When event is deleted, all attendees are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_attendee_event' AND table_name = 'attendees'
    ) THEN
        ALTER TABLE attendees
        ADD CONSTRAINT fk_attendee_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET TYPES -> EVENT
-- =============================================================================
-- Add foreign key constraint for ticket types to events
-- ON DELETE CASCADE: When event is deleted, all ticket types are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ticket_type_event' AND table_name = 'ticket_types'
    ) THEN
        ALTER TABLE ticket_types
        ADD CONSTRAINT fk_ticket_type_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET CHECKOUTS -> EVENT
-- =============================================================================
-- Add foreign key constraint for ticket checkouts to events
-- ON DELETE CASCADE: When event is deleted, all checkouts are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_checkout_event' AND table_name = 'ticket_checkouts'
    ) THEN
        ALTER TABLE ticket_checkouts
        ADD CONSTRAINT fk_checkout_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET CHECKOUT ITEMS -> TICKET CHECKOUT
-- =============================================================================
-- Add foreign key constraint for checkout items to checkouts
-- ON DELETE CASCADE: When checkout is deleted, all items are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_checkout_item_checkout' AND table_name = 'ticket_checkout_items'
    ) THEN
        ALTER TABLE ticket_checkout_items
        ADD CONSTRAINT fk_checkout_item_checkout
        FOREIGN KEY (checkout_id) REFERENCES ticket_checkouts(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET CHECKOUT ITEMS -> TICKET TYPE
-- =============================================================================
-- Add foreign key constraint for checkout items to ticket types
-- ON DELETE RESTRICT: Prevent deletion of ticket type if checkout items exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_checkout_item_ticket_type' AND table_name = 'ticket_checkout_items'
    ) THEN
        ALTER TABLE ticket_checkout_items
        ADD CONSTRAINT fk_checkout_item_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- =============================================================================
-- EVENT WAITLIST ENTRIES -> EVENT
-- =============================================================================
-- Add foreign key constraint for waitlist entries to events
-- ON DELETE CASCADE: When event is deleted, all waitlist entries are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_waitlist_event' AND table_name = 'event_waitlist_entries'
    ) THEN
        ALTER TABLE event_waitlist_entries
        ADD CONSTRAINT fk_waitlist_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- ATTENDEE INVITES -> EVENT
-- =============================================================================
-- Add foreign key constraint for attendee invites to events
-- ON DELETE CASCADE: When event is deleted, all invites are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_attendee_invite_event' AND table_name = 'attendee_invites'
    ) THEN
        ALTER TABLE attendee_invites
        ADD CONSTRAINT fk_attendee_invite_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET PRICE TIERS -> TICKET TYPE
-- =============================================================================
-- Add foreign key constraint for price tiers to ticket types
-- ON DELETE CASCADE: When ticket type is deleted, all price tiers are deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_price_tier_ticket_type' AND table_name = 'ticket_price_tiers'
    ) THEN
        ALTER TABLE ticket_price_tiers
        ADD CONSTRAINT fk_price_tier_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- TICKET TYPE DEPENDENCIES -> TICKET TYPE
-- =============================================================================
-- Add foreign key constraints for ticket type dependencies
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_dependency_ticket_type' AND table_name = 'ticket_type_dependencies'
    ) THEN
        ALTER TABLE ticket_type_dependencies
        ADD CONSTRAINT fk_dependency_ticket_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_dependency_required_ticket_type' AND table_name = 'ticket_type_dependencies'
    ) THEN
        ALTER TABLE ticket_type_dependencies
        ADD CONSTRAINT fk_dependency_required_ticket_type
        FOREIGN KEY (required_ticket_type_id) REFERENCES ticket_types(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- CREATE INDEXES FOR FOREIGN KEYS (improves query performance)
-- =============================================================================
-- Note: Index creation commented out as Hibernate will create appropriate indexes
-- when it creates the tables with ddl-auto=update. This migration focuses only
-- on adding foreign key constraints to ensure referential integrity.

-- Indexes will be auto-created by Hibernate based on @Index annotations in entities
