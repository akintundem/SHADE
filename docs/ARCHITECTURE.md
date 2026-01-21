# Sade Event Planner - System Architecture

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Feature Modules](#feature-modules)
4. [Integration Patterns](#integration-patterns)
5. [Data Flow](#data-flow)
6. [Technology Stack](#technology-stack)

---

## Overview

Sade Event Planner is a comprehensive event management platform built as a Spring Boot monolith with clear module boundaries. The system manages the complete event lifecycle including planning, ticketing, attendee management, and social engagement.

### Core Principles
- **Modularity**: Features organized in bounded contexts
- **Reusability**: Shared infrastructure across features
- **Consistency**: Standardized patterns for DTOs, services, repositories
- **Security**: RBAC-based access control throughout
- **Scalability**: Denormalized counts, indexed queries, lazy loading

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          REST API Layer                          │
│                    (Spring MVC Controllers)                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                       Service Layer                              │
│  ┌───────────┐  ┌──────────┐  ┌────────┐  ┌────────┐  ┌───────┐│
│  │  Events   │  │ Tickets  │  │ Feeds  │  │ Social │  │ Other ││
│  └───────────┘  └──────────┘  └────────┘  └────────┘  └───────┘│
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                    Common Infrastructure                         │
│  ┌─────────────┐  ┌───────────┐  ┌──────────────┐  ┌─────────┐ │
│  │  Storage    │  │   RBAC    │  │ Notification │  │  Auth   │ │
│  │  (S3)       │  │           │  │              │  │ (Cognito)│ │
│  └─────────────┘  └───────────┘  └──────────────┘  └─────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                     Data Access Layer                            │
│              (Spring Data JPA Repositories)                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  PostgreSQL Database                             │
│                  (with Flyway Migrations)                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Feature Modules

### 1. **Events Module** (`eventplanner.features.event`)

**Purpose**: Core event management - creating, updating, and organizing events.

**Key Entities**:
- `Event` - Main event entity with metadata, dates, location
- `EventSeries` - Recurring event series management
- `EventWaitlistEntry` - Event-level waitlist
- `EventStoredObject` - Media and file storage references
- `EventNotificationSettings` - Notification preferences
- `EventReminder` - Scheduled reminders

**Key Services**:
- `EventService` - CRUD operations, cloning, status automation
- `EventAccessControlService` - Permission checks based on event type
- `EventSeriesService` - Recurring event management
- `EventWaitlistService` - Waitlist promotion

**Integration Points**:
- **Tickets**: Events have multiple ticket types
- **Attendees**: Events have attendees via RSVP or tickets
- **Feeds**: Events have feed posts for social engagement
- **Social**: Users can subscribe to events
- **Collaboration**: Events have collaborators with roles

---

### 2. **Tickets Module** (`eventplanner.features.ticket`)

**Purpose**: Ticket sales, management, validation, and distribution.

**Key Entities**:
- `Ticket` - Individual ticket instance
- `TicketType` - Template for ticket categories (VIP, GA, etc.)
- `TicketPriceTier` - Time-based or quantity-based pricing
- `TicketCheckout` - Cart/checkout session
- `TicketWaitlistEntry` - Ticket-type specific waitlist
- `TicketApprovalRequest` - Approval workflow for restricted tickets
- `TicketTypeTemplate` - Reusable ticket type configurations

**Key Services**:
- `TicketService` - Ticket lifecycle management
- `TicketTypeService` - Ticket type configuration
- `TicketCheckoutService` - Cart and purchase flow
- `TicketWaitlistService` - Waitlist management
- `TicketApprovalService` - Approval workflow

**Integration Points**:
- **Events**: Tickets belong to events
- **Attendees**: Tickets can be assigned to attendees
- **Storage**: QR codes stored in S3
- **Notifications**: Email tickets, reminders

**Business Rules**:
- Dynamic pricing based on tiers
- Capacity management with sold/reserved tracking
- Transferable tickets with audit trail
- QR code validation at check-in

---

### 3. **Attendees Module** (`eventplanner.features.attendee`)

**Purpose**: RSVP management, guest lists, and attendee invitations.

**Key Entities**:
- `Attendee` - Person attending an event
- `AttendeeInvite` - Invitation to RSVP for event
- `RsvpHistory` - Audit trail of RSVP changes

**Key Services**:
- `AttendeeService` - Attendee management, bulk operations
- `AttendeeInviteService` - Invitation creation and management

**Integration Points**:
- **Events**: Attendees belong to events
- **Tickets**: Attendees can have tickets
- **Notifications**: RSVP confirmations, reminders

**Business Rules**:
- RSVP status: GOING, NOT_GOING, MAYBE, NO_RESPONSE
- Plus-one support with guest names
- Bulk invite and RSVP update operations

---

### 4. **Feeds Module** (`eventplanner.features.feeds`)

**Purpose**: Twitter-like social posts within events.

**Key Entities**:
- `EventFeedPost` - Posts (TEXT, IMAGE, VIDEO) within event timeline
- `PostComment` - Comments on posts
- `PostLike` - Likes on posts

**Key Services**:
- `FeedPostService` - Post creation, media upload, reposting
- `PostCommentService` - Comment CRUD with notifications
- `PostLikeService` - Like/unlike with counts

**Integration Points**:
- **Events**: Posts belong to events
- **Social**: Repost counts, quote posts
- **Storage**: Presigned uploads for images/videos
- **Notifications**: Push notifications on likes/comments

**Features**:
- Presigned S3 uploads for media
- Repost and quote-post functionality
- Engagement metrics (likes, comments, reposts)
- Access control via event permissions

---

### 5. **Social Module** (`eventplanner.features.social`)

**Purpose**: User relationships and event subscriptions for discovery.

**Key Entities**:
- `UserFollow` - User-to-user follow relationships
- `EventSubscription` - User subscriptions to events

**Key Services**:
- `UserFollowService` - Follow/unfollow, followers/following lists
- `EventSubscriptionService` - Subscribe to events for updates

**Integration Points**:
- **Users**: Follow relationships between users
- **Events**: Subscription to events
- **Feeds**: Social context for posts (followers who liked, etc.)

**Features**:
- Follow status: ACTIVE, PENDING (for future private profiles), BLOCKED
- Mutual follow detection
- Subscription types: FOLLOW (timeline), NOTIFY (push), BOTH
- Follow stats and social context in responses

---

### 6. **Collaboration Module** (`eventplanner.features.collaboration`)

**Purpose**: Multi-user event management with roles.

**Key Entities**:
- `EventCollaborator` - User with specific role on event
- `CollaboratorInvite` - Invitation to collaborate

**Roles**:
- `OWNER` - Full control
- `CO_HOST` - Manage most aspects
- `ORGANIZER` - Event details and logistics
- `MEDIA_MANAGER` - Media and marketing
- `TICKETING_MANAGER` - Ticket operations

**Integration Points**:
- **Events**: Collaborators belong to events
- **RBAC**: Roles define permissions

---

### 7. **Budget Module** (`eventplanner.features.budget`)

**Purpose**: Event budget planning and expense tracking.

**Key Entities**:
- `Budget` - Overall event budget
- `BudgetCategory` - Categorized line items
- `BudgetLineItem` - Individual expenses/revenue items

**Features**:
- Real-time totals (revenue, expenses, profit)
- Auto-save for line items
- Budget variance tracking

---

### 8. **Timeline Module** (`eventplanner.features.timeline`)

**Purpose**: Event planning checklists and task management.

**Key Entities**:
- `Checklist` - Collection of tasks
- `Task` - Individual to-do item

**Features**:
- Task completion tracking
- Auto-save functionality
- Deadline management

---

## Integration Patterns

### 1. **Storage Pattern** (Reused across features)

All features use the centralized `S3StorageService` with `PresignedUploadService`:

```java
// Common pattern for media uploads
PresignedUploadRequest request = new PresignedUploadRequest();
request.setFileName(fileName);
request.setContentType(contentType);
request.setCategory("post"); // or "event", "ticket", etc.

PresignedUploadResponse response = presignedUploadService.generatePresignedUpload(
    request,
    mediaId -> buildObjectKey(entityId, mediaId),
    BucketAlias.EVENT, // or USER
    Duration.ofMinutes(10)
);
```

**Used by**:
- Events: Cover images, event media
- Feeds: Post images/videos
- Tickets: QR codes
- Users: Profile pictures

**Benefits**:
- Single source of truth for S3 operations
- Consistent presigned URL generation
- Centralized media metadata tracking
- Automatic cleanup of failed uploads

---

### 2. **Access Control Pattern**

`EventAccessControlService` provides consistent permission checks:

```java
// Check if user can view event content
accessControlService.requireMediaView(principal, eventId);

// Check if user can upload media
accessControlService.requireMediaUpload(principal, eventId);

// Check if user can manage media
accessControlService.requireMediaManage(principal, eventId);
```

**Rules**:
- **OPEN**: Anyone can view
- **RSVP_REQUIRED**: Must have RSVP to access
- **INVITE_ONLY**: Must have invite
- **TICKETED**: Must have ticket or be collaborator/owner

**Used by**:
- Feeds (post creation, viewing)
- Events (media uploads)
- Attendees (guest list access)

---

### 3. **Notification Pattern**

Centralized `NotificationService` for email and push notifications:

```java
NotificationRequest notification = NotificationRequest.builder()
    .type(CommunicationType.PUSH_NOTIFICATION)
    .to(userId.toString())
    .subject("New comment on your post")
    .templateVariables(Map.of("postId", postId, "body", commentText))
    .eventId(eventId)
    .build();

notificationService.send(notification);
```

**Used by**:
- Feeds: Comment/like notifications
- Tickets: Ticket delivery
- Attendees: RSVP confirmations
- Events: Event reminders

---

### 4. **DTO Response Pattern**

All response DTOs follow consistent structure:

```java
@Getter
@Setter
@Schema(description = "...")
public class EntityResponse {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity-specific fields

    // Nested objects for relationships
    // (avoid circular references, use IDs or minimal DTOs)
}
```

**Naming Conventions**:
- Request DTOs: `Create{Entity}Request`, `Update{Entity}Request`
- Response DTOs: `{Entity}Response`
- List responses: `{Entity}ListResponse` with pagination metadata

---

### 5. **Soft Delete Pattern**

All entities extend `BaseEntity` with soft delete support:

```java
@MappedSuperclass
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Long version;
}
```

**Benefits**:
- Audit trail preservation
- Restore capability
- Optimistic locking with `@Version`

---

## Data Flow

### Example: Creating a Feed Post with Image

```
1. Client → POST /events/{id}/posts (with media details)
   ↓
2. FeedPostController → FeedPostService.create()
   ↓
3. EventAccessControlService.requireMediaUpload() ✓
   ↓
4. PresignedUploadService.generatePresignedUpload()
   ↓
5. EventFeedPost created (status=PENDING)
   ↓
6. Client ← Presigned URL + PostID
   ↓
7. Client → S3 (direct upload)
   ↓
8. Client → POST /events/{id}/posts/{postId}/media/{mediaId}/complete
   ↓
9. FeedPostService.completeMediaUpload()
   ↓
10. EventStoredObject created
    ↓
11. EventFeedPost status → COMPLETED
    ↓
12. Client ← FeedPostResponse (with engagement data)
```

### Example: User Following Another User & Seeing Posts

```
1. User A → POST /users/{userB}/follow
   ↓
2. UserFollowService.followUser()
   ↓
3. UserFollow created (follower=A, followee=B, status=ACTIVE)
   ↓
4. [Later] User B creates post on Event X
   ↓
5. User A → GET /events/{eventX}/posts
   ↓
6. FeedPostService enriches with social context:
   - isFollowing=true (A follows B)
   - isMutual (if B also follows A)
   ↓
7. Client ← Posts with follow status highlighted
```

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **ORM**: Hibernate / Spring Data JPA
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Authentication**: AWS Cognito (JWT)
- **Authorization**: Custom RBAC with Casbin integration
- **Storage**: AWS S3 (via presigned URLs)
- **Caching**: Redis (Lettuce client)
- **Messaging**: RabbitMQ (for async notifications)

### API
- **REST**: Spring MVC
- **Documentation**: OpenAPI 3.0 / Springdoc
- **Validation**: Jakarta Validation (JSR-380)
- **Error Handling**: Zalando Problem (RFC 7807)

### Data
- **Primary Keys**: UUID (gen_random_uuid())
- **Timestamps**: Automatic via `@CreationTimestamp`, `@UpdateTimestamp`
- **Soft Delete**: `deleted_at` column with `@SQLRestriction`
- **Optimistic Locking**: `@Version` on all entities

### Security
- **Authentication**: JWT tokens from AWS Cognito
- **User Provisioning**: Auto-create users on first JWT validation
- **RBAC**: Policy-based permissions (Casbin CSV policies)
- **API Gateway**: Kong (API key validation)
- **Data Sanitization**: ResponseSanitizer for error messages

---

## Module Dependency Graph

```
┌─────────────┐
│   Common    │ ← Base infrastructure (Storage, RBAC, Notifications)
└──────┬──────┘
       │
   ┌───▼───────────────────────────────┐
   │                                   │
┌──▼───┐  ┌────────┐  ┌──────┐  ┌────▼────┐
│Events│  │Tickets │  │Feeds │  │  Social │
└──┬───┘  └───┬────┘  └──┬───┘  └────┬────┘
   │          │           │           │
   ▼          ▼           ▼           ▼
┌──────────────────────────────────────┐
│        Event-Centric Features        │
│  Attendees, Collaboration, Budget,   │
│  Timeline, Waitlist                  │
└──────────────────────────────────────┘
```

**Key Relationships**:
- `Event` is the central entity
- `Ticket` and `Attendee` are tightly coupled to events
- `Feeds` provides social content within events
- `Social` connects users across events
- `Common` provides infrastructure for all features

---

## Best Practices & Patterns

### 1. **No Circular Dependencies**
- DTOs use minimal nested objects
- Use UUIDs instead of full entities in responses when appropriate
- Lazy loading for relationships

### 2. **Service Layer**
- Business logic in services, not controllers
- Services are transactional (`@Transactional`)
- Controllers are thin, focused on HTTP concerns

### 3. **Database Optimization**
- Indexes on foreign keys
- Denormalized counts for performance (`likeCount`, `commentCount`, `repostCount`)
- Batch loading to avoid N+1 queries

### 4. **Error Handling**
- Domain-specific exceptions (`BadRequestException`, `ResourceNotFoundException`)
- Centralized exception handler (`GlobalExceptionHandler`)
- Message sanitization to prevent information leakage

### 5. **API Design**
- RESTful resource naming
- Pagination for list endpoints (Page<T>)
- Versioned APIs (`/api/v1/...`)
- Consistent HTTP status codes

---

## Future Enhancements

### Planned Features
1. **Home Timeline Feed** - Aggregated posts from followed users and events
2. **Recommendation Engine** - Event suggestions based on social graph
3. **Notification System** - Mentions, engagement alerts
4. **Content Moderation** - Reporting, flagging, admin queue
5. **Analytics Dashboard** - Event metrics, engagement stats

### Technical Improvements
1. **Replace IllegalArgumentException** - Use domain exceptions everywhere
2. **API Rate Limiting** - Per-user, per-endpoint limits
3. **Caching Strategy** - Redis for feed posts, follow relationships
4. **Event Sourcing** - Audit trail for sensitive operations
5. **GraphQL API** - Alternative to REST for complex queries

---

## Conclusion

The Sade Event Planner architecture balances modularity with cohesion. Features are clearly bounded but share common infrastructure, ensuring consistency and reusability. The event-centric design makes relationships intuitive, and the Twitter-like social features enable vibrant community engagement around events.

**Key Strengths**:
✅ Clear module boundaries
✅ Consistent patterns across features
✅ Reusable infrastructure (storage, auth, notifications)
✅ Strong type safety with domain-specific DTOs
✅ Comprehensive access control
✅ Scalable data model with optimizations

This architecture supports both current feature requirements and future growth while maintaining code quality and developer productivity.
