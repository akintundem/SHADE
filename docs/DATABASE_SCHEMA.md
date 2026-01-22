# Sade Event Planner - Database Schema & ER Diagram

## Table of Contents
1. [Entity Relationship Overview](#entity-relationship-overview)
2. [Core Entities](#core-entities)
3. [Feature-Specific Entities](#feature-specific-entities)
4. [Relationship Details](#relationship-details)
5. [Indexes & Performance](#indexes--performance)
6. [Database Migrations](#database-migrations)
7. [Data Integrity Rules](#data-integrity-rules)

---

## Entity Relationship Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        AUTHENTICATION & USERS                             │
└──────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │   auth_users        │
                    │ ─────────────────── │
                    │ id (PK)             │
                    │ email (UNIQUE)      │
                    │ username (UNIQUE)   │
                    │ cognito_sub (UNIQUE)│
                    │ name                │
                    │ profile_picture_url │
                    │ user_type           │
                    │ status              │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   user_settings     │
                    │ ─────────────────── │
                    │ id (PK)             │
                    │ user_id (FK)        │
                    │ location_id (FK)    │
                    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        EVENT MANAGEMENT CORE                              │
└──────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │      events         │
                    │ ─────────────────── │
                    │ id (PK)             │
                    │ name                │
                    │ owner_id (FK)       │──────► auth_users.id
                    │ event_type          │
                    │ event_status        │
                    │ access_type         │
                    │ start_date_time     │
                    │ end_date_time       │
                    │ parent_series_id(FK)│──────► event_series.id
                    │ is_series_master    │
                    │ is_series_exception │
                    │ timeline_published  │
                    │ is_archived         │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┬──────────────┐
        │                      │                       │              │
        ▼                      ▼                       ▼              ▼
┌──────────────┐      ┌─────────────┐        ┌──────────────┐  ┌──────────┐
│ event_series │      │ event_roles│        │ event_stored │  │event_waitlist│
│              │      │            │        │ _objects     │  │_entries   │
└──────────────┘      └─────────────┘        └──────────────┘  └──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ event_notification  │
                    │ _settings           │
                    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        TICKETING SYSTEM                                   │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   ticket_types      │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)      │──────► events.id
    │ name                │
    │ price_minor         │
    │ currency            │
    │ quantity_available │
    │ quantity_sold       │
    │ quantity_reserved   │
    │ is_active           │
    │ sale_start_date     │
    │ sale_end_date       │
    └──────────┬──────────┘
               │
       ┌───────┴───────┬─────────────────┬─────────────────┬──────────────┐
       │               │                 │                 │              │
       ▼               ▼                 ▼                 ▼              ▼
┌──────────┐   ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────┐
│ tickets  │   │ticket_price │  │ticket_waitlist│  │ticket_approval│  │ticket_type│
│          │   │_tiers       │  │_entries       │  │_requests     │  │_templates│
│id (PK)   │   └─────────────┘  └──────────────┘  └──────────────┘  └──────────┘
│event_id  │
│type_id(FK│
│attendee_id│◄──── attendees.id
│ticket_number│
│qr_code_data│
│status     │
└───────────┘
       │
       ▼
┌─────────────────────┐
│ ticket_checkouts     │
│ ─────────────────── │
│ id (PK)             │
│ event_id (FK)       │
│ user_id (FK)        │
│ status              │
│ total_amount        │
│ expires_at          │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ ticket_checkout_items│
│ ─────────────────── │
│ id (PK)             │
│ checkout_id (FK)    │
│ ticket_type_id (FK) │
│ quantity            │
└─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        ATTENDEES & RSVP                                   │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   attendees         │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │──────► events.id
    │ user_id (FK)        │──────► auth_users.id (nullable)
    │ name                │
    │ email               │
    │ rsvp_status         │
    │ checked_in_at       │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ attendee_invites    │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │
    │ email               │
    │ token (UNIQUE)      │
    │ status              │
    │ expires_at          │
    └─────────────────────┘
               │
               ▼
    ┌─────────────────────┐
    │ attendee_rsvp_history│
    │ ─────────────────── │
    │ id (PK)             │
    │ attendee_id (FK)    │
    │ old_status          │
    │ new_status          │
    │ changed_by (FK)     │
    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        SOCIAL & ENGAGEMENT                                │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   event_posts        │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │──────► events.id
    │ created_by (FK)     │──────► auth_users.id
    │ post_type           │
    │ content             │
    │ media_object_id     │──────► event_stored_objects.id
    │ media_upload_status │
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
│UNIQUE    │    │content       │
└──────────┘    └──────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        SOCIAL GRAPH                                       │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   user_follows       │
    │ ─────────────────── │
    │ id (PK)             │
    │ follower_id (FK)    │──────► auth_users.id
    │ followee_id (FK)    │──────► auth_users.id
    │ status              │
    │ UNIQUE(follower,    │
    │        followee)    │
    └─────────────────────┘

    ┌─────────────────────┐
    │ event_subscriptions │
    │ ─────────────────── │
    │ id (PK)             │
    │ user_id (FK)        │──────► auth_users.id
    │ event_id (FK)       │──────► events.id
    │ subscription_type   │
    │ UNIQUE(user, event) │
    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        COLLABORATION                                      │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   event_users        │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │──────► events.id
    │ user_id (FK)        │──────► auth_users.id
    │ user_type           │
    │ registration_status │
    │ is_volunteer        │
    │ volunteer_hours     │
    │ UNIQUE(event, user) │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ event_user_permissions│
    │ ─────────────────── │
    │ id (PK)             │
    │ event_user_id (FK)  │
    │ permission          │
    └─────────────────────┘

    ┌─────────────────────┐
    │ event_collaborator  │
    │ _invites            │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │
    │ inviter_user_id (FK)│
    │ invitee_user_id (FK)│
    │ invitee_email       │
    │ role                │
    │ status              │
    │ token_hash          │
    │ expires_at          │
    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        BUDGET & PLANNING                                  │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   budgets           │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │──────► events.id (UNIQUE)
    │ owner_id (FK)       │──────► auth_users.id
    │ total_budget        │
    │ currency            │
    │ total_revenue       │
    │ projected_revenue   │
    │ net_position        │
    │ total_estimated     │
    │ total_actual        │
    │ variance            │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ budget_categories   │
    │ ─────────────────── │
    │ id (PK)             │
    │ budget_id (FK)      │
    │ name                │
    │ allocated_amount    │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ budget_line_items   │
    │ ─────────────────── │
    │ id (PK)             │
    │ budget_id (FK)      │
    │ category_id (FK)    │
    │ task_id (FK)        │──────► tasks.id (nullable)
    │ description         │
    │ amount              │
    │ item_type           │
    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        TIMELINE & TASKS                                   │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   tasks             │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │──────► events.id
    │ assigned_to (FK)    │──────► auth_users.id (nullable)
    │ title               │
    │ description         │
    │ start_date          │
    │ due_date            │
    │ priority            │
    │ category            │
    │ status              │
    │ progress_percentage │
    │ task_order          │
    │ is_draft            │
    │ completed_subtasks  │
    │ total_subtasks      │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │   checklists        │
    │ ─────────────────── │
    │ id (PK)             │
    │ task_id (FK)        │
    │ title               │
    │ description         │
    │ due_date            │
    │ status              │
    │ assigned_to (FK)    │
    │ task_order          │
    │ is_draft            │
    └─────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        NOTIFICATIONS                                      │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────┐
    │   communications     │
    │ ─────────────────── │
    │ id (PK)             │
    │ event_id (FK)       │
    │ communication_type  │
    │ status              │
    │ recipient_type      │
    │ recipient_id        │
    │ subject             │
    │ content             │
    └─────────────────────┘

    ┌─────────────────────┐
    │   device_tokens     │
    │ ─────────────────── │
    │ id (PK)             │
    │ user_id (FK)        │──────► auth_users.id
    │ token               │
    │ platform            │
    │ device_id           │
    └─────────────────────┘
```

---

## Core Entities

### `auth_users` (User Accounts)
Central identity table for all authenticated users.

**Columns**:
- `id` (UUID, PK) - Unique user identifier
- `cognito_sub` (VARCHAR(120), UNIQUE) - AWS Cognito subject
- `email` (VARCHAR(180), UNIQUE, NOT NULL) - Email address
- `username` (VARCHAR(40), UNIQUE) - Public handle/username
- `name` (VARCHAR(120), NOT NULL) - Display name
- `phone_number` (VARCHAR(40)) - Phone number
- `date_of_birth` (DATE) - Date of birth
- `user_type` (VARCHAR(30), NOT NULL) - INDIVIDUAL, ORGANIZATION, etc.
- `status` (VARCHAR) - ACTIVE, INACTIVE, SUSPENDED
- `profile_picture_url` (VARCHAR(500)) - Avatar URL
- `profile_completed` (BOOLEAN, NOT NULL) - Profile completion status
- `accept_terms` (BOOLEAN, NOT NULL) - Terms acceptance
- `accept_privacy` (BOOLEAN, NOT NULL) - Privacy policy acceptance
- `marketing_opt_in` (BOOLEAN, NOT NULL) - Marketing consent
- `preferences` (TEXT) - JSON preferences
- Standard BaseEntity columns: `created_at`, `updated_at`, `deleted_at`, `version`

**Relationships**:
- Has one: UserSettings
- Has many: Events (owner), Collaborators, Attendees, Tickets, Posts, Comments, Likes, Follows

**Indexes**:
- `cognito_sub` (unique)
- `email` (unique)
- `username` (unique)
- `name` (for search)

---

### `events` (Events)
Core entity for event management.

**Columns**:
- `id` (UUID, PK)
- `name` (VARCHAR, NOT NULL) - Event name
- `owner_id` (UUID, FK → auth_users, NOT NULL) - Event owner
- `description` (TEXT) - Event description
- `event_type` (VARCHAR) - Event classification
- `event_status` (VARCHAR) - PLANNING, PUBLISHED, ONGOING, COMPLETED, CANCELLED
- `access_type` (VARCHAR, NOT NULL) - OPEN, RSVP_REQUIRED, INVITE_ONLY, TICKETED
- `start_date_time` (TIMESTAMP) - Event start
- `end_date_time` (TIMESTAMP) - Event end
- `registration_deadline` (TIMESTAMP) - Registration deadline
- `capacity` (INT) - Maximum attendees
- `current_attendee_count` (INT, DEFAULT 0) - Current count
- `is_public` (BOOLEAN, DEFAULT true) - Public visibility
- `requires_approval` (BOOLEAN, DEFAULT false) - Approval required
- `cover_image_url` (TEXT) - Cover image
- `event_website_url` (TEXT) - External website
- `hashtag` (VARCHAR) - Event hashtag
- `theme`, `objectives`, `target_audience`, `success_metrics` (TEXT) - Planning fields
- `branding_guidelines`, `venue_requirements`, `technical_requirements` (TEXT) - Planning fields
- `accessibility_features`, `emergency_plan`, `backup_plan` (TEXT) - Planning fields
- `post_event_tasks`, `metadata` (TEXT) - Additional fields
- `venue_id` (UUID) - Venue reference
- `venue` (JSON/Embedded) - Venue details
- `platform_payment_id` (UUID) - Payment tracking
- `creation_fee_paid` (BOOLEAN, DEFAULT false) - Fee payment status
- `creation_fee_amount` (DECIMAL(10,2)) - Fee amount
- `payment_date` (TIMESTAMP) - Payment timestamp
- `feeds_public_after_event` (BOOLEAN, NOT NULL, DEFAULT false) - Feed visibility after event
- `timeline_published` (BOOLEAN, NOT NULL, DEFAULT false) - Timeline publication status
- `timeline_published_at` (TIMESTAMP) - Publication timestamp
- `timeline_published_by` (UUID, FK → auth_users) - Publisher
- `timeline_publish_message` (TEXT) - Publication message
- `is_archived` (BOOLEAN, NOT NULL, DEFAULT false) - Archive status
- `archived_at` (TIMESTAMP) - Archive timestamp
- `archived_by` (UUID, FK → auth_users) - User who archived
- `archive_reason` (TEXT) - Archive reason
- `restored_at` (TIMESTAMP) - Restore timestamp
- `restored_by` (UUID, FK → auth_users) - User who restored
- `parent_series_id` (UUID, FK → event_series) - Series parent
- `series_occurrence_number` (INT) - Occurrence number
- `is_series_master` (BOOLEAN, NOT NULL, DEFAULT false) - Master event flag
- `is_series_exception` (BOOLEAN, NOT NULL, DEFAULT false) - Series exception flag
- `original_start_date_time` (TIMESTAMP) - Original scheduled date
- Standard BaseEntity columns

**Relationships**:
- Belongs to: UserAccount (owner), EventSeries (parent)
- Has many: Tickets, TicketTypes, Attendees, Posts, Collaborators, Waitlist, Subscriptions, Budget, Tasks

**Indexes**:
- `owner_id`, `parent_series_id`, `event_type`, `event_status`
- `start_date_time`, `end_date_time`
- Composite: `(is_archived, event_status)` for listing active events

**Business Rules**:
- `access_type` determines access control
- `event_status` affects what operations are allowed
- Archived events hide feed posts by default
- One budget per event (one-to-one)

---

## Feature-Specific Entities

### TICKETING

#### `ticket_types`
**Purpose**: Templates for ticket categories (VIP, General Admission, etc.)

**Key Columns**:
- `id`, `event_id` (FK)
- `name` (VARCHAR(100), NOT NULL)
- `category` (VARCHAR(50)) - VIP, GENERAL_ADMISSION, etc.
- `description` (TEXT)
- `price_minor` (BIGINT) - Price in minor units (cents)
- `currency` (VARCHAR(3), NOT NULL, DEFAULT 'USD')
- `quantity_available` (INT, NOT NULL, DEFAULT 0)
- `quantity_sold` (INT, NOT NULL, DEFAULT 0) - Denormalized
- `quantity_reserved` (INT, NOT NULL, DEFAULT 0) - Denormalized
- `is_active` (BOOLEAN, NOT NULL, DEFAULT true)
- `sale_start_date`, `sale_end_date` (TIMESTAMP)
- `requires_approval` (BOOLEAN, DEFAULT false)
- `max_per_order` (INT) - Maximum tickets per order
- `transferable` (BOOLEAN, DEFAULT true)
- Standard BaseEntity columns

**Relationships**:
- Belongs to: Event
- Has many: Tickets, PriceTiers, Dependencies, WaitlistEntries, Templates

**Constraints**:
- `CHECK (quantity_sold + quantity_reserved <= quantity_available)`
- Unique `(event_id, name)` for same event

**Indexes**:
- `event_id`, `category`
- Composite: `(event_id, is_active)`
- `(sale_start_date, sale_end_date)`

---

#### `tickets`
**Purpose**: Individual ticket instances.

**Key Columns**:
- `id`, `event_id` (FK), `ticket_type_id` (FK)
- `ticket_number` (VARCHAR(50), UNIQUE, NOT NULL) - Unique ticket identifier
- `attendee_id` (FK → attendees) - Nullable
- `owner_email`, `owner_name` (VARCHAR) - For email-only tickets
- `qr_code_data` (TEXT, NOT NULL) - QR code data string
- `status` (VARCHAR(20), NOT NULL) - PENDING, ISSUED, VALIDATED, CANCELLED, REFUNDED, EXPIRED
- `pending_at`, `issued_at`, `validated_at`, `cancelled_at` (TIMESTAMP)
- `payment_id` (UUID) - Payment gateway reference
- `issued_by` (UUID, FK → auth_users) - User who issued
- `validated_by` (UUID, FK → auth_users) - User who validated
- `checkout_id` (UUID, FK → ticket_checkouts) - Checkout session
- `cancellation_reason` (TEXT)
- `metadata` (TEXT) - JSON metadata
- Standard BaseEntity columns

**Relationships**:
- Belongs to: Event, TicketType, Attendee (optional), UserAccount (issued_by, validated_by), TicketCheckout

**Indexes**:
- `ticket_number` (unique)
- `event_id`, `ticket_type_id`, `attendee_id`
- `status`
- Composite: `(event_id, status)` for filtering

**Business Rules**:
- Either `attendee_id` or `owner_email`/`owner_name` must be provided
- PENDING tickets expire after 15 minutes
- Status transitions: PENDING → ISSUED → VALIDATED
- QR code format: `ticket:{ticketId}:{ticketNumber}:{eventId}:{hash}`

---

#### `ticket_price_tiers`
**Purpose**: Time-based or quantity-based pricing.

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `name` (VARCHAR) - e.g., "Early Bird", "Regular"
- `price_minor` (BIGINT) - Price in minor units
- `currency` (VARCHAR(3))
- `start_date`, `end_date` (TIMESTAMP) - Active period
- `quantity_from`, `quantity_to` (INT) - Quantity range
- `priority` (INT) - Tier priority
- Standard BaseEntity columns

**Business Logic**:
- Active tier determined by current date or sold count
- Multiple tiers per ticket type for dynamic pricing
- Priority determines which tier applies when multiple match

---

#### `ticket_checkouts`
**Purpose**: Shopping cart / checkout session.

**Key Columns**:
- `id`, `event_id` (FK), `user_id` (FK → auth_users)
- `status` (VARCHAR) - PENDING, COMPLETED, EXPIRED, CANCELLED
- `total_amount` (DECIMAL) - Total checkout amount
- `currency` (VARCHAR(3))
- `expires_at` (TIMESTAMP) - Session expiration
- `payment_intent_id` (VARCHAR) - Stripe payment intent
- `metadata` (TEXT) - JSON metadata
- Standard BaseEntity columns

**Relationships**:
- Has many: CheckoutItems, Tickets (created after payment)

**Indexes**:
- `event_id`, `user_id`, `status`
- `expires_at` for cleanup

---

#### `ticket_checkout_items`
**Purpose**: Items in checkout cart.

**Key Columns**:
- `id`, `checkout_id` (FK), `ticket_type_id` (FK)
- `quantity` (INT, NOT NULL)
- `unit_price_minor` (BIGINT) - Price at time of checkout
- `total_price_minor` (BIGINT) - Total for this item
- Standard BaseEntity columns

---

#### `ticket_waitlist_entries`
**Purpose**: Waitlist for sold-out ticket types.

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `user_id` (FK → auth_users), `email` (VARCHAR)
- `status` (VARCHAR) - WAITING, PROMOTED, CANCELLED
- `promoted_at` (TIMESTAMP), `promoted_by` (FK → auth_users)
- Standard BaseEntity columns

**Business Logic**:
- Auto-promote when tickets become available
- FIFO queue management

---

#### `ticket_approval_requests`
**Purpose**: Approval workflow for restricted tickets.

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `user_id` (FK → auth_users), `email` (VARCHAR)
- `status` (VARCHAR) - PENDING, APPROVED, REJECTED
- `requested_at` (TIMESTAMP)
- `reviewed_at` (TIMESTAMP), `reviewed_by` (FK → auth_users)
- `review_notes` (TEXT)
- Standard BaseEntity columns

---

#### `ticket_type_templates`
**Purpose**: Reusable ticket type configurations.

**Key Columns**:
- `id`, `created_by` (FK → auth_users)
- `name` (VARCHAR, NOT NULL)
- `template_data` (TEXT) - JSON template configuration
- Standard BaseEntity columns

---

#### `ticket_type_dependencies`
**Purpose**: Ticket type prerequisites (e.g., must buy VIP to buy add-on).

**Key Columns**:
- `id`, `ticket_type_id` (FK)
- `required_ticket_type_id` (FK → ticket_types)
- `min_quantity` (INT, DEFAULT 1)
- Standard BaseEntity columns

---

#### `ticket_promotions`
**Purpose**: Promotional campaigns for tickets.

**Key Columns**:
- `id`, `event_id` (FK), `ticket_type_id` (FK)
- `promotion_code` (VARCHAR, UNIQUE)
- `discount_type` (VARCHAR) - PERCENTAGE, FIXED_AMOUNT
- `discount_value` (DECIMAL)
- `start_date`, `end_date` (TIMESTAMP)
- `max_uses` (INT) - Maximum redemptions
- `used_count` (INT, DEFAULT 0)
- `is_active` (BOOLEAN, DEFAULT true)
- Standard BaseEntity columns

**Indexes**:
- `promotion_code` (unique)
- `event_id`, `ticket_type_id`
- `(start_date, end_date)`

---

### ATTENDEES & RSVP

#### `attendees`
**Purpose**: Guest list for RSVP-based events.

**Key Columns**:
- `id`, `event_id` (FK)
- `user_id` (FK → auth_users) - Nullable for guest attendees
- `name` (VARCHAR, NOT NULL)
- `email` (VARCHAR)
- `rsvp_status` (VARCHAR) - PENDING, GOING, NOT_GOING, MAYBE, NO_RESPONSE
- `checked_in_at` (TIMESTAMP)
- `participation_visibility` (VARCHAR(30), NOT NULL, DEFAULT 'PUBLIC')
- `created_at`, `updated_at` (TIMESTAMP)

**Relationships**:
- Belongs to: Event, UserAccount (optional)
- May have: Ticket (for ticketed events)

**Indexes**:
- `event_id`, `user_id`, `email`
- `rsvp_status`
- Composite: `(event_id, rsvp_status)` for guest list filtering

---

#### `attendee_invites`
**Purpose**: Invitations to RSVP.

**Key Columns**:
- `id`, `event_id` (FK)
- `email` (VARCHAR, NOT NULL)
- `invited_by` (FK → auth_users)
- `token` (VARCHAR, UNIQUE) - Invite code
- `status` (VARCHAR) - PENDING, ACCEPTED, DECLINED, EXPIRED
- `expires_at` (TIMESTAMP)
- `accepted_at` (TIMESTAMP)
- Standard BaseEntity columns

**Business Logic**:
- Token-based RSVP acceptance
- Expired invites cannot be used

---

#### `attendee_rsvp_history`
**Purpose**: Audit trail of RSVP changes.

**Key Columns**:
- `id`, `attendee_id` (FK)
- `old_status` (VARCHAR)
- `new_status` (VARCHAR)
- `changed_by` (FK → auth_users)
- `changed_at` (TIMESTAMP)
- `notes` (TEXT)
- Standard BaseEntity columns

---

### FEEDS & SOCIAL

#### `event_posts`
**Purpose**: Twitter-like posts within events.

**Key Columns**:
- `id`, `event_id` (FK), `created_by` (FK → auth_users)
- `post_type` (VARCHAR) - TEXT, IMAGE, VIDEO
- `content` (TEXT)
- `media_object_id` (UUID → event_stored_objects)
- `media_upload_status` (VARCHAR) - PENDING, COMPLETED
- `reposted_from_id` (UUID, FK → event_posts) - Self-reference
- `quote_text` (TEXT) - Comment on repost
- `repost_count` (BIGINT, DEFAULT 0) - Denormalized
- Standard BaseEntity columns

**Relationships**:
- Belongs to: Event, UserAccount (author), EventFeedPost (original post for reposts)
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
- `id`, `post_id` (FK → event_posts), `user_id` (FK → auth_users)
- `content` (TEXT, NOT NULL)
- Standard BaseEntity columns

**Indexes**:
- `post_id`, `user_id`
- Composite: `(post_id, created_at ASC)` for chronological display

**Business Logic**:
- Creator can edit/delete own comments
- Comment count computed on read (not stored)

---

#### `post_likes`
**Purpose**: Likes on posts.

**Key Columns**:
- `id`, `post_id` (FK), `user_id` (FK)
- Standard BaseEntity columns

**Indexes**:
- Unique composite: `(post_id, user_id)` - One like per user per post
- Separate indexes on `post_id`, `user_id`

**Business Logic**:
- Like count computed on read (not stored)
- `isLiked` flag computed per user in real-time

---

### SOCIAL GRAPH

#### `user_follows`
**Purpose**: User-to-user follow relationships.

**Key Columns**:
- `id`, `follower_id` (FK → auth_users), `followee_id` (FK → auth_users)
- `status` (VARCHAR) - ACTIVE, PENDING, BLOCKED
- Standard BaseEntity columns

**Indexes**:
- Unique composite: `(follower_id, followee_id)`
- Separate indexes on `follower_id`, `followee_id`
- Index on `status` for filtering active follows

**Business Logic**:
- ACTIVE: Follow relationship confirmed
- PENDING: For future private profile support
- BLOCKED: Follower blocked by followee
- Mutual follow detected via bidirectional query

---

#### `event_subscriptions`
**Purpose**: User subscriptions to events (without attending).

**Key Columns**:
- `id`, `user_id` (FK), `event_id` (FK)
- `subscription_type` (VARCHAR) - FOLLOW, NOTIFY, BOTH
- Standard BaseEntity columns

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
- `user_type` (VARCHAR, NOT NULL) - OWNER, CO_HOST, ORGANIZER, MEDIA_MANAGER, TICKETING_MANAGER, STAFF, VOLUNTEER
- `registration_status` (VARCHAR) - Registration status
- `registration_date` (TIMESTAMP)
- `is_volunteer` (BOOLEAN, NOT NULL, DEFAULT false)
- `volunteer_hours` (INT)
- Standard BaseEntity columns

**Indexes**:
- Unique composite: `(event_id, user_id)`
- Index on `user_type` for filtering

---

#### `event_user_permissions`
**Purpose**: Granular permission overrides for collaborators.

**Key Columns**:
- `id`, `event_user_id` (FK)
- `permission` (VARCHAR(120), NOT NULL) - VIEW_EVENT, EDIT_EVENT_DETAILS, MANAGE_COLLABORATORS, MANAGE_INVITES, MANAGE_SCHEDULE, MANAGE_BUDGET, MANAGE_TICKETS, MANAGE_CONTENT
- Standard BaseEntity columns

**Indexes**:
- Index on `event_user_id`
- Index on `permission`

---

#### `event_collaborator_invites`
**Purpose**: Pending/accepted collaborator invitations.

**Key Columns**:
- `id`, `event_id` (FK)
- `inviter_user_id` (FK), `invitee_user_id` (FK, nullable), `invitee_email` (nullable)
- `role` (VARCHAR) - Collaborator role for acceptance
- `status` (VARCHAR) - PENDING, ACCEPTED, DECLINED, EXPIRED
- `token_hash` (VARCHAR) - Invite token hash
- `expires_at` (TIMESTAMP)
- `responded_at` (TIMESTAMP)
- Standard BaseEntity columns

---

### BUDGET & PLANNING

#### `budgets`
**Purpose**: Overall event budget.

**Key Columns**:
- `id`, `event_id` (FK, UNIQUE - one budget per event), `owner_id` (FK → auth_users)
- `total_budget` (DECIMAL(12,2), NOT NULL)
- `currency` (VARCHAR(3), DEFAULT 'USD')
- `contingency_percentage` (DECIMAL(5,2), DEFAULT 10.00)
- `contingency_amount` (DECIMAL(12,2))
- `total_estimated` (DECIMAL(12,2)) - Estimated expenses
- `total_actual` (DECIMAL(12,2)) - Actual expenses
- `variance` (DECIMAL(12,2)) - Difference
- `variance_percentage` (DECIMAL(5,2))
- `total_revenue` (DECIMAL(12,2), DEFAULT 0) - Actual revenue from tickets
- `projected_revenue` (DECIMAL(12,2), DEFAULT 0) - Projected if all tickets sold
- `net_position` (DECIMAL(12,2), DEFAULT 0) - Revenue minus expenses
- `budget_status` (VARCHAR, DEFAULT 'DRAFT')
- `notes` (TEXT)
- Standard BaseEntity columns

**Relationships**:
- Has many: BudgetCategories → BudgetLineItems

**Indexes**:
- `event_id` (unique)

---

#### `budget_categories`
**Purpose**: Categorized budget sections.

**Key Columns**:
- `id`, `budget_id` (FK)
- `name` (VARCHAR, NOT NULL)
- `allocated_amount` (DECIMAL(12,2))
- `description` (TEXT)
- `category_order` (INT)
- Standard BaseEntity columns

**Indexes**:
- `budget_id`

---

#### `budget_line_items`
**Purpose**: Individual expenses/revenue items.

**Key Columns**:
- `id`, `budget_id` (FK), `budget_category_id` (FK)
- `task_id` (UUID, FK → tasks) - Optional reference to timeline task
- `description` (TEXT)
- `amount` (DECIMAL(12,2))
- `item_type` (VARCHAR) - EXPENSE, REVENUE
- `status` (VARCHAR) - DRAFT, CONFIRMED
- `notes` (TEXT)
- Standard BaseEntity columns

**Indexes**:
- `budget_id`, `budget_category_id`
- `task_id` (for timeline integration)

---

### TIMELINE & TASKS

#### `tasks`
**Purpose**: High-level event planning tasks.

**Key Columns**:
- `id`, `event_id` (FK)
- `assigned_to` (UUID, FK → auth_users) - Nullable
- `title` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `start_date`, `due_date` (TIMESTAMP)
- `priority` (VARCHAR, DEFAULT 'MEDIUM') - LOW, MEDIUM, HIGH
- `category` (VARCHAR) - Task category
- `status` (VARCHAR) - PENDING, IN_PROGRESS, COMPLETED, CANCELLED
- `progress_percentage` (INT, DEFAULT 0)
- `task_order` (INT) - Display order
- `is_draft` (BOOLEAN, DEFAULT true)
- `completed_subtasks_count` (INT, DEFAULT 0) - Denormalized
- `total_subtasks_count` (INT, DEFAULT 0) - Denormalized
- Standard BaseEntity columns

**Relationships**:
- Has many: Checklists (subtasks)

**Indexes**:
- `event_id`, `assigned_to`
- `status`, `priority`
- `(event_id, status, task_order)` for ordered listing

---

#### `checklists`
**Purpose**: Subtasks/checklist items within tasks.

**Key Columns**:
- `id`, `task_id` (FK)
- `assigned_to` (UUID, FK → auth_users) - Nullable
- `title` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `due_date` (TIMESTAMP)
- `status` (VARCHAR) - PENDING, COMPLETED, CANCELLED
- `task_order` (INT) - Display order within task
- `is_draft` (BOOLEAN, DEFAULT true)
- Standard BaseEntity columns

**Indexes**:
- `task_id`
- `(task_id, status, task_order)` for ordered listing

---

### EVENT SERIES

#### `event_series`
**Purpose**: Recurring event series management.

**Key Columns**:
- `id`
- `name` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `recurrence_pattern` (VARCHAR) - DAILY, WEEKLY, MONTHLY, YEARLY
- `recurrence_interval` (INT) - Every N occurrences
- `recurrence_end_type` (VARCHAR) - NEVER, AFTER_COUNT, ON_DATE
- `recurrence_end_count` (INT) - Number of occurrences
- `recurrence_end_date` (TIMESTAMP) - End date
- `created_by` (UUID, FK → auth_users)
- Standard BaseEntity columns

**Relationships**:
- Has many: Events (occurrences)

**Indexes**:
- `created_by`

---

### EVENT SUPPORTING ENTITIES

#### `event_stored_objects`
**Purpose**: Media and file storage references.

**Key Columns**:
- `id`, `event_id` (FK)
- `uploaded_by` (FK → auth_users)
- `object_key` (VARCHAR) - S3 object key
- `bucket_name` (VARCHAR) - S3 bucket
- `content_type` (VARCHAR)
- `file_size` (BIGINT)
- `metadata` (TEXT) - JSON metadata
- Standard BaseEntity columns

---

#### `event_reminders`
**Purpose**: Scheduled event reminders.

**Key Columns**:
- `id`, `event_id` (FK)
- `reminder_type` (VARCHAR) - EMAIL, PUSH, BOTH
- `reminder_time` (TIMESTAMP) - When to send
- `message` (TEXT)
- `sent_at` (TIMESTAMP)
- Standard BaseEntity columns

---

#### `event_notification_settings`
**Purpose**: Event-level notification preferences.

**Key Columns**:
- `id`, `event_id` (FK, UNIQUE)
- `email_enabled` (BOOLEAN, DEFAULT true)
- `push_enabled` (BOOLEAN, DEFAULT true)
- `notification_types` (TEXT) - JSON array of enabled types
- Standard BaseEntity columns

---

#### `event_waitlist_entries`
**Purpose**: Event-level waitlist (separate from ticket waitlist).

**Key Columns**:
- `id`, `event_id` (FK)
- `user_id` (FK → auth_users), `email` (VARCHAR)
- `status` (VARCHAR) - WAITING, PROMOTED, CANCELLED
- `promoted_at` (TIMESTAMP)
- Standard BaseEntity columns

---

#### `event_roles`
**Purpose**: Role definitions for events (legacy/RBAC integration).

**Key Columns**:
- `id`, `event_id` (FK)
- `user_id` (FK → auth_users)
- `role_definition_id` (UUID)
- `assigned_by` (FK → auth_users)
- Standard BaseEntity columns

---

### NOTIFICATIONS

#### `communications`
**Purpose**: Communication history (emails, push notifications).

**Key Columns**:
- `id`, `event_id` (FK) - Nullable
- `communication_type` (VARCHAR) - EMAIL, PUSH_NOTIFICATION
- `status` (VARCHAR) - PENDING, SENT, FAILED, DELIVERED
- `recipient_type` (VARCHAR) - USER, EMAIL
- `recipient_id` (UUID) - User ID or email reference
- `subject` (VARCHAR)
- `content` (TEXT)
- `sent_at` (TIMESTAMP)
- `delivered_at` (TIMESTAMP)
- `error_message` (TEXT)
- Standard BaseEntity columns

**Indexes**:
- `event_id`, `recipient_id`
- `status`, `communication_type`

---

#### `device_tokens`
**Purpose**: Push notification device tokens.

**Key Columns**:
- `id`, `user_id` (FK → auth_users)
- `token` (VARCHAR, NOT NULL) - Device token
- `platform` (VARCHAR) - IOS, ANDROID, WEB
- `device_id` (VARCHAR) - Device identifier
- `last_used_at` (TIMESTAMP)
- Standard BaseEntity columns

**Indexes**:
- `user_id`
- `(user_id, platform)` for user's devices

---

### USER SETTINGS

#### `user_settings`
**Purpose**: User preferences and settings.

**Key Columns**:
- `id`, `user_id` (FK → auth_users, UNIQUE)
- `location_id` (FK → locations) - Nullable
- `language_preference` (VARCHAR)
- `theme_preference` (VARCHAR)
- `timezone` (VARCHAR)
- `notification_preferences` (TEXT) - JSON
- `privacy_settings` (TEXT) - JSON
- Standard BaseEntity columns

---

#### `locations`
**Purpose**: Geographic location data.

**Key Columns**:
- `id`
- `city` (VARCHAR)
- `state` (VARCHAR)
- `country` (VARCHAR)
- `latitude` (DECIMAL)
- `longitude` (DECIMAL)
- `timezone` (VARCHAR)
- Standard BaseEntity columns

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
tasks          ─────► events       (Many tasks per event)
checklists     ─────► tasks        (Many checklist items per task)
budget_categories ───► budgets     (Many categories per budget)
budget_line_items ───► budget_categories (Many items per category)
budget_line_items ───► tasks       (Optional task reference)
events         ─────► event_series (Many events per series)
```

### One-to-One Relationships

```
events         ─────► budgets      (One budget per event)
events         ─────► event_notification_settings (One settings per event)
auth_users     ─────► user_settings (One settings per user)
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
events.parent_series_id ──► event_series.id   (Recurring events)
```

---

## Indexes & Performance

### Critical Indexes

```sql
-- Foreign keys (auto-indexed in most cases, explicit for clarity)
CREATE INDEX idx_tickets_event_id ON tickets(event_id);
CREATE INDEX idx_tickets_ticket_type_id ON tickets(ticket_type_id);
CREATE INDEX idx_tickets_attendee_id ON tickets(attendee_id);
CREATE INDEX idx_attendees_event_id ON attendees(event_id);
CREATE INDEX idx_event_posts_event_id ON event_posts(event_id);
CREATE INDEX idx_tasks_event_id ON tasks(event_id);
CREATE INDEX idx_budget_line_items_budget_id ON budget_line_items(budget_id);

-- Composite indexes for common queries
CREATE INDEX idx_tickets_event_status ON tickets(event_id, status);
CREATE INDEX idx_attendees_event_rsvp ON attendees(event_id, rsvp_status);
CREATE INDEX idx_posts_event_completed ON event_posts(event_id, media_upload_status, created_at DESC);
CREATE INDEX idx_tasks_event_status_order ON tasks(event_id, status, task_order);
CREATE INDEX idx_ticket_types_event_active ON ticket_types(event_id, is_active);

-- Unique constraints (also act as indexes)
CREATE UNIQUE INDEX uk_user_follows_follower_followee ON user_follows(follower_id, followee_id);
CREATE UNIQUE INDEX uk_event_subscriptions_user_event ON event_subscriptions(user_id, event_id);
CREATE UNIQUE INDEX uk_post_likes_post_user ON post_likes(post_id, user_id);
CREATE UNIQUE INDEX uk_event_users_event_user ON event_users(event_id, user_id);
CREATE UNIQUE INDEX uk_tickets_ticket_number ON tickets(ticket_number);
CREATE UNIQUE INDEX uk_budgets_event_id ON budgets(event_id);

-- Soft delete aware indexes
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_event_subscriptions_user ON event_subscriptions(user_id) WHERE deleted_at IS NULL;
```

### Denormalized Counts

To avoid expensive COUNT queries, several counts are denormalized:

- `ticket_types.quantity_sold`, `ticket_types.quantity_reserved`
- `event_posts.repost_count`
- `tasks.completed_subtasks_count`, `tasks.total_subtasks_count`
- `budgets.total_revenue`, `budgets.projected_revenue`, `budgets.net_position`
- `events.current_attendee_count`

### Query Optimization Patterns

1. **Batch Loading**: Load likes/comments for multiple posts in single query
2. **Lazy Loading**: Relationships use `FetchType.LAZY` to avoid N+1
3. **Pagination**: All list endpoints use `Pageable` with configurable size
4. **Indexed Queries**: WHERE clauses match indexed columns
5. **Composite Indexes**: Multi-column indexes for common filter combinations

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
**V10**: Collaboration permissions system (event_user_permissions table)
**V11**: Budget revenue tracking and timeline integration (revenue fields, task_id in line items)

### Migration Strategy

- **Idempotent**: Use `IF NOT EXISTS`, `IF EXISTS` checks
- **Backward Compatible**: Add nullable columns first, then populate, then make NOT NULL
- **Data Preservation**: Soft deletes, never DROP tables with data
- **Conditional Updates**: Check column existence before altering

### Current Schema Version

After V11, the schema includes:
- 40+ tables
- 200+ columns
- 60+ foreign key constraints
- 80+ indexes
- Full soft delete support across all entities
- Optimistic locking with `@Version` on all entities

---

## Data Integrity Rules

### Cascading Deletes

```sql
-- When event is deleted (soft delete), cascade to:
ticket_types, attendees, event_posts, collaborators, waitlist, subscriptions, budget, tasks

-- When user is deleted (soft delete), cascade to:
user_follows (both follower and followee), event_subscriptions, device_tokens

-- When ticket_type is deleted:
RESTRICT if tickets exist (protect sold tickets)
CASCADE waitlist entries

-- When event_post is deleted, cascade to:
post_likes, post_comments

-- When task is deleted, cascade to:
checklists

-- When budget is deleted, cascade to:
budget_categories, budget_line_items
```

### Check Constraints

```sql
-- Ticket capacity validation
CHECK (quantity_sold + quantity_reserved <= quantity_available)

-- Date validation
CHECK (end_date_time >= start_date_time)
CHECK (sale_end_date >= sale_start_date)

-- Price validation
CHECK (price_minor >= 0)
CHECK (amount >= 0)

-- Progress validation
CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
CHECK (completed_subtasks_count <= total_subtasks_count)
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

-- One budget per event
UNIQUE (event_id)

-- One settings per user
UNIQUE (user_id)

-- Unique ticket number
UNIQUE (ticket_number)

-- Unique promotion code
UNIQUE (promotion_code)
```

---

## Example Queries

### Get Event with Full Details

```sql
SELECT e.*,
       (SELECT COUNT(*) FROM attendees WHERE event_id = e.id AND rsvp_status = 'GOING') AS going_count,
       (SELECT COUNT(*) FROM tickets WHERE event_id = e.id AND status = 'ISSUED') AS ticket_count,
       (SELECT COUNT(*) FROM event_posts WHERE event_id = e.id AND media_upload_status = 'COMPLETED') AS post_count,
       b.total_revenue, b.net_position
FROM events e
LEFT JOIN budgets b ON b.event_id = e.id
WHERE e.id = ?;
```

### Get User's Timeline (followed events + subscriptions)

```sql
SELECT p.*
FROM event_posts p
WHERE p.event_id IN (
    SELECT event_id FROM event_subscriptions WHERE user_id = ?
    UNION
    SELECT e.id FROM events e
    INNER JOIN user_follows uf ON uf.followee_id = e.owner_id
    WHERE uf.follower_id = ? AND uf.status = 'ACTIVE'
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
       (tt.quantity_available - tt.quantity_sold - tt.quantity_reserved) AS available
FROM ticket_types tt
WHERE tt.event_id = ?
  AND tt.is_active = true
  AND (tt.sale_start_date IS NULL OR tt.sale_start_date <= NOW())
  AND (tt.sale_end_date IS NULL OR tt.sale_end_date >= NOW())
  AND (tt.quantity_available - tt.quantity_sold - tt.quantity_reserved) > 0
ORDER BY tt.price_minor ASC;
```

### Get Budget with Revenue Tracking

```sql
SELECT b.*,
       SUM(CASE WHEN bli.item_type = 'EXPENSE' THEN bli.amount ELSE 0 END) AS total_expenses,
       SUM(CASE WHEN bli.item_type = 'REVENUE' THEN bli.amount ELSE 0 END) AS total_revenue_items
FROM budgets b
LEFT JOIN budget_categories bc ON bc.budget_id = b.id
LEFT JOIN budget_line_items bli ON bli.budget_category_id = bc.id
WHERE b.event_id = ?
GROUP BY b.id;
```

---

## Conclusion

The Sade Event Planner database schema is designed for:
- **Flexibility**: Support multiple event types and workflows
- **Performance**: Denormalized counts, strategic indexes, composite indexes
- **Integrity**: Foreign keys, constraints, soft deletes
- **Scalability**: Paginated queries, lazy loading, batch operations
- **Auditability**: Timestamps, version tracking, soft deletes, RSVP history
- **Integration**: Task-budget linkage, revenue tracking from tickets

The schema evolves through Flyway migrations, ensuring zero-downtime deployments and data preservation across all changes. All entities support soft deletes and optimistic locking for data integrity and concurrent access safety.
