# Sade Event Planner - Database Schema & ER Diagram

## Table of Contents
1. [Entity Relationship Overview](#entity-relationship-overview)
2. [Core Entities](#core-entities)
3. [Feature-Specific Entities](#feature-specific-entities)
4. [Relationship Details](#relationship-details)
5. [Indexes & Performance](#indexes--performance)
6. [Database Migrations](#database-migrations)

---

## Entity Relationship Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        EVENT MANAGEMENT CORE                              │
└──────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │   auth_users        │
                    │ ─────────────────── │
                    │ id (PK)             │
                    │ name                │
                    │ email               │
                    │ cognito_sub         │
                    │ profile_picture_url │
                    └──────────┬──────────┘
                               │
       ┌───────────────────────┼───────────────────────┬──────────────┐
       │                       │                       │              │
       ▼                       ▼                       ▼              ▼
┌──────────────┐      ┌─────────────┐        ┌──────────────┐  ┌──────────┐
│  user_       │      │ event_      │        │ event_       │  │ event_   │
│  follows     │      │ subscriptions│       │ collaborators│  │ posts    │
└──────────────┘      └─────────────┘        └──────────────┘  └──────────┘
                               │                       │              │
                               │                       │              │
                               ▼                       ▼              │
                    ┌─────────────────────┐                          │
                    │      events         │◄─────────────────────────┘
                    │ ─────────────────── │
                    │ id (PK)             │
                    │ title               │
                    │ event_type          │◄───────────┐
                    │ event_status        │            │
                    │ start_datetime      │            │
                    │ end_datetime        │            │
                    │ location            │            │
                    │ created_by (FK)     │            │
                    │ is_series_master    │            │
                    │ series_id (FK)      │───────┐    │
                    └──────────┬──────────┘        │    │
                               │                   │    │
           ┌───────────────────┼───────────────────┼────┼──────────────┐
           │                   │                   │    │              │
           ▼                   ▼                   ▼    ▼              ▼
    ┌──────────┐     ┌──────────────┐    ┌─────────────────┐  ┌──────────────┐
    │attendees │     │ ticket_types │    │ event_series    │  │event_waitlist│
    └─────┬────┘     └──────┬───────┘    └─────────────────┘  │_entries      │
          │                 │                                   └──────────────┘
          │                 │
          ▼                 ▼
    ┌──────────┐     ┌──────────────┐
    │ tickets  │◄────│ tickets      │
    └──────────┘     └──────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        SOCIAL & ENGAGEMENT                                │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   event_posts       │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │───────► events.id
    │ created_by (FK)     │───────► auth_users.id
    │ post_type           │
    │ content             │
    │ media_object_id     │───────► event_stored_objects.id
    │ reposted_from_id(FK)│───┐
    │ quote_text          │   │
    │ repost_count        │   │
    └──────────┬──────────┘   │
               │              │
               │◄─────────────┘ (self-reference for reposts)
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌──────────┐    ┌──────────────┐
│post_likes│    │post_comments │
│──────────│    │──────────────│
│post_id(FK│    │post_id (FK)  │
│user_id(FK│    │user_id (FK)  │
└──────────┘    │content       │
                └──────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        TICKETING SYSTEM                                   │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   ticket_types      │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │───────► events.id
    │ name                │
    │ price               │
    │ capacity            │
    │ sold_count          │
    │ reserved_count      │
    └──────────┬──────────┘
               │
       ┌───────┴───────┬─────────────────┬─────────────────┐
       │               │                 │                 │
       ▼               ▼                 ▼                 ▼
┌──────────┐   ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
│ tickets  │   │ticket_price │  │ticket_waitlist│ │ticket_approval│
│          │   │_tiers       │  │_entries       │  │_requests     │
│id (PK)   │   └─────────────┘  └──────────────┘  └──────────────┘
│type_id(FK│
│attendee_id│◄──── attendees.id
│holder_name│
│qr_code    │
│status     │
└───────────┘
```

---

## Core Entities

### `auth_users` (User Accounts)
Central identity table for all authenticated users.

**Columns**:
- `id` (UUID, PK) - Unique user identifier
- `cognito_sub` (VARCHAR, UNIQUE) - AWS Cognito subject
- `name` (VARCHAR) - Display name
- `email` (VARCHAR, UNIQUE) - Email address
- `profile_picture_url` (TEXT) - Avatar URL
- `created_at`, `updated_at`, `deleted_at`, `version`

**Relationships**:
- Has many: Events (created_by), Collaborators, Attendees, Tickets, Posts, Comments, Likes, Follows
- Used everywhere as FK for user references

**Indexes**:
- `cognito_sub` (unique)
- `email` (unique)

---

### `events` (Events)
Core entity for event management.

**Columns**:
- `id` (UUID, PK)
- `title` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `event_type` (VARCHAR) - OPEN, RSVP_REQUIRED, INVITE_ONLY, TICKETED
- `event_status` (VARCHAR) - DRAFT, PUBLISHED, ONGOING, COMPLETED, CANCELLED
- `start_datetime` (TIMESTAMP, NOT NULL)
- `end_datetime` (TIMESTAMP)
- `location` (TEXT)
- `cover_image_url` (TEXT)
- `created_by` (UUID, FK → auth_users)
- `series_id` (UUID, FK → event_series)
- `is_series_master` (BOOLEAN, NOT NULL DEFAULT false)
- `is_series_exception` (BOOLEAN, NOT NULL DEFAULT false)
- `is_archived` (BOOLEAN DEFAULT false)
- `feeds_public_after_event` (BOOLEAN DEFAULT false)
- Standard BaseEntity columns

**Relationships**:
- Belongs to: User (creator), EventSeries
- Has many: Tickets, TicketTypes, Attendees, Posts, Collaborators, Waitlist, Subscriptions

**Indexes**:
- `created_by`, `series_id`, `event_type`, `event_status`
- `start_datetime`, `end_datetime`
- Composite: `(is_archived, event_status)` for listing active events

**Business Rules**:
- `event_type` determines access control
- `event_status` affects what operations are allowed
- Archived events hide feed posts by default

---

## Feature-Specific Entities

### TICKETING

#### `ticket_types`
**Purpose**: Templates for ticket categories (VIP, General Admission, etc.)

**Key Columns**:
- `id`, `event_id` (FK)
- `name`, `description`
- `price` (DECIMAL)
- `capacity` (INT)
- `sold_count`, `reserved_count` (INT) - Denormalized for performance
- `is_active` (BOOLEAN)
- `sale_start`, `sale_end` (TIMESTAMP)
- `requires_approval` (BOOLEAN)

**Relationships**:
- Belongs to: Event
- Has many: Tickets, PriceTiers, Dependencies, WaitlistEntries

**Constraints**:
- `CHECK (sold_count + reserved_count <= capacity)`
- Unique `(event_id, name)` for same event

---

#### `tickets`
**Purpose**: Individual ticket instances.

**Key Columns**:
- `id`, `event_id` (FK), `ticket_type_id` (FK)
- `attendee_id` (FK → attendees) - Nullable
- `holder_name`, `holder_email`
- `qr_code` (TEXT) - Base64 encoded QR
- `status` (VARCHAR) - RESERVED, ISSUED, CHECKED_IN, CANCELLED, EXPIRED
- `issued_at`, `checked_in_at`, `cancelled_at`

**Relationships**:
- Belongs to: Event, TicketType, Attendee (optional)

**Indexes**:
- `event_id`, `ticket_type_id`, `attendee_id`
- `qr_code` for fast validation
- Composite: `(event_id, status)` for filtering

---

#### `ticket_price_tiers`
**Purpose**: Time-based or quantity-based pricing.

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `name` (e.g., "Early Bird", "Regular")
- `price` (DECIMAL)
- `start_date`, `end_date` (TIMESTAMP)
- `quantity_from`, `quantity_to` (INT)

**Business Logic**:
- Active tier determined by current date or sold count
- Multiple tiers per ticket type for dynamic pricing

---

#### `ticket_checkouts`
**Purpose**: Shopping cart / checkout session.

**Key Columns**:
- `id`, `event_id` (FK), `user_id` (FK)
- `status` (VARCHAR) - PENDING, COMPLETED, EXPIRED, CANCELLED
- `total_amount` (DECIMAL)
- `expires_at` (TIMESTAMP)

**Relationships**:
- Has many: CheckoutItems
- One-to-many: Tickets (created after payment)

---

#### `ticket_waitlist_entries`
**Purpose**: Waitlist for sold-out ticket types.

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `user_id` (FK), `email`
- `status` (VARCHAR) - WAITING, PROMOTED, CANCELLED
- `promoted_at`, `promoted_by` (FK)

**Business Logic**:
- Auto-promote when tickets become available
- FIFO queue management

---

### ATTENDEES & RSVP

#### `attendees`
**Purpose**: Guest list for RSVP-based events.

**Key Columns**:
- `id`, `event_id` (FK)
- `user_id` (FK → auth_users) - Nullable for guest attendees
- `name`, `email`
- `rsvp_status` (VARCHAR) - GOING, NOT_GOING, MAYBE, NO_RESPONSE
- `plus_one_allowed` (BOOLEAN)
- `plus_one_name` (VARCHAR)
- `checked_in_at` (TIMESTAMP)

**Relationships**:
- Belongs to: Event, User (optional)
- May have: Ticket (for ticketed events)

**Indexes**:
- `event_id`, `user_id`, `rsvp_status`
- Composite: `(event_id, rsvp_status)` for guest list filtering

---

#### `attendee_invites`
**Purpose**: Invitations to RSVP.

**Key Columns**:
- `id`, `event_id` (FK)
- `email`, `invited_by` (FK)
- `token` (VARCHAR, UNIQUE) - Invite code
- `status` (VARCHAR) - PENDING, ACCEPTED, DECLINED, EXPIRED
- `expires_at` (TIMESTAMP)

**Business Logic**:
- Token-based RSVP acceptance
- Expired invites cannot be used

---

### FEEDS & SOCIAL

#### `event_posts`
**Purpose**: Twitter-like posts within events.

**Key Columns**:
- `id`, `event_id` (FK), `created_by` (FK)
- `post_type` (VARCHAR) - TEXT, IMAGE, VIDEO
- `content` (TEXT)
- `media_object_id` (UUID → event_stored_objects)
- `media_upload_status` (VARCHAR) - PENDING, COMPLETED
- `reposted_from_id` (UUID, FK → event_posts) - Self-reference
- `quote_text` (TEXT) - Comment on repost
- `repost_count` (BIGINT) - Denormalized

**Relationships**:
- Belongs to: Event, User (author), EventFeedPost (original post for reposts)
- Has many: Likes, Comments

**Indexes**:
- `event_id`, `created_by`, `media_upload_status`
- `reposted_from_id` for repost chains
- Composite: `(event_id, media_upload_status, created_at DESC)` for feeds

**Unique Features**:
- Repost/quote system (Twitter-like)
- Presigned S3 upload flow
- Immutable after creation (no edit)

---

#### `post_comments`
**Purpose**: Comments on posts.

**Key Columns**:
- `id`, `post_id` (FK → event_posts), `user_id` (FK)
- `content` (TEXT, NOT NULL)

**Indexes**:
- `post_id`, `user_id`
- Composite: `(post_id, created_at ASC)` for chronological display

**Business Logic**:
- Creator can edit/delete own comments
- Comment count denormalized in parent post response

---

#### `post_likes`
**Purpose**: Likes on posts.

**Key Columns**:
- `id`, `post_id` (FK), `user_id` (FK)

**Indexes**:
- Unique composite: `(post_id, user_id)` - One like per user per post
- Separate indexes on `post_id`, `user_id`

**Business Logic**:
- Like count denormalized in post response
- `isLiked` flag computed per user in real-time

---

### SOCIAL GRAPH

#### `user_follows`
**Purpose**: User-to-user follow relationships.

**Key Columns**:
- `id`, `follower_id` (FK → auth_users), `followee_id` (FK → auth_users)
- `status` (VARCHAR) - ACTIVE, PENDING, BLOCKED

**Indexes**:
- Unique composite: `(follower_id, followee_id)`
- Separate indexes on `follower_id`, `followee_id`
- Index on `status` for filtering active follows

**Business Logic**:
- ACTIVE: Follow relationship confirmed
- PENDING: For future private profile support
- BLOCKED: Follower blocked by followee
- Mutual follow detection via bidirectional query

---

#### `event_subscriptions`
**Purpose**: User subscriptions to events (without attending).

**Key Columns**:
- `id`, `user_id` (FK), `event_id` (FK)
- `subscription_type` (VARCHAR) - FOLLOW, NOTIFY, BOTH

**Indexes**:
- Unique composite: `(user_id, event_id)`
- Separate indexes on `user_id`, `event_id`

**Business Logic**:
- FOLLOW: See posts in timeline
- NOTIFY: Get push notifications
- BOTH: Timeline + notifications
- Independent of ticket/RSVP status

---

### COLLABORATION

#### `event_users`
**Purpose**: Event collaborators and staff memberships.

**Key Columns**:
- `id`, `event_id` (FK), `user_id` (FK)
- `user_type` (VARCHAR) - ORGANIZER, COORDINATOR, ADMIN, COLLABORATOR, STAFF, VOLUNTEER, etc.
- `registration_status`, `registration_date`
- `is_volunteer`, `volunteer_hours`

**Indexes**:
- Unique composite: `(event_id, user_id)`
- Index on `user_type` for filtering

---

#### `event_user_permissions`
**Purpose**: Optional granular overrides for collaborator permissions.

**Key Columns**:
- `id`, `event_user_id` (FK)
- `permission` (VARCHAR) - VIEW_EVENT, MANAGE_SCHEDULE, MANAGE_BUDGET, etc.

**Indexes**:
- Index on `event_user_id`
- Index on `permission`

---

#### `event_collaborator_invites`
**Purpose**: Pending/accepted collaborator invitations.

**Key Columns**:
- `id`, `event_id` (FK)
- `inviter_user_id` (FK), `invitee_user_id` (FK, nullable), `invitee_email` (nullable)
- `role` (VARCHAR) - collaborator role for acceptance
- `status` (VARCHAR), `token_hash`, `expires_at`, `responded_at`

---

### BUDGET & PLANNING

#### `budgets`
**Purpose**: Overall event budget.

**Key Columns**:
- `id`, `event_id` (FK, UNIQUE - one budget per event)
- `total_revenue`, `total_expenses` (DECIMAL) - Computed

**Relationships**:
- Has many: BudgetCategories → BudgetLineItems

---

#### `tasks`
**Purpose**: Event planning tasks.

**Key Columns**:
- `id`, `event_id` (FK)
- `title`, `description`
- `start_date`, `due_date`
- `priority`, `category`, `status`
- `progress_percentage`, `task_order`
- `assigned_to` (FK → auth_users)
- `is_draft`, `completed_subtasks_count`, `total_subtasks_count`

---

#### `checklists`
**Purpose**: Subtasks attached to tasks.

**Key Columns**:
- `id`, `task_id` (FK)
- `title`, `description`
- `due_date`, `status`
- `assigned_to` (FK → auth_users)
- `task_order`, `is_draft`

---

## Relationship Details

### Many-to-One Relationships

```
ticket_types   ─────► events       (Many ticket types per event)
tickets        ─────► ticket_types (Many tickets per type)
tickets        ─────► attendees    (Many tickets per attendee)
attendees      ─────► events       (Many attendees per event)
event_posts    ─────► events       (Many posts per event)
post_comments  ─────► event_posts  (Many comments per post)
post_likes     ─────► event_posts  (Many likes per post)
event_posts    ─────► event_posts  (Repost references original)
```

### One-to-One Relationships

```
events         ─────► budgets      (One budget per event)
```

### Many-to-Many (via join tables)

```
users ◄─── user_follows ───► users              (Follow relationships)
users ◄─── event_subscriptions ───► events      (Event subscriptions)
users ◄─── event_users ───► events              (Collaborators)
```

### Self-Referencing

```
event_posts.reposted_from_id ──► event_posts.id (Repost chain)
events.series_id ──► event_series.id             (Recurring events)
```

---

## Indexes & Performance

### Critical Indexes

```sql
-- Foreign keys (auto-indexed in most cases, explicit for clarity)
CREATE INDEX idx_tickets_event_id ON tickets(event_id);
CREATE INDEX idx_tickets_ticket_type_id ON tickets(ticket_type_id);
CREATE INDEX idx_attendees_event_id ON attendees(event_id);
CREATE INDEX idx_event_posts_event_id ON event_posts(event_id);

-- Composite indexes for common queries
CREATE INDEX idx_tickets_event_status ON tickets(event_id, status);
CREATE INDEX idx_attendees_event_rsvp ON attendees(event_id, rsvp_status);
CREATE INDEX idx_posts_event_completed ON event_posts(event_id, media_upload_status, created_at DESC);

-- Unique constraints (also act as indexes)
CREATE UNIQUE INDEX uk_user_follows_follower_followee ON user_follows(follower_id, followee_id);
CREATE UNIQUE INDEX uk_event_subscriptions_user_event ON event_subscriptions(user_id, event_id);
CREATE UNIQUE INDEX uk_post_likes_post_user ON post_likes(post_id, user_id);

-- Soft delete aware indexes
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_event_subscriptions_user ON event_subscriptions(user_id) WHERE deleted_at IS NULL;
```

### Denormalized Counts

To avoid expensive COUNT queries, several counts are denormalized:

- `ticket_types.sold_count`, `ticket_types.reserved_count`
- `event_posts.repost_count`
- `FeedPostResponse.likeCount`, `commentCount` (computed on read, not stored)

### Query Optimization Patterns

1. **Batch Loading**: Load likes/comments for multiple posts in single query
2. **Lazy Loading**: Relationships use `FetchType.LAZY` to avoid N+1
3. **Pagination**: All list endpoints use `Pageable` with configurable size
4. **Indexed Queries**: WHERE clauses match indexed columns

---

## Database Migrations

### Flyway Versions

**V1**: Cognito-only authentication cleanup
**V2**: Drop local auth columns
**V3**: Ticket waitlist and approval requests
**V4**: Attendee RSVP history
**V5**: Ticket type pricing templates
**V6**: Foreign key constraints
**V7**: Set defaults for series columns
**V8**: Fix series columns NOT NULL
**V9**: Feeds social enhancements (reposts, follows, subscriptions)

### Migration Strategy

- **Idempotent**: Use `IF NOT EXISTS`, `IF EXISTS` checks
- **Backward Compatible**: Add nullable columns first, then populate, then make NOT NULL
- **Data Preservation**: Soft deletes, never DROP tables with data
- **Conditional Updates**: Check column existence before altering

### Current Schema Version

After V9, the schema includes:
- 34 tables
- 150+ columns
- 50+ foreign key constraints
- 60+ indexes
- Full soft delete support across all entities

---

## Data Integrity Rules

### Cascading Deletes

```sql
-- When event is deleted, cascade to:
ticket_types, attendees, event_posts, collaborators, waitlist, subscriptions

-- When user is deleted, cascade to:
user_follows (both follower and followee), event_subscriptions

-- When ticket_type is deleted:
RESTRICT if tickets exist (protect sold tickets)
CASCADE waitlist entries

-- When event_post is deleted, cascade to:
post_likes, post_comments
```

### Check Constraints

```sql
-- Ticket capacity validation
CHECK (sold_count + reserved_count <= capacity)

-- Date validation
CHECK (end_datetime >= start_datetime)

-- Price validation
CHECK (price >= 0)
```

### Unique Constraints

```sql
-- One follow relationship per user pair
UNIQUE (follower_id, followee_id)

-- One subscription per user per event
UNIQUE (user_id, event_id)

-- One like per user per post
UNIQUE (post_id, user_id)

-- One collaborator role per user per event
UNIQUE (event_id, user_id)
```

---

## Example Queries

### Get Event with Full Details

```sql
SELECT e.*,
       (SELECT COUNT(*) FROM attendees WHERE event_id = e.id AND rsvp_status = 'GOING') AS going_count,
       (SELECT COUNT(*) FROM tickets WHERE event_id = e.id AND status = 'ISSUED') AS ticket_count,
       (SELECT COUNT(*) FROM event_posts WHERE event_id = e.id AND media_upload_status = 'COMPLETED') AS post_count
FROM events e
WHERE e.id = ?;
```

### Get User's Timeline (followed events + subscriptions)

```sql
SELECT p.*
FROM event_posts p
WHERE p.event_id IN (
    SELECT event_id FROM event_subscriptions WHERE user_id = ?
)
AND p.media_upload_status = 'COMPLETED'
ORDER BY p.created_at DESC
LIMIT 50;
```

### Get Post with Engagement

```sql
SELECT p.*,
       (SELECT COUNT(*) FROM post_likes WHERE post_id = p.id) AS like_count,
       (SELECT COUNT(*) FROM post_comments WHERE post_id = p.id) AS comment_count,
       EXISTS(SELECT 1 FROM post_likes WHERE post_id = p.id AND user_id = ?) AS is_liked
FROM event_posts p
WHERE p.id = ?;
```

### Get Available Ticket Types

```sql
SELECT tt.*,
       (tt.capacity - tt.sold_count - tt.reserved_count) AS available
FROM ticket_types tt
WHERE tt.event_id = ?
  AND tt.is_active = true
  AND (tt.sale_start IS NULL OR tt.sale_start <= NOW())
  AND (tt.sale_end IS NULL OR tt.sale_end >= NOW())
  AND (tt.capacity - tt.sold_count - tt.reserved_count) > 0
ORDER BY tt.price ASC;
```

---

## Conclusion

The Sade Event Planner database schema is designed for:
- **Flexibility**: Support multiple event types and workflows
- **Performance**: Denormalized counts, strategic indexes
- **Integrity**: Foreign keys, constraints, soft deletes
- **Scalability**: Paginated queries, lazy loading
- **Auditability**: Timestamps, version tracking, soft deletes

The schema evolves through Flyway migrations, ensuring zero-downtime deployments and data preservation across all changes.
