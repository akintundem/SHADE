-- V12: Normalize user preferences from JSON to relational tables
-- Creates user_preferences, user_notification_preferences, user_privacy_settings tables

-- ============================================================================
-- USER PREFERENCES (from auth_users.preferences JSON)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value VARCHAR(500),
    description VARCHAR(255),

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id)
        REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_preferences UNIQUE (user_id, preference_key)
);

CREATE INDEX idx_user_preferences_user ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_key ON user_preferences(preference_key);
CREATE INDEX idx_user_preferences_deleted ON user_preferences(deleted_at);

COMMENT ON TABLE user_preferences IS 'Normalized user preferences (theme, language, timezone, etc.)';
COMMENT ON COLUMN user_preferences.preference_key IS 'Preference identifier (e.g., theme, language, timezone)';
COMMENT ON COLUMN user_preferences.preference_value IS 'Preference value as string';

-- ============================================================================
-- USER NOTIFICATION PREFERENCES (from user_settings.notification_preferences JSON)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    frequency VARCHAR(20),

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_user_notif_prefs_user FOREIGN KEY (user_id)
        REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_notif_prefs UNIQUE (user_id, notification_type, channel)
);

CREATE INDEX idx_user_notif_prefs_user ON user_notification_preferences(user_id);
CREATE INDEX idx_user_notif_prefs_type ON user_notification_preferences(notification_type);
CREATE INDEX idx_user_notif_prefs_deleted ON user_notification_preferences(deleted_at);

COMMENT ON TABLE user_notification_preferences IS 'Granular notification preferences per type and channel';
COMMENT ON COLUMN user_notification_preferences.notification_type IS 'Type of notification (EVENT_REMINDER, TICKET_SOLD, NEW_POST, etc.)';
COMMENT ON COLUMN user_notification_preferences.channel IS 'Communication channel (EMAIL, PUSH, SMS)';
COMMENT ON COLUMN user_notification_preferences.frequency IS 'Frequency setting (IMMEDIATE, DAILY_DIGEST, WEEKLY_DIGEST)';

-- ============================================================================
-- USER PRIVACY SETTINGS (from user_settings.privacy_settings JSON)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(100) NOT NULL,
    description VARCHAR(255),

    -- Audit columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_by_id UUID,
    updated_by_id UUID,

    -- Constraints
    CONSTRAINT fk_user_privacy_settings_user FOREIGN KEY (user_id)
        REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_privacy_settings UNIQUE (user_id, setting_key)
);

CREATE INDEX idx_user_privacy_settings_user ON user_privacy_settings(user_id);
CREATE INDEX idx_user_privacy_settings_key ON user_privacy_settings(setting_key);
CREATE INDEX idx_user_privacy_settings_deleted ON user_privacy_settings(deleted_at);

COMMENT ON TABLE user_privacy_settings IS 'User privacy settings as key-value pairs';
COMMENT ON COLUMN user_privacy_settings.setting_key IS 'Privacy setting identifier (profile_visibility, email_visible, etc.)';
COMMENT ON COLUMN user_privacy_settings.setting_value IS 'Setting value (PUBLIC, PRIVATE, FRIENDS_ONLY, etc.)';

-- Note: Data migration from JSON columns will be handled by application code
-- Old columns (auth_users.preferences, user_settings.notification_preferences, user_settings.privacy_settings)
-- will be deprecated in a future migration after verification
