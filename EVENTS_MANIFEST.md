# Events API Integration Guide

**For Frontend Developers**

This guide provides everything you need to integrate the Events feature into your frontend application, including all endpoints, DTOs, and detailed information about what they do.

---

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Base Configuration](#base-configuration)
3. [CRUD Operations](#1-crud-operations)
4. [User-Event Relationships](#2-user-event-relationships)
5. [Event Status & Lifecycle](#3-event-status--lifecycle)
6. [Event Discovery & Search](#4-event-discovery--search)
7. [Event Capacity & Registration](#5-event-capacity--registration)
8. [Event QR Code](#6-event-qr-code)
9. [Event Visibility & Access Control](#7-event-visibility--access-control)
10. [Event Sharing](#8-event-sharing)
11. [Event Analytics](#9-event-analytics)
12. [Event Duplication](#10-event-duplication)
13. [Event Validation & Health Check](#11-event-validation--health-check)
14. [Event Media](#12-event-media)
15. [Event Notifications](#13-event-notifications)
16. [Event Collaboration](#14-event-collaboration)
17. [DTOs Reference](#dtos-reference)
18. [Error Handling](#error-handling)
19. [Quick Reference](#quick-reference)

---

## Quick Start

### Base URL
```
https://your-api-domain.com/api/v1/events
```

### Required Headers
```javascript
{
  'Content-Type': 'application/json',
  'Authorization': 'Bearer {accessToken}',
  'X-Device-Id': '{deviceId}' // Optional but recommended
}
```

### Special Headers
- **Idempotency-Key**: For create operations to prevent duplicate creation on retries
- **If-Match**: For optimistic locking on update/delete operations (contains version number)
- **ETag**: Returned in responses for version tracking

---

## Base Configuration

### Environment Variables
```javascript
const API_BASE_URL = process.env.REACT_APP_API_URL || 'https://your-api-domain.com';
const EVENTS_ENDPOINT = `${API_BASE_URL}/api/v1/events`;
```

### TypeScript Types

```typescript
// Event Status Enum
type EventStatus = 
  | 'DRAFT'
  | 'PLANNING'
  | 'PUBLISHED'
  | 'REGISTRATION_OPEN'
  | 'REGISTRATION_CLOSED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'POSTPONED';

// Event Type Enum
type EventType = 
  | 'CONFERENCE'
  | 'WORKSHOP'
  | 'SEMINAR'
  | 'MEETING'
  | 'PARTY'
  | 'WEDDING'
  | 'BIRTHDAY'
  | 'CORPORATE_EVENT'
  | 'TRADE_SHOW'
  | 'CONCERT'
  | 'FESTIVAL'
  | 'SPORTS_EVENT'
  | 'CHARITY_EVENT'
  | 'NETWORKING'
  | 'TRAINING'
  | 'RETREAT'
  | 'OTHER';

// Venue DTO
interface VenueDTO {
  name?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  latitude?: number;
  longitude?: number;
  capacity?: number;
  amenities?: string[];
}

// Event Response
interface EventResponse {
  id: string; // UUID
  name: string;
  description?: string;
  eventType: EventType;
  eventStatus: EventStatus;
  startDateTime: string; // ISO datetime
  endDateTime?: string; // ISO datetime
  registrationDeadline?: string; // ISO datetime
  capacity?: number;
  currentAttendeeCount: number;
  isPublic: boolean;
  requiresApproval: boolean;
  qrCodeEnabled: boolean;
  qrCode?: string;
  coverImageUrl?: string;
  eventWebsiteUrl?: string;
  hashtag?: string;
  theme?: string;
  objectives?: string;
  targetAudience?: string;
  successMetrics?: string;
  brandingGuidelines?: string;
  venueRequirements?: string;
  technicalRequirements?: string;
  accessibilityFeatures?: string;
  emergencyPlan?: string;
  backupPlan?: string;
  postEventTasks?: string;
  metadata?: string;
  ownerId: string; // UUID
  venueId?: string; // UUID
  venue?: VenueDTO;
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
}
```

---

## 1. CRUD Operations

### 1.1 List Events

**Endpoint:** `GET /api/v1/events`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** List events with pagination, filtering, and search. Supports filtering by status, visibility, date range, and search by name/tag.

**Query Parameters (EventListRequest):**

```typescript
{
  page?: number;           // Page number (0-indexed), default: 0
  size?: number;          // Items per page (1-100), default: 20
  status?: EventStatus;    // Filter by event status
  isPublic?: boolean;      // Filter by visibility
  startDateFrom?: string;  // ISO datetime - events starting on or after
  startDateTo?: string;    // ISO datetime - events starting before
  isArchived?: boolean;    // Filter by archived status, default: false
  search?: string;         // Search term (name, description, hashtag, theme)
  sortBy?: string;        // Sort field: "startDateTime" | "createdAt" | "name" | "currentAttendeeCount"
  sortDirection?: string;  // "ASC" | "DESC", default: "ASC"
}
```

**Success Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Annual Company Conference",
      "description": "Our annual company-wide conference",
      "eventType": "CONFERENCE",
      "eventStatus": "PUBLISHED",
      "startDateTime": "2024-06-15T09:00:00",
      "endDateTime": "2024-06-15T17:00:00",
      "capacity": 200,
      "currentAttendeeCount": 25,
      "isPublic": true,
      ...
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

**Example:**

```javascript
async function listEvents(filters = {}) {
  const params = new URLSearchParams();
  if (filters.page) params.append('page', filters.page);
  if (filters.size) params.append('size', filters.size);
  if (filters.status) params.append('status', filters.status);
  if (filters.search) params.append('search', filters.search);
  
  const response = await fetch(`${EVENTS_ENDPOINT}?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  
  return await handleApiError(response);
}
```

---

### 1.2 Get Event by ID

**Endpoint:** `GET /api/v1/events/{id}`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve a specific event by its unique identifier. Only accessible if event is public, user is owner, or user has appropriate role.

**Path Parameters:**
- `id` (UUID): Event ID

**Success Response:** `200 OK`

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Annual Company Conference",
  "description": "Our annual company-wide conference",
  "eventType": "CONFERENCE",
  "eventStatus": "PUBLISHED",
  ...
}
```

**Response Headers:**
- `ETag`: Version number for optimistic locking (e.g., "5")

**Example:**

```javascript
async function getEvent(eventId) {
  const response = await fetch(`${EVENTS_ENDPOINT}/${eventId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  
  // Store ETag for optimistic locking
  const etag = response.headers.get('ETag');
  if (etag) {
    localStorage.setItem(`event-${eventId}-version`, etag);
  }
  
  return await handleApiError(response);
}
```

---

### 1.3 Create Event

**Endpoint:** `POST /api/v1/events`

**Authentication:** Required

**Permission:** `EVENT_CREATE`

**Description:** Create a new event with the provided details. Supports Idempotency-Key header to prevent duplicate creation on retries.

**Request Headers:**
- `Idempotency-Key` (optional): Unique key to prevent duplicate creation

**Request Body (CreateEventRequest):**

```typescript
{
  name: string;                    // Required, max 255 chars
  description?: string;            // Max 10000 chars
  eventType: EventType;            // Required
  eventStatus?: EventStatus;       // Default: PLANNING
  startDateTime?: string;          // ISO datetime, must be future or present
  endDateTime?: string;            // ISO datetime
  registrationDeadline?: string;   // ISO datetime
  capacity?: number;
  currentAttendeeCount?: number;
  isPublic?: boolean;              // Default: true
  requiresApproval?: boolean;      // Default: false
  qrCodeEnabled?: boolean;         // Default: false
  qrCode?: string;
  coverImageUrl?: string;
  eventWebsiteUrl?: string;
  hashtag?: string;
  theme?: string;
  objectives?: string;
  targetAudience?: string;
  successMetrics?: string;
  brandingGuidelines?: string;
  venueRequirements?: string;
  technicalRequirements?: string;
  accessibilityFeatures?: string;
  emergencyPlan?: string;
  backupPlan?: string;
  postEventTasks?: string;
  metadata?: string;
  venue?: VenueDTO;
}
```

**Success Responses:**
- `201 Created`: Event created successfully
- `200 OK`: Event already exists (idempotent replay) - includes `X-Idempotency-Replay: true` header

**Example:**

```javascript
async function createEvent(eventData, idempotencyKey = null) {
  const headers = getAuthHeaders();
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  
  const response = await fetch(EVENTS_ENDPOINT, {
    method: 'POST',
    headers,
    body: JSON.stringify(eventData),
  });
  
  const isReplay = response.headers.get('X-Idempotency-Replay') === 'true';
  if (isReplay) {
    console.log('Idempotent replay - event already exists');
  }
  
  return await handleApiError(response);
}

// Usage with idempotency
const idempotencyKey = crypto.randomUUID();
await createEvent({
  name: 'Annual Conference',
  eventType: 'CONFERENCE',
  startDateTime: '2024-06-15T09:00:00',
  capacity: 200,
}, idempotencyKey);
```

---

### 1.4 Update Event

**Endpoint:** `PUT /api/v1/events/{id}`

**Authentication:** Required

**Permission:** `EVENT_UPDATE` (with resource: `event_id=#id`)

**Description:** Update an existing event with new details. Supports If-Match header for optimistic locking.

**Path Parameters:**
- `id` (UUID): Event ID

**Request Headers:**
- `If-Match` (optional): Version number for optimistic locking (e.g., "5")

**Request Body (UpdateEventRequest):**

All fields are optional - only include fields you want to update:

```typescript
{
  name?: string;                   // Max 255 chars
  description?: string;            // Max 10000 chars
  eventType?: EventType;
  eventStatus?: EventStatus;
  startDateTime?: string;          // ISO datetime
  endDateTime?: string;            // ISO datetime
  registrationDeadline?: string;  // ISO datetime
  capacity?: number;
  currentAttendeeCount?: number;
  isPublic?: boolean;
  requiresApproval?: boolean;
  qrCodeEnabled?: boolean;
  qrCode?: string;
  coverImageUrl?: string;
  eventWebsiteUrl?: string;
  hashtag?: string;
  theme?: string;
  objectives?: string;
  targetAudience?: string;
  successMetrics?: string;
  brandingGuidelines?: string;
  venueRequirements?: string;
  technicalRequirements?: string;
  accessibilityFeatures?: string;
  emergencyPlan?: string;
  backupPlan?: string;
  postEventTasks?: string;
  metadata?: string;
  venue?: VenueDTO;
  venueCleared?: boolean;         // Set to true to remove venue association
}
```

**Success Response:** `200 OK`

**Error Responses:**
- `409 Conflict`: Event version mismatch (optimistic locking failure)

**Example:**

```javascript
async function updateEvent(eventId, updates, version = null) {
  const headers = getAuthHeaders();
  if (version) {
    headers['If-Match'] = version.toString();
  }
  
  const response = await fetch(`${EVENTS_ENDPOINT}/${eventId}`, {
    method: 'PUT',
    headers,
    body: JSON.stringify(updates),
  });
  
  if (response.status === 409) {
    throw new Error('Event has been modified by another user. Please refresh and try again.');
  }
  
  const updated = await handleApiError(response);
  // Update stored version
  const newVersion = response.headers.get('ETag');
  if (newVersion) {
    localStorage.setItem(`event-${eventId}-version`, newVersion);
  }
  
  return updated;
}
```

---

### 1.5 Archive Event

**Endpoint:** `POST /api/v1/events/{id}/archive`

**Authentication:** Required

**Permission:** `EVENT_DELETE` (with resource: `event_id=#id`)

**Description:** Archive an event (soft delete) with audit metadata. Use this instead of DELETE for recoverable removal.

**Path Parameters:**
- `id` (UUID): Event ID

**Query Parameters:**
- `reason` (optional): Reason for archiving

**Success Response:** `200 OK` - Returns EventResponse

**Example:**

```javascript
async function archiveEvent(eventId, reason = null) {
  const url = reason 
    ? `${EVENTS_ENDPOINT}/${eventId}/archive?reason=${encodeURIComponent(reason)}`
    : `${EVENTS_ENDPOINT}/${eventId}/archive`;
    
  const response = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  
  return await handleApiError(response);
}
```

---

### 1.6 Restore Archived Event

**Endpoint:** `POST /api/v1/events/{id}/restore`

**Authentication:** Required

**Permission:** `EVENT_DELETE` (with resource: `event_id=#id`)

**Description:** Restore a previously archived event.

**Path Parameters:**
- `id` (UUID): Event ID

**Success Response:** `200 OK` - Returns EventResponse

---

### 1.7 Delete Event (Hard Delete)

**Endpoint:** `DELETE /api/v1/events/{id}`

**Authentication:** Required

**Permission:** `EVENT_DELETE` (with resource: `event_id=#id`)

**Description:** Permanently delete an event. Consider using archive endpoint instead for recoverable removal. Supports If-Match header for optimistic locking.

**Path Parameters:**
- `id` (UUID): Event ID

**Request Headers:**
- `If-Match` (optional): Version number for optimistic locking

**Success Response:** `204 No Content`

**Example:**

```javascript
async function deleteEvent(eventId, version = null) {
  const headers = getAuthHeaders();
  if (version) {
    headers['If-Match'] = version.toString();
  }
  
  const response = await fetch(`${EVENTS_ENDPOINT}/${eventId}`, {
    method: 'DELETE',
    headers,
  });
  
  if (response.status === 204) {
    return { success: true };
  }
  
  return await handleApiError(response);
}
```

---

## 2. User-Event Relationships

### 2.1 Get All Events for User

**Endpoint:** `GET /api/v1/events/user/{userId}`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

**Description:** Retrieve all events a user has a relationship with (owned, attending, etc.).

**Path Parameters:**
- `userId` (UUID): User ID (must match authenticated user)

**Success Response:** `200 OK`

```json
[
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "eventName": "Annual Conference",
    "eventDescription": "Description",
    "eventType": "CONFERENCE",
    "eventStatus": "PUBLISHED",
    "startDateTime": "2024-06-15T09:00:00",
    "endDateTime": "2024-06-15T17:00:00",
    "isOwner": true,
    "capacity": 200,
    "currentAttendeeCount": 25,
    "isPublic": true,
    "coverImageUrl": "https://...",
    "eventWebsiteUrl": "https://...",
    "hashtag": "#Conference2024"
  }
]
```

---

### 2.2 Get Events Owned by User

**Endpoint:** `GET /api/v1/events/user/{userId}/owned`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

**Description:** Retrieve events owned by a specific user.

**Path Parameters:**
- `userId` (UUID): User ID (must match authenticated user)

---

### 2.3 Get Upcoming Events for User

**Endpoint:** `GET /api/v1/events/user/{userId}/upcoming`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

**Description:** Retrieve upcoming events for a specific user.

---

### 2.4 Get Past Events for User

**Endpoint:** `GET /api/v1/events/user/{userId}/past`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

**Description:** Retrieve past events for a specific user.

---

### 2.5 Get Current User's Events Summary

**Endpoint:** `GET /api/v1/events/my-events`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

**Description:** Retrieve all events for the current authenticated user with summary information.

**Success Response:** `200 OK`

```json
{
  "totalEvents": 10,
  "ownedEvents": 5,
  "upcomingEvents": 3,
  "pastEvents": 2,
  "events": [...]
}
```

---

### 2.6 Get Current User's Owned Events

**Endpoint:** `GET /api/v1/events/my-events/owned`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

---

### 2.7 Get Current User's Upcoming Events

**Endpoint:** `GET /api/v1/events/my-events/upcoming`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

---

### 2.8 Get Current User's Past Events

**Endpoint:** `GET /api/v1/events/my-events/past`

**Authentication:** Required

**Permission:** `MY_EVENTS_READ` (with resource: `user_id=#principal.id`)

---

## 3. Event Status & Lifecycle

### 3.1 Get Event Status

**Endpoint:** `GET /api/v1/events/{id}/status`

**Authentication:** Required

**Permission:** `EVENT_READ` (with resource: `event_id=#id`)

**Description:** Retrieve the current status of an event.

**Success Response:** `200 OK`

```json
"PUBLISHED"
```

---

### 3.2 Update Event Status

**Endpoint:** `PUT /api/v1/events/{id}/status`

**Authentication:** Required

**Permission:** `EVENT_UPDATE` (with resource: `event_id=#id`)

**Description:** Update the status of an event.

**Request Body (EventStatusUpdateRequest):**

```typescript
{
  eventStatus: EventStatus; // Required
}
```

**Success Response:** `200 OK` - Returns EventResponse

---

### 3.3 Publish Event

**Endpoint:** `POST /api/v1/events/{id}/publish`

**Authentication:** Required

**Permission:** `EVENT_PUBLISH` (with resource: `event_id=#id`)

**Description:** Publish an event to make it visible.

**Success Response:** `200 OK` - Returns EventResponse

---

### 3.4 Cancel Event

**Endpoint:** `POST /api/v1/events/{id}/cancel`

**Authentication:** Required

**Permission:** `EVENT_CANCEL` (with resource: `event_id=#id`)

**Description:** Cancel an event.

**Success Response:** `200 OK` - Returns EventResponse

---

### 3.5 Complete Event

**Endpoint:** `POST /api/v1/events/{id}/complete`

**Authentication:** Required

**Permission:** `EVENT_COMPLETE` (with resource: `event_id=#id`)

**Description:** Mark an event as completed.

**Success Response:** `200 OK` - Returns EventResponse

---

### 3.6 Open Registration

**Endpoint:** `POST /api/v1/events/{id}/open-registration`

**Authentication:** Required

**Permission:** `EVENT_REGISTRATION_OPEN` (with resource: `event_id=#id`)

**Description:** Open registration for an event.

**Success Response:** `200 OK` - Returns EventResponse

---

### 3.7 Close Registration

**Endpoint:** `POST /api/v1/events/{id}/close-registration`

**Authentication:** Required

**Permission:** `EVENT_REGISTRATION_CLOSE` (with resource: `event_id=#id`)

**Description:** Close registration for an event.

**Success Response:** `200 OK` - Returns EventResponse

---

## 4. Event Discovery & Search

### 4.1 Search Events

**Endpoint:** `GET /api/v1/events/search`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Search events with various filters.

**Query Parameters:**
- `q` (optional): Search term
- `type` (optional): Event type filter
- `status` (optional): Event status filter
- `dateFrom` (optional): Start date from (ISO datetime)
- `dateTo` (optional): Start date to (ISO datetime)

**Success Response:** `200 OK` - Returns List<EventResponse>

---

### 4.2 Get Public Events

**Endpoint:** `GET /api/v1/events/public`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve all public events.

**Success Response:** `200 OK` - Returns List<EventResponse>

---

### 4.3 Get Featured Events

**Endpoint:** `GET /api/v1/events/featured`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve featured events.

**Query Parameters:**
- `page` (optional): Page number
- `size` (optional): Page size

**Success Response:** `200 OK` - Returns List<EventResponse>

---

### 4.4 Get Trending Events

**Endpoint:** `GET /api/v1/events/trending`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve trending events.

**Query Parameters:**
- `page` (optional): Page number
- `size` (optional): Page size

---

### 4.5 Get Upcoming Events

**Endpoint:** `GET /api/v1/events/upcoming`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve upcoming public events.

---

### 4.6 Get Events by Type

**Endpoint:** `GET /api/v1/events/by-type/{type}`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve events by event type.

**Path Parameters:**
- `type` (string): Event type (e.g., "CONFERENCE", "WORKSHOP")

---

### 4.7 Get Events by Status

**Endpoint:** `GET /api/v1/events/by-status/{status}`

**Authentication:** Required

**Permission:** `PUBLIC_EVENTS_SEARCH`

**Description:** Retrieve events by event status.

**Path Parameters:**
- `status` (string): Event status (e.g., "PUBLISHED", "REGISTRATION_OPEN")

---

## 5. Event Capacity & Registration

### 5.1 Get Event Capacity

**Endpoint:** `GET /api/v1/events/{id}/capacity`

**Authentication:** Required

**Permission:** `EVENT_READ` (with resource: `event_id=#id`)

**Description:** Retrieve capacity information for an event.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "capacity": 200,
  "currentAttendeeCount": 25,
  "availableSpots": 175,
  "utilizationPercentage": 12.5,
  "isRegistrationOpen": true
}
```

---

### 5.2 Update Event Capacity

**Endpoint:** `PUT /api/v1/events/{id}/capacity`

**Authentication:** Required

**Permission:** `EVENT_UPDATE` (with resource: `event_id=#id`)

**Description:** Update the capacity of an event.

**Request Body (EventCapacityUpdateRequest):**

```typescript
{
  capacity: number; // Required
}
```

**Success Response:** `200 OK` - Returns EventResponse

---

### 5.3 Get Available Capacity

**Endpoint:** `GET /api/v1/events/{id}/capacity/available`

**Authentication:** Required

**Permission:** `EVENT_READ` (with resource: `event_id=#id`)

**Description:** Get the number of available spots for an event.

**Success Response:** `200 OK`

```json
175
```

---

### 5.4 Update Registration Deadline

**Endpoint:** `PUT /api/v1/events/{id}/registration-deadline`

**Authentication:** Required

**Permission:** `EVENT_UPDATE` (with resource: `event_id=#id`)

**Description:** Update the registration deadline for an event.

**Request Body (EventRegistrationDeadlineRequest):**

```typescript
{
  deadline: string; // ISO datetime, required
}
```

**Success Response:** `200 OK` - Returns EventResponse

---

## 6. Event QR Code

### 6.1 Get Event QR Code

**Endpoint:** `GET /api/v1/events/{id}/qr-code`

**Authentication:** Required

**Permission:** `EVENT_QR_CODE_GENERATE` (with resource: `event_id=#id`)

**Description:** Retrieve the QR code for an event, including Base64 image.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "qrCode": "https://app.shade.events/events/123e4567-e89b-12d3-a456-426614174000",
  "qrCodeEnabled": true,
  "qrCodeImageBase64": "data:image/png;base64,iVBORw0KGgoAAAANS...",
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 6.2 Generate QR Code

**Endpoint:** `POST /api/v1/events/{id}/qr-code/generate`

**Authentication:** Required

**Permission:** `EVENT_QR_CODE_GENERATE` (with resource: `event_id=#id`)

**Description:** Generate a QR code for an event.

**Success Response:** `200 OK` - Returns EventResponse

---

### 6.3 Regenerate QR Code

**Endpoint:** `POST /api/v1/events/{id}/qr-code/regenerate`

**Authentication:** Required

**Permission:** `EVENT_QR_CODE_REGENERATE` (with resource: `event_id=#id`)

**Description:** Regenerate the QR code for an event.

**Success Response:** `200 OK` - Returns EventResponse

---

### 6.4 Get QR Code Image

**Endpoint:** `GET /api/v1/events/{id}/qr-code/image`

**Authentication:** Required

**Permission:** `EVENT_QR_CODE_GENERATE` (with resource: `event_id=#id`)

**Description:** Retrieve the QR code image as PNG bytes for an event.

**Success Response:** `200 OK`

**Content-Type:** `image/png`

**Response Body:** PNG image bytes

**Example:**

```javascript
async function getQRCodeImage(eventId) {
  const response = await fetch(`${EVENTS_ENDPOINT}/${eventId}/qr-code/image`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  
  if (response.ok) {
    const blob = await response.blob();
    const imageUrl = URL.createObjectURL(blob);
    return imageUrl;
  }
  
  return await handleApiError(response);
}
```

---

### 6.5 Disable QR Code

**Endpoint:** `DELETE /api/v1/events/{id}/qr-code`

**Authentication:** Required

**Permission:** `EVENT_QR_CODE_DELETE` (with resource: `event_id=#id`)

**Description:** Disable the QR code for an event.

**Success Response:** `200 OK` - Returns EventResponse

---

## 7. Event Visibility & Access Control

### 7.1 Get Event Visibility

**Endpoint:** `GET /api/v1/events/{id}/visibility`

**Authentication:** Required

**Permission:** `EVENT_READ` (with resource: `event_id=#id`)

**Description:** Get the visibility settings for an event.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "isPublic": true,
  "requiresApproval": false,
  "accessLevel": "public",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 7.2 Update Event Visibility

**Endpoint:** `PUT /api/v1/events/{id}/visibility`

**Authentication:** Required

**Permission:** `EVENT_VISIBILITY_UPDATE` (with resource: `event_id=#id`)

**Description:** Update the visibility settings for an event.

**Request Body (EventVisibilityUpdateRequest):**

```typescript
{
  isPublic: boolean; // Required
}
```

**Success Response:** `200 OK` - Returns EventResponse

---

### 7.3 Make Event Public

**Endpoint:** `POST /api/v1/events/{id}/make-public`

**Authentication:** Required

**Permission:** `EVENT_VISIBILITY_UPDATE` (with resource: `event_id=#id`)

**Description:** Make an event public.

**Success Response:** `200 OK` - Returns EventResponse

---

### 7.4 Make Event Private

**Endpoint:** `POST /api/v1/events/{id}/make-private`

**Authentication:** Required

**Permission:** `EVENT_VISIBILITY_UPDATE` (with resource: `event_id=#id`)

**Description:** Make an event private.

**Success Response:** `200 OK` - Returns EventResponse

---

## 8. Event Sharing

### 8.1 Get Sharing Options

**Endpoint:** `GET /api/v1/events/{id}/share`

**Authentication:** Required

**Permission:** `EVENT_READ` (with resource: `event_id=#id`)

**Description:** Retrieve available sharing channels and options for an event.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "availableChannels": ["EMAIL", "LINK", "SOCIAL"],
  "shareLink": "https://app.shade.events/share/123e4567-e89b-12d3-a456-426614174000",
  "qrCodeAvailable": true,
  "isPublic": true,
  "socialMediaOptions": ["FACEBOOK", "TWITTER", "LINKEDIN"],
  "emailOptions": ["INVITE_ATTENDEES", "SEND_UPDATE"]
}
```

---

### 8.2 Share Event

**Endpoint:** `POST /api/v1/events/{id}/share`

**Authentication:** Required

**Permission:** `COMMUNICATION_SEND` (with resource: `event_id=#id`)

**Description:** Share an event with attendees via email, social channels, or links.

**Request Body (EventShareRequest):**

```typescript
{
  channel: string;              // "EMAIL" | "LINK" | "SOCIAL", required
  recipients?: string[];       // Email addresses for EMAIL channel
  message?: string;             // Custom message
  includeEventDetails?: boolean; // Include event details
  includeQRCode?: boolean;      // Include QR code
  expirationDate?: string;      // ISO datetime for link expiration
}
```

**Success Response:** `200 OK`

```json
{
  "shareId": "456e7890-e89b-12d3-a456-426614174001",
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "channel": "EMAIL",
  "recipientCount": 5,
  "successfulRecipients": ["user1@example.com", "user2@example.com"],
  "failedRecipients": [],
  "status": "SCHEDULED",
  "shareLink": null,
  "message": "Check out this event!",
  "includeEventDetails": true,
  "includeQRCode": true,
  "createdAt": "2024-01-15T10:30:00",
  "expirationDate": "2024-01-22T10:30:00"
}
```

---

## 9. Event Analytics

### 9.1 Get Event Analytics

**Endpoint:** `GET /api/v1/events/{id}/analytics`

**Authentication:** Required

**Permission:** `EVENT_ANALYTICS_READ` (with resource: `event_id=#id`)

**Description:** Get comprehensive analytics for an event.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "totalViews": 1250,
  "uniqueVisitors": 850,
  "registrationRate": 68.0,
  "attendanceRate": 95.0,
  "engagementMetrics": {
    "averageTimeOnPage": 180,
    "bounceRate": 15.5
  },
  "socialMetrics": {
    "shares": 45,
    "likes": 120
  },
  "geographicDistribution": {
    "US": 60,
    "UK": 20,
    "CA": 10
  },
  "analyticsPeriod": "30d"
}
```

---

## 10. Event Duplication

### 10.1 Duplicate Event

**Endpoint:** `POST /api/v1/events/{id}/duplicate`

**Authentication:** Required

**Permission:** `EVENT_DUPLICATE` (with resource: `event_id=#id`)

**Description:** Create a duplicate of an existing event.

**Request Body (EventDuplicateRequest):**

```typescript
{
  newEventName?: string; // Optional, defaults to "{original name} (Copy)"
}
```

**Success Response:** `200 OK` - Returns EventResponse (new event)

---

## 11. Event Validation & Health Check

### 11.1 Validate Event

**Endpoint:** `GET /api/v1/events/{id}/validation`

**Authentication:** Required

**Permission:** `EVENT_VALIDATION_READ` (with resource: `event_id=#id`)

**Description:** Validate event data and return validation results.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "isValid": true,
  "validationScore": 95,
  "errors": [],
  "warnings": ["Cover image not set"],
  "validationDetails": {
    "hasName": true,
    "hasDescription": true,
    "hasStartDate": true,
    "hasVenue": false
  },
  "validatedAt": "2024-01-15T10:30:00"
}
```

---

### 11.2 Event Health Check

**Endpoint:** `GET /api/v1/events/{id}/health`

**Authentication:** Required

**Permission:** `EVENT_HEALTH_READ` (with resource: `event_id=#id`)

**Description:** Perform a health check on an event.

**Success Response:** `200 OK`

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "healthStatus": "healthy",
  "healthScore": 90,
  "issues": [],
  "recommendations": ["Consider adding a cover image"],
  "componentHealth": {
    "registration": "healthy",
    "notifications": "healthy",
    "media": "warning"
  },
  "checkedAt": "2024-01-15T10:30:00"
}
```

---

## 12. Event Media

### 12.1 Get Event Media

**Endpoint:** `GET /api/v1/events/{id}/media`

**Authentication:** Required

**Permission:** `EVENT_MEDIA_READ` (with resource: `event_id=#id`)

**Description:** Get all media associated with an event.

**Query Parameters:**
- `category` (optional): Media category filter
- `type` (optional): Media type filter

**Success Response:** `200 OK` - Returns List<EventMediaResponse>

---

### 12.2 Upload Event Media

**Endpoint:** `POST /api/v1/events/{id}/media`

**Authentication:** Required

**Permission:** `EVENT_MEDIA_UPLOAD` (with resource: `event_id=#id`)

**Description:** Upload media for an event. Returns presigned URL for upload.

**Request Body (EventMediaUploadRequest):**

```typescript
{
  fileName: string;      // Required
  contentType: string;   // Required (e.g., "image/jpeg")
  fileSize: number;      // Required (bytes)
  category?: string;     // Optional
  description?: string;  // Optional
}
```

**Success Response:** `200 OK`

```json
{
  "uploadUrl": "https://s3.amazonaws.com/...",
  "fileKey": "events/123e4567-e89b-12d3-a456-426614174000/media/...",
  "expiresAt": "2024-01-15T11:00:00"
}
```

---

### 12.3 Get Specific Media

**Endpoint:** `GET /api/v1/events/{id}/media/{mediaId}`

**Authentication:** Required

**Permission:** `EVENT_MEDIA_READ` (with resource: `event_id=#id`)

**Description:** Get details of specific media.

---

### 12.4 Update Media

**Endpoint:** `PUT /api/v1/events/{id}/media/{mediaId}`

**Authentication:** Required

**Permission:** `EVENT_MEDIA_UPDATE` (with resource: `event_id=#id`)

**Description:** Update media information.

**Request Body (EventMediaRequest):**

```typescript
{
  description?: string;
  category?: string;
}
```

---

### 12.5 Delete Media

**Endpoint:** `DELETE /api/v1/events/{id}/media/{mediaId}`

**Authentication:** Required

**Permission:** `EVENT_MEDIA_DELETE` (with resource: `event_id=#id`)

**Description:** Delete media from event.

**Success Response:** `204 No Content`

---

### 12.6 Get Event Assets

**Endpoint:** `GET /api/v1/events/{id}/assets`

**Authentication:** Required

**Permission:** `EVENT_ASSETS_READ` (with resource: `event_id=#id`)

**Description:** Get all assets associated with an event.

---

### 12.7 Upload Event Asset

**Endpoint:** `POST /api/v1/events/{id}/assets`

**Authentication:** Required

**Permission:** `EVENT_ASSETS_UPLOAD` (with resource: `event_id=#id`)

**Description:** Upload an asset for an event.

---

### 12.8 Update Cover Image

**Endpoint:** `PUT /api/v1/events/{id}/cover-image`

**Authentication:** Required

**Permission:** `EVENT_COVER_IMAGE_UPDATE` (with resource: `event_id=#id`)

**Description:** Update the cover image for an event. Returns presigned URL for upload.

**Request Body (EventMediaUploadRequest):**

Same as media upload request.

---

### 12.9 Remove Cover Image

**Endpoint:** `DELETE /api/v1/events/{id}/cover-image`

**Authentication:** Required

**Permission:** `EVENT_COVER_IMAGE_DELETE` (with resource: `event_id=#id`)

**Description:** Remove the cover image from an event.

---

## 13. Event Notifications

### 13.1 Get Notification Settings

**Endpoint:** `GET /api/v1/events/{id}/notifications`

**Authentication:** Required

**Permission:** `EVENT_NOTIFICATION_READ` (with resource: `event_id=#id`)

**Description:** Get notification settings for an event.

**Success Response:** `200 OK` - Returns EventNotificationSettingsResponse

---

### 13.2 Update Notification Settings

**Endpoint:** `PUT /api/v1/events/{id}/notifications`

**Authentication:** Required

**Permission:** `EVENT_NOTIFICATION_UPDATE` (with resource: `event_id=#id`)

**Description:** Update notification settings for an event.

**Request Body (EventNotificationSettingsRequest):**

```typescript
{
  emailNotifications?: boolean;
  smsNotifications?: boolean;
  pushNotifications?: boolean;
  // ... other notification settings
}
```

---

### 13.3 Send Notification

**Endpoint:** `POST /api/v1/events/{id}/notifications/send`

**Authentication:** Required

**Permission:** `EVENT_NOTIFICATION_UPDATE` (with resource: `event_id=#id`)

**Description:** Send a notification for an event.

**Request Body (EventNotificationRequest):**

```typescript
{
  type: string;           // Notification type
  subject?: string;       // Email subject
  message: string;       // Notification message
  recipients?: string[];  // Specific recipients (optional)
}
```

**Success Response:** `200 OK` - Returns EventNotificationResponse

---

### 13.4 Get Event Reminders

**Endpoint:** `GET /api/v1/events/{id}/reminders`

**Authentication:** Required

**Permission:** `EVENT_REMINDER_READ` (with resource: `event_id=#id`)

**Description:** Get all reminders for an event.

**Query Parameters:**
- `page` (optional): Page number, default: 0
- `size` (optional): Page size, default: 20

**Success Response:** `200 OK` - Returns List<EventReminderResponse>

---

### 13.5 Create Reminder

**Endpoint:** `POST /api/v1/events/{id}/reminders`

**Authentication:** Required

**Permission:** `EVENT_REMINDER_CREATE` (with resource: `event_id=#id`)

**Description:** Create a new reminder for an event.

**Request Body (EventReminderRequest):**

```typescript
{
  reminderType: string;        // Required
  reminderDateTime: string;    // ISO datetime, required
  message?: string;            // Optional
  recipients?: string[];      // Optional
}
```

---

### 13.6 Update Reminder

**Endpoint:** `PUT /api/v1/events/{id}/reminders/{reminderId}`

**Authentication:** Required

**Permission:** `EVENT_REMINDER_UPDATE` (with resource: `event_id=#id`)

**Description:** Update an existing reminder.

---

### 13.7 Delete Reminder

**Endpoint:** `DELETE /api/v1/events/{id}/reminders/{reminderId}`

**Authentication:** Required

**Permission:** `EVENT_REMINDER_DELETE` (with resource: `event_id=#id`)

**Description:** Delete a reminder.

**Success Response:** `204 No Content`

---

### 13.8 Get Specific Reminder

**Endpoint:** `GET /api/v1/events/{id}/reminders/{reminderId}`

**Authentication:** Required

**Permission:** `EVENT_REMINDER_READ` (with resource: `event_id=#id`)

**Description:** Get details of a specific reminder.

---

## 14. Event Collaboration

### 14.1 Get Event Collaborators

**Endpoint:** `GET /api/v1/events/{id}/collaborators`

**Authentication:** Required

**Permission:** `ROLE_READ` (with resource: `event_id=#id`)

**Description:** Get list of event collaborators.

**Query Parameters:**
- `page` (optional): Page number, default: 0
- `size` (optional): Page size, default: 20

**Success Response:** `200 OK` - Returns List<EventCollaboratorResponse>

---

### 14.2 Add Collaborator

**Endpoint:** `POST /api/v1/events/{id}/collaborators`

**Authentication:** Required

**Permission:** `ROLE_ASSIGN` (with resource: `event_id=#id`)

**Description:** Add a new collaborator to an event.

**Request Body (EventCollaboratorRequest):**

```typescript
{
  userId: string;        // UUID, required
  role: string;          // Required (e.g., "COLLABORATOR", "EDITOR")
  permissions?: string[]; // Optional
}
```

**Success Response:** `200 OK` - Returns EventCollaboratorResponse

---

### 14.3 Update Collaborator

**Endpoint:** `PUT /api/v1/events/{id}/collaborators/{collaboratorId}`

**Authentication:** Required

**Permission:** `ROLE_UPDATE` (with resource: `event_id=#id`)

**Description:** Update collaborator information.

**Request Body (EventCollaboratorRequest):**

Same as add collaborator.

---

### 14.4 Remove Collaborator

**Endpoint:** `DELETE /api/v1/events/{id}/collaborators/{collaboratorId}`

**Authentication:** Required

**Permission:** `ROLE_REMOVE` (with resource: `event_id=#id`)

**Description:** Remove a collaborator from an event.

**Success Response:** `204 No Content`

---

## DTOs Reference

### Request DTOs

#### CreateEventRequest
- **Purpose:** Create a new event
- **Required Fields:** `name`, `eventType`
- **Optional Fields:** All other event properties
- **Validation:**
  - `name`: Max 255 characters, required
  - `description`: Max 10000 characters
  - `startDateTime`: Must be future or present

#### UpdateEventRequest
- **Purpose:** Update an existing event
- **All Fields:** Optional (only include fields to update)
- **Special Fields:**
  - `venueCleared`: Set to `true` to remove venue association

#### EventListRequest
- **Purpose:** Filter and paginate event listings
- **Fields:**
  - `page`: 0-indexed, default: 0
  - `size`: 1-100, default: 20
  - `status`: Filter by EventStatus
  - `isPublic`: Filter by visibility
  - `startDateFrom`, `startDateTo`: Date range filter
  - `isArchived`: Filter archived events
  - `search`: Search term
  - `sortBy`: Sort field
  - `sortDirection`: "ASC" | "DESC"

#### EventStatusUpdateRequest
- **Purpose:** Update event status
- **Required Fields:** `eventStatus`

#### EventCapacityUpdateRequest
- **Purpose:** Update event capacity
- **Required Fields:** `capacity` (number)

#### EventRegistrationDeadlineRequest
- **Purpose:** Update registration deadline
- **Required Fields:** `deadline` (ISO datetime)

#### EventVisibilityUpdateRequest
- **Purpose:** Update event visibility
- **Required Fields:** `isPublic` (boolean)

#### EventDuplicateRequest
- **Purpose:** Duplicate an event
- **Optional Fields:** `newEventName`

#### EventShareRequest
- **Purpose:** Share an event
- **Required Fields:** `channel` ("EMAIL" | "LINK" | "SOCIAL")
- **Optional Fields:** `recipients`, `message`, `includeEventDetails`, `includeQRCode`, `expirationDate`

#### EventMediaUploadRequest
- **Purpose:** Upload media/asset
- **Required Fields:** `fileName`, `contentType`, `fileSize`
- **Optional Fields:** `category`, `description`

#### EventMediaRequest
- **Purpose:** Update media information
- **Optional Fields:** `description`, `category`

#### EventNotificationSettingsRequest
- **Purpose:** Update notification settings
- **Fields:** Various notification preferences (all optional)

#### EventNotificationRequest
- **Purpose:** Send notification
- **Required Fields:** `type`, `message`
- **Optional Fields:** `subject`, `recipients`

#### EventReminderRequest
- **Purpose:** Create/update reminder
- **Required Fields:** `reminderType`, `reminderDateTime`
- **Optional Fields:** `message`, `recipients`

#### EventCollaboratorRequest
- **Purpose:** Add/update collaborator
- **Required Fields:** `userId`, `role`
- **Optional Fields:** `permissions`

### Response DTOs

#### EventResponse
- **Purpose:** Standard event information
- **Contains:** All event properties including metadata, timestamps, venue info

#### UserEventRelationshipResponse
- **Purpose:** Event information in context of user relationship
- **Contains:** Event details plus `isOwner` flag

#### UserEventsSummaryResponse
- **Purpose:** Summary of user's events
- **Contains:** Counts and lists of events by category

#### EventCapacityResponse
- **Purpose:** Capacity information
- **Contains:** `capacity`, `currentAttendeeCount`, `availableSpots`, `utilizationPercentage`, `isRegistrationOpen`

#### EventQRCodeResponse
- **Purpose:** QR code information
- **Contains:** `qrCode`, `qrCodeEnabled`, `qrCodeImageBase64`, `generatedAt`

#### EventVisibilityResponse
- **Purpose:** Visibility settings
- **Contains:** `isPublic`, `requiresApproval`, `accessLevel`

#### EventShareResponse
- **Purpose:** Share operation result
- **Contains:** `shareId`, `channel`, `recipientCount`, `status`, `shareLink`, etc.

#### EventSharingOptionsResponse
- **Purpose:** Available sharing options
- **Contains:** `availableChannels`, `shareLink`, `socialMediaOptions`, etc.

#### EventAnalyticsResponse
- **Purpose:** Event analytics data
- **Contains:** Views, visitors, rates, engagement metrics, social metrics, geographic distribution

#### EventValidationResponse
- **Purpose:** Validation results
- **Contains:** `isValid`, `validationScore`, `errors`, `warnings`, `validationDetails`

#### EventHealthCheckResponse
- **Purpose:** Health check results
- **Contains:** `healthStatus`, `healthScore`, `issues`, `recommendations`, `componentHealth`

#### EventMediaResponse
- **Purpose:** Media information
- **Contains:** Media details, URLs, metadata

#### EventPresignedUploadResponse
- **Purpose:** Presigned upload URL
- **Contains:** `uploadUrl`, `fileKey`, `expiresAt`

#### EventNotificationSettingsResponse
- **Purpose:** Notification settings
- **Contains:** All notification preferences

#### EventNotificationResponse
- **Purpose:** Notification send result
- **Contains:** Notification details, status, recipients

#### EventReminderResponse
- **Purpose:** Reminder information
- **Contains:** Reminder details, schedule, status

#### EventCollaboratorResponse
- **Purpose:** Collaborator information
- **Contains:** User details, role, permissions

---

## Error Handling

### Error Response Format

All errors follow this structure:

```typescript
{
  error?: string;    // Error code (e.g., "VALIDATION_ERROR")
  message: string;   // Human-readable error message
  status?: number;   // HTTP status code
}
```

### Common Error Codes

| Status | Error Code | Message | What to Show User |
|--------|------------|---------|-------------------|
| `400` | `VALIDATION_ERROR` | Field-specific messages | Show validation errors next to form fields |
| `401` | `AUTHENTICATION_REQUIRED` | "Authentication required" | Redirect to login page |
| `403` | `ACCESS_DENIED` | "Insufficient permissions" | Show error: "You don't have permission to perform this action" |
| `404` | `EVENT_NOT_FOUND` | "Event not found or access denied" | Show error: "Event not found" |
| `409` | `VERSION_MISMATCH` | "Event has been modified by another user" | Show error: "Event was updated. Please refresh and try again." |
| `409` | `OPERATION_IN_PROGRESS` | "Event creation already in progress" | Show error: "Please wait a moment and try again" |

### Error Handling Utility

```javascript
async function handleApiError(response) {
  if (response.ok) {
    return await response.json();
  }

  let errorMessage = 'An error occurred';
  
  try {
    const errorData = await response.json();
    errorMessage = errorData.message || errorMessage;
    
    // Handle specific error codes
    if (response.status === 401) {
      // Unauthorized - clear tokens and redirect to login
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('deviceId');
      window.location.href = '/login';
      throw new Error('Session expired. Please login again.');
    }
    
    if (response.status === 409 && errorData.message.includes('version')) {
      // Optimistic locking failure - refresh event
      throw new Error('Event was updated by another user. Please refresh and try again.');
    }
    
  } catch {
    errorMessage = `HTTP ${response.status}: ${response.statusText}`;
  }

  throw new Error(errorMessage);
}
```

---

## Quick Reference

### Endpoints Summary

| Endpoint | Method | Auth Required | Permission | Purpose |
|----------|--------|----------------|------------|---------|
| `/events` | GET | Yes | `PUBLIC_EVENTS_SEARCH` | List events with filters |
| `/events/{id}` | GET | Yes | `PUBLIC_EVENTS_SEARCH` | Get event by ID |
| `/events` | POST | Yes | `EVENT_CREATE` | Create new event |
| `/events/{id}` | PUT | Yes | `EVENT_UPDATE` | Update event |
| `/events/{id}/archive` | POST | Yes | `EVENT_DELETE` | Archive event |
| `/events/{id}/restore` | POST | Yes | `EVENT_DELETE` | Restore archived event |
| `/events/{id}` | DELETE | Yes | `EVENT_DELETE` | Delete event |
| `/events/user/{userId}` | GET | Yes | `MY_EVENTS_READ` | Get user's events |
| `/events/{id}/status` | GET | Yes | `EVENT_READ` | Get event status |
| `/events/{id}/status` | PUT | Yes | `EVENT_UPDATE` | Update event status |
| `/events/{id}/publish` | POST | Yes | `EVENT_PUBLISH` | Publish event |
| `/events/{id}/cancel` | POST | Yes | `EVENT_CANCEL` | Cancel event |
| `/events/{id}/complete` | POST | Yes | `EVENT_COMPLETE` | Complete event |
| `/events/{id}/open-registration` | POST | Yes | `EVENT_REGISTRATION_OPEN` | Open registration |
| `/events/{id}/close-registration` | POST | Yes | `EVENT_REGISTRATION_CLOSE` | Close registration |
| `/events/search` | GET | Yes | `PUBLIC_EVENTS_SEARCH` | Search events |
| `/events/public` | GET | Yes | `PUBLIC_EVENTS_SEARCH` | Get public events |
| `/events/{id}/capacity` | GET | Yes | `EVENT_READ` | Get capacity info |
| `/events/{id}/capacity` | PUT | Yes | `EVENT_UPDATE` | Update capacity |
| `/events/{id}/qr-code` | GET | Yes | `EVENT_QR_CODE_GENERATE` | Get QR code |
| `/events/{id}/qr-code/generate` | POST | Yes | `EVENT_QR_CODE_GENERATE` | Generate QR code |
| `/events/{id}/qr-code/regenerate` | POST | Yes | `EVENT_QR_CODE_REGENERATE` | Regenerate QR code |
| `/events/{id}/qr-code/image` | GET | Yes | `EVENT_QR_CODE_GENERATE` | Get QR code image |
| `/events/{id}/qr-code` | DELETE | Yes | `EVENT_QR_CODE_DELETE` | Disable QR code |
| `/events/{id}/visibility` | GET | Yes | `EVENT_READ` | Get visibility |
| `/events/{id}/visibility` | PUT | Yes | `EVENT_VISIBILITY_UPDATE` | Update visibility |
| `/events/{id}/share` | GET | Yes | `EVENT_READ` | Get sharing options |
| `/events/{id}/share` | POST | Yes | `COMMUNICATION_SEND` | Share event |
| `/events/{id}/analytics` | GET | Yes | `EVENT_ANALYTICS_READ` | Get analytics |
| `/events/{id}/duplicate` | POST | Yes | `EVENT_DUPLICATE` | Duplicate event |
| `/events/{id}/validation` | GET | Yes | `EVENT_VALIDATION_READ` | Validate event |
| `/events/{id}/health` | GET | Yes | `EVENT_HEALTH_READ` | Health check |
| `/events/{id}/media` | GET | Yes | `EVENT_MEDIA_READ` | Get media |
| `/events/{id}/media` | POST | Yes | `EVENT_MEDIA_UPLOAD` | Upload media |
| `/events/{id}/notifications` | GET | Yes | `EVENT_NOTIFICATION_READ` | Get notification settings |
| `/events/{id}/notifications` | PUT | Yes | `EVENT_NOTIFICATION_UPDATE` | Update notification settings |
| `/events/{id}/notifications/send` | POST | Yes | `EVENT_NOTIFICATION_UPDATE` | Send notification |
| `/events/{id}/reminders` | GET | Yes | `EVENT_REMINDER_READ` | Get reminders |
| `/events/{id}/reminders` | POST | Yes | `EVENT_REMINDER_CREATE` | Create reminder |
| `/events/{id}/collaborators` | GET | Yes | `ROLE_READ` | Get collaborators |
| `/events/{id}/collaborators` | POST | Yes | `ROLE_ASSIGN` | Add collaborator |

### Required Headers for Authenticated Endpoints

```javascript
{
  'Content-Type': 'application/json',
  'Authorization': 'Bearer {accessToken}',
  'X-Device-Id': '{deviceId}' // Optional but recommended
}
```

### Special Headers

- **Idempotency-Key**: For create operations (POST /events)
- **If-Match**: For optimistic locking (PUT/DELETE /events/{id})
- **ETag**: Returned in GET responses for version tracking

### Important Notes

- ✅ All dates use ISO 8601 format (`YYYY-MM-DDTHH:mm:ss`)
- ✅ All timestamps are in UTC
- ✅ Event IDs are UUIDs
- ✅ Pagination is 0-indexed
- ✅ Optimistic locking uses version numbers via ETag/If-Match headers
- ✅ Idempotency keys prevent duplicate creation on retries
- ✅ Access control is enforced at the permission level
- ✅ Public events are accessible to all authenticated users
- ✅ Private events require ownership or appropriate role

---

## Support

For questions or issues, contact the backend team or refer to the API documentation.

**Base URL:** `/api/v1/events`  
**Version:** 1.0

