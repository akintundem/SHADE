erDiagram
  AUTH_USERS {
    UUID id PK
    string email
    string username
    string cognito_sub
  }

  USER_SETTINGS {
    UUID id PK
    UUID user_id FK
    UUID location_id FK
  }

  LOCATIONS {
    UUID id PK
    string city
    string country
  }

  EVENTS {
    UUID id PK
    UUID owner_id FK
    UUID timeline_published_by FK
    UUID archived_by FK
    UUID restored_by FK
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

  EVENT_ROLES {
    UUID id PK
    UUID event_id
    UUID user_id
  }

  COMMUNICATIONS {
    UUID id PK
    UUID event_id
  }

  DEVICE_TOKENS {
    UUID id PK
    UUID user_id
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

  TICKET_PROMOTIONS {
    UUID id PK
    UUID event_id FK
    UUID ticket_type_id FK
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
    UUID issued_by FK
    UUID validated_by FK
    UUID checkout_id FK
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

  EVENT_USERS {
    UUID id PK
    UUID event_id FK
    UUID user_id FK
  }

  EVENT_USER_PERMISSIONS {
    UUID id PK
    UUID event_user_id FK
  }

  EVENT_COLLABORATOR_INVITES {
    UUID id PK
    UUID event_id FK
    UUID inviter_user_id FK
    UUID invitee_user_id FK
  }

  EVENT_POSTS {
    UUID id PK
    UUID event_id FK
    UUID created_by FK
    UUID media_object_id
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

  TASKS {
    UUID id PK
    UUID event_id FK
    UUID assigned_to FK
  }

  CHECKLISTS {
    UUID id PK
    UUID task_id FK
    UUID assigned_to FK
  }

  AUTH_USERS ||--|| USER_SETTINGS : has
  LOCATIONS ||--o{ USER_SETTINGS : located_in

  AUTH_USERS ||--o{ EVENTS : owns
  AUTH_USERS ||--o{ EVENTS : publishes
  AUTH_USERS ||--o{ EVENTS : archives
  AUTH_USERS ||--o{ EVENTS : restores

  EVENTS ||--|| BUDGETS : budget
  AUTH_USERS ||--o{ BUDGETS : owns
  BUDGETS ||--o{ BUDGET_CATEGORIES : includes
  BUDGETS ||--o{ BUDGET_LINE_ITEMS : includes
  BUDGET_CATEGORIES ||--o{ BUDGET_LINE_ITEMS : contains

  EVENTS ||--o{ EVENT_REMINDERS : has
  EVENTS ||--|| EVENT_NOTIFICATION_SETTINGS : settings
  EVENTS ||--o{ EVENT_STORED_OBJECTS : media
  AUTH_USERS ||--o{ EVENT_STORED_OBJECTS : uploads

  EVENTS ||--o{ EVENT_POSTS : posts
  AUTH_USERS ||--o{ EVENT_POSTS : creates
  EVENT_POSTS ||--o{ POST_COMMENTS : comments
  EVENT_POSTS ||--o{ POST_LIKES : likes
  AUTH_USERS ||--o{ POST_COMMENTS : writes
  AUTH_USERS ||--o{ POST_LIKES : likes

  EVENTS ||--o{ TASKS : tasks
  AUTH_USERS ||--o{ TASKS : assigned
  TASKS ||--o{ CHECKLISTS : checklist
  AUTH_USERS ||--o{ CHECKLISTS : assigned

  EVENTS ||--o{ TICKET_TYPES : ticket_types
  EVENTS ||--o{ TICKET_PROMOTIONS : promotions
  TICKET_TYPES ||--o{ TICKET_PROMOTIONS : applies_to
  EVENTS ||--o{ TICKET_CHECKOUTS : checkouts
  AUTH_USERS ||--o{ TICKET_CHECKOUTS : purchases
  TICKET_CHECKOUTS ||--o{ TICKET_CHECKOUT_ITEMS : items
  TICKET_TYPES ||--o{ TICKET_CHECKOUT_ITEMS : priced_as
  EVENTS ||--o{ TICKETS : tickets
  TICKET_TYPES ||--o{ TICKETS : type
  ATTENDEES ||--o{ TICKETS : holds
  AUTH_USERS ||--o{ TICKETS : issued_by
  AUTH_USERS ||--o{ TICKETS : validated_by
  TICKET_CHECKOUTS ||--o{ TICKETS : produces

  EVENTS ||--o{ ATTENDEES : attendees
  AUTH_USERS ||--o{ ATTENDEES : users

  EVENTS ||--o{ ATTENDEE_INVITES : invites
  AUTH_USERS ||--o{ ATTENDEE_INVITES : inviter
  AUTH_USERS ||--o{ ATTENDEE_INVITES : invitee

  EVENTS ||--o{ EVENT_USERS : members
  AUTH_USERS ||--o{ EVENT_USERS : members
  EVENT_USERS ||--o{ EVENT_USER_PERMISSIONS : permissions

  EVENTS ||--o{ EVENT_COLLABORATOR_INVITES : collab_invites
  AUTH_USERS ||--o{ EVENT_COLLABORATOR_INVITES : inviter
  AUTH_USERS ||--o{ EVENT_COLLABORATOR_INVITES : invitee

  EVENTS ||--o{ EVENT_ROLES : roles
  AUTH_USERS ||--o{ EVENT_ROLES : roles

  AUTH_USERS ||--o{ DEVICE_TOKENS : devices
  EVENTS ||--o{ COMMUNICATIONS : communications
