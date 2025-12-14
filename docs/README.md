## Event API (SHDE)

This document describes **only** the Event-related endpoints in this backend.

- **Base**: `/api/v1/events`
- **Auth**: Most endpoints require `Authorization: Bearer <accessToken>`

### Common error response
Most errors are returned as `ErrorResponse`:

```json
{
  "timestamp": "2025-12-11T22:08:34",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/api/v1/events/...",
  "validationErrors": {
    "field": "Validation message"
  }
}
```

---

## Events (CRUD)

#### GET /api/v1/events
Query:
- `page` (int, default `0`)
- `size` (int, default `20`, max `100`)
- `status` (enum `EventStatus`, optional)
- `eventType` (enum `EventType`, optional)
- `isPublic` (boolean, optional)
- `mine` (boolean, optional) → `true` to only return events you own
- `timeframe` (string, optional) → `UPCOMING|PAST` (only meaningful with `mine=true`)
- `startDateFrom` (ISO datetime, optional)
- `startDateTo` (ISO datetime, optional)
- `isArchived` (boolean, optional)
- `search` (string, optional)
- `sortBy` (string, default `startDateTime`, allowed: `startDateTime|createdAt|name|currentAttendeeCount`)
- `sortDirection` (string, default `ASC`, allowed: `ASC|DESC`)

Examples:
- **Search**: `GET /api/v1/events?search=test`
- **Public only**: `GET /api/v1/events?isPublic=true`
- **Featured** (newest published public): `GET /api/v1/events?isPublic=true&status=PUBLISHED&sortBy=createdAt&sortDirection=DESC`
- **Trending** (most attendees published public): `GET /api/v1/events?isPublic=true&status=PUBLISHED&sortBy=currentAttendeeCount&sortDirection=DESC`
- **Upcoming**: `GET /api/v1/events?isPublic=true&timeframe=UPCOMING`
- **By type**: `GET /api/v1/events?eventType=WORKSHOP`
- **By status**: `GET /api/v1/events?status=PUBLISHED`

Body:
- None

Response:
- `200 OK` → `Page<EventResponse>`

---

### My events via `GET /api/v1/events` filters

Use the main listing endpoint instead of multiple “my events” list endpoints:

- `GET /api/v1/events?mine=true` → events you own
- `GET /api/v1/events?mine=true&timeframe=UPCOMING` → upcoming owned events
- `GET /api/v1/events?mine=true&timeframe=PAST` → past owned events

These can be combined with the existing pagination/sort/search filters.

---

## Status & lifecycle

#### POST /api/v1/events/{id}/publish
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventResponse`

---

## Capacity & registration

#### GET /api/v1/events/{id}/capacity
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventCapacityResponse`

---

#### PUT /api/v1/events/{id}/capacity
Query:
- None

Body: `EventCapacityUpdateRequest`

```json
{
  "capacity": 200
}
```

Response:
- `200 OK` → `EventResponse`

---

#### PUT /api/v1/events/{id}/registration-deadline
Query:
- None

Body: `EventRegistrationDeadlineRequest`

```json
{
  "deadline": "2026-06-10T23:59:59"
}
```

Response:
- `200 OK` → `EventResponse`

---

## Visibility & access

#### GET /api/v1/events/{id}/visibility
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventVisibilityResponse`

---

#### PUT /api/v1/events/{id}/visibility
Query:
- None

Body: `EventVisibilityUpdateRequest`

```json
{
  "isPublic": true,
  "requiresApproval": false
}
```

Response:
- `200 OK` → `EventResponse`

---

## Sharing

#### GET /api/v1/events/{id}/share
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventSharingOptionsResponse`

---

#### POST /api/v1/events/{id}/share
Query:
- None

Body: `EventShareRequest`

```json
{
  "channel": "EMAIL",
  "recipients": ["person1@example.com", "person2@example.com"],
  "message": "You're invited!",
  "includeEventDetails": true,
  "expirationDate": "2026-06-20T00:00:00",
  "requiresAuth": false
}
```

Response:
- `200 OK` → `EventShareResponse`

---

## Media & assets

#### GET /api/v1/events/{id}/media
Query:
- `category` (string, optional)
- `type` (string, optional)

Body:
- None

Response:
- `200 OK` → `List<EventMediaResponse>`

---

#### POST /api/v1/events/{id}/media
Query:
- None

Body: `EventMediaUploadRequest`

```json
{
  "fileName": "photo.jpg",
  "contentType": "image/jpeg",
  "category": "gallery",
  "isPublic": true,
  "tags": "stage,opening",
  "description": "Opening photo",
  "metadata": { "source": "mobile" }
}
```

Response:
- `200 OK` → `EventPresignedUploadResponse`

