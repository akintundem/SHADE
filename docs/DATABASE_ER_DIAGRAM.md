# Database Entity Relationship Diagram

This document provides a visual representation of the database structure using Mermaid ER diagram syntax.

```mermaid
erDiagram
    %% Authentication & Users
    auth_users {
        uuid id PK
        string email UK
        string username UK
        string name
        string user_type
        string status
    }
    
    user_settings {
        uuid id PK
        uuid user_id FK
        uuid location_id FK
        string timezone
    }
    
    locations {
        uuid id PK
        string city
        string country
    }
    
    %% Event Management Core
    events {
        uuid id PK
        string name
        uuid owner_id FK
        string event_status
        string access_type
        timestamp start_date_time
        timestamp end_date_time
        uuid parent_series_id FK
    }
    
    event_series {
        uuid id PK
        string name
        string recurrence_pattern
        uuid created_by FK
    }
    
    event_stored_objects {
        uuid id PK
        uuid event_id FK
        string object_key
        string content_type
    }
    
    event_notification_settings {
        uuid id PK
        uuid event_id FK
        boolean email_enabled
        boolean push_enabled
    }
    
    event_waitlist_entries {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        string status
    }
    
    %% Ticketing System
    ticket_types {
        uuid id PK
        uuid event_id FK
        string name
        bigint price_minor
        string currency
        int quantity_available
        int quantity_sold
        boolean is_active
    }
    
    tickets {
        uuid id PK
        uuid event_id FK
        uuid ticket_type_id FK
        uuid attendee_id FK
        string ticket_number UK
        string status
    }
    
    ticket_checkouts {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        string status
        decimal total_amount
    }
    
    ticket_checkout_items {
        uuid id PK
        uuid checkout_id FK
        uuid ticket_type_id FK
        int quantity
    }
    
    ticket_waitlist_entries {
        uuid id PK
        uuid ticket_type_id FK
        uuid user_id FK
        string status
    }
    
    ticket_price_tiers {
        uuid id PK
        uuid ticket_type_id FK
        string name
        bigint price_minor
    }
    
    %% Attendees & RSVP
    attendees {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        string name
        string email
        string rsvp_status
    }
    
    attendee_invites {
        uuid id PK
        uuid event_id FK
        string email
        string token UK
        string status
    }
    
    attendee_rsvp_history {
        uuid id PK
        uuid attendee_id FK
        string old_status
        string new_status
    }
    
    %% Social & Engagement
    event_posts {
        uuid id PK
        uuid event_id FK
        uuid created_by FK
        string post_type
        text content
        uuid reposted_from_id FK
    }
    
    post_likes {
        uuid id PK
        uuid post_id FK
        uuid user_id FK
    }
    
    post_comments {
        uuid id PK
        uuid post_id FK
        uuid user_id FK
        text content
    }
    
    %% Social Graph
    user_follows {
        uuid id PK
        uuid follower_id FK
        uuid followee_id FK
        string status
    }
    
    event_subscriptions {
        uuid id PK
        uuid user_id FK
        uuid event_id FK
        string subscription_type
    }
    
    %% Collaboration
    event_users {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        string user_type
        string registration_status
    }
    
    event_user_permissions {
        uuid id PK
        uuid event_user_id FK
        string permission
    }
    
    event_collaborator_invites {
        uuid id PK
        uuid event_id FK
        uuid inviter_user_id FK
        uuid invitee_user_id FK
        string role
        string status
    }
    
    %% Budget & Planning
    budgets {
        uuid id PK
        uuid event_id FK UK
        uuid owner_id FK
        decimal total_budget
        string currency
        decimal total_revenue
    }
    
    budget_categories {
        uuid id PK
        uuid budget_id FK
        string name
        decimal allocated_amount
    }
    
    budget_line_items {
        uuid id PK
        uuid budget_id FK
        uuid budget_category_id FK
        uuid task_id FK
        decimal amount
        string item_type
    }
    
    %% Timeline & Tasks
    tasks {
        uuid id PK
        uuid event_id FK
        uuid assigned_to FK
        string title
        string status
        int progress_percentage
    }
    
    checklists {
        uuid id PK
        uuid task_id FK
        string title
        string status
    }
    
    %% Notifications
    communications {
        uuid id PK
        uuid event_id FK
        string communication_type
        string status
        string recipient_type
    }
    
    device_tokens {
        uuid id PK
        uuid user_id FK
        string token
        string platform
    }
    
    %% Relationships
    auth_users ||--o| user_settings : "has"
    user_settings }o--o| locations : "references"
    
    auth_users ||--o{ events : "owns"
    events }o--o| event_series : "belongs_to"
    events ||--o{ event_stored_objects : "has"
    events ||--o| event_notification_settings : "has"
    events ||--o{ event_waitlist_entries : "has"
    
    events ||--o{ ticket_types : "has"
    ticket_types ||--o{ tickets : "has"
    ticket_types ||--o{ ticket_price_tiers : "has"
    ticket_types ||--o{ ticket_waitlist_entries : "has"
    tickets }o--o| attendees : "linked_to"
    
    events ||--o{ ticket_checkouts : "has"
    ticket_checkouts ||--o{ ticket_checkout_items : "has"
    ticket_checkout_items }o--|| ticket_types : "references"
    
    events ||--o{ attendees : "has"
    attendees }o--o| auth_users : "may_be"
    events ||--o{ attendee_invites : "has"
    attendees ||--o{ attendee_rsvp_history : "has"
    
    events ||--o{ event_posts : "has"
    event_posts }o--o| auth_users : "created_by"
    event_posts }o--o| event_posts : "reposted_from"
    event_posts ||--o{ post_likes : "has"
    event_posts ||--o{ post_comments : "has"
    post_likes }o--|| auth_users : "liked_by"
    post_comments }o--|| auth_users : "commented_by"
    
    auth_users ||--o{ user_follows : "follower"
    auth_users ||--o{ user_follows : "followee"
    
    auth_users ||--o{ event_subscriptions : "subscribes"
    events ||--o{ event_subscriptions : "subscribed_to"
    
    events ||--o{ event_users : "has"
    event_users }o--|| auth_users : "is"
    event_users ||--o{ event_user_permissions : "has"
    events ||--o{ event_collaborator_invites : "has"
    event_collaborator_invites }o--|| auth_users : "inviter"
    event_collaborator_invites }o--o| auth_users : "invitee"
    
    events ||--|| budgets : "has"
    budgets }o--|| auth_users : "owned_by"
    budgets ||--o{ budget_categories : "has"
    budget_categories ||--o{ budget_line_items : "has"
    budget_line_items }o--o| tasks : "linked_to"
    
    events ||--o{ tasks : "has"
    tasks }o--o| auth_users : "assigned_to"
    tasks ||--o{ checklists : "has"
    
    events ||--o{ communications : "has"
    auth_users ||--o{ device_tokens : "has"
```

