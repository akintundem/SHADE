# SHADE — Event Planning Platform (Sunsetted)

> **Status:** Sunsetted, April 2026. Public archive of the SHADE backend monolith — the core API and supporting services for what was meant to be a full-stack event planning platform. The code runs, the architecture is real, and most of the features below are implemented end to end. It is no longer actively developed and the hosted environment has been torn down.

---

## What SHADE was

SHADE was an event planning platform aimed at people who organize events seriously — not casual meetups, but birthdays-with-budgets, fundraisers, weddings, ticketed gatherings, anything where the host has to think about RSVPs, money, vendors, logistics, and communications all at once.

The thesis was that existing tools fragment this work across five products — Eventbrite for tickets, Splitwise for budgets, Google Sheets for guest lists, WhatsApp for coordination, Canva for invites — and an organizer ends up duct-taping their event together across tabs. SHADE wanted to be the single workspace where you could:

- Create an event with rich access controls (open, RSVP-only, invite-only, ticketed)
- Sell tickets with tiers, promo codes, dependencies, and waitlists
- Track the budget in real time as money came in and went out
- Manage tasks and a timeline with collaborators
- Invite attendees, take RSVPs, check them in at the door with QR codes
- Run a private event feed where invitees could post, comment, and react
- Auto-generate a cover image with AI when you couldn't be bothered
- Send transactional email and push notifications without writing a line of code
- Bring co-hosts on with granular role-based permissions

The vision was to take the operational chaos out of hosting, so the host could spend time on the parts that actually matter — the people, the food, the moment.

---

## Why it's sunsetted

I joined the **North Forge accelerator program** with SHADE and went through the customer discovery phase — dozens of interviews with the people I thought were my customers. The pattern that came back was uncomfortable but unmistakable:

> People will choose to be stressed for free over paying to be calm.

The folks I interviewed had real, repeated pain points organizing events. They described the duct-tape-across-five-tools problem in their own words. They could quantify the wasted hours. And then, when asked what they'd pay to make it go away — they would not. Even small monthly numbers got pushback. The free-but-painful path won, every time, because the pain was diffuse and the cost was concrete.

I could have pushed harder — pivoted to B2B, gone after professional event planners, layered on a marketplace, found a wedge. But the customer-discovery work made it clear the consumer thesis I cared about was structurally weak, and I lost the conviction to keep going. Building a startup you don't believe in is the worst thing you can do with your time.

So I'm putting SHADE down with the codebase intact, public, and honest about what it is and isn't.

---

## What's actually built

This is the **`sade-mono`** repo — the backend monolith and the three supporting microservices. There was a separate React Native mobile app and a web client; those aren't included here.

### Architecture at a glance

A Java Spring Boot monolith behind a Kong API gateway, with three out-of-process workers (AI, email, push) and external Postgres / Redis / MinIO. Auth is handled by Auth0 OIDC. Async work runs over RabbitMQ with a proper DLX/DLQ topology. See [`documentation/architecture.md`](documentation/architecture.md) for the full diagram, ports, and request flow — it's a serious doc and it's accurate.

```
Web/Mobile → Auth0 (sign-in) → Kong gateway → Spring Boot monolith → Postgres
                                            ↘ AI service (FastAPI)   ↘ Redis (cache, JWT revocation)
                                            ↘ RabbitMQ → email worker (Resend)
                                                       → push worker (Firebase)
                                            ↘ MinIO (S3-compatible, pre-signed URLs only)
```

### Tech stack

| Layer | Choice |
|---|---|
| Gateway | Kong 3.x, declarative config |
| API | Java 17, Spring Boot 3.3, Spring Security 6 (OAuth2 Resource Server) |
| Auth | Auth0 OIDC (migrated from AWS Cognito mid-build) |
| DB | PostgreSQL + PostGIS (geometry on venues) |
| Cache | Redis (Lettuce) — Caffeine in-process fallback, JWT revocation blocklist |
| Queue | RabbitMQ 3.13 with DLX/DLQ + retry headers |
| Storage | MinIO (S3-compatible), private buckets, pre-signed URLs everywhere |
| AI | Python FastAPI + OpenAI image generation |
| Email | Node/TypeScript worker (Resend + React Email templates) |
| Push | Node worker (Firebase Cloud Messaging) |
| RBAC | Custom `@RequiresPermission` aspect + YAML policy with SHA-256 integrity check |
| Resilience | Resilience4j circuit breakers + Spring Retry |

---

## Features — where each one stands

This is the honest accounting. "Done" means fully implemented with security review applied. "Working" means it runs but I'd want to revisit it. "Stub" means scaffolding only.

