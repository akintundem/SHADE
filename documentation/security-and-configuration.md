# Security and configuration

This document covers the **security posture** and **deployment configuration** for the Event Planner backend. It complements [Architecture](architecture.md) and [ER diagram](er-diagram.md). For the complete security audit and remediation list, see `SECURITY_REVIEW.md` in the repository root.

---

## Configuration model

- **Single config file:** `src/main/resources/application.yml`. No `application-prod.yml` or profile-specific YAML for core settings. All overrides come from environment variables.
- **Schema:** Hibernate `ddl-auto: update` or `create` for initial setup. Flyway migrations in `src/main/resources/db/migration/` for production. After initial creation, set `ddl-auto: validate` or rely on Flyway.
- **Secrets:** Never hardcoded in the repo. Use environment variables or a secret manager. Copy `.env.example` to `.env` — do not commit `.env`.

---

## Environment variables reference

### Core application

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile (e.g. `dev`, `prod`, `docker`) |
| `APP_BASE_URL` | Base URL used in outbound emails/links |
| `APP_TICKET_QR_SECRET` | Secret for signing/verifying ticket QR codes |

### Database (PostgreSQL)

| Variable | Purpose |
|----------|---------|
| `DB_HOST` | Database host. Use `host.docker.internal` when app is in Docker and DB is on host. |
| `DB_PORT` | Database port (default 5432) |
| `DB_NAME` | Database name (default `shade_dev`) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `DB_SSLMODE` | SSL mode: `require` (production default), `disable` (local dev) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | One-time `create` for initial schema; then remove or set to `validate` |

### Redis

| Variable | Purpose |
|----------|---------|
| `SPRING_REDIS_HOST` | Redis host |
| `SPRING_REDIS_PORT` | Redis port (default 6379) |
| `SPRING_REDIS_PASSWORD` | Redis password (leave empty if none) |
| `SPRING_REDIS_TIMEOUT` | Connection timeout |
| `SPRING_REDIS_DATABASE` | Redis DB index (default 0) |
| `SPRING_REDIS_POOL_MAX_ACTIVE` | Lettuce pool max active connections |
| `SPRING_REDIS_POOL_MAX_IDLE` | Lettuce pool max idle |
| `SPRING_REDIS_POOL_MIN_IDLE` | Lettuce pool min idle |

Redis is required for the JWT revocation blocklist (keys: `token:revoked:{jti}`) and in-process caching. Without Redis, logout will not revoke JWTs server-side.

### OIDC / Auth0

| Variable | Purpose |
|----------|---------|
| `OIDC_ISSUER_URI` | JWT issuer (e.g. `https://your-tenant.auth0.com/`) |
| `OIDC_JWK_SET_URI` | JWKS URL for JWT validation |
| `OIDC_AUDIENCE` | JWT audience |
| `AUTH0_DOMAIN` | Auth0 domain (optional; Management API) |
| `AUTH0_MANAGEMENT_CLIENT_ID` | Auth0 Management API client ID (optional) |
| `AUTH0_MANAGEMENT_CLIENT_SECRET` | Auth0 Management API client secret (optional) |
| `SECURITY_JWT_AUTO_PROVISION` | `true` to auto-create users on first JWT validation (requires `email_verified=true`) |
| `RBAC_POLICY_LOCATION` | RBAC policy path (e.g. `classpath:rbac/RBAC_policy.yml`) |

### Service authentication (Kong ↔ Monolith)

| Variable | Purpose |
|----------|---------|
| `SERVICE_AUTH_ENABLED` | Enable service API key validation |
| `SERVICE_AUTH_REQUIRE_HEADER` | Require `X-API-Key` for non-actuator requests |
| `SERVICE_API_KEY` | Primary API key (must match `GATEWAY_SERVICE_API_KEY` in Kong) |
| `SERVICE_API_KEY_SECONDARY` | Secondary API key for zero-downtime rotation (optional) |
| `SERVICE_ROLE_ALLOWED_PATHS` | Paths requiring the service key even with a JWT |

### Kong gateway

| Variable | Purpose |
|----------|---------|
| `GATEWAY_SERVICE_API_KEY` | Key Kong injects as `X-API-Key` for the monolith |
| `AI_GATEWAY_SHARED_SECRET` | Secret Kong injects as `x-ai-secret` for the AI service |
| `KONG_CORS_ORIGINS` | Comma-separated CORS origins for production |

### RabbitMQ

