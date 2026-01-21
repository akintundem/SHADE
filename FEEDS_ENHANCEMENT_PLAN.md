# Feeds Enhancement Plan: Event Timeline & Social Features

## Vision

Transform event feeds into a **Twitter-like timeline** where each event becomes a central hub for conversations, experiences, and social interactions. Users can:

- **Post content**: Text, images, videos about their event experience
- **Engage**: Comment, like, repost, and quote-post
- **Follow**: Follow other users and events to build their social circle
- **Discover**: Get personalized event recommendations based on their network
- **Document**: Share their journey and memories from events

Think of a **major football match** - the event feed becomes the central place where fans discuss the game, share photos, react to moments, and connect with each other in real-time.

---

## Current State Analysis

### ✅ What's Already Built

#### 1. **Basic Feed Infrastructure**
- **Location**: `FeedPostService`, `EventFeedPost` entity
- **Features**:
  - Create posts (TEXT, IMAGE, VIDEO)
  - Presigned S3 uploads for media
  - List posts with pagination
  - Filter by post type
  - Cleanup incomplete uploads

#### 2. **Engagement Services**
- **Location**: `PostLikeService`, `PostCommentService`
- **Features**:
  - Like count tracking
  - Comment count tracking
  - User-specific "isLiked" status
  - Enrichment in `FeedPostService.enrichPostsWithEngagement()`

#### 3. **Access Control**
- **Location**: `EventAccessControlService`
- **Features**:
  - Access control based on event type (OPEN, RSVP_REQUIRED, INVITE_ONLY, TICKETED)
  - Post-event public feed access (`feedsPublicAfterEvent`)
  - Owner/collaborator permissions
  - Media upload/view permissions

#### 4. **Event Feed Aggregation**
- **Location**: `EventService.aggregateAllFeedPosts()`, `EventService.buildEventFeed()`
- **Features**:
  - Aggregate posts from multiple sources
  - Pagination support
  - Access control validation
  - Archive filtering

#### 5. **Media Handling**
- **Location**: `FeedPostService`, S3 integration
- **Features**:
  - Presigned upload URLs
  - Media upload completion callbacks
  - Presigned download URLs
  - Media status tracking (PENDING, COMPLETED)

---

## Missing Features for Full Timeline Experience

### 🔴 Critical Gaps

#### 1. **Follow/Social Graph**
- ❌ No user follow relationship model
- ❌ No "follow event" functionality
- ❌ No social context in feed responses
- ❌ Recommendations don't use social graph

**Impact**: Can't build "your circle" or personalized recommendations

#### 2. **Full Engagement Features**
- ⚠️ Like/Comment services exist but **no REST endpoints** exposed
- ❌ No repost/quote-post functionality (only basic repost helper)
- ❌ No threaded replies (parent-child relationships)
- ❌ No mention/notification system

**Impact**: Users can't actually interact with posts

#### 3. **Home/For-You Feeds**
- ⚠️ `EventService.getForYouFeed()` exists but is **placeholder**
- ❌ No home timeline combining:
  - Events you follow
  - Posts from users you follow
  - Recommended events based on social graph
- ❌ No cross-event feed aggregation

**Impact**: No personalized discovery experience

#### 4. **Post Features**
- ❌ No post visibility controls (public vs. attendees-only)
- ❌ No moderation/reporting system
- ❌ No rate limiting or spam controls
- ❌ No post editing (currently immutable)
- ❌ No post deletion by users (only by creators/admins)

**Impact**: Limited content control and safety

#### 5. **Notifications**
- ❌ No notifications for:
  - Likes on your posts
  - Comments on your posts
  - Reposts of your content
  - Mentions (@username)
  - New posts from followed users/events

**Impact**: Users miss engagement on their content

#### 6. **Media Enhancements**
- ⚠️ Basic upload works, but missing:
  - Multiple images per post (albums)
  - Video duration limits
  - File size validation
  - Media compression/optimization
  - Thumbnail generation

**Impact**: Limited media sharing capabilities

---

## Implementation Plan

### Phase 1: Core Engagement APIs (MVP Foundation)

**Goal**: Make existing like/comment services accessible via REST APIs

