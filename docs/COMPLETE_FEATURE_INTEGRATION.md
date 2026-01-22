# Complete Feature Integration Guide

## System Overview

The Sade Event Planner is a comprehensive event management platform with 8 integrated feature modules working together to provide end-to-end event planning, execution, and social engagement.

---

## Feature Ecosystem

```
┌─────────────────────────────────────────────────────────────┐
│                       EVENT CORE                            │
│  Central hub - Event creation, management, cloning          │
└──────────────┬──────────────────────────────────────────────┘
               │
     ┌─────────┼─────────┬──────────────┬──────────────┐
     │         │         │              │              │
┌────▼───┐ ┌──▼───┐  ┌──▼──────┐  ┌────▼──────┐  ┌───▼────┐
│TICKETS │ │BUDGET│  │TIMELINE │  │ATTENDEES  │  │ FEEDS  │
│Revenue │ │Track │  │Planning │  │Management │  │Social  │
│        │ │      │  │Tasks    │  │RSVPs      │  │Content │
└────┬───┘ └──┬───┘  └──┬──────┘  └────┬──────┘  └───┬────┘
     │        │         │              │              │
     └────────┼─────────┼──────────────┼──────────────┘
              │         │              │
         ┌────▼─────────▼──────────────▼───┐
         │     COLLABORATION                │
         │   Team + Permissions             │
         └──────────────┬───────────────────┘
                        │
                  ┌─────▼──────┐
                  │  SOCIAL    │
                  │  Follows   │
                  │  Subscribe │
                  └────────────┘
```

---

## Feature Integration Matrix

| Feature | Integrates With | How They Connect |
|---------|----------------|------------------|
| **Events** | All features | Central entity referenced by all modules |
| **Tickets** | Events, Attendees, Budget | Creates attendees, generates revenue |
| **Budget** | Events, Tickets, Timeline, Collaboration | Tracks expenses, ticket revenue, task costs |
| **Timeline** | Events, Collaboration, Budget | Tasks assigned to collaborators, linked to expenses |
| **Attendees** | Events, Tickets, Feeds | RSVP management, auto-created from tickets |
| **Feeds** | Events, Social, Collaboration | Event posts, access controlled by collaboration |
| **Collaboration** | All features | Permission-based access control for all operations |
| **Social** | Events, Feeds, Users | User follows, event subscriptions, engagement |

---

## 1. Events ↔ Tickets Integration

### Purpose
Ticketed events generate revenue and create attendees automatically.

### How It Works
```java
// When ticket is issued
1. Ticket created with price → Adds to budget revenue
2. Attendee created automatically → Linked to ticket
3. Access granted → User can view/participate in event
```

### Data Flow
```
Ticket Sale → Budget.totalRevenue += ticketPrice
           → Attendee created
           → Event access granted
```

### Key Methods
- `TicketService.issueTicket()` - Creates ticket and attendee
- `BudgetSyncService.syncTicketRevenue(eventId)` - Updates budget revenue
- `TicketRepository.sumActualRevenue(eventId)` - Calculates total revenue

### Revenue Tracking
```java
Budget budget = budgetRepository.findByEventId(eventId);
BigDecimal actualRevenue = ticketRepository.sumActualRevenue(eventId);
BigDecimal projectedRevenue = ticketTypeRepository.sumProjectedRevenue(eventId);

budget.setTotalRevenue(actualRevenue);
budget.setProjectedRevenue(projectedRevenue);
budget.setNetPosition(actualRevenue - totalExpenses);
```

---

## 2. Events ↔ Budget Integration

### Purpose
Track event finances: revenue from tickets, expenses from timeline tasks.

### Budget Structure
```
Budget (One per event)
├─ Revenue Side
│  ├─ totalRevenue (from sold tickets)
│  ├─ projectedRevenue (if all tickets sold)
│  └─ Auto-synced when tickets sold/refunded
│
└─ Expense Side
   ├─ Categories (Venue, Catering, Marketing, etc.)
   │  └─ Line Items (individual expenses)
   │     ├─ estimatedCost
   │     ├─ actualCost
   │     └─ taskId (linked to timeline)
   │
   └─ Totals
      ├─ totalEstimated
      ├─ totalActual
      └─ netPosition (revenue - expenses)
```

