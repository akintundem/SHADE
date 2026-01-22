# Collaboration & Timeline Capabilities

## Overview

The Collaboration and Timeline modules provide comprehensive event planning and team management capabilities. Together, they enable event organizers to:
- Build teams with granular role-based permissions
- Plan and execute events using task management boards
- Track progress and assign responsibilities
- Ensure seamless integration with the events system

---

## Collaboration System

### Purpose
Manage event collaborators with intuitive role-based permissions that control both backend access and frontend UI behavior.

### Architecture

#### Permission Model
The system uses a **two-tier permission model**:

1. **Role-Based Defaults**: Each collaborator role comes with default permissions
2. **Granular Overrides**: Optional per-user permission customization for fine-grained control

**Key Components**:
- `EventUser` - Core collaborator membership entity
- `EventUserPermission` - Granular permission overrides
- `EventPermissionEvaluator` - Permission evaluation service
- `EventPermissionDefaults` - Default permissions by role

---

### Collaborator Roles

| Role | User Type | Default Permissions | Use Case |
|------|-----------|---------------------|----------|
| **ADMIN** | `EventUserType.ADMIN` | All permissions | Full event control |
| **ORGANIZER** | `EventUserType.ORGANIZER` | All permissions | Primary event manager |
| **COORDINATOR** | `EventUserType.COORDINATOR` | All permissions | Co-manager with full access |
| **COLLABORATOR** | `EventUserType.COLLABORATOR` | View + Schedule | General team member |
| **STAFF** | `EventUserType.STAFF` | View + Schedule | Execution team |
| **VOLUNTEER** | `EventUserType.VOLUNTEER` | View + Schedule | Volunteer coordinator |
| **MEDIA** | `EventUserType.MEDIA` | View + Content | Media/content manager |
| **SPEAKER** | `EventUserType.SPEAKER` | View only | Event speaker (limited access) |
| **SPONSOR** | `EventUserType.SPONSOR` | View only | Event sponsor (limited access) |
| **ATTENDEE** | `EventUserType.ATTENDEE` | View only | Regular attendee |

---

### Granular Permissions

| Permission | Description | Default Roles |
|------------|-------------|---------------|
| `VIEW_EVENT` | View event details and timeline | All roles |
| `EDIT_EVENT_DETAILS` | Modify event information | ADMIN, ORGANIZER, COORDINATOR |
| `MANAGE_COLLABORATORS` | Add/remove team members | ADMIN, ORGANIZER, COORDINATOR |
| `MANAGE_INVITES` | Send collaborator invitations | ADMIN, ORGANIZER, COORDINATOR |
| `MANAGE_SCHEDULE` | Create/edit timeline tasks | ADMIN, ORGANIZER, COORDINATOR, COLLABORATOR, STAFF, VOLUNTEER |
| `MANAGE_BUDGET` | View/edit event budget | ADMIN, ORGANIZER, COORDINATOR |
| `MANAGE_TICKETS` | Ticket operations | ADMIN, ORGANIZER, COORDINATOR |
| `MANAGE_CONTENT` | Upload/manage event media | ADMIN, ORGANIZER, COORDINATOR, MEDIA |

**Permission Overrides**: Any collaborator can have custom permissions that override role defaults. If no overrides exist, role defaults apply.

---

## API Endpoints - Collaboration

### Base Path: `/api/v1/events/{eventId}/collaborators`

---

#### **GET** `/api/v1/events/{eventId}/collaborators`
**Summary**: Get event collaborators
**Auth**: Required
**Permission**: `ROLE_READ` on event
**Query Params**:
- `page` (int, default: 0) - Page number
- `size` (int, default: 20) - Page size

**Response**: `200 OK`
```json
[
  {
    "collaboratorId": "uuid",
    "eventId": "uuid",
    "userId": "uuid",
    "email": "user@example.com",
    "userName": "John Doe",
    "role": "ORGANIZER",
    "permissions": ["VIEW_EVENT", "EDIT_EVENT_DETAILS", "MANAGE_SCHEDULE"],
    "registrationStatus": "CONFIRMED",
    "invitationSent": false,
    "invitationSentAt": null,
    "addedAt": "2026-01-22T00:00:00Z",
    "updatedAt": "2026-01-22T00:00:00Z"
  }
]
```

**What It Does**: Returns paginated list of all collaborators for the event with their roles, permissions, and status.

---

