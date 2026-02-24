# API overview

This document gives a concise overview of the Event Planner REST API: base URL, versioning, authentication, main resource paths, and where to find full specs.

---

## Base URL and versioning

- **Base URL (via Kong):** `http://localhost:8000` (or your deployed host). Use port 8443 for HTTPS when configured.
- **Monolith APIs:** All under **`/api/v1/`**. Version is in the path; no version in headers.
- **AI service:** Under **`/ai-service/`** (Kong strips the prefix and forwards to the AI service). Example: `POST /ai-service/generate-event-cover-image`.

---

## Authentication

### User requests (web/mobile)

1. User signs in with the IdP (e.g. Auth0) and receives a JWT.
2. Client sends every request with:
   - **Header:** `Authorization: Bearer <JWT>`
3. Kong forwards the request and injects the gateway key (`X-API-Key`). The monolith validates the JWT via OIDC JWKS and enforces RBAC per endpoint.

### Service / internal requests

- Kong injects `X-API-Key` with the value of `GATEWAY_SERVICE_API_KEY`. The monolith validates this when there is no Bearer JWT or when the path is a service-role path.
- For **AI service**, Kong injects `x-ai-secret` with `AI_GATEWAY_SHARED_SECRET`. The AI service validates this (or an optional OIDC JWT).

Clients must **not** send their own `X-API-Key` or `x-ai-secret`; Kong overwrites them to avoid spoofing.

---

## Main REST resource paths

All paths below are relative to the **monolith** (e.g. `http://localhost:8080` when run alone, or `http://localhost:8000` when going through Kong; Kong routes `/` to the monolith).

| Area | Base path | Notes |
|------|------------|--------|
| **Auth** | `/api/v1/auth` | Current user, signup, logout, profile image. |
| **Users** | `/api/v1/auth/users` | User management. |
| **Preferences** | `/api/v1/users/me/preferences` | User preferences. |
| **Social** | `/api/v1/users` | Follow/unfollow, follow status (e.g. `/api/v1/users/{id}/follow`). |
| **Events** | `/api/v1/events` | CRUD, media, reminders, notification settings, waitlist. |
| **Collaboration** | `/api/v1/events/{eventId}/...` | Members, roles, invites. Collaborator invites also at `/api/v1/events/{eventId}/collaborator-invites` and `/api/v1/collaborator-invites/...`. |
| **Budget** | `/api/v1/events/{eventId}/budget` | Budget, categories, line items. |
| **Timeline** | `/api/v1/events/{eventId}/tasks` | Tasks. Checklists: `/api/v1/tasks/{taskId}/checklist`. |
| **Ticket types** | `/api/v1/events/{eventId}/ticket-types` | Ticket type CRUD. |
| **Checkout** | `/api/v1/events/{eventId}/tickets/checkout` | Checkout sessions. |
| **Tickets** | `/api/v1/events/{eventId}/tickets`, `/api/v1/tickets` | Request/validate tickets; ticket wallet. |
| **Templates** | `/api/v1/ticket-type-templates` | User-level ticket type templates. |
| **Attendees** | `/api/v1/attendees` | Registration, invites, RSVP, check-in, QR codes. Invite accept: `POST /api/v1/attendees/invites/accept` with body `{ "token", "status" }`. |
| **Venues** | `/api/v1/venues` | Venue CRUD. |
| **Currencies** | `/api/v1/currencies` | List currencies. |
| **Push** | `/api/v1/push-notifications` | Device token registration. |
| **Feeds** | Under `/api/v1/events/{eventId}/...` | Posts, comments, likes (see Event controllers). |

---

## OpenAPI / Swagger

- **Swagger UI:** `http://localhost:8080/swagger-ui` (or same path via gateway if proxied).
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`.

Use these for exact request/response schemas, path parameters, and required headers.

---

## Example flows

### Create event and upload cover

1. `POST /api/v1/events` with JWT — create event (body: name, dates, access type, etc.).
2. Optionally call AI: `POST /ai-service/generate-event-cover-image` (via Kong; returns image URL or base64).
3. `PUT /api/v1/events/{eventId}/cover-image` or equivalent with image data / URL.

### Attendee invite accept (secure)

1. User receives email with link like `https://app.example.com/invite/accept#<token>` (token in **fragment**).
2. Front end reads fragment and calls `POST /api/v1/attendees/invites/accept` with body `{ "token": "<token>", "status": "ACCEPTED" }` and user’s JWT.

### Collaborator invite accept

1. `POST /api/v1/collaborator-invites/accept` with body containing invite token (or `POST /api/v1/collaborator-invites/{inviteId}/accept`).
2. Requires authenticated user; RBAC permission `collaborator.invite.accept`.

---

## Actuator and health

- **Health:** `GET /actuator/health` — DB, Redis, and other components.
- **Info:** `GET /actuator/info`
- **Metrics:** `GET /actuator/metrics`
- **Prometheus:** `GET /actuator/prometheus`

These are exposed by the monolith; in production they are often only reachable via the gateway or an internal network. The `env` endpoint is intentionally not exposed.

---

## Errors and headers

- **4xx/5xx:** JSON error body (structure may be sanitized; see `SanitizingResponseBodyAdvice` and global exception handler).
- **Kong:** Adds security headers (e.g. `X-Content-Type-Options`, `Referrer-Policy`), `X-Request-ID` (correlation ID), and optional rate-limit headers when enabled.

For full API details, use Swagger UI or `/v3/api-docs` and the [Architecture](architecture.md) and [Security and configuration](security-and-configuration.md) docs for auth and RBAC.
