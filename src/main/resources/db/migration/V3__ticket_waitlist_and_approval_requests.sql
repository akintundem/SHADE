-- Add ticket waitlist entries and approval requests.
-- Postgres dialect.

CREATE TABLE ticket_approval_requests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    event_id UUID NOT NULL REFERENCES events(id),
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    requester_user_id UUID REFERENCES auth_users(id),
    requester_email VARCHAR(180),
    requester_name VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    decided_by UUID REFERENCES auth_users(id),
    decided_at TIMESTAMP,
    decision_note TEXT
);

CREATE INDEX idx_ticket_approval_event_id ON ticket_approval_requests (event_id);
CREATE INDEX idx_ticket_approval_ticket_type_id ON ticket_approval_requests (ticket_type_id);
CREATE INDEX idx_ticket_approval_status ON ticket_approval_requests (status);
CREATE INDEX idx_ticket_approval_requester ON ticket_approval_requests (requester_user_id);
CREATE INDEX idx_ticket_approval_event_status ON ticket_approval_requests (event_id, status);

CREATE TABLE ticket_waitlist_entries (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    event_id UUID NOT NULL REFERENCES events(id),
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    requester_user_id UUID REFERENCES auth_users(id),
    requester_email VARCHAR(180),
    requester_name VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    fulfilled_by UUID REFERENCES auth_users(id),
    fulfilled_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX idx_ticket_waitlist_event_id ON ticket_waitlist_entries (event_id);
CREATE INDEX idx_ticket_waitlist_ticket_type_id ON ticket_waitlist_entries (ticket_type_id);
CREATE INDEX idx_ticket_waitlist_status ON ticket_waitlist_entries (status);
CREATE INDEX idx_ticket_waitlist_requester ON ticket_waitlist_entries (requester_user_id);
CREATE INDEX idx_ticket_waitlist_event_status ON ticket_waitlist_entries (event_id, status);