| Variable | Purpose |
|----------|---------|
| `RABBITMQ_USER`, `RABBITMQ_PASS` | Broker credentials (no default fallback — required) |
| `RABBITMQ_URL` | AMQP URL (e.g. `amqp://user:pass@rabbitmq:5672`) |
| `RABBITMQ_EXCHANGE` | Exchange name for notification jobs |
| `RABBITMQ_EMAIL_QUEUE`, `RABBITMQ_EMAIL_ROUTING_KEY` | Email queue name and routing key |
| `RABBITMQ_PUSH_QUEUE`, `RABBITMQ_PUSH_ROUTING_KEY` | Push queue name and routing key |
| `RABBITMQ_PREFETCH` | Consumer prefetch count (e.g. `10`) |
| `RABBITMQ_RECONNECT_MS` | Reconnect delay after connection close (e.g. `5000`) |
| `RABBITMQ_REQUEUE_ON_ERROR` | Deprecated — retry is now handled by DLX/`x-retry-count` header; this var is ignored |

### Email (Resend) and push (Firebase)

| Variable | Purpose |
|----------|---------|
| `RESEND_API_KEY` | Resend API key |
| `RESEND_FROM` | Default sender address |
| `ALLOWED_TEMPLATES` | Comma-list of allowed template IDs (empty = all allowed) |
| `APP_ASSET_BUCKET` | Bucket name for email template assets |
| `LOGO_URL` | Logo URL used in email templates |
| `FIREBASE_SERVICE_ACCOUNT_KEY_PATH` | Path inside push-service container (e.g. `/app/cred/firebase.json`). File must live under `push-service/cred/` and is mounted as a read-only volume. |
| `FIREBASE_PROJECT_ID` | Firebase project ID |
| `EXTERNAL_EMAIL_FROM`, `EXTERNAL_EMAIL_FROM_EVENTS` | Default from addresses for outbound email |

### Object storage (MinIO / S3)

| Variable | Purpose |
|----------|---------|
| `AWS_ACCESS_KEY_ID` | MinIO root user (or AWS access key in production) |
| `AWS_SECRET_ACCESS_KEY` | MinIO root password (or AWS secret key in production) |
| `AWS_REGION` | Region (e.g. `us-east-1`; any value works for MinIO) |
| `AWS_S3_USER_BUCKET` | User assets bucket name (e.g. `shade-user-assets`) |
| `AWS_S3_EVENT_BUCKET` | Event assets bucket name (e.g. `shade-event-assets`) |
| `AWS_S3_ENDPOINT` | MinIO API URL. Local (host): `http://localhost:9000`. In Docker: `http://host.docker.internal:9000`. Production AWS: leave empty. |
| `AWS_S3_PATH_STYLE` | `true` for MinIO; `false` for AWS S3 production |

### MinIO (host-level config)

| Variable | Purpose |
|----------|---------|
| `MINIO_ROOT_USER` | MinIO root username. Must match `AWS_ACCESS_KEY_ID`. |
| `MINIO_ROOT_PASSWORD` | MinIO root password. Must match `AWS_SECRET_ACCESS_KEY`. |

