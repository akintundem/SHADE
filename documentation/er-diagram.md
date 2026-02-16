# Entity-relationship diagram

This diagram reflects the main domain model and database tables. Schema is managed by **Flyway** migrations under `src/main/resources/db/migration/`; Hibernate uses `ddl-auto: validate` and does not change the schema.

## Diagram

```mermaid
erDiagram
  AUTH_USERS {
    UUID id PK
    string email
    string username
    string auth_sub
    string name
  }

  USER_SETTINGS {
    UUID id PK
    UUID user_id FK
    UUID location_id FK
  }

  USER_PREFERENCES {
    UUID id PK
    UUID user_id FK
    string preference_key
    string value
  }

  LOCATIONS {
    UUID id PK
    string city
    string state
    string country
  }

  VENUES {
    UUID id PK
    string name
    string address
    string city
    string country
  }

  EVENTS {
    UUID id PK
    UUID owner_id FK
    UUID venue_id FK
    UUID timeline_published_by FK
    UUID archived_by FK
    string name
    string event_status
    string access_type
  }

  EVENT_METADATA {
    UUID id PK
    UUID event_id FK
    string metadata_key
    string value
  }

  EVENT_REMINDERS {
    UUID id PK
    UUID event_id FK
  }

  EVENT_NOTIFICATION_SETTINGS {
    UUID id PK
    UUID event_id FK
  }

  EVENT_STORED_OBJECTS {
    UUID id PK
    UUID event_id FK
    UUID uploaded_by FK
  }

  EVENT_WAITLIST_ENTRIES {
    UUID id PK
    UUID event_id FK
    UUID user_id FK
  }

  EVENT_USERS {
    UUID id PK
    UUID event_id FK
    UUID user_id FK
    string user_type
  }

  EVENT_USER_PERMISSIONS {
    UUID id PK
    UUID event_user_id FK
    string permission
  }

  EVENT_ROLES {
    UUID id PK
    UUID event_id FK
    UUID user_id FK
    string role_name
    UUID assigned_by FK
  }

  EVENT_COLLABORATOR_INVITES {
    UUID id PK
    UUID event_id FK
    UUID inviter_user_id FK
    UUID invitee_user_id FK
  }

  CURRENCIES {
    UUID id PK
    string code
  }

  BUDGETS {
    UUID id PK
    UUID event_id FK
    UUID owner_id FK
  }

  BUDGET_CATEGORIES {
    UUID id PK
    UUID budget_id FK
  }

  BUDGET_LINE_ITEMS {
    UUID id PK
    UUID budget_id FK
    UUID budget_category_id FK
  }

  TICKET_TYPES {
    UUID id PK
    UUID event_id FK
  }

  TICKET_TYPE_METADATA {
    UUID id PK
    UUID ticket_type_id FK
    string metadata_key
    string value
  }

  TICKET_PRICE_TIERS {
    UUID id PK
    UUID ticket_type_id FK
    bigint price_minor
  }

  TICKET_TYPE_DEPENDENCIES {
    UUID id PK
    UUID ticket_type_id FK
    UUID required_ticket_type_id FK
  }

  TICKET_PROMOTIONS {
    UUID id PK
    UUID event_id FK
    UUID ticket_type_id FK
  }

  TICKET_TYPE_TEMPLATES {
    UUID id PK
    UUID created_by FK
  }

  TICKET_CHECKOUTS {
    UUID id PK
    UUID event_id FK
    UUID purchaser_id FK
  }

  TICKET_CHECKOUT_ITEMS {
    UUID id PK
    UUID checkout_id FK
    UUID ticket_type_id FK
  }

  TICKETS {
    UUID id PK
    UUID event_id FK
    UUID ticket_type_id FK
    UUID attendee_id FK
    UUID checkout_id FK
  }

  TICKET_METADATA {
    UUID id PK
    UUID ticket_id FK
    string metadata_key
    string value
  }

  TICKET_WAITLIST_ENTRIES {
    UUID id PK
    UUID ticket_type_id FK
    UUID user_id FK
  }

  TICKET_APPROVAL_REQUESTS {
    UUID id PK
    UUID ticket_type_id FK
    UUID user_id FK
  }

  ATTENDEES {
    UUID id PK
    UUID event_id FK
    UUID user_id FK
  }

  ATTENDEE_INVITES {
    UUID id PK
    UUID event_id FK
    UUID inviter_user_id FK
    UUID invitee_user_id FK
  }

  ATTENDEE_RSVP_HISTORY {
    UUID id PK
    UUID event_id FK
    UUID attendee_id FK
    UUID changed_by FK
    string previous_status
    string new_status
  }

  TASKS {
    UUID id PK
    UUID event_id FK
    UUID assigned_to FK
    string title
    string status
  }

  CHECKLISTS {
    UUID id PK
    UUID task_id FK
  }

  EVENT_POSTS {
    UUID id PK
    UUID event_id FK
    UUID created_by FK
  }

  POST_COMMENTS {
    UUID id PK
    UUID post_id FK
    UUID user_id FK
  }

  POST_LIKES {
    UUID id PK
    UUID post_id FK
    UUID user_id FK
  }

  USER_FOLLOWS {
    UUID id PK
    UUID follower_id FK
    UUID following_id FK
  }

  DEVICE_TOKENS {
    UUID id PK
    UUID user_id FK
  }

  COMMUNICATIONS {
    UUID id PK
    UUID event_id FK
  }

  AUTH_USERS ||--|| USER_SETTINGS : has
  LOCATIONS ||--o{ USER_SETTINGS : located_in
  AUTH_USERS ||--o{ USER_PREFERENCES : has

  AUTH_USERS ||--o{ EVENTS : owns
  VENUES ||--o{ EVENTS : venue
  AUTH_USERS ||--o{ EVENTS : timeline_published_by
  AUTH_USERS ||--o{ EVENTS : archived_by
  EVENTS ||--o{ EVENT_METADATA : has
  EVENTS ||--o{ EVENT_REMINDERS : has
  EVENTS ||--|| EVENT_NOTIFICATION_SETTINGS : settings
  EVENTS ||--o{ EVENT_STORED_OBJECTS : media
  AUTH_USERS ||--o{ EVENT_STORED_OBJECTS : uploads
  EVENTS ||--o{ EVENT_WAITLIST_ENTRIES : waitlist
  AUTH_USERS ||--o{ EVENT_WAITLIST_ENTRIES : user

  EVENTS ||--o{ EVENT_USERS : members
  AUTH_USERS ||--o{ EVENT_USERS : user
  EVENT_USERS ||--o{ EVENT_USER_PERMISSIONS : permissions
  EVENTS ||--o{ EVENT_ROLES : roles
  AUTH_USERS ||--o{ EVENT_ROLES : user
  AUTH_USERS ||--o{ EVENT_ROLES : assigned_by
  EVENTS ||--o{ EVENT_COLLABORATOR_INVITES : invites
  AUTH_USERS ||--o{ EVENT_COLLABORATOR_INVITES : inviter
  AUTH_USERS ||--o{ EVENT_COLLABORATOR_INVITES : invitee

  EVENTS ||--|| BUDGETS : budget
  AUTH_USERS ||--o{ BUDGETS : owns
  BUDGETS ||--o{ BUDGET_CATEGORIES : includes
  BUDGETS ||--o{ BUDGET_LINE_ITEMS : includes
  BUDGET_CATEGORIES ||--o{ BUDGET_LINE_ITEMS : contains

  EVENTS ||--o{ TICKET_TYPES : ticket_types
  TICKET_TYPES ||--o{ TICKET_TYPE_METADATA : metadata
  TICKET_TYPES ||--o{ TICKET_PRICE_TIERS : tiers
  TICKET_TYPES ||--o{ TICKET_TYPE_DEPENDENCIES : depends
  TICKET_TYPES ||--o{ TICKET_TYPE_DEPENDENCIES : required
  EVENTS ||--o{ TICKET_PROMOTIONS : promotions
  TICKET_TYPES ||--o{ TICKET_PROMOTIONS : applies_to
  AUTH_USERS ||--o{ TICKET_TYPE_TEMPLATES : creates
  EVENTS ||--o{ TICKET_CHECKOUTS : checkouts
  AUTH_USERS ||--o{ TICKET_CHECKOUTS : purchases
  TICKET_CHECKOUTS ||--o{ TICKET_CHECKOUT_ITEMS : items
  TICKET_TYPES ||--o{ TICKET_CHECKOUT_ITEMS : type
  EVENTS ||--o{ TICKETS : tickets
  TICKET_TYPES ||--o{ TICKETS : type
  ATTENDEES ||--o{ TICKETS : holds
  TICKET_CHECKOUTS ||--o{ TICKETS : produces
  TICKETS ||--o{ TICKET_METADATA : metadata
  TICKET_TYPES ||--o{ TICKET_WAITLIST_ENTRIES : waitlist
  AUTH_USERS ||--o{ TICKET_WAITLIST_ENTRIES : user
  TICKET_TYPES ||--o{ TICKET_APPROVAL_REQUESTS : approval_requests
  AUTH_USERS ||--o{ TICKET_APPROVAL_REQUESTS : user

  EVENTS ||--o{ ATTENDEES : attendees
  AUTH_USERS ||--o{ ATTENDEES : user
  EVENTS ||--o{ ATTENDEE_INVITES : invites
  AUTH_USERS ||--o{ ATTENDEE_INVITES : inviter
  AUTH_USERS ||--o{ ATTENDEE_INVITES : invitee
  EVENTS ||--o{ ATTENDEE_RSVP_HISTORY : rsvp_history
  ATTENDEES ||--o{ ATTENDEE_RSVP_HISTORY : rsvp_history
  AUTH_USERS ||--o{ ATTENDEE_RSVP_HISTORY : changed_by

  EVENTS ||--o{ TASKS : tasks
  AUTH_USERS ||--o{ TASKS : assigned_to
  TASKS ||--o{ CHECKLISTS : checklist

  EVENTS ||--o{ EVENT_POSTS : posts
  AUTH_USERS ||--o{ EVENT_POSTS : creates
  EVENT_POSTS ||--o{ POST_COMMENTS : comments
  AUTH_USERS ||--o{ POST_COMMENTS : user
  EVENT_POSTS ||--o{ POST_LIKES : likes
  AUTH_USERS ||--o{ POST_LIKES : user

  AUTH_USERS ||--o{ USER_FOLLOWS : follower
  AUTH_USERS ||--o{ USER_FOLLOWS : following

  AUTH_USERS ||--o{ DEVICE_TOKENS : devices
  EVENTS ||--o{ COMMUNICATIONS : communications
```

## Notes

- **Auth:** Users are in `auth_users` (IdP `sub` in `auth_sub`, email, username). `user_settings` and `locations` support profile and location search. `user_preferences` stores key-value preferences.
- **Events:** `events` has embedded venue fields and optional `venue_id` to a standalone `venues` table. Event metadata is in `event_metadata` (key-value). Access is controlled by `access_type` (open, RSVP, invite-only, ticketed).
- **Collaboration:** `event_users` is the membership table; `event_user_permissions` holds per-member permission overrides; `event_roles` assigns a role name per user per event; `event_collaborator_invites` for invite flow (accept by token in POST body).
- **Tickets:** Types, price tiers, dependencies, promotions, checkouts, and tickets. Waitlist and approval requests live in `ticket_waitlist_entries` and `ticket_approval_requests`. Metadata tables exist for events, ticket types, and tickets.
- **Timeline:** Represented by `tasks` and `checklists`; the event has timeline publication state (e.g. `timeline_published`, `timeline_published_by`). There are no separate `event_timelines` or `timeline_items` tables.
- **Feeds:** `event_posts`, `post_comments`, `post_likes`.
- **Communications:** `device_tokens` for push; `communications` stores send records with redacted content for audit.