#### **POST** `/api/v1/events/{eventId}/collaborators`
**Summary**: Add event collaborator
**Auth**: Required (Owner or Admin only)
**Permission**: `ROLE_ASSIGN` on event

**Request Body**:
```json
{
  "userId": "uuid",
  "role": "COLLABORATOR",
  "permissions": ["VIEW_EVENT", "MANAGE_SCHEDULE"]
}
```

**Response**: `200 OK` - Same as GET response single object

**What It Does**:
- Adds a user as collaborator to the event
- Assigns the specified role
- Optionally sets custom permissions (if omitted, role defaults apply)
- Sends welcome email and push notification to the collaborator
- Prevents duplicate collaborators
- Only event owner or admin can add collaborators

**Validation**:
- `userId` required, must exist
- `role` required
- User cannot be already a collaborator
- Only owner/admin can execute

---

#### **PUT** `/api/v1/events/{eventId}/collaborators/{collaboratorId}`
**Summary**: Update event collaborator
**Auth**: Required (Owner or Admin only)
**Permission**: `ROLE_UPDATE` on event

**Request Body**:
```json
{
  "role": "ORGANIZER",
  "permissions": ["VIEW_EVENT", "EDIT_EVENT_DETAILS", "MANAGE_COLLABORATORS"]
}
```

**Response**: `200 OK` - Updated collaborator object

**What It Does**:
- Updates collaborator role and/or permissions
- Changes apply immediately
- If `permissions` provided, overrides role defaults
- If `permissions` is null/empty, role defaults apply
- Only owner/admin can update

---

#### **DELETE** `/api/v1/events/{eventId}/collaborators/{collaboratorId}`
**Summary**: Remove event collaborator
**Auth**: Required (Owner or Admin only)
**Permission**: `ROLE_REMOVE` on event

**Response**: `204 No Content`

**What It Does**:
- Removes collaborator from event team
- Soft deletes the membership record
- Only owner/admin can remove collaborators
- Collaborator loses all access to event planning features

---

## Timeline System

### Purpose
Provide a Kanban-style task management board for planning and executing events. Acts as the central collaboration workspace where teams organize work.

### Architecture

**Key Components**:
- `Task` - Main task/card entity
- `Checklist` - Subtask/checklist item within a task
- `TimelineTaskService` - Business logic and access control
- Integrated with collaboration permissions

---

### Task Structure

**Task Fields**:
- `title`, `description` - Task details
- `startDate`, `dueDate` - Scheduling
- `priority` - Task importance
- `category` - Task grouping
- `status` - TO_DO, IN_PROGRESS, COMPLETED
- `progressPercentage` - 0-100% completion
- `taskOrder` - Display order
- `assignedTo` - UserAccount reference
- `isDraft` - Auto-save support
- `completedSubtasksCount`, `totalSubtasksCount` - Progress tracking

**Checklist (Subtask) Fields**:
- `title`, `description`
- `dueDate`
- `status` - TO_DO, IN_PROGRESS, COMPLETED
- `assignedTo` - UserAccount reference
- `taskOrder` - Display order within task
- `isDraft` - Auto-save support

---

### Permission Model

Timeline access is controlled by collaboration permissions:

| Action | Required Permission | Fallback |
|--------|---------------------|----------|
| **View Timeline** | `MANAGE_SCHEDULE` or `VIEW_EVENT` | Owner/Admin always allowed |
| **Create/Edit Tasks** | `MANAGE_SCHEDULE` | Owner/Admin always allowed |
| **Delete Tasks** | `MANAGE_SCHEDULE` | Owner/Admin always allowed |
| **Assign Tasks** | `MANAGE_SCHEDULE` | Can only assign to collaborators |

**Access Control**:
- Uses `EventPermissionEvaluator` to check permissions
- Event owner and admins bypass permission checks
- Non-collaborators are denied access
- Assignees must be event collaborators or owner

---

## API Endpoints - Timeline

### Base Path: `/api/v1/events/{eventId}/timeline`

---

#### **GET** `/api/v1/events/{eventId}/timeline/tasks`
**Summary**: Get all tasks for event
**Auth**: Required
**Permission**: `VIEW_EVENT` or `MANAGE_SCHEDULE`

