-- Rename Cognito subject column to provider-agnostic auth_sub (Auth0/OIDC).
ALTER TABLE auth_users RENAME COLUMN cognito_sub TO auth_sub;

DROP INDEX IF EXISTS uk_auth_users_cognito_sub;
CREATE UNIQUE INDEX uk_auth_users_auth_sub ON auth_users (auth_sub) WHERE auth_sub IS NOT NULL;
