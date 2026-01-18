-- Flyway migration: switch to Cognito-only auth and clean legacy tables.
-- Postgres dialect.

-- Add Cognito subject linkage and relax password hash requirement
ALTER TABLE auth_users ADD COLUMN IF NOT EXISTS cognito_sub VARCHAR(120);
ALTER TABLE auth_users ALTER COLUMN password_hash DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = 'uk_auth_users_cognito_sub'
    ) THEN
        CREATE UNIQUE INDEX uk_auth_users_cognito_sub ON auth_users (cognito_sub) WHERE cognito_sub IS NOT NULL;
    END IF;
END$$;

-- Drop legacy local-auth/session tables if they exist
DROP TABLE IF EXISTS auth_user_sessions;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS auth_password_reset_tokens;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS auth_email_verification_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