### Collaboration Integration
- `MANAGE_BUDGET` permission required to edit
- Event owner/admin always have access
- Collaborators can view if they have `VIEW_EVENT` permission

### Auto-Sync Triggers
1. **Ticket sold** → `BudgetSyncService.syncTicketRevenue()`
2. **Ticket refunded** → Revenue recalculated
3. **Line item added** → Budget totals recalculated
4. **Line item deleted** → Totals updated

---

## 3. Events ↔ Timeline Integration

### Purpose
Plan event execution with tasks, assign to collaborators, track costs.

### Structure
```
Task
├─ Basic Info: title, description, dates
├─ Assignment: assignedTo (must be collaborator)
├─ Progress: status, progressPercentage
├─ Checklists (subtasks)
└─ Budget Link: taskId in BudgetLineItem
```

### Collaboration Integration
- **View**: Requires `VIEW_EVENT` or `MANAGE_SCHEDULE` permission
- **Edit**: Requires `MANAGE_SCHEDULE` permission
- **Assign**: Can only assign to event collaborators

### Budget Integration
```java
// Link task to budget expense
BudgetLineItem expense = new BudgetLineItem();
expense.setTaskId(taskId);  // Links to timeline task
expense.setDescription("Catering for 100 guests");
expense.setEstimatedCost(new BigDecimal("5000.00"));
```

**Use Case**: Task "Book Venue" → Linked expense "$2,500" in budget

---

## 4. Events ↔ Collaboration Integration

### Purpose
Build teams with role-based permissions controlling all features.

### Permission Model
```
EventUser (Collaborator)
├─ Role: ORGANIZER, COLLABORATOR, MEDIA, etc.
└─ Permissions (optional overrides)
   ├─ VIEW_EVENT
   ├─ EDIT_EVENT_DETAILS
   ├─ MANAGE_COLLABORATORS
   ├─ MANAGE_SCHEDULE  → Controls timeline access
   ├─ MANAGE_BUDGET    → Controls budget access
   ├─ MANAGE_TICKETS   → Controls ticketing
   └─ MANAGE_CONTENT   → Controls feeds
```

### How Features Use Permissions

#### Timeline
```java
// Check schedule permission
EventUser membership = eventUserRepository.findByEventIdAndUserId(eventId, userId);
if (!permissionEvaluator.hasPermission(membership, EventPermission.MANAGE_SCHEDULE)) {
    throw new ForbiddenException();
}
```

#### Budget
```java
// Check budget permission
requireBudgetAccess(eventId, user, EventPermission.MANAGE_BUDGET);
```

#### Feeds
```java
// Check content permission
accessControlService.requireMediaUpload(principal, eventId);
```

### Event Owner
- Always has full access to all features
- Cannot be removed as collaborator
- Bypasses all permission checks

---

## 5. Events ↔ Attendees Integration

### Purpose
Manage event guest list, RSVPs, and access control.

### Attendee Sources
1. **Manual RSVP**: User RSVPs to RSVP_REQUIRED event
2. **Ticket Purchase**: Auto-created when ticket issued
3. **Invitation**: Invited by organizer

### Access Control
```java
// RSVP events - check attendee status
boolean hasRsvp = attendeeRepository.existsByEventIdAndUserId(eventId, userId);
if (event.eventType == RSVP_REQUIRED && !hasRsvp) {
    throw new ForbiddenException();
}

// Ticketed events - check ticket
boolean hasTicket = ticketRepository.hasValidTicketByUserId(eventId, userId);
if (event.eventType == TICKETED && !hasTicket) {
    throw new ForbiddenException();
}
```

### Collaboration Link
- Attendees ≠ Collaborators
- Attendees can access event content
- Collaborators can manage event
- Overlap allowed (collaborator can also attend)

---

