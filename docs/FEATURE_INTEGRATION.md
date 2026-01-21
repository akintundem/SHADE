# Feature Integration Guide

## Overview

This guide explains how features in the Sade Event Planner integrate with each other, ensuring consistency, reusability, and avoiding duplication.

---

## Integration Matrix

| Feature → Uses ↓ | Events | Tickets | Attendees | Feeds | Social | Storage | Notifications | RBAC |
|------------------|--------|---------|-----------|-------|--------|---------|---------------|------|
| **Events**       | -      | ✓       | ✓         | ✓     | ✓      | ✓       | ✓             | ✓    |
| **Tickets**      | ✓      | -       | ✓         | -     | -      | ✓       | ✓             | ✓    |
| **Attendees**    | ✓      | ✓       | -         | -     | -      | -       | ✓             | ✓    |
| **Feeds**        | ✓      | -       | -         | -     | ✓      | ✓       | ✓             | ✓    |
| **Social**       | ✓      | -       | -         | ✓     | -      | -       | -             | ✓    |

---

## 1. Events ↔ Tickets Integration

### Relationship
- **One-to-Many**: One event has many ticket types
- **One-to-Many**: One ticket type has many tickets

### Access Pattern
```java
// From Events → Tickets
Event event = eventRepository.findById(eventId);
List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);

// From Tickets → Events
Ticket ticket = ticketRepository.findById(ticketId);
Event event = ticket.getEvent(); // Lazy loaded
```

### Business Rules
- **Ticketed Events**: `event.eventType == EventType.TICKETED`
- **Access Control**: Only ticket holders can access ticketed events
- **Capacity**: Event capacity is sum of ticket type capacities

### Integration Points
| Feature | Implementation | Location |
|---------|---------------|----------|
| Event Access Control | Checks if user has valid ticket | `EventAccessControlService.hasValidTicket()` |
| Event Capacity | Aggregates ticket type capacities | `EventResponse.ticketTypes[]` |
| Ticket Validation | Links to event for check-in | `TicketService.validateTicket()` |

---

## 2. Events ↔ Attendees Integration

### Relationship
- **One-to-Many**: One event has many attendees
- **Many-to-One**: Attendee optionally has one ticket

### Access Pattern
```java
// Get attendees for event
List<Attendee> attendees = attendeeRepository.findByEventId(eventId);

// Get attendee with ticket
Attendee attendee = attendeeRepository.findById(attendeeId);
Optional<Ticket> ticket = Optional.ofNullable(attendee.getTicket());
```

### Business Rules
- **RSVP Events**: `event.eventType == EventType.RSVP_REQUIRED`
- **Ticketed Events**: Attendees automatically created when ticket issued
- **Access Control**: Must be attendee or have ticket to access

### Integration Points
| Feature | Implementation | Location |
|---------|---------------|----------|
| RSVP Access | Checks attendee RSVP status | `EventAccessControlService.hasRsvp()` |
| Ticket Assignment | Links ticket to attendee | `TicketService.issueTicket()` |
| Guest List | Attendees + ticket holders | `AttendeeService.list()` |

---

## 3. Events ↔ Feeds Integration

### Relationship
- **One-to-Many**: One event has many feed posts
- **Foreign Key**: `event_posts.event_id → events.id`

### Access Pattern
```java
// Get posts for event
List<EventFeedPost> posts = feedPostRepository
    .findByEventIdAndMediaUploadStatusOrderByCreatedAtDesc(eventId, COMPLETED, pageable);

// Check event access before viewing posts
accessControlService.requireMediaView(principal, eventId);
```

### Business Rules
- **Access Control**: Respects event type (OPEN, TICKETED, etc.)
- **Post-Event Public**: `feedsPublicAfterEvent` makes posts public after event ends
- **Archived Events**: Posts hidden for archived events

### Integration Points
| Feature | Implementation | Location |
|---------|---------------|----------|
| Post Access | Uses event access control | `FeedPostService.list()` |
| Media Upload | Shares S3 presigned upload service | `FeedPostService.create()` |
| Event Timeline | Aggregates posts from event | `EventService.buildEventFeed()` |

**Example Flow**:
```
1. User creates post on event
   ↓
2. EventAccessControlService checks if user has access
   ↓
3. For TICKETED event: Check hasValidTicket()
4. For RSVP_REQUIRED: Check hasRsvp()
5. For OPEN: Allow all
   ↓
6. Post created and visible to authorized users
```

---

## 4. Feeds ↔ Social Integration

### Relationship
- **Engagement**: Posts can be liked, commented, reposted
- **Social Context**: Posts enriched with follow status

### Access Pattern
```java
// Get post with social context
FeedPostResponse post = feedPostService.get(eventId, postId, principal);
// post.originalPost contains repost details
// post.isLiked indicates if current user liked it

// Repost functionality
FeedPostResponse repost = feedPostService.repost(eventId, postId, principal, quoteText);
```