### Authentication & users — **Done**

- Auth0 OIDC JWT validation with JWKS rotation
- Auto-provisioning on first login (gated on `email_verified` — verified emails only, deliberate)
- Logout revokes the JWT's `jti` via a Redis blocklist; `TokenRevocationFilter` checks before validation on every request
- Account-linking guard: ADMIN accounts cannot be linked through the OIDC flow (prevents privilege-escalation via OIDC-merge attacks)
- Service-to-service auth via Kong-injected `X-API-Key` (Kong strips client-supplied values to prevent spoofing)
- API key rotation supported (primary + secondary)

### Events — **Done**

- Full CRUD with status, visibility, four access types (open, RSVP, invite-only, ticketed)
- Venue: embedded address or linked venue entity with PostGIS geometry
- Cover images generated by AI or uploaded directly; stored in MinIO; surfaced via pre-signed URLs at every read boundary (1-hour expiry)
- Reminders, notification settings, waitlist support
- Per-event feeds (posts, comments, likes) — basically a private social wall scoped to invitees

### Collaboration — **Done**

- Event members (`event_users`), event roles (`event_roles`), per-member permissions (`event_user_permissions`)
- Collaborator invites with token-based accept flow — token comes via POST body, **never query string** (deliberate; query-string tokens leak to logs and Referer headers)
- RBAC permission `collaborator.invite.accept` enforced at the controller layer

### Attendees — **Done**

- Registration, RSVP, check-in, invites
- Invite token delivered in URL **fragment** (`#<token>`) so the front end reads it client-side and POSTs it to the API — fragment doesn't hit server logs, doesn't leak in Referer
- QR codes generated for tickets and check-in (HMAC-signed; the signing secret is `APP_TICKET_QR_SECRET`)

### Tickets — **Done**

- Ticket types with price tiers, promo codes, dependencies, waitlist support, approval requests
- Pessimistic write lock on validation to prevent the TOCTOU race that lets the same ticket get redeemed twice
- Max page size 50 (deliberate cap — also in `sortBy` whitelist to prevent ORDER BY injection)
- User-level ticket type templates (so you can save your "early bird / general / VIP" structure and reuse it across events)
- Checkout sessions

### Budget — **Done**

- One budget per event, multiple categories, line items, revenue tracking
- Designed to feed off the ticket revenue so it updates in real time as ticketed events sold

### Timeline / Tasks — **Working**

- Tasks scoped to events, with checklists per task
- The data model is solid; the front-end view of this was the part that needed the most polish

### Feeds — **Done**

- Posts, comments, likes — scoped to event invitees
- Author avatar URLs pre-signed at every read boundary (avoids the trap of storing public S3 URLs that need to be rotated when buckets change)
- Cleanup scheduler for incomplete uploads (cron-based, configurable max-age)

### Social — **Done**

- Follow / unfollow / follow-status
- Profile responses include pre-signed avatar URLs

### Communications — **Done**

- Email and push are out-of-process workers consuming from RabbitMQ
- DLX/DLQ topology with `x-retry-count` header — max 3 attempts on transient errors, immediate dead-letter on permanent (e.g. HTTP 400 from Resend)
- Email templates: React Email, server-side rendered, all template variables HTML-escaped (closes a stored-XSS vector that was open early on)
- Allowlist of permitted template IDs (`ALLOWED_TEMPLATES`) to prevent template-injection from compromised callers
- Push payloads validated before publishing to the queue
- Connection-loss handling: workers `process.exit(1)` on hard errors so the orchestrator restarts them; soft reconnects on graceful close

### AI cover-image generation — **Done**

- FastAPI service, OpenAI image generation
- Kong-injected `x-ai-secret` validated with `secrets.compare_digest` (constant-time, prevents timing attacks)
- SSRF protection: image-fetch URLs validated against an allowlist of hosts
- Image dimensions validated against an `ALLOWED_IMAGE_SIZES` whitelist (prevents resource-exhaustion via giant requested sizes)
- Migrated `python-jose` → `PyJWT[cryptography]` mid-project after the `python-jose` advisory

### Object storage — **Done**

- All media private. No anonymous public reads. No `mc anonymous set download`.
- Bare object URL stored in DB; `S3StorageService.presignedGetUrlFromBareUrl()` parses the stored URL, strips the bucket segment, generates a time-limited signed URL on every read
- Applied at every read boundary: event responses, event media, profile pictures, user accounts, feed post avatars, comment avatars, follow/follower responses

### RBAC — **Done**