## 6. Events ↔ Feeds Integration

### Purpose
Create event-specific social timeline for sharing experiences.

### Feed Structure
```
EventFeedPost
├─ Type: TEXT, IMAGE, VIDEO, REPOST
├─ Content: text, mediaUrl
├─ Social: likes, comments, reposts
└─ Access: Controlled by event type
```

### Access Control Integration
```java
// Respect event type
switch (event.eventType) {
    case OPEN:
        // Anyone can view/post
        break;
    case TICKETED:
        // Must have valid ticket
        if (!accessControlService.hasValidTicket(user, eventId)) {
            throw new ForbiddenException();
        }
        break;
    case RSVP_REQUIRED:
        // Must be attendee
        if (!accessControlService.hasRsvp(user, eventId)) {
            throw new ForbiddenException();
        }
        break;
    case INVITE_ONLY:
        // Must be invited attendee or collaborator
        break;
}
```

### Post-Event Visibility
```java
// Make feeds public after event ends
if (event.feedsPublicAfterEvent && event.endDateTime.isBefore(now)) {
    // Allow public access
}
```

---

## 7. Feeds ↔ Social Integration

### Purpose
Enable social engagement: likes, comments, reposts, follows.

### Social Features
```
Post Engagement
├─ Likes: PostLikeService
├─ Comments: PostCommentService
├─ Reposts: FeedPostService.repost()
└─ Quote Posts: Repost with commentary
```

### User Social Graph
```
UserFollow
├─ Follower → Followee relationship
├─ Status: ACTIVE, PENDING, BLOCKED
└─ Mutual follows detected
```

### Event Subscriptions
```
EventSubscription
├─ Type: FOLLOW, NOTIFY, BOTH
├─ FOLLOW: Posts appear in timeline
└─ NOTIFY: Push notifications
```

---

## 8. Collaboration ↔ All Features

### Centralized Permission Control

All features check collaboration permissions:

| Feature | Read Permission | Write Permission |
|---------|----------------|------------------|
| Events | `VIEW_EVENT` | `EDIT_EVENT_DETAILS` |
| Budget | `VIEW_EVENT` | `MANAGE_BUDGET` |
| Timeline | `VIEW_EVENT` or `MANAGE_SCHEDULE` | `MANAGE_SCHEDULE` |
| Tickets | `VIEW_EVENT` | `MANAGE_TICKETS` |
| Feeds | Event type access rules | `MANAGE_CONTENT` |
| Attendees | `VIEW_EVENT` | `EDIT_EVENT_DETAILS` |
| Collaborators | `VIEW_EVENT` | `MANAGE_COLLABORATORS` |

### Permission Evaluation
```java
// 1. Check if admin/owner (bypass)
if (authorizationService.isAdmin(user) || authorizationService.isEventOwner(user, eventId)) {
    return; // Full access
}

// 2. Check collaborator membership
EventUser membership = eventUserRepository.findByEventIdAndUserId(eventId, userId)
    .orElseThrow(() -> new ForbiddenException());

// 3. Evaluate permission
if (!permissionEvaluator.hasPermission(membership, requiredPermission)) {
    throw new ForbiddenException();
}
```

---

## Complete User Journey Examples

### Example 1: Conference Organizer

```
1. Create Event
   └─ Budget auto-created with standard categories

2. Build Team
   └─ Add collaborators: 2 organizers, 3 staff, 1 media manager

3. Create Timeline
   └─ Tasks: Book venue, arrange catering, setup AV
   └─ Assign tasks to staff members
   └─ Link expenses to budget

4. Setup Ticketing
   └─ Create ticket types: VIP ($500), GA ($100)
   └─ Budget auto-tracks projected revenue

5. Ticket Sales
   └─ Attendees auto-created
   └─ Budget revenue updates in real-time
   └─ Access granted to ticketed event

6. Event Day
   └─ Staff posts photos to event feed
   └─ Attendees engage (like, comment)
   └─ Media manager uploads professional photos

7. Post-Event
   └─ Feed becomes public
   └─ Final budget shows: Revenue $50k, Expenses $35k, Net $15k
```