#### 1.1 Comment Endpoints
```
POST   /events/{eventId}/feed/posts/{postId}/comments
GET    /events/{eventId}/feed/posts/{postId}/comments
DELETE /events/{eventId}/feed/posts/{postId}/comments/{commentId}
PUT    /events/{eventId}/feed/posts/{postId}/comments/{commentId}
```

**Files to Create/Modify**:
- `FeedPostController` - Add comment endpoints
- `PostCommentService` - Expose existing methods via controller
- `CommentResponse` DTO - Response model

**Features**:
- Create comments with text
- List comments with pagination
- Edit/delete own comments
- Access control (respect event access type)

#### 1.2 Like Endpoints
```
POST   /events/{eventId}/feed/posts/{postId}/like
DELETE /events/{eventId}/feed/posts/{postId}/like
GET    /events/{eventId}/feed/posts/{postId}/likes
```

**Files to Create/Modify**:
- `FeedPostController` - Add like endpoints
- `PostLikeService` - Expose existing methods
- `LikeResponse` DTO - Response model

**Features**:
- Like/unlike posts
- List users who liked a post
- Access control

#### 1.3 Enhanced Repost
```
POST   /events/{eventId}/feed/posts/{postId}/repost
POST   /events/{eventId}/feed/posts/{postId}/quote
```

**Files to Create/Modify**:
- `FeedPostService` - Enhance repost method
- `EventFeedPost` entity - Add `repostedFromId`, `quoteText` fields
- `FeedPostController` - Add repost/quote endpoints

**Features**:
- Simple repost (shares original)
- Quote post (repost with comment)
- Track repost count
- Link to original post

---

### Phase 2: Follow Graph & Social Context

**Goal**: Build user follow relationships and event subscriptions

#### 2.1 User Follow Model
```sql
CREATE TABLE user_follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES auth_users(id),
    followee_id UUID NOT NULL REFERENCES auth_users(id),
    status VARCHAR(20) NOT NULL, -- PENDING, ACCEPTED, BLOCKED
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(follower_id, followee_id)
);
```

**Files to Create**:
- `UserFollow` entity
- `UserFollowRepository`
- `UserFollowService`
- `UserFollowController`

**Endpoints**:
```
POST   /users/{userId}/follow
DELETE /users/{userId}/follow
GET    /users/{userId}/followers
GET    /users/{userId}/following
GET    /users/{userId}/follow-status
```

**Features**:
- Follow/unfollow users
- List followers/following
- Check follow status
- Mutual follow detection

#### 2.2 Event Follow/Subscribe
```sql
CREATE TABLE event_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    subscription_type VARCHAR(20) NOT NULL, -- FOLLOW, NOTIFY, BOTH
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, event_id)
);
```

**Files to Create**:
- `EventSubscription` entity
- `EventSubscriptionRepository`
- `EventSubscriptionService`
- Add to `EventController`

**Endpoints**:
```
POST   /events/{eventId}/follow
DELETE /events/{eventId}/follow
GET    /events/{eventId}/followers
GET    /users/me/subscribed-events
```

**Features**:
- Follow events (even without ticket/RSVP)
- Get notifications for event updates
- List event followers
- List user's subscribed events

#### 2.3 Social Context in Feeds
**Modify**: `FeedPostResponse`, `FeedPostService.enrichPostsWithEngagement()`

**Add Fields**:
- `isFollowedByUser` - Is post author followed by current user
- `mutualFollowers` - Count of mutual followers with author
- `followersWhoLiked` - List of followers who liked this post
- `friendsWhoAttending` - Friends attending this event

---

### Phase 3: Home & For-You Feeds

**Goal**: Build personalized discovery feeds

#### 3.1 Home Timeline
**Modify**: `EventService.getForYouFeed()` → `getHomeFeed()`

**Algorithm**:
1. **Events you follow** (recent posts)
2. **Events you're attending** (your posts + others)
3. **Posts from users you follow** (across all accessible events)
4. **Recommended events** (based on social graph)

**Endpoints**:
```
GET /feeds/home?page=0&size=20
GET /feeds/for-you?page=0&size=20
GET /feeds/following?page=0&size=20
```

**Files to Create/Modify**:
- `FeedService` (new) - Centralized feed logic
- `EventService` - Enhance feed methods
- `FeedController` (new) - Feed endpoints

**Features**:
- Chronological timeline
- Personalized ranking
- Cross-event aggregation
- Access control per post