## Entity Summary

### Core Entities
- **auth_users**: User accounts and authentication
- **events**: Core event entity
- **event_series**: Recurring event series

### Ticketing
- **ticket_types**: Ticket categories (VIP, General, etc.)
- **tickets**: Individual ticket instances
- **ticket_checkouts**: Shopping cart/checkout sessions
- **ticket_waitlist_entries**: Waitlist for sold-out tickets

### Attendees
- **attendees**: Event participants
- **attendee_invites**: RSVP invitations
- **attendee_rsvp_history**: RSVP change audit trail

### Social & Feeds
- **event_posts**: Twitter-like posts within events
- **post_likes**: Post likes
- **post_comments**: Post comments
- **user_follows**: User-to-user follow relationships
- **event_subscriptions**: User subscriptions to events

### Collaboration
- **event_users**: Event collaborators and staff
- **event_user_permissions**: Granular permissions
- **event_collaborator_invites**: Collaborator invitations

### Budget & Planning
- **budgets**: Event budgets (one per event)
- **budget_categories**: Budget sections
- **budget_line_items**: Individual expenses/revenue

### Timeline & Tasks
- **tasks**: Event planning tasks
- **checklists**: Subtasks/checklist items

### Supporting
- **event_stored_objects**: Media/file storage references
- **event_notification_settings**: Event-level notification preferences
- **communications**: Communication history
- **device_tokens**: Push notification tokens
- **user_settings**: User preferences
- **locations**: Geographic location data
