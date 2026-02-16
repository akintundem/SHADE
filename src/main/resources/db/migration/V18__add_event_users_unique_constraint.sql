-- Prevent duplicate collaborator memberships for the same user on the same event.
-- Remove any existing duplicates first (keep the earliest record).
DELETE FROM event_users eu1
WHERE EXISTS (
    SELECT 1 FROM event_users eu2
    WHERE eu2.event_id = eu1.event_id
      AND eu2.user_id = eu1.user_id
      AND eu2.created_at < eu1.created_at
);

ALTER TABLE event_users
    ADD CONSTRAINT uq_event_users_event_user UNIQUE (event_id, user_id);