#### 3.2 Recommendation Engine
**Algorithm**:
1. **Social proximity**: Events attended by users you follow
2. **Interest matching**: Events similar to ones you've attended
3. **Location-based**: Events near you
4. **Popularity**: Trending events in your network

**Files to Create**:
- `EventRecommendationService`
- Recommendation algorithms

**Features**:
- Personalized event suggestions
- "Events in your circle" section
- Trending events
- Similar events

---

### Phase 4: Notifications & Real-time Updates

**Goal**: Keep users engaged with timely notifications

#### 4.1 Notification Types
- **Engagement**: Likes, comments, reposts on your posts
- **Mentions**: @username mentions in posts/comments
- **Social**: New followers, mutual connections
- **Event**: New posts from followed events, event updates
- **Recommendations**: Suggested events, friends attending

#### 4.2 Notification System
**Files to Create/Modify**:
- `FeedNotificationService`
- `NotificationController`
- Extend existing `NotificationService`

**Endpoints**:
```
GET    /notifications?type=feed&unread=true
POST   /notifications/{id}/read
POST   /notifications/read-all
GET    /notifications/unread-count
```

**Features**:
- Real-time push notifications
- Email digests
- Notification preferences
- Mark as read/unread

#### 4.3 Mention System
**Modify**: `EventFeedPost`, `PostComment` entities

**Add Fields**:
- `mentions` - Array of user IDs mentioned
- `mentionText` - Processed text with mentions

**Features**:
- @username parsing
- Mention notifications
- Link to user profiles

---

### Phase 5: Content Moderation & Safety

**Goal**: Keep feeds safe and high-quality

#### 5.1 Moderation Features
**Add to**: `EventFeedPost`, `PostComment` entities

**Fields**:
- `isHidden` - Hidden by moderator
- `isReported` - Has reports
- `reportCount` - Number of reports
- `moderationStatus` - PENDING, APPROVED, REJECTED

**Files to Create**:
- `ContentModerationService`
- `ReportService`
- `ModerationController`

**Endpoints**:
```
POST   /events/{eventId}/feed/posts/{postId}/report
GET    /admin/moderation/queue
POST   /admin/moderation/{postId}/approve
POST   /admin/moderation/{postId}/reject
```

**Features**:
- Report posts/comments
- Moderation queue for admins
- Auto-hide based on report threshold
- Spam detection

#### 5.2 Rate Limiting
**Add to**: `FeedPostService.create()`

**Features**:
- Posts per hour limit
- Comments per post limit
- Like rate limiting
- Prevent spam/abuse

#### 5.3 Content Controls
**Features**:
- Post visibility (public, attendees-only, followers-only)
- Comment threading (reply to comments)
- Edit/delete own posts
- Soft delete (hide but preserve)

---

### Phase 6: Media Enhancements

**Goal**: Rich media sharing experience

#### 6.1 Multiple Media Support
**Modify**: `EventFeedPost` entity

**Change**:
- `mediaObjectId` (single) → `mediaObjectIds` (array)
- Support multiple images per post
- Support image + video combinations

**Features**:
- Image albums (up to 10 images)
- Video + thumbnail
- Media carousel in UI

#### 6.2 Media Processing
**Files to Create**:
- `MediaProcessingService`
- Image/video processing pipeline

**Features**:
- Image compression
- Thumbnail generation
- Video transcoding
- Format validation

#### 6.3 Media Limits
**Add Validation**:
- Max file size (10MB images, 100MB videos)
- Max duration (60 seconds for videos)
- Allowed formats (JPEG, PNG, MP4, etc.)
- Max media per post (10 images, 1 video)

---

## Technical Design Considerations

### Database Schema Additions