**Response**: `200 OK`
```json
[
  {
    "id": "uuid",
    "title": "Book venue",
    "description": "Contact and confirm venue booking",
    "startDate": "2026-01-25T00:00:00Z",
    "dueDate": "2026-01-30T00:00:00Z",
    "priority": "HIGH",
    "category": "VENUE",
    "status": "IN_PROGRESS",
    "progressPercentage": 50,
    "taskOrder": 1,
    "assignedTo": {
      "id": "uuid",
      "name": "Jane Smith",
      "email": "jane@example.com"
    },
    "isDraft": false,
    "checklists": [
      {
        "id": "uuid",
        "title": "Contact venue manager",
        "status": "COMPLETED",
        "assignedTo": {...},
        "dueDate": "2026-01-26T00:00:00Z",
        "taskOrder": 0
      }
    ],
    "completedSubtasksCount": 1,
    "totalSubtasksCount": 3,
    "createdAt": "2026-01-22T00:00:00Z",
    "updatedAt": "2026-01-22T00:00:00Z"
  }
]
```

**What It Does**: Returns all tasks for the event, ordered by `taskOrder`, with embedded checklists and assignee details.

---

#### **POST** `/api/v1/events/{eventId}/timeline/tasks/autosave`
**Summary**: Auto-save task draft
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**:
```json
{
  "taskId": "uuid",  // Optional: null for new task, uuid for update
  "title": "Setup registration desk",
  "description": "Prepare check-in area",
  "startDate": "2026-02-01T08:00:00Z",
  "dueDate": "2026-02-01T09:00:00Z",
  "priority": "MEDIUM",
  "category": "LOGISTICS",
  "status": "TO_DO",
  "progressPercentage": 0,
  "assignedTo": "uuid"  // Must be collaborator or owner
}
```

**Response**: `200 OK` - Task detail response

**What It Does**:
- Creates new task or updates existing
- Marks as `isDraft: true` for auto-saving
- Validates assignee is collaborator
- Sends notification if assignee changed
- Allows incremental task creation

---

#### **POST** `/api/v1/events/{eventId}/timeline/tasks/{taskId}/finalize`
**Summary**: Finalize task draft
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**: Same as autosave

**Response**: `200 OK` - Task detail response

**What It Does**:
- Converts draft to final task (`isDraft: false`)
- Validates all required fields
- Task becomes visible to all collaborators
- Optional: can update fields during finalization

---

#### **POST** `/api/v1/events/{eventId}/timeline/tasks/{taskId}/checklists/autosave`
**Summary**: Auto-save checklist item
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**:
```json
{
  "itemId": "uuid",  // Optional: null for new, uuid for update
  "title": "Call catering service",
  "description": "Confirm menu and headcount",
  "dueDate": "2026-01-28T00:00:00Z",
  "status": "TO_DO",
  "assignedTo": "uuid"
}
```

**Response**: `200 OK` - Checklist detail response

**What It Does**:
- Creates or updates checklist item within task
- Marks as draft
- Validates assignee is collaborator
- Recalculates parent task progress
- Sends notification if assignee changed

---

#### **POST** `/api/v1/events/{eventId}/timeline/tasks/{taskId}/checklists/{itemId}/finalize`
**Summary**: Finalize checklist item
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**: Same as checklist autosave

**Response**: `200 OK` - Checklist detail response

**What It Does**:
- Converts draft checklist to final
- Updates parent task progress
- Validates fields
- Checklist becomes visible

---

#### **PUT** `/api/v1/events/{eventId}/timeline/tasks/reorder`
**Summary**: Reorder tasks
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**:
```json
{
  "taskIds": ["uuid1", "uuid2", "uuid3"]  // Ordered list
}
```

**Response**: `204 No Content`

**What It Does**:
- Updates `taskOrder` field for all tasks
- Array index becomes new order
- Allows drag-and-drop reordering in UI

---

#### **PUT** `/api/v1/events/{eventId}/timeline/tasks/{taskId}/checklists/reorder`
**Summary**: Reorder checklist items
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Request Body**:
```json
{
  "itemIds": ["uuid1", "uuid2", "uuid3"]
}
```

**Response**: `204 No Content`

**What It Does**: Updates checklist item order within a task.

---

#### **DELETE** `/api/v1/events/{eventId}/timeline/tasks/{taskId}`
**Summary**: Delete task
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Response**: `204 No Content`

**What It Does**:
- Soft deletes task
- Cascades to all checklists
- Removes from timeline board

---

#### **DELETE** `/api/v1/events/{eventId}/timeline/tasks/{taskId}/checklists/{itemId}`
**Summary**: Delete checklist item
**Auth**: Required
**Permission**: `MANAGE_SCHEDULE`

**Response**: `204 No Content`

**What It Does**:
- Soft deletes checklist item
- Recalculates parent task progress