### Example 2: Small Party Organizer

```
1. Create RSVP Event
   └─ Free, invite-only

2. Invite Friends
   └─ Send RSVP invitations
   └─ Friends RSVP → Become attendees

3. Plan with Timeline
   └─ Create tasks: Buy decorations, prepare playlist
   └─ Track costs in budget

4. Share Updates
   └─ Post photos before/during event
   └─ Only attendees can see/comment

5. After Party
   └─ Feeds stay private (feedsPublicAfterEvent = false)
```

---

## Database Integration Summary

### Foreign Key Relationships

```sql
-- Events as central hub
tickets.event_id → events.id
attendees.event_id → events.id
budgets.event_id → events.id  (one-to-one)
tasks.event_id → events.id
event_posts.event_id → events.id
event_users.event_id → events.id
event_subscriptions.event_id → events.id

-- Cross-feature links
tickets.attendee_id → attendees.id
budget_line_items.task_id → tasks.id  (optional link)
attendees.ticket_id → tickets.id  (optional)
event_users.user_id → auth_users.id

-- Social graph
user_follows.follower_id → auth_users.id
user_follows.followee_id → auth_users.id
event_posts.reposted_from_id → event_posts.id
```

---

## API Integration Patterns

### Pattern 1: Feature with Collaboration Check

```java
@PostMapping("/api/v1/events/{eventId}/timeline/tasks")
@RequiresPermission(value = RbacPermissions.TASK_CREATE, resources = {"event_id=#eventId"})
public ResponseEntity<TaskResponse> createTask(
    @PathVariable UUID eventId,
    @AuthenticationPrincipal UserPrincipal user,
    @RequestBody TaskRequest request
) {
    // Check collaboration permission
    requireTimelineManageAccess(user, eventId);

    // Validate assignee is collaborator
    if (request.getAssignedTo() != null) {
        validateCollaborator(eventId, request.getAssignedTo());
    }

    // Create task
    Task task = timelineService.createTask(eventId, request);

    return ResponseEntity.ok(toResponse(task));
}
```

### Pattern 2: Auto-Sync Across Features

```java
// When ticket is sold
@Transactional
public Ticket issueTicket(TicketCheckoutRequest request) {
    // 1. Create ticket
    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatus.ISSUED);
    ticketRepository.save(ticket);

    // 2. Create attendee
    Attendee attendee = attendeeService.createFromTicket(ticket);
    ticket.setAttendee(attendee);

    // 3. Update budget revenue
    budgetSyncService.syncTicketRevenue(ticket.getEvent().getId());

    // 4. Send notifications
    notificationService.sendTicketDelivery(ticket);

    return ticket;
}
```

### Pattern 3: Permission-Based Response Filtering

```java
public EventResponse getEvent(UUID eventId, UserPrincipal user) {
    Event event = eventRepository.findById(eventId).orElseThrow();

    // Base response
    EventResponse response = EventResponse.fromEntity(event);

    // Add sensitive data based on permissions
    if (hasCollaboratorAccess(user, eventId)) {
        response.setBudgetSummary(getBudgetSummary(eventId));
        response.setCollaborators(getCollaborators(eventId));
    }

    // Add ticket info if user is owner or has tickets
    if (isOwner(user, eventId) || hasTickets(user, eventId)) {
        response.setMyTickets(getUserTickets(user, eventId));
    }

    return response;
}
```

---

## Common Infrastructure

### 1. Storage (S3)
**Used By**: Events (covers), Feeds (media), Tickets (QR codes), Users (avatars)

```java
// Shared presigned upload pattern
PresignedUploadResponse upload = presignedUploadService.generatePresignedUpload(
    request,
    mediaId -> buildObjectKey(entityId, mediaId),
    BucketAlias.EVENT,
    Duration.ofMinutes(10)
);
```

### 2. Notifications
**Used By**: Tickets, Attendees, Feeds, Collaboration, Timeline

