# Sade Event Planner - System Architecture

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Feature Modules](#feature-modules)
4. [Integration Patterns](#integration-patterns)
5. [Data Flow](#data-flow)
6. [Technology Stack](#technology-stack)
7. [Deployment Architecture](#deployment-architecture)

---

## Overview

Sade Event Planner is a comprehensive event management platform built as a Spring Boot monolith with clear module boundaries. The system manages the complete event lifecycle including planning, ticketing, attendee management, social engagement, budget tracking, and timeline management.

### Core Principles
- **Modularity**: Features organized in bounded contexts
- **Reusability**: Shared infrastructure across features
- **Consistency**: Standardized patterns for DTOs, services, repositories
- **Security**: RBAC-based access control throughout
- **Scalability**: Denormalized counts, indexed queries, lazy loading
- **Microservices**: Email, push notifications, and AI services as separate services

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kong API Gateway (Port 8000)                  │
│                    - API Key Validation                          │
│                    - CORS Handling                               │
│                    - Request Routing                             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                    │
        ▼                                    ▼
┌──────────────────┐              ┌──────────────────┐
│   /api/v1/*      │              │   /ai-service/*  │
│  Spring Boot     │              │   AI Service     │
│   Monolith       │              │   (Python)       │
└────────┬─────────┘              └──────────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                    REST API Layer                               │
│              (Spring MVC Controllers)                           │
└────────┬───────────────────────────────────────────────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                    Service Layer                                │
│  ┌──────────┐  ┌──────────┐  ┌────────┐  ┌────────┐  ┌───────┐│
│  │  Events   │  │ Tickets  │  │ Feeds  │  │ Social │  │ Budget││
│  └──────────┘  └──────────┘  └────────┘  └────────┘  └───────┘│
│  ┌──────────┐  ┌──────────┐  ┌────────┐  ┌────────┐           │
│  │ Attendees│  │ Timeline │  │Collabor│  │  Auth  │           │
│  └──────────┘  └──────────┘  └────────┘  └────────┘           │
└────────┬───────────────────────────────────────────────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                 Common Infrastructure                           │
│  ┌─────────────┐  ┌───────────┐  ┌──────────────┐  ┌─────────┐│
│  │  Storage    │  │   RBAC    │  │ Notification │  │  Auth   ││
│  │  (S3)       │  │           │  │  (RabbitMQ)  │  │(Cognito) ││
│  └─────────────┘  └───────────┘  └──────────────┘  └─────────┘│
└────────┬───────────────────────────────────────────────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                  Data Access Layer                              │
│           (Spring Data JPA Repositories)                       │
└────────┬───────────────────────────────────────────────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                  PostgreSQL Database                            │
│               (with Flyway Migrations)                          │
└────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    External Services                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Email Service│  │ Push Service │  │  AI Service  │          │
│  │  (Node.js)   │  │  (Node.js)   │  │  (Python)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                 │                 │                    │
│         └─────────────────┴─────────────────┘                    │
│                           │                                      │
│                    RabbitMQ Message Broker                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Feature Modules

### 1. **Events Module** (`eventplanner.features.event`)

**Purpose**: Core event management - creating, updating, and organizing events.

**Key Entities**:
- `Event` - Main event entity with metadata, dates, location, access control
- `EventSeries` - Recurring event series management
- `EventWaitlistEntry` - Event-level waitlist
- `EventStoredObject` - Media and file storage references
- `EventNotificationSettings` - Notification preferences
- `EventReminder` - Scheduled reminders
- `EventRole` - Role-based access control for events

**Key Services**:
- `EventService` - CRUD operations, cloning, status automation
- `EventAccessControlService` - Permission checks based on event access type
- `EventSeriesService` - Recurring event management
- `EventWaitlistService` - Waitlist promotion

**Integration Points**:
- **Tickets**: Events have multiple ticket types
- **Attendees**: Events have attendees via RSVP or tickets
- **Feeds**: Events have feed posts for social engagement
- **Social**: Users can subscribe to events
- **Collaboration**: Events have collaborators with roles
- **Budget**: One budget per event
- **Timeline**: Tasks and checklists for event planning

**Access Control Types**:
- `OPEN`: Anyone can view and RSVP
- `RSVP_REQUIRED`: Users must RSVP to access content
- `INVITE_ONLY`: Only invited users can see/access
- `TICKETED`: Users must purchase a ticket to access

---

### 2. **Tickets Module** (`eventplanner.features.ticket`)

**Purpose**: Ticket sales, management, validation, and distribution.

**Key Entities**:
- `Ticket` - Individual ticket instance
- `TicketType` - Template for ticket categories (VIP, GA, etc.)
- `TicketPriceTier` - Time-based or quantity-based pricing
- `TicketCheckout` - Cart/checkout session
- `TicketCheckoutItem` - Items in checkout cart
- `TicketWaitlistEntry` - Ticket-type specific waitlist
- `TicketApprovalRequest` - Approval workflow for restricted tickets
- `TicketTypeTemplate` - Reusable ticket type configurations
- `TicketTypeDependency` - Ticket type prerequisites
- `TicketPromotion` - Promotional campaigns

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
- **Budget**: Ticket sales tracked as revenue

**Business Rules**:
- Dynamic pricing based on tiers
- Capacity management with sold/reserved tracking
- Transferable tickets with audit trail
- QR code validation at check-in
- Group discounts and promotional pricing

---

### 3. **Attendees Module** (`eventplanner.features.attendee`)

**Purpose**: RSVP management, guest lists, and attendee invitations.

**Key Entities**:
- `Attendee` - Person attending an event
- `AttendeeInvite` - Invitation to RSVP for event
- `AttendeeRsvpHistory` - Audit trail of RSVP changes

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
- Check-in tracking

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
- Media upload status tracking (PENDING, COMPLETED)

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

**Purpose**: Multi-user event management with roles and granular permissions.

**Key Entities**:
- `EventUser` - User with specific role on event
- `EventUserPermission` - Granular permission overrides
- `EventCollaboratorInvite` - Invitation to collaborate

**Roles** (via `EventUserType`):
- `OWNER` - Full control
- `CO_HOST` - Manage most aspects
- `ORGANIZER` - Event details and logistics
- `MEDIA_MANAGER` - Media and marketing
- `TICKETING_MANAGER` - Ticket operations
- `STAFF` - Event staff members
- `VOLUNTEER` - Volunteer workers

**Permissions**:
- `VIEW_EVENT` - View event details
- `EDIT_EVENT_DETAILS` - Modify event information
- `MANAGE_COLLABORATORS` - Add/remove collaborators
- `MANAGE_INVITES` - Manage attendee invitations
- `MANAGE_SCHEDULE` - Update timeline and tasks
- `MANAGE_BUDGET` - Budget management
- `MANAGE_TICKETS` - Ticket operations
- `MANAGE_CONTENT` - Feed posts and media

**Integration Points**:
- **Events**: Collaborators belong to events
- **RBAC**: Roles define permissions
- **Timeline**: Task assignment to collaborators
- **Budget**: Budget access control

---

### 7. **Budget Module** (`eventplanner.features.budget`)

**Purpose**: Event budget planning, expense tracking, and revenue management.

**Key Entities**:
- `Budget` - Overall event budget
- `BudgetCategory` - Categorized line items
- `BudgetLineItem` - Individual expenses/revenue items

**Key Services**:
- `BudgetService` - Budget CRUD, calculations
- `BudgetSyncService` - Real-time sync with ticket sales

**Features**:
- Real-time totals (revenue, expenses, profit)
- Auto-save for line items
- Budget variance tracking
- Revenue tracking from ticket sales
- Projected revenue calculations
- Net position (revenue - expenses)
- Task linkage (line items can reference timeline tasks)

**Integration Points**:
- **Events**: One budget per event
- **Tickets**: Revenue automatically synced from ticket sales
- **Timeline**: Line items can link to tasks

---

### 8. **Timeline Module** (`eventplanner.features.timeline`)

**Purpose**: Event planning checklists and task management.

**Key Entities**:
- `Task` - High-level planning tasks
- `Checklist` - Subtasks/checklist items within tasks

**Key Services**:
- `TimelineTaskService` - Task and checklist management

**Features**:
- Task completion tracking
- Auto-save functionality for drafts
- Deadline management
- Task ordering and prioritization
- Progress percentage tracking
- Subtask completion counts
- Task assignment to collaborators

**Integration Points**:
- **Events**: Tasks belong to events
- **Budget**: Tasks can be linked to budget line items
- **Collaboration**: Tasks can be assigned to collaborators

---

### 9. **Security & Authentication Module** (`eventplanner.security`)

**Purpose**: User authentication, authorization, and access control.

**Key Entities**:
- `UserAccount` - User identity and profile
- `UserSettings` - User preferences and settings
- `Location` - Geographic location data

**Key Services**:
- `CognitoUserService` - AWS Cognito integration
- `UserAccountService` - User profile management
- `AuthorizationService` - Permission checking
- `RbacAuthorizationService` - Role-based access control

**Authentication**:
- AWS Cognito JWT token validation
- Auto-provisioning of users on first login
- Session management

**Authorization**:
- RBAC (Role-Based Access Control) with Casbin
- Permission-based access control
- Resource-level permissions
- Event-level access control

**Integration Points**:
- **All Features**: User context and permissions
- **Events**: Owner and collaborator access
- **Feeds**: Post creation permissions

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

Centralized `NotificationService` for email and push notifications via RabbitMQ:

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

**Architecture**:
- Spring Boot publishes jobs to RabbitMQ
- Email Service (Node.js) consumes email jobs
- Push Service (Node.js) consumes push notification jobs
- Async processing for scalability

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
1. Client → POST /api/v1/events/{id}/posts (with media details)
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
8. Client → POST /api/v1/events/{id}/posts/{postId}/media/{mediaId}/complete
   ↓
9. FeedPostService.completeMediaUpload()
   ↓
10. EventStoredObject created
    ↓
11. EventFeedPost status → COMPLETED
    ↓
12. Client ← FeedPostResponse (with engagement data)
```

### Example: Ticket Purchase Flow

```
1. Client → POST /api/v1/tickets/checkout (add items to cart)
   ↓
2. TicketCheckoutService.createCheckout()
   ↓
3. TicketCheckout created (status=PENDING, expires_at set)
   ↓
4. Client → POST /api/v1/tickets/checkout/{checkoutId}/complete (payment)
   ↓
5. TicketCheckoutService.completeCheckout()
   ↓
6. Tickets created (status=ISSUED)
   ↓
7. QR codes generated and stored in S3
   ↓
8. Budget.totalRevenue updated via BudgetSyncService
   ↓
9. Email notification sent via RabbitMQ
   ↓
10. Client ← TicketResponse (with QR code URLs)
```

### Example: User Following Another User & Seeing Posts

```
1. User A → POST /api/v1/users/{userB}/follow
   ↓
2. UserFollowService.followUser()
   ↓
3. UserFollow created (follower=A, followee=B, status=ACTIVE)
   ↓
4. [Later] User B creates post on Event X
   ↓
5. User A → GET /api/v1/events/{eventX}/posts
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
- **Framework**: Spring Boot 3.3.0
- **Language**: Java 17+
- **ORM**: Hibernate / Spring Data JPA
- **Database**: PostgreSQL
- **Migrations**: Flyway 10.10.0
- **Authentication**: AWS Cognito (JWT)
- **Authorization**: Custom RBAC with Casbin integration
- **Storage**: AWS S3 (via presigned URLs)
- **Caching**: Redis (Lettuce client) / Caffeine
- **Messaging**: RabbitMQ (for async notifications)

### API
- **REST**: Spring MVC
- **Documentation**: OpenAPI 3.0 / Springdoc 2.5.0
- **Validation**: Jakarta Validation (JSR-380)
- **Error Handling**: Zalando Problem (RFC 7807)
- **Gateway**: Kong 3.7 (API key validation, CORS)

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

### External Services
- **Email Service**: Node.js with Resend API
- **Push Service**: Node.js with Firebase Cloud Messaging
- **AI Service**: Python (FastAPI) for AI-powered features
- **Payment**: Stripe integration (for ticket purchases)

---

## Deployment Architecture

### Docker Compose Services

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Network                            │
│              (event-planner-network)                          │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Email Service│  │ Push Service │  │  AI Service  │      │
│  │  (Node.js)   │  │  (Node.js)   │  │  (Python)    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │               │
│         └─────────────────┴─────────────────┘               │
│                           │                                 │
│                    ┌──────▼──────┐                          │
│                    │  RabbitMQ   │                          │
│                    │  (Port 5672)│                          │
│                    └─────────────┘                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │     Java Spring Boot Application (Port 8080)        │   │
│  │  - Hot reload enabled (Spring DevTools)             │   │
│  │  - Source code mounted for live updates             │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │     Kong API Gateway (Port 8000, 8443)               │   │
│  │  - Routes /api/* to Spring Boot                      │   │
│  │  - Routes /ai-service/* to AI Service                │   │
│  │  - API key validation                                │   │
│  │  - CORS handling                                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ (host.docker.internal)
                           │
        ┌──────────────────┴──────────────────┐
        │                                    │
        ▼                                    ▼
┌──────────────┐                    ┌──────────────┐
│  PostgreSQL  │                    │    Redis     │
│  (External)  │                    │  (External)  │
└──────────────┘                    └──────────────┘
```

### Service Communication

- **Spring Boot ↔ Email/Push Services**: RabbitMQ message queues
- **Spring Boot ↔ AI Service**: HTTP via Kong gateway
- **Spring Boot ↔ PostgreSQL**: Direct JDBC connection
- **Spring Boot ↔ Redis**: Direct connection
- **Spring Boot ↔ S3**: AWS SDK (presigned URLs)
- **Spring Boot ↔ Cognito**: AWS SDK (JWT validation)

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
- `Security` provides authentication and authorization

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
- Denormalized counts for performance (`likeCount`, `commentCount`, `repostCount`, `soldCount`)
- Batch loading to avoid N+1 queries
- Composite indexes for common query patterns

### 4. **Error Handling**
- Domain-specific exceptions (`BadRequestException`, `ResourceNotFoundException`)
- Centralized exception handler (`GlobalExceptionHandler`)
- Message sanitization to prevent information leakage
- RFC 7807 Problem Details format

### 5. **API Design**
- RESTful resource naming
- Pagination for list endpoints (Page<T>)
- Versioned APIs (`/api/v1/...`)
- Consistent HTTP status codes
- OpenAPI documentation

### 6. **Security**
- JWT validation on every request
- RBAC permission checks via `@RequiresPermission`
- Resource-level access control
- API key validation at gateway level

---

## Future Enhancements

### Planned Features
1. **Home Timeline Feed** - Aggregated posts from followed users and events
2. **Recommendation Engine** - Event suggestions based on social graph
3. **Enhanced Notification System** - Mentions, engagement alerts
4. **Content Moderation** - Reporting, flagging, admin queue
5. **Analytics Dashboard** - Event metrics, engagement stats
6. **Mobile App** - Native iOS/Android applications

### Technical Improvements
1. **API Rate Limiting** - Per-user, per-endpoint limits
2. **Enhanced Caching Strategy** - Redis for feed posts, follow relationships
3. **Event Sourcing** - Audit trail for sensitive operations
4. **GraphQL API** - Alternative to REST for complex queries
5. **WebSocket Support** - Real-time updates for feeds and notifications
6. **Microservices Migration** - Split monolith into domain services

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
✅ Microservices for async operations (email, push, AI)
✅ API Gateway for unified entry point

This architecture supports both current feature requirements and future growth while maintaining code quality and developer productivity.
