-- Remove legacy local-auth columns now that Cognito is the sole auth provider.
-- Postgres dialect.

ALTER TABLE auth_users
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS email_verified,
    DROP COLUMN IF EXISTS failed_login_attempts,
    DROP COLUMN IF EXISTS locked_until,
    DROP COLUMN IF EXISTS last_login_at;