```sql
-- User Follows
CREATE TABLE user_follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(follower_id, followee_id),
    FOREIGN KEY (follower_id) REFERENCES auth_users(id),
    FOREIGN KEY (followee_id) REFERENCES auth_users(id)
);

-- Event Subscriptions
CREATE TABLE event_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    subscription_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, event_id),
    FOREIGN KEY (user_id) REFERENCES auth_users(id),
    FOREIGN KEY (event_id) REFERENCES events(id)
);

-- Post Reports
CREATE TABLE post_reports (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(50),
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (post_id) REFERENCES event_posts(id),
    FOREIGN KEY (reporter_id) REFERENCES auth_users(id)
);

-- Enhance EventFeedPost
ALTER TABLE event_posts ADD COLUMN reposted_from_id UUID;
ALTER TABLE event_posts ADD COLUMN quote_text TEXT;
ALTER TABLE event_posts ADD COLUMN mentions UUID[];
ALTER TABLE event_posts ADD COLUMN is_hidden BOOLEAN DEFAULT FALSE;
ALTER TABLE event_posts ADD COLUMN report_count INTEGER DEFAULT 0;
ALTER TABLE event_posts ADD COLUMN moderation_status VARCHAR(20);
ALTER TABLE event_posts ADD COLUMN visibility VARCHAR(20) DEFAULT 'PUBLIC';
```

### API Design Patterns

#### Response Models
```java
// FeedPostResponse (enhanced)
{
    "id": "uuid",
    "eventId": "uuid",
    "type": "TEXT|IMAGE|VIDEO",
    "content": "string",
    "author": {
        "id": "uuid",
        "name": "string",
        "avatarUrl": "string",
        "isFollowedByUser": boolean
    },
    "engagement": {
        "likeCount": 0,
        "commentCount": 0,
        "repostCount": 0,
        "isLiked": boolean,
        "isReposted": boolean
    },
    "repost": {
        "originalPostId": "uuid",
        "originalAuthor": {...},
        "quoteText": "string"
    },
    "media": [...],
    "mentions": [...],
    "createdAt": "timestamp"
}
```

#### Pagination
- Use cursor-based pagination for feeds (better for real-time)
- Page-based for comments/likes (simpler for UI)

### Performance Considerations

1. **Caching Strategy**:
   - Cache feed posts (Redis)
   - Cache like/comment counts
   - Cache follow relationships
   - Invalidate on updates

2. **Database Indexing**:
   - Index on `user_follows(follower_id, followee_id)`
   - Index on `event_subscriptions(user_id, event_id)`
   - Index on `event_posts(event_id, created_at)`
   - Index on `post_likes(post_id, user_id)`

3. **Query Optimization**:
   - Batch load authors, likes, comments
   - Use JOINs for feed aggregation
   - Limit feed size (50 posts max per request)

### Security & Access Control

1. **Feed Access Rules**:
   - Respect event access type (TICKETED, RSVP_REQUIRED, etc.)
   - Check ticket/RSVP status
   - Honor `feedsPublicAfterEvent` setting
   - Block archived events

2. **Content Moderation**:
   - Auto-flag spam patterns
   - Report threshold (5 reports = auto-hide)
   - Admin review queue
   - User blocking

3. **Rate Limiting**:
   - 10 posts per hour per user
   - 50 comments per hour
   - 100 likes per hour
   - Prevent abuse

---

## Implementation Priority

### MVP (Phase 1-2)
1. ✅ Comment/Like REST APIs
2. ✅ Enhanced repost/quote
3. ✅ User follow system
4. ✅ Event subscriptions
5. ✅ Social context in feeds

### Post-MVP (Phase 3-4)
6. Home timeline
7. For-You recommendations
8. Notifications
9. Mentions

### Future (Phase 5-6)
10. Moderation system
11. Media enhancements
12. Advanced features

---

## Success Metrics

- **Engagement**: Average comments/likes per post
- **Retention**: Daily active users on feeds
- **Growth**: Follow relationships created
- **Quality**: Report rate, moderation actions
- **Performance**: Feed load time, API response time

---

## Next Steps

1. **Review this plan** with team
2. **Prioritize features** based on user needs
3. **Create detailed tickets** for Phase 1
4. **Design API contracts** (OpenAPI specs)
5. **Set up database migrations** for new tables
6. **Implement Phase 1** (Comment/Like APIs + Follow system)

---

## Questions to Consider

1. **Follow Model**: One-way (Twitter) or two-way (Facebook friends)?
2. **Event Follows**: Should non-attendees see all posts or just public ones?
3. **Moderation**: Auto-moderation or human review only?
4. **Notifications**: Real-time push or batched digests?
5. **Media Limits**: What file sizes/durations are acceptable?
6. **Post Editing**: Allow edits with edit history or immutable posts?

---

This plan transforms your event feeds into a **vibrant social timeline** where users can document experiences, connect with others, and discover events through their network. The phased approach ensures you can ship MVP features quickly while building toward the full vision.
