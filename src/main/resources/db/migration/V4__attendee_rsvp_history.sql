-- Add RSVP history/audit trail table.
-- Postgres dialect.

CREATE TABLE attendee_rsvp_history (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT,
    event_id UUID NOT NULL REFERENCES events(id),
    attendee_id UUID NOT NULL REFERENCES attendees(id),
    changed_by UUID REFERENCES auth_users(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    note TEXT
);

CREATE INDEX idx_rsvp_history_event_id ON attendee_rsvp_history (event_id);
CREATE INDEX idx_rsvp_history_attendee_id ON attendee_rsvp_history (attendee_id);
CREATE INDEX idx_rsvp_history_source ON attendee_rsvp_history (source);