---

#### GET /api/v1/events/{id}/media/{mediaId}
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventMediaResponse`

---

#### PUT /api/v1/events/{id}/media/{mediaId}
Query:
- None

Body: `EventMediaRequest`

```json
{
  "mediaType": "image",
  "mediaName": "Opening photo",
  "description": "Updated description",
  "category": "gallery",
  "isPublic": true,
  "tags": "opening",
  "mediaUrl": "https://cdn.example.com/...",
  "metadata": "{\"camera\":\"iphone\"}"
}
```

Response:
- `200 OK` → `EventMediaResponse`

---

#### DELETE /api/v1/events/{id}/media/{mediaId}
Query:
- None

Body:
- None

Response:
- `204 No Content`

---

#### GET /api/v1/events/{id}/assets
Query:
- None

Body:
- None

Response:
- `200 OK` → `List<EventMediaResponse>`

---

#### POST /api/v1/events/{id}/assets
Query:
- None

Body: `EventMediaUploadRequest`

Response:
- `200 OK` → `EventPresignedUploadResponse`

---

#### PUT /api/v1/events/{id}/cover-image
Query:
- None

Body: `EventMediaUploadRequest`

Response:
- `200 OK` → `EventPresignedUploadResponse`

---

#### DELETE /api/v1/events/{id}/cover-image
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventCoverImageResponse`

---

## Notifications & reminders

#### GET /api/v1/events/{id}/notifications
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventNotificationSettingsResponse`

---

#### PUT /api/v1/events/{id}/notifications
Query:
- None

Body: `EventNotificationSettingsRequest`

```json
{
  "enabledChannels": ["EMAIL", "PUSH"],
  "reminderEnabled": true,
  "defaultReminderMinutes": 1440
}
```

Response:
- `200 OK` → `EventNotificationSettingsResponse`

---

#### POST /api/v1/events/{id}/notifications/send
Query:
- None

Body: `EventNotificationRequest`

```json
{
  "channel": "EMAIL",
  "subject": "Event update",
  "content": "Schedule has changed.",
  "recipientUserIds": ["00000000-0000-0000-0000-000000000000"],
  "recipientEmails": ["person1@example.com"],
  "scheduledAt": null,
  "includeEventDetails": true,
  "priority": "NORMAL",
  "templateId": null
}
```

Response:
- `200 OK` → `EventNotificationResponse`

---

#### GET /api/v1/events/{id}/reminders
Query:
- `page` (int, default `0`)
- `size` (int, default `20`)

Body:
- None

Response:
- `200 OK` → `List<EventReminderResponse>`

---

#### POST /api/v1/events/{id}/reminders
Query:
- None

Body: `EventReminderRequest`

```json
{
  "title": "Reminder",
  "description": "Don’t forget",
  "reminderTime": "2026-06-15T08:00:00",
  "channel": "email",
  "recipientUserIds": ["00000000-0000-0000-0000-000000000000"],
  "recipientEmails": ["person1@example.com"],
  "reminderType": "custom",
  "isActive": true,
  "customMessage": "See you there!",
  "includeEventDetails": true
}
```

Response:
- `200 OK` → `EventReminderResponse`

---

#### PUT /api/v1/events/{id}/reminders/{reminderId}
Query:
- None

Body: `EventReminderRequest`

Response:
- `200 OK` → `EventReminderResponse`

---

#### DELETE /api/v1/events/{id}/reminders/{reminderId}
Query:
- None

Body:
- None

Response:
- `204 No Content`

---

#### GET /api/v1/events/{id}/reminders/{reminderId}
Query:
- None

Body:
- None

Response:
- `200 OK` → `EventReminderResponse`

---

## Collaborators

#### GET /api/v1/events/{id}/collaborators
Query:
- `page` (int, default `0`)
- `size` (int, default `20`)

Body:
- None

Response:
- `200 OK` → `List<EventCollaboratorResponse>`

---

#### POST /api/v1/events/{id}/collaborators
Query:
- None

Body: `EventCollaboratorRequest`

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "email": "collab@example.com",
  "role": "COLLABORATOR",
  "permissions": ["EVENT_UPDATE"],
  "notes": "Helping with logistics",
  "sendInvitation": true,
  "invitationMessage": "Please join as collaborator."
}
```

Response:
- `200 OK` → `EventCollaboratorResponse`

---

#### PUT /api/v1/events/{id}/collaborators/{collaboratorId}
Query:
- None

Body: `EventCollaboratorRequest`

Response:
- `200 OK` → `EventCollaboratorResponse`

---

#### DELETE /api/v1/events/{id}/collaborators/{collaboratorId}
Query:
- None

Body:
- None

Response:
- `204 No Content`