### Integration Points
| Feature | Implementation | Location |
|---------|---------------|----------|
| Reposts | Posts reference original posts | `EventFeedPost.repostedFrom` |
| Quote Posts | Repost with additional commentary | `EventFeedPost.quoteText` |
| Like Notifications | Notify post author | `PostLikeService.likePost()` |
| Comment Notifications | Notify post author | `PostCommentService.createComment()` |

---

## 5. Events ↔ Social Integration

### Relationship
- **Event Subscriptions**: Users subscribe to events for timeline updates
- **Foreign Key**: `event_subscriptions.event_id → events.id`

### Access Pattern
```java
// Subscribe to event
EventSubscriptionResponse sub = eventSubscriptionService
    .subscribeToEvent(eventId, principal, request);

// Get subscribed events for user
List<UUID> eventIds = eventSubscriptionRepository.findEventIdsByUserId(userId);
```

### Business Rules
- **Independent of Attendance**: Can subscribe without ticket/RSVP
- **Subscription Types**: FOLLOW (timeline), NOTIFY (push), BOTH
- **Timeline Feed**: Subscribed events appear in user's timeline

### Integration Points
| Feature | Implementation | Location |
|---------|---------------|----------|
| Event Discovery | Subscribe to follow event updates | `EventSubscriptionController` |
| Timeline Aggregation | Posts from subscribed events | Future: Timeline feed service |
| Notifications | Push updates for subscribed events | `subscriptionType == NOTIFY \|\| BOTH` |

---

## Common Infrastructure Integration

### 1. **Storage Service** (`S3StorageService` + `PresignedUploadService`)

**Used By**: Events (cover images), Feeds (post media), Tickets (QR codes), Users (profile pictures)

**Pattern**:
```java
// Step 1: Generate presigned upload URL
PresignedUploadRequest request = new PresignedUploadRequest();
request.setFileName(fileName);
request.setContentType(contentType);
request.setCategory("post"); // or "event", "ticket", "user"

PresignedUploadResponse response = presignedUploadService.generatePresignedUpload(
    request,
    mediaId -> buildObjectKey(entityId, mediaId),
    BucketAlias.EVENT, // or USER
    Duration.ofMinutes(10)
);

// Step 2: Client uploads to S3 using response.uploadUrl

// Step 3: Complete upload
presignedUploadService.completeUpload(
    entityId, mediaId, purpose, completeRequest, objectKey, bucket, principal, callback
);
```

**Reusability Benefits**:
- ✅ Single presigned URL generation logic
- ✅ Centralized media metadata tracking (`event_stored_objects`)
- ✅ Consistent upload/download flow
- ✅ Automatic cleanup of incomplete uploads

**No Duplication**: The feeds `PresignedUploadResponse` was removed in favor of `common.storage.s3.dto.PresignedUploadResponse`.

---

### 2. **Access Control** (`EventAccessControlService`)

**Used By**: Events, Feeds, Tickets, Attendees, Collaboration

**Pattern**:
```java
// Media view (read posts, see content)
accessControlService.requireMediaView(principal, eventId);

// Media upload (create posts)
accessControlService.requireMediaUpload(principal, eventId);

// Media manage (delete others' posts)
accessControlService.requireMediaManage(principal, eventId);
```

**Rules by Event Type**:
| Event Type | Who Can View | Who Can Upload |
|------------|--------------|----------------|
| OPEN | Anyone | Anyone |
| RSVP_REQUIRED | Attendees + Collaborators | Same |
| INVITE_ONLY | Invited + Collaborators | Same |
| TICKETED | Ticket holders + Collaborators | Same |

**Post-Event**: If `feedsPublicAfterEvent == true`, posts become public after event ends.

---

### 3. **Notifications** (`NotificationService`)

**Used By**: Tickets, Attendees, Feeds, Events

**Pattern**:
```java
NotificationRequest notification = NotificationRequest.builder()
    .type(CommunicationType.PUSH_NOTIFICATION) // or EMAIL
    .to(userId.toString())
    .subject("Subject line")
    .templateVariables(Map.of(
        "body", "Message content",
        "eventId", eventId.toString(),
        "actionUrl", "https://app.com/events/" + eventId
    ))
    .eventId(eventId)
    .build();

notificationService.send(notification);
```

**Use Cases**:
- **Tickets**: Ticket delivery email with QR code
- **Attendees**: RSVP confirmation, event reminders
- **Feeds**: Comment/like notifications to post author
- **Events**: Event update notifications to subscribers

**Integration with RabbitMQ**: Notifications sent asynchronously via message queue.

---

### 4. **RBAC** (`@RequiresPermission` + Casbin)

**Used By**: All feature controllers

**Pattern**:
```java
@PostMapping("/{id}/posts")
@RequiresPermission(value = RbacPermissions.EVENT_UPDATE, resources = {"event_id=#id"})
public ResponseEntity<CreateFeedPostResponse> create(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody FeedPostCreateRequest request
) {
    return ResponseEntity.ok(feedPostService.create(id, principal, request));
}
```