- `@RequiresPermission("event.update")` style annotations on controller methods
- Policy in `src/main/resources/rbac/RBAC_policy.yml`
- SHA-256 digest of the policy file logged at startup (so a policy tamper shows up in logs)
- Service roles configurable via `SERVICE_ROLE_ALLOWED_PATHS`

### Observability — **Working**

- Spring Actuator: health, info, metrics, prometheus, circuitbreakers, retry
- `/actuator/health` requires `ACTUATOR_ADMIN` for full details (`when-authorized`)
- `/actuator/env` deliberately not exposed
- Custom Prometheus metrics for notification and email send rates
- Kong correlation ID (`X-Request-ID`) propagated through to monolith logs
- DLQ alert thresholds documented (`dlq.push.messages > 0` or `dlq.email.messages > 0`)

### Resilience — **Done**

- Resilience4j circuit breakers on `notificationService`, `emailService`, `s3Service`
- Spring Retry with exponential backoff; ignores `BadRequestException` and similar non-retryable exceptions
- HikariCP tuned (max 20, min idle 5)
- All Dockerfiles run as non-root user
- Container resource limits set in `docker-compose.yml`

---

## Repository layout

```
sade-mono/
├── documentation/          # Detailed architecture, ER diagram, security & config, API overview
├── src/
│   ├── main/java/eventplanner/
│   │   ├── features/       # Domain modules: event, ticket, attendee, budget, timeline,
│   │   │                   # feeds, social, collaboration, venue, currency
│   │   ├── security/       # Filters, RBAC, auth, OIDC config
│   │   └── common/         # Storage (MinIO), communications (RabbitMQ), exception, config
│   └── main/resources/
│       ├── application.yml
│       ├── db/migration/   # Flyway migrations
│       ├── rbac/RBAC_policy.yml
│       └── emails/         # React Email templates (TSX)
├── ai-service/             # Python FastAPI — OpenAI image generation
├── email/                  # Node/TypeScript — Resend + React Email worker
├── push-service/           # Node — Firebase Cloud Messaging worker
├── infra/kong/             # Kong declarative config
├── scripts/                # Local dev: setup, seeding, smoke tests, DB reset
├── docker-compose.yml      # Full stack except Postgres/Redis/MinIO (host-side)
└── pom.xml                 # Java 17, Spring Boot 3.3
```

For deeper docs see [`documentation/`](documentation/) — the architecture, ER diagram, security review, and getting-started guides are all there and accurate as of sunset.

---

## Running it (if you want to)

The project is sunsetted but the code still runs locally. You'll need:

- Java 17, Maven, Docker, Postgres + PostGIS, Redis, MinIO + `mc`
- Node 18+, Python 3.11+ if you want to run the workers natively
- An Auth0 tenant if you want full auth (or stub it out)

```bash
git clone https://github.com/akintundem/SHADE.git sade-mono
cd sade-mono
cp .env.example .env
# fill in DB_*, OIDC_*, SERVICE_API_KEY, AI_*, MINIO_*, etc.
./scripts/setup-local.sh         # PostgreSQL + PostGIS + Redis (macOS/Homebrew)
docker compose up -d
```

See [`documentation/getting-started.md`](documentation/getting-started.md) for the full setup, including the MinIO bootstrap and seed scripts.

---

## What I'd do differently

A short, honest list. If anyone reading this is building something similar:

- **Validate willingness-to-pay before architecture.** I built six months of platform before doing serious customer interviews. The interviews would have killed the project before any of this code existed if I'd done them first. The build was technically rewarding; commercially it was the wrong order.
- **Pick one anchor feature and ship it.** SHADE tried to be five products. Even sunsetted, the codebase tells you that — the breadth was a hedge against not knowing which feature would land. A sharper version would have shipped just ticketing or just collaboration and learned from real users earlier.
- **Don't migrate auth providers mid-build.** Going from Cognito to Auth0 cost two weeks of yak-shaving. Pick one early, accept the trade-offs.
- **Pre-signed URLs from day one.** Setting up object storage with public ACLs and migrating to private + pre-signed URLs is painful. Start private, never go back.
- **DLQ before you need it.** Adding dead-letter queues to RabbitMQ retroactively is a refactor; building them in from the first email job is a config file.

---

## License

No open license. This repo is public-readable as an archive and portfolio reference. All rights reserved — code here is not licensed for use, modification, or redistribution. If you want to do something with any part of it, reach out.

---

## Contact

Built by **Mayokun Akintunde** as part of the North Forge accelerator program, 2025–2026.

The bones of SHADE are here. If any of it is useful to someone else figuring out an event-platform problem, that's worth more than the project shipping.