MinIO runs natively on the host (Homebrew). See [getting-started.md](getting-started.md#4-minio-object-storage--native-homebrew) for setup.

### AI service

| Variable | Purpose |
|----------|---------|
| `AI_SERVICE_SECRET` | Shared secret for Kong → AI service auth (Kong uses `AI_GATEWAY_SHARED_SECRET`) |
| `EXTERNAL_AI_SERVICE_SECRET` | Optional alternate secret |
| `OPENAI_API_KEY` | OpenAI API key (required for image generation) |
| `AI_OIDC_ISSUER`, `AI_OIDC_AUDIENCE` | Optional OIDC for JWT auth to AI service |
| `OPENAI_IMAGE_MODEL` | OpenAI image model (default `gpt-image-1.5`; falls back to `dall-e-3`) |
| `OPENAI_CHAT_MODEL` | OpenAI chat model for prompt refinement (default `gpt-4o`) |
| `AI_DEBUG_LOGGING` | `true` to log prompt metadata (never logs prompt content in production) |
| `RETURN_BASE64` | `true` to fetch and return generated images as base64 (SSRF-safe) |

### Feeds and cleanup

| Variable | Purpose |
|----------|---------|
| `FEEDS_CLEANUP_MAX_AGE_MINUTES` | Max age (minutes) for incomplete feed uploads before cleanup |
| `FEEDS_CLEANUP_CRON` | Spring cron schedule for cleanup job |

---

## Gateway (Kong)

| Item | Detail |
|------|--------|
| **Service API key** | Kong injects `X-API-Key` using `GATEWAY_SERVICE_API_KEY`. Rotate using secondary key (`SERVICE_API_KEY_SECONDARY`): add secondary → deploy → update callers → promote to primary → clear secondary. |
| **API key always validated** | If `X-API-Key` is present in any request, it is always validated — even when a Bearer JWT is also present. There is no bypass path. |
| **Admin API** | Port 8001 bound to `127.0.0.1` in the container; not published in `docker-compose`. For debug only, temporarily expose with `- "8001:8001"`. |
| **CORS** | Explicit allowlist; no wildcard when `credentials: true`. Dev defaults in `kong.yml`; production sets `KONG_CORS_ORIGINS`. |
| **Header injection** | Kong strips client-supplied `X-API-Key` and `x-ai-secret` before forwarding. Server-side values are injected. |
| **Request size** | 50MB limit configured globally. |
| **Security headers** | Applied globally: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Strict-Transport-Security`, `Content-Security-Policy`, `Permissions-Policy`. |

---

## Monolith (Spring Boot)

| Item | Detail |
|------|--------|
| **Service auth** | `ServiceApiKeyFilter` validates `X-API-Key` when present. If the header is present, it is always checked — never skipped due to a Bearer token. |
| **User auth** | JWT from Auth0. Validated against JWKS. `TokenRevocationFilter` checks Redis JTI blocklist on every request before JWT decode. |
| **Auto-provision** | When `SECURITY_JWT_AUTO_PROVISION=true`, new users are created on first valid JWT. Requires `email_verified=true` in the JWT claims. ADMIN account type cannot be provisioned via OIDC flow. |
| **RBAC** | Policy in `src/main/resources/rbac/RBAC_policy.yml`. SHA-256 digest logged at startup. Permissions with `conditions` are **denied** until condition evaluators are implemented. |
| **Logout** | `POST /api/v1/auth/logout` — extracts `jti` from JWT, stores in Redis with TTL = token remaining validity. All subsequent requests with the same token return 401. |
| **Swagger** | Restricted to `ROLE_ADMIN`. Disabled by default (`SWAGGER_UI_ENABLED=false`, `OPENAPI_DOCS_ENABLED=false`). |
| **Actuator** | `show-details: when-authorized` — requires `ACTUATOR_ADMIN` role. `env` endpoint is not exposed. |
| **CSRF** | Disabled for `/api/**` (JWT stateless). |
| **CORS** | Re-enabled with `CorsConfigurationSource`. Origins from `CORS_ALLOWED_ORIGINS` env var (default `localhost:3000`). |
| **Security headers** | `SecurityHeadersFilter` applies security headers unconditionally (no User-Agent gating). |

---

## JWT revocation (logout)

After logout, the token JTI is stored in Redis:

```
Key:   token:revoked:{jti}
Value: "1"
TTL:   remaining token validity (seconds)
```

`TokenRevocationFilter` runs before the JWT decoder in the security filter chain. If the JTI is in the blocklist, the request is rejected with 401 before any business logic runs.

This requires Redis to be running. If Redis is unavailable, the revocation check will fail and the application will not start successfully.

---

## Object storage security

All S3/MinIO objects are **private**. No bucket uses `mc anonymous set download` or public policies. Object access is exclusively via pre-signed URLs generated at the API read boundary.

**Pattern:** `bare URL (in DB) → S3StorageService.presignedGetUrlFromBareUrl() → signed URL (returned to client)`

Pre-signed URLs default to 1-hour validity. They are single-use in the sense that they expire — the same URL works until expiry. For a new pre-signed URL, the client must call the API again.

See [architecture.md](architecture.md#pre-signed-url-pattern) for the full list of read boundaries where pre-signing is applied.

---

## RBAC summary

- **Policy file:** `src/main/resources/rbac/RBAC_policy.yml`. SHA-256 digest logged at startup for integrity.
- **Evaluation order:** resource resolution → is_authenticated → role_grant → owns_scope → is_member → conditions. **Deny by default.**
- **Scopes:** `SYSTEM` (user-level), `EVENT` (event-scoped), `ORGANIZATION`, `PUBLIC` (unauthenticated allowed).
- **System roles:** `SUPER_ADMIN`, `ORGANIZER`, `USER`. Grant permissions like `event.create`, `event.read`, `user.update`, `admin.*` (SUPER_ADMIN only).
- **Event roles:** `ORGANIZER` (full event control), `COORDINATOR` (no delete/role-assign/budget-approve), `STAFF` (no delete/budget-approve), `VOLUNTEER` (check-in, read, limited timeline), plus specialized roles (SECURITY, TECHNICAL, CATERING, CLEANUP, REGISTRATION, PHOTOGRAPHER, VIDEOGRAPHER, DJ, MC, SPEAKER, MODERATOR, MEDIA, GUEST). `ATTENDEE` is a virtual role for invite/ticket-based access.
- **Conditions:** Permissions that list `conditions` in the policy are **denied** until evaluators are implemented.

---

## Attendee invites

- Token is accepted via **POST** `/api/v1/attendees/invites/accept` with body `{ "token": "...", "status": "ACCEPTED" }`.
- Email links use the token in the **fragment** (e.g. `.../invite/accept#<token>`) — never in the URL query string or path. The front end reads the fragment and calls the POST endpoint.
- Never accept invite tokens via GET or query parameter — they would appear in server logs, browser history, and referrer headers.

---

## AI service

| Item | Detail |
|------|--------|
| **Auth** | Kong-injected `x-ai-secret` validated with `secrets.compare_digest` (constant-time). If `SHARED_SECRET` is configured and `REQUIRE_SECRET=true`, only the shared secret is accepted — no JWT fallback. |
| **Image sizes** | Validated against `ALLOWED_IMAGE_SIZES = {"1024x1024", "1792x1024", "1024x1792"}` before calling OpenAI. Invalid sizes return HTTP 400. |
| **Error responses** | Generic messages only (`"Failed to generate image"`). Exception type logged internally; no stack traces or API error details returned to callers. |
| **SSRF protection** | `RETURN_BASE64=true` fetches generated images from OpenAI CDN. URL validated against allowlisted hosts (`oaidalleapiprodscus.blob.core.windows.net`, `dalleproduse.blob.core.windows.net`). Private/link-local addresses rejected. |
| **Random isolation** | `random.Random(variation_seed)` per request — does not affect global Python random state. |
| **JWKS caching** | JWKS keys cached for `AI_JWKS_CACHE_SECONDS` (default 3600s). |
| **`prompt_used`** | Removed from all API responses. Prompt content never returned to callers. |

---

## Email and push workers

| Item | Detail |
|------|--------|
| **Email recipients** | Max 50 total (`to` + `cc` + `bcc`) per message |
| **Email validation** | Basic format check + 254-char length limit per address |
| **Subject/from limits** | Subject ≤ 500 chars; from ≤ 200 chars |
| **Template registry** | All templates go through a typed registry keyed by constants. No dynamic template loading from user input. |
| **Push token limit** | Max 500 FCM tokens per message |
| **Push payload limits** | title ≤ 500, body ≤ 2000, data ≤ 20 keys, each value ≤ 500 chars |
| **Retry** | Max 3 attempts via `x-retry-count` header; then dead-lettered to `dlq.push` / `dlq.email` |
| **DLQ** | `dlq.push` and `dlq.email` — durable queues. Monitor for non-zero message counts. |
| **Connection errors** | `process.exit(1)` on error; container restarts via orchestrator. Graceful close triggers reconnect after `RABBITMQ_RECONNECT_MS`. |
| **Non-root user** | All three Dockerfiles run as `appuser` (non-root) |

---

## User directory and search

- Directory and user-post list endpoints cap page size at **50** to limit enumeration.
- Ticket listing max page size: **50** (reduced from 100).

---

## Environment and .env files

- **Never commit:** `.env`, `push-service/.env`, `ai-service/.env`, `push-service/cred/*.json`
- Use templates: `.env.example`, `ai-service/.env.example`, `push-service/.env.example`
- Firebase credentials: place under `push-service/cred/`; gitignored; mounted read-only in container
- `docker-compose` loads monolith env from `${ENV_FILE:-.env}`

If `.env` files were ever committed accidentally:
```bash
git rm --cached .env push-service/.env ai-service/.env
echo '.env' >> .gitignore
git commit -m "remove accidentally committed .env files"
```

---

## Reference

- Full security audit and remediation table: `SECURITY_REVIEW.md` (repository root)
- Architecture diagram and service topology: [architecture.md](architecture.md)
- MinIO setup and smoke test: [getting-started.md](getting-started.md#4-minio-object-storage--native-homebrew)
- DLQ operational guide: `SECURITY_REVIEW.md` Section 3
