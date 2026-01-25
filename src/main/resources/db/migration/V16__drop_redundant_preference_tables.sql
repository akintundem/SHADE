-- V16: Drop redundant user preference tables
-- UserSettings entity is the single source of truth for notification and privacy settings
-- user_preferences table is kept for generic key-value preferences (theme, timezone, etc.)

-- ============================================================================
-- DROP REDUNDANT NOTIFICATION PREFERENCES TABLE
-- These settings are already in user_settings as typed boolean columns:
-- email_notifications_enabled, push_notifications_enabled, sms_notifications_enabled,
-- event_invitations_enabled, event_updates_enabled, event_reminders_enabled, etc.
-- ============================================================================

DROP INDEX IF EXISTS idx_user_notif_prefs_deleted;
DROP INDEX IF EXISTS idx_user_notif_prefs_type;
DROP INDEX IF EXISTS idx_user_notif_prefs_user;
DROP TABLE IF EXISTS user_notification_preferences;

-- ============================================================================
-- DROP REDUNDANT PRIVACY SETTINGS TABLE
-- These settings are already in user_settings as typed enum columns:
-- profile_visibility, event_participation_visibility, search_visibility, etc.
-- ============================================================================

DROP INDEX IF EXISTS idx_user_privacy_settings_deleted;
DROP INDEX IF EXISTS idx_user_privacy_settings_key;
DROP INDEX IF EXISTS idx_user_privacy_settings_user;
DROP TABLE IF EXISTS user_privacy_settings;

-- Note: user_preferences table is KEPT for generic key-value preferences
-- (theme overrides, timezone, date_format, custom preferences, etc.)
