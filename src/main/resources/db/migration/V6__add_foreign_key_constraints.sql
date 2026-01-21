-- Migration V6: Add Foreign Key Constraints for Data Integrity
-- This migration adds database-level foreign key constraints to ensure referential integrity
-- between events, tickets, ticket types, attendees, and related entities.

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

-- Tickets indexes
CREATE INDEX IF NOT EXISTS idx_tickets_event_id ON tickets(event_id);
CREATE INDEX IF NOT EXISTS idx_tickets_ticket_type_id ON tickets(ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_tickets_attendee_id ON tickets(attendee_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_event_status ON tickets(event_id, status);

-- Attendees indexes
CREATE INDEX IF NOT EXISTS idx_attendees_event_id ON attendees(event_id);
CREATE INDEX IF NOT EXISTS idx_attendees_user_id ON attendees(user_id);
CREATE INDEX IF NOT EXISTS idx_attendees_email ON attendees(email);
CREATE INDEX IF NOT EXISTS idx_attendees_event_email ON attendees(event_id, email);
CREATE INDEX IF NOT EXISTS idx_attendees_event_user ON attendees(event_id, user_id);

-- Ticket types indexes
CREATE INDEX IF NOT EXISTS idx_ticket_types_event_id ON ticket_types(event_id);
CREATE INDEX IF NOT EXISTS idx_ticket_types_event_active ON ticket_types(event_id, is_active);

-- Ticket checkouts indexes
CREATE INDEX IF NOT EXISTS idx_ticket_checkouts_event_id ON ticket_checkouts(event_id);
CREATE INDEX IF NOT EXISTS idx_ticket_checkouts_status ON ticket_checkouts(status);
CREATE INDEX IF NOT EXISTS idx_ticket_checkouts_purchaser_id ON ticket_checkouts(purchaser_id);

-- Ticket checkout items indexes
CREATE INDEX IF NOT EXISTS idx_checkout_items_checkout_id ON ticket_checkout_items(checkout_id);
CREATE INDEX IF NOT EXISTS idx_checkout_items_ticket_type_id ON ticket_checkout_items(ticket_type_id);

-- Waitlist indexes
CREATE INDEX IF NOT EXISTS idx_waitlist_event_id ON event_waitlist_entries(event_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_user_id ON event_waitlist_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_status ON event_waitlist_entries(status);

-- Attendee invites indexes
CREATE INDEX IF NOT EXISTS idx_attendee_invites_event_id ON attendee_invites(event_id);
CREATE INDEX IF NOT EXISTS idx_attendee_invites_status ON attendee_invites(status);
CREATE INDEX IF NOT EXISTS idx_attendee_invites_token ON attendee_invites(token);

-- Price tiers indexes
CREATE INDEX IF NOT EXISTS idx_price_tiers_ticket_type_id ON ticket_price_tiers(ticket_type_id);

-- Ticket type dependencies indexes
CREATE INDEX IF NOT EXISTS idx_dependencies_ticket_type_id ON ticket_type_dependencies(ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_dependencies_required_ticket_type_id ON ticket_type_dependencies(required_ticket_type_id);