**Permissions Hierarchy**:
- `EVENT_READ`: View event details
- `EVENT_UPDATE`: Modify event, create posts
- `EVENT_DELETE`: Delete event
- `TICKET_MANAGE`: Manage tickets
- `ATTENDEE_MANAGE`: Manage guest list

**Resource-Based**: Permissions checked against specific resources (`event_id`, `ticket_id`, etc.)

---

## Anti-Patterns to Avoid

### ❌ Circular Dependencies
**Problem**: FeatureA depends on FeatureB, FeatureB depends on FeatureA.

**Solution**: Use common infrastructure or events/callbacks.

**Example**: Instead of Tickets depending on Attendees AND Attendees depending on Tickets, both depend on Events, and Tickets optionally reference Attendees.

---

### ❌ Duplicate DTOs
**Problem**: Multiple features define same response DTO.

**Solution**: Use common DTOs when structure is identical, feature-specific when semantics differ.

**Fixed**: `feeds.PresignedUploadResponse` removed, now uses `common.storage.s3.dto.PresignedUploadResponse`.

**Keep Separate**: `FeedPostResponse` vs `EventResponse` - different domains, different fields.

---

### ❌ Bypassing Access Control
**Problem**: Direct database access without permission checks.

**Solution**: Always use `EventAccessControlService` before accessing event-related data.

**Correct Pattern**:
```java
// Always check access first
accessControlService.requireMediaView(principal, eventId);

// Then retrieve data
List<FeedPostResponse> posts = feedPostService.list(eventId, principal);
```

---

### ❌ Hardcoded Business Logic in Controllers
**Problem**: Controllers contain business rules.

**Solution**: Keep controllers thin, move logic to services.

**Correct Pattern**:
```java
// ❌ Bad: Logic in controller
if (event.getEventType() == EventType.TICKETED && !hasTicket) {
    throw new ForbiddenException();
}

// ✓ Good: Logic in service
accessControlService.requireMediaView(principal, eventId);
```

---

## Best Practices

### 1. **Consistent Error Handling**
All features use domain exceptions:
- `BadRequestException` - Invalid input
- `ResourceNotFoundException` - Entity not found
- `ForbiddenException` - Access denied
- `UnauthorizedException` - Not authenticated
- `ConflictException` - State conflict

### 2. **Pagination Everywhere**
All list endpoints return `Page<T>` with metadata:
```java
Page<FeedPostResponse> posts = feedPostService.listPaginated(eventId, principal, page, size);
```

### 3. **Lazy Loading + Batch Loading**
- Relationships use `FetchType.LAZY` to avoid N+1 queries
- Batch load engagement data (likes, comments) in service layer

### 4. **Denormalize Counts**
Store counts for fast reads:
- `ticket_types.sold_count`, `reserved_count`
- `event_posts.repost_count`
- Compute `likeCount`, `commentCount` on read (not stored, but batched)

### 5. **Soft Deletes**
All entities extend `BaseEntity` with `deleted_at`:
- Never hard delete user data
- Allow restore functionality
- Maintain audit trail

---

## Feature Addition Checklist

When adding a new feature:

- [ ] **Entity**: Extend `BaseEntity`, use UUID primary keys
- [ ] **Repository**: Extend `JpaRepository`, use `@Query` for complex queries
- [ ] **Service**: Add `@Service`, `@Transactional`, business logic here
- [ ] **Controller**: Thin layer, use `@RestController`, `@RequiresPermission`
- [ ] **DTOs**: Separate request/response, use `@Schema` for API docs
- [ ] **Access Control**: Integrate with `EventAccessControlService` or RBAC
- [ ] **Storage**: Use `PresignedUploadService` for media uploads
- [ ] **Notifications**: Use `NotificationService` for push/email
- [ ] **Migrations**: Add Flyway migration (V{X}__{description}.sql)
- [ ] **Tests**: Unit tests for services, integration tests for controllers
- [ ] **Documentation**: Update ARCHITECTURE.md, DATABASE_SCHEMA.md

---

## Summary

The Sade Event Planner features are designed to work together seamlessly:

✅ **No Duplication**: Shared infrastructure (storage, auth, notifications)
✅ **Consistent Patterns**: DTOs, services, controllers follow standards
✅ **Proper Boundaries**: Features interact via well-defined APIs
✅ **Reusability**: Common services used across features
✅ **Access Control**: Centralized permission checks
✅ **Performance**: Denormalized counts, indexed queries, lazy loading

Features integrate through:
1. **Foreign Keys**: Events ↔ Tickets, Attendees, Feeds, Subscriptions
2. **Shared Services**: Storage, Access Control, Notifications, RBAC
3. **DTOs**: Consistent request/response patterns
4. **Business Rules**: Enforced in service layer, not controllers

This architecture ensures scalability, maintainability, and developer productivity as the platform grows.
