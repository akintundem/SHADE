-- Migration V9: Feeds Social Enhancements
-- This migration adds:
-- 1. Repost/quote functionality to event posts
-- 2. User follow system
-- 3. Event subscriptions

-- =============================================================================
-- ENHANCE EVENT_POSTS TABLE FOR REPOST/QUOTE FUNCTIONALITY
-- =============================================================================

DO $$
BEGIN
    -- Add repost reference column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'event_posts' AND column_name = 'reposted_from_id') THEN
        ALTER TABLE event_posts ADD COLUMN reposted_from_id UUID;
        ALTER TABLE event_posts ADD CONSTRAINT fk_event_posts_reposted_from
            FOREIGN KEY (reposted_from_id) REFERENCES event_posts(id) ON DELETE SET NULL;
        CREATE INDEX idx_event_posts_reposted_from ON event_posts(reposted_from_id);
    END IF;

    -- Add quote text column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'event_posts' AND column_name = 'quote_text') THEN
        ALTER TABLE event_posts ADD COLUMN quote_text TEXT;
    END IF;

    -- Add repost count column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'event_posts' AND column_name = 'repost_count') THEN
        ALTER TABLE event_posts ADD COLUMN repost_count BIGINT DEFAULT 0;
        -- Update existing posts to have 0 repost count
        UPDATE event_posts SET repost_count = 0 WHERE repost_count IS NULL;
    END IF;
END $$;

-- =============================================================================
-- CREATE USER_FOLLOWS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_user_follows_follower_followee UNIQUE (follower_id, followee_id),
    CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follows_followee FOREIGN KEY (followee_id) REFERENCES auth_users(id) ON DELETE CASCADE
);

-- Create indexes for user follows
CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows(follower_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_follows_followee ON user_follows(followee_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_follows_status ON user_follows(status) WHERE deleted_at IS NULL;

-- =============================================================================
-- CREATE EVENT_SUBSCRIPTIONS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS event_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    subscription_type VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_event_subscriptions_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_event_subscriptions_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_subscriptions_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- Create indexes for event subscriptions
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_user ON event_subscriptions(user_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_event ON event_subscriptions(event_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_type ON event_subscriptions(subscription_type) WHERE deleted_at IS NULL;

-- =============================================================================
-- ADD COMMENTS
-- =============================================================================

COMMENT ON TABLE user_follows IS 'User follow relationships for social graph';
COMMENT ON COLUMN user_follows.follower_id IS 'User who is following';
COMMENT ON COLUMN user_follows.followee_id IS 'User being followed';
COMMENT ON COLUMN user_follows.status IS 'Follow status: ACTIVE, PENDING, or BLOCKED';

COMMENT ON TABLE event_subscriptions IS 'User subscriptions to events for timeline updates';
COMMENT ON COLUMN event_subscriptions.user_id IS 'User who subscribed';
COMMENT ON COLUMN event_subscriptions.event_id IS 'Event being subscribed to';
COMMENT ON COLUMN event_subscriptions.subscription_type IS 'Type: FOLLOW, NOTIFY, or BOTH';

COMMENT ON COLUMN event_posts.reposted_from_id IS 'Reference to original post if this is a repost';
COMMENT ON COLUMN event_posts.quote_text IS 'Additional text when quote-posting';
COMMENT ON COLUMN event_posts.repost_count IS 'Number of times this post has been reposted';