```java
NotificationRequest notification = NotificationRequest.builder()
    .type(CommunicationType.PUSH_NOTIFICATION)
    .to(userId.toString())
    .subject("You've been assigned a task")
    .templateVariables(Map.of("taskTitle", task.getTitle()))
    .eventId(eventId)
    .build();

notificationService.send(notification);
```

### 3. Access Control
**Used By**: All features

```java
// Centralized in EventAccessControlService
accessControlService.requireMediaView(principal, eventId);
accessControlService.requireMediaUpload(principal, eventId);
accessControlService.requireMediaManage(principal, eventId);
```

---

## Feature Capability Summary

### Events
✅ Create, update, clone events
✅ Event series and recurring events
✅ Multiple event types (OPEN, TICKETED, RSVP, INVITE_ONLY)
✅ Automatic status automation (DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED)

### Tickets
✅ Multiple ticket types per event
✅ Tiered pricing, early bird, promo codes
✅ Ticket dependencies (VIP requires GA)
✅ Waitlist management
✅ Approval workflows
✅ QR code generation
✅ Revenue auto-sync to budget

### Budget
✅ Revenue tracking from tickets
✅ Expense categorization (21 categories)
✅ Line items with estimated/actual costs
✅ Timeline task cost linking
✅ Contingency planning
✅ Net position calculation (revenue - expenses)
✅ Budget health status (ON_TRACK, WARNING, CRITICAL)
✅ Collaboration permission integration

### Timeline
✅ Task management with checklists
✅ Assign to collaborators
✅ Progress tracking
✅ Drag-and-drop reordering
✅ Auto-save drafts
✅ Due dates and priorities
✅ Link to budget expenses

### Attendees
✅ RSVP management
✅ Invitations
✅ Auto-creation from tickets
✅ Guest list with status
✅ Access control integration

### Feeds
✅ Event-specific social timeline
✅ Text, image, video posts
✅ Likes, comments, reposts
✅ Quote posts
✅ Access controlled by event type
✅ Post-event public visibility option

### Collaboration
✅ 10 role types with defaults
✅ 8 granular permissions
✅ Custom permission overrides
✅ Controls all feature access
✅ Welcome notifications

### Social
✅ User-to-user follows
✅ Event subscriptions
✅ Mutual follow detection
✅ Timeline aggregation (future)

---

## Summary: Why Features Work Well Together

### 1. **Single Source of Truth**
- Events are central entity
- All features reference event ID
- No data duplication

### 2. **Consistent Patterns**
- All use collaboration permissions
- All use common infrastructure (storage, notifications)
- All follow service → repository → entity pattern

### 3. **Auto-Synchronization**
- Ticket sales → Budget revenue updates
- Attendee creation → Access granted
- Task assignment → Notifications sent
- Permission changes → Access updated

### 4. **Proper Boundaries**
- Features interact via well-defined services
- No circular dependencies
- Clear ownership of data

### 5. **Scalable Architecture**
- Lazy loading prevents N+1 queries
- Denormalized counts for performance
- Indexed foreign keys
- Soft deletes maintain audit trail

### 6. **User-Centric Design**
- One event, all features accessible
- Permissions control UI rendering
- Real-time updates across features
- Seamless user experience

---

## Next Steps for Integration

### Recommended Enhancements

1. **Real-Time Updates**
   - WebSocket notifications when budget changes
   - Live task updates on timeline board
   - Real-time feed updates

2. **Analytics Dashboard**
   - Budget vs actual charts
   - Ticket sales trends
   - Task completion rates
   - Feed engagement metrics

3. **Export/Reporting**
   - Budget PDF export
   - Attendee CSV export
   - Timeline Gantt chart
   - Event summary report

4. **Mobile Optimization**
   - Push notifications for all events
   - Mobile-optimized feed
   - QR code scanning for check-in

---

**The platform now provides complete event lifecycle management:**
Plan (Timeline) → Finance (Budget) → Sell (Tickets) → Execute (Collaboration) → Share (Feeds) → Engage (Social)