---

## Integration with Events

### Event Ownership
- Event owner (`Event.owner`) always has full access
- Owner bypasses all permission checks
- Cannot be removed as collaborator

### Access Layers
1. **Event Owner** - Full access, no restrictions
2. **Admin Users** - Full access via `AuthorizationService`
3. **Collaborators** - Access based on role + permission overrides
4. **Non-members** - Denied access

### Event Types Integration
Timeline access respects event visibility:
- **Private events**: Only owner and collaborators
- **Public events**: Timeline still requires collaboration membership

### Notifications
- Task assignments send notifications via `NotificationService`
- Collaborator additions send welcome emails
- Uses common notification infrastructure

---

## Frontend Usage Guidelines

### Permission-Based UI

**Example: Show/Hide Features**
```typescript
// Fetch collaborator for current user
const collaborator = await fetchCollaborator(eventId, currentUserId);

// Check permissions
const canEditEvent = collaborator.permissions.includes('EDIT_EVENT_DETAILS');
const canManageSchedule = collaborator.permissions.includes('MANAGE_SCHEDULE');
const canManageTickets = collaborator.permissions.includes('MANAGE_TICKETS');

// Render UI conditionally
{canEditEvent && <EditEventButton />}
{canManageSchedule && <TimelineBoard />}
{canManageTickets && <TicketingPanel />}
```

### Role-Based Defaults
If you only need quick role-based checks (not granular permissions):
```typescript
const isOrganizer = collaborator.role === 'ORGANIZER';
const isAdmin = collaborator.role === 'ADMIN';

// Show all features for ADMIN/ORGANIZER
{(isAdmin || isOrganizer) && <FullControlPanel />}
```

### Timeline Board UI
```typescript
// Fetch tasks
const tasks = await fetchTasks(eventId);

// Group by status for Kanban board
const todoTasks = tasks.filter(t => t.status === 'TO_DO');
const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS');
const completedTasks = tasks.filter(t => t.status === 'COMPLETED');

// Render columns
<Board>
  <Column title="To Do">{todoTasks.map(renderTask)}</Column>
  <Column title="In Progress">{inProgressTasks.map(renderTask)}</Column>
  <Column title="Completed">{completedTasks.map(renderTask)}</Column>
</Board>
```

### Auto-Save Pattern
```typescript
// Debounce auto-save
const autoSaveTask = debounce(async (taskData) => {
  await fetch(`/api/v1/events/${eventId}/timeline/tasks/autosave`, {
    method: 'POST',
    body: JSON.stringify(taskData)
  });
}, 1000);

// On input change
onChange={(field, value) => {
  updateLocalState(field, value);
  autoSaveTask(localTaskState);
}}
```

---

## Best Practices

### Collaboration Management
1. **Start with Roles**: Assign appropriate roles first
2. **Override When Needed**: Only add custom permissions for exceptions
3. **Limit Overrides**: Keep permission model simple and predictable
4. **Audit Access**: Regularly review collaborator list

### Timeline Planning
1. **Break Down Work**: Use tasks for major items, checklists for subtasks
2. **Assign Clearly**: Every task should have an owner
3. **Set Deadlines**: Use `dueDate` for accountability
4. **Track Progress**: Update status and progress percentage regularly
5. **Reorder Strategically**: Prioritize tasks using drag-and-drop

### Performance
1. **Paginate Collaborators**: Use `page` and `size` params for large teams
2. **Lazy Load Checklists**: Expand task details on demand
3. **Batch Updates**: Reorder multiple tasks in single API call

---

## Summary

| Feature | Collaboration | Timeline |
|---------|---------------|----------|
| **Purpose** | Team management | Event planning board |
| **Access Control** | Role + granular permissions | Collaboration permissions |
| **Key Entities** | EventUser, EventUserPermission | Task, Checklist |
| **Frontend Use** | Permission-based UI rendering | Kanban board for tasks |
| **Integration** | Event ownership + RBAC | Collaboration + Events |
| **Notifications** | Welcome emails, push | Assignment notifications |

**Key Capabilities**:
✅ Intuitive role-based permissions with custom overrides
✅ Timeline task management integrated with collaboration
✅ Auto-save drafts for seamless UX
✅ Drag-and-drop reordering
✅ Progress tracking with subtasks
✅ Notification system for assignments
✅ Event owner always has full access
✅ Frontend-friendly permission APIs

This system provides a complete solution for collaborative event planning with granular control over what each team member can see and do.
