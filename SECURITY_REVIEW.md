# Security Review — sade-mono
**Date:** 2026-02-24
**Scope:** Spring Boot monolith + embedded microservices (`push-service`, `email`, `ai-service`)
**Reviewer:** Claude Code (claude-sonnet-4-6)

---

## Table of Contents

1. [Spring Boot Monolith Review](#1-spring-boot-monolith-review)
   - [Authentication & JWT](#authentication--jwt)
   - [Service-to-Service Auth](#service-to-service-auth)
   - [CORS & Security Headers](#cors--security-headers)
   - [Swagger / OpenAPI Exposure](#swagger--openapi-exposure)
   - [Actuator Exposure](#actuator-exposure)
   - [Email Template Injection](#email-template-injection)
   - [Ticket Endpoint Hardening](#ticket-endpoint-hardening)
   - [RBAC Policy Integrity](#rbac-policy-integrity)
   - [JWT Revocation (Logout)](#jwt-revocation-logout)
   - [Storage: Pre-signed URLs](#storage-pre-signed-urls)
   - [Database Security](#database-security)
   - [What Is Good (Monolith)](#what-is-good-monolith)
2. [Microservices Review](#2-microservices-review)
   - [push-service (Node.js)](#push-service-nodejs)
   - [email-service (Node.js/TypeScript)](#email-service-nodejstypescript)
   - [ai-service (Python/FastAPI)](#ai-service-pythonfastapi)
   - [docker-compose Orchestration](#docker-compose-orchestration)
   - [What Is Good (Microservices)](#what-is-good-microservices)
3. [Messaging Architecture: DLX & DLQ](#3-messaging-architecture-dlx--dlq)
   - [Topology](#topology)
   - [Message Lifecycle](#message-lifecycle)
   - [Retry Protocol](#retry-protocol)
   - [Operating the DLQ](#operating-the-dlq)
4. [Fixes Applied](#4-fixes-applied)
5. [Open Findings](#5-open-findings)
6. [Priority Action List](#6-priority-action-list)

---

## 1. Spring Boot Monolith Review

### Authentication & JWT

**Files:** `OidcJwtAuthenticationConverter.java`, `WebSecurityConfig.java`

The monolith acts as an OAuth 2.0 Resource Server. Auth0 issues JWTs; the application validates them against the Auth0 JWKS endpoint. Auto-provisioning (creating a local `UserAccount` on first valid JWT) is controlled by `SECURITY_JWT_AUTO_PROVISION`.

#### CRITICAL — Account Takeover via Unverified Email in Auto-Provisioning
**Severity:** Critical → **Fixed**
**File:** `OidcJwtAuthenticationConverter.java`

The `linkOrProvisionUser` method previously accepted JWTs without verifying the `email_verified` claim. An attacker could register an Auth0 account with an unverified email matching a victim's address, obtain a valid JWT, and have the system auto-link that JWT to the victim's existing account.

**Fix applied:** `linkOrProvisionUser` now checks `email_verified == true` before performing any account linkage. If the claim is absent or false, the user is denied account creation/linking with a 403.

#### HIGH — ADMIN Account Type Linkable via Standard OIDC Flow
**Severity:** High → **Fixed**
**File:** `OidcJwtAuthenticationConverter.java`

Standard OIDC login could previously complete provisioning for accounts whose type resolved to `ADMIN`, allowing privilege escalation via IdP manipulation.

**Fix applied:** An explicit guard in `linkOrProvisionUser` blocks any provisioning or linking attempt that would result in an `ADMIN` account type. Admin accounts can only be created out-of-band.

---

### Service-to-Service Auth

**Files:** `ServiceApiKeyFilter.java`, `ServiceAuthProperties.java`, `application.yml`

Kong authenticates external clients and forwards requests to the Spring app with an `X-API-Key` header.

#### HIGH — API Key Bypassed When Bearer Token Present
**Severity:** High → **Fixed**
**File:** `ServiceApiKeyFilter.java`

The filter previously skipped API key validation when a `Bearer` token was also present. A caller who possessed any valid JWT could bypass the service API key entirely.

**Fix applied:** If `X-API-Key` is present in the request, the filter **always** validates it regardless of other headers. The Bearer token path and the API key path are now fully independent checks.

#### LOW — No API Key Rotation Support
**Severity:** Low → **Fixed**
**Files:** `ServiceAuthProperties.java`, `ServiceApiKeyFilter.java`, `application.yml`

Single API key meant any rotation required a hard cutover with a downtime window.

**Fix applied:** A secondary API key (`SERVICE_API_KEY_SECONDARY`) is now accepted by the filter in parallel with the primary. To rotate: populate the secondary, deploy, update all callers to use the new key, promote the new key to primary, clear the secondary.

---

### CORS & Security Headers

**Files:** `WebSecurityConfig.java`, `SecurityHeadersFilter.java`

#### HIGH — CORS Disabled at Application Level
**Severity:** High → **Fixed**
**File:** `WebSecurityConfig.java`

CORS was disabled with `cors().disable()`. While Kong enforces CORS at the gateway, disabling it at the application layer removed a defence-in-depth control for direct connections (e.g. internal tooling, staging environments without a gateway).

**Fix applied:** CORS is re-enabled with a restrictive `CorsConfigurationSource`. Allowed origins are read from `CORS_ALLOWED_ORIGINS` (default `localhost:3000`). Only required HTTP methods and headers are permitted.

#### LOW — Security Headers Bypassed via User-Agent Spoofing
**Severity:** Low → **Fixed**
**File:** `SecurityHeadersFilter.java`

The filter previously checked the `User-Agent` header and only applied security headers for browser-like agents. Any client that omitted or spoofed the User-Agent received no security headers.

**Fix applied:** User-Agent detection removed entirely. `SecurityHeadersFilter` now unconditionally sets all security headers (`X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`, `Content-Security-Policy`, etc.) on every response.

---

### Swagger / OpenAPI Exposure

**Files:** `WebSecurityConfig.java`, `application.yml`

#### MEDIUM — Swagger Accessible to All Authenticated Users
**Severity:** Medium → **Fixed**

Swagger UI and the OpenAPI spec were accessible to any authenticated user, exposing full API documentation (endpoint paths, request/response schemas, auth requirements).

**Fix applied:**
- Swagger UI access restricted to `ROLE_ADMIN` only in the security filter chain.
- `SWAGGER_UI_ENABLED` defaults to `false` in `application.yml`. Must be explicitly set to `true` to activate.
- `OPENAPI_DOCS_ENABLED` similarly defaults to `false`.

---

### Actuator Exposure

**File:** `application.yml`

#### MEDIUM — Actuator Health Details Publicly Exposed
**Severity:** Medium → **Fixed**

Actuator endpoints returned full internal details (datasource status, disk space, memory, etc.) without authentication, leaking infrastructure topology.

**Fix applied:** `management.endpoint.health.show-details` changed from `always` to `when-authorized`. An `ACTUATOR_ADMIN` role is required to see component-level details. The anonymous response only returns `{"status":"UP"}`.

---

### Email Template Injection

**Files:** `EmailService.java`, `EmailVariableSanitizer.java` (new)

#### HIGH — HTML Injection via Email Template Variables
**Severity:** High → **Fixed**

Email template variables (event names, user display names, descriptions) were passed directly into React Email HTML templates without escaping. An attacker who could control any of these values could inject arbitrary HTML/script content into outbound emails.

**Fix applied:** `EmailVariableSanitizer` (new class) HTML-escapes all string values in the `templateVariables` map before they are queued in `NotificationRequest`. The sanitizer recursively escapes nested maps. Raw HTML in template variables now renders as literal text, not markup.

---

### Ticket Endpoint Hardening

**Files:** `TicketController.java`, `TicketRepository.java`, `TicketService.java`

#### HIGH — SQL Injection via Arbitrary Sort Column
**Severity:** High → **Fixed**
**File:** `TicketController.java`

The `sortBy` query parameter was interpolated directly into a JPA sort expression without validation. Any column name (or SQL fragment) was accepted.

**Fix applied:** `ALLOWED_SORT_FIELDS` whitelist defined in `TicketController`. Requests with an unknown `sortBy` value receive HTTP 400 before the query is executed.

#### HIGH — TOCTOU Race on Ticket Validation
**Severity:** High → **Fixed**
**Files:** `TicketRepository.java`, `TicketService.java`

Concurrent ticket scan requests could both pass the "is ticket valid?" check before either had committed the "mark as used" update — allowing double-entry with a single ticket.

**Fix applied:** `TicketRepository` now provides `findByQrCodeDataForUpdate` using `@Lock(LockModeType.PESSIMISTIC_WRITE)`. `TicketService.validateTicket` is annotated `@Transactional` and uses this method, ensuring only one request can hold the row lock at a time.

#### LOW — Ticket Pagination Max Too Large
**Severity:** Low → **Fixed**
**File:** `TicketController.java`

`MAX_PAGE_SIZE` was 100, allowing bulk data exfiltration per request.

**Fix applied:** Lowered to 50.

---

### RBAC Policy Integrity

**File:** `RbacPolicyStore.java`

#### MEDIUM — RBAC Policy File Tampering Undetectable
**Severity:** Medium → **Fixed**

The RBAC YAML policy loaded at startup from `${RBAC_POLICY_LOCATION}` with no integrity verification. A tampered policy (e.g. via a misconfigured volume mount) would load silently.

**Fix applied:** `RbacPolicyStore` now computes and logs the SHA-256 digest of the raw policy bytes at load time. This log line (`[rbac] policy SHA-256: <hex>`) can be monitored and compared against a known-good value in CI/CD or alerting.

---

### JWT Revocation (Logout)

**Files:** `TokenRevocationService.java` (new), `TokenRevocationFilter.java` (new), `AuthInfoController.java`, `WebSecurityConfig.java`

#### HIGH — No Server-Side Logout / JWT Revocation
**Severity:** High → **Fixed**

Logout was client-side only (delete the token). Stolen tokens remained valid until natural expiry. No mechanism existed to invalidate a specific JWT early.

**Fix applied:**
- `TokenRevocationService`: stores a revoked JWT's `jti` claim in Redis with a TTL matching the token's remaining validity (`token:revoked:{jti}`).
- `TokenRevocationFilter`: runs before the JWT decoder in the filter chain. If the `jti` of the incoming token is in the Redis blocklist, the request is rejected with HTTP 401.
- `AuthInfoController.logout`: extracts the `jti` from the authenticated principal and calls `TokenRevocationService.revoke()`.
- Redis is required for this feature. Key format: `token:revoked:{jti}`.

---

### Storage: Pre-signed URLs

**Files:** `S3StorageService.java`, `EventService.java`, `EventMediaService.java`, `ProfileImageService.java`, `UserAccountService.java`, `FeedPostService.java`, `PostCommentService.java`, `UserFollowService.java`

#### MEDIUM — MinIO/S3 Objects Publicly Readable
**Severity:** Medium → **Fixed**

S3/MinIO buckets were initialized with `mc anonymous set download`, making all objects (profile photos, event cover images, assets) publicly accessible via guessable URLs.

**Fix applied (full implementation):**

Buckets are now private. Object access is exclusively via pre-signed URLs generated at the API read boundary.

**Pattern:**
```
bare URL (stored in DB) → S3StorageService.presignedGetUrlFromBareUrl(BucketAlias, bareUrl, Duration) → time-limited signed URL (returned to caller)
```

`presignedGetUrlFromBareUrl()` parses the stored URL, strips the bucket-name path segment, extracts the object key, and calls `generatePresignedGetUrl()`. Pre-signed URLs default to 1-hour validity.

**Applied at all read boundaries:**
- `EventService.toResponse()`, `toFeedResponse()`, author avatar in feed responses
- `EventMediaService.completeCoverImageUpload()`
- `ProfileImageService.presignProfilePictureUrl()`
- `UserAccountService.toSecureResponse()` — now calls `AuthMapper.toSecureUserResponse(UserAccount, String profilePictureUrl)` overload passing the presigned URL
- `FeedPostService` — post author avatar + repost author avatar
- `PostCommentService.presignAvatar()` — `CommentResponse.from()` now accepts an explicit `avatarUrl` parameter
- `UserFollowService.buildProfileResponse()` — injects `ProfileImageService` for avatar presigning

---

### Database Security

**Files:** `application.yml`, `LocationRepository.java`

#### LOW — Database SSL Not Enforced (Verified Correct)
**Severity:** Low → **No change needed**

Review confirmed `sslmode=require` is the default in the JDBC URL. No override to `disable` is present; any such override would require an explicit environment variable.

#### LOW — Native SQL Queries (PostGIS) Parameterized (Verified Correct)
**Severity:** Low → **No change needed**

All native SQL queries in `LocationRepository` use parameterized placeholders (`?1`, `?2`, etc.) with Spring Data. No string interpolation.

---

### What Is Good (Monolith)

- **Auth0 OIDC integration** — JWKS-based JWT validation, `iss`/`aud`/`exp` checks, no shared-secret auth for end users.
- **RBAC via aspect** — `@RequiresPermission` enforced via AOP; policy loaded from external YAML, not hardcoded.
- **Double-check in controllers** — `AuthorizationService.canAccessEvent()` called in addition to RBAC aspect.
- **Jakarta validation** — `@Valid` enforced at every controller entry point; DTO-level validation not bypassable.
- **Resilience4j circuit breakers** — external calls (S3, email) are wrapped; failures don't cascade to the entire request.
- **CSRF correctly scoped** — CSRF protection disabled only for `/api/**` (stateless JWT API). Session endpoints (if any) remain protected.

---

## 2. Microservices Review

### push-service (Node.js)

**Role:** RabbitMQ consumer — receives push notification jobs from the monolith and delivers them via Firebase Admin SDK (FCM).
**HTTP surface:** None (pure queue consumer). Correct architecture.

---

#### HIGH — All Three Microservice Containers Run as Root
**Severity:** High → **Fixed**
**Files:** `push-service/Dockerfile`, `email/Dockerfile`, `ai-service/Dockerfile`

None of the three Dockerfiles originally included a `USER` directive. All processes ran as UID 0 (root) inside the container. If a container escape or RCE vulnerability was exploited, the attacker would have root on the container filesystem and any mounted volumes.

**Fix applied (`push-service/Dockerfile`):**
```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

Current `push-service/Dockerfile` (complete):
```dockerfile
FROM node:18-alpine
WORKDIR /app

COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi

COPY server.js config.js ./

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

CMD ["npm", "run", "start"]
```

---

#### HIGH — Silent Error Swallowing in RabbitMQ Connection Handlers
**Severity:** High → **Fixed**
**Files:** `push-service/server.js`, `email/server.ts`

Both services originally registered empty error handlers:
```js
connection.on('error', () => {})
channel.on('error', () => {})
```
Connection drops, channel errors, and heartbeat timeouts were silently discarded. The service appeared healthy while processing no messages.

**Fix applied:**
```js
connection.on("error", (err) => {
    console.error("[rabbitmq] connection error:", err);
    process.exit(1);
});
connection.on("close", () => {
    setTimeout(startRabbitConsumer, rabbitmqReconnectMs);
});
```
Errors trigger a logged exit (letting the orchestrator restart the container). Normal connection close triggers reconnect after `RABBITMQ_RECONNECT_MS`.

---

#### HIGH — No Dead Letter Queue — Bad Messages Retry Forever
**Severity:** High → **Fixed**
**Files:** `push-service/server.js`, `email/server.ts`

See [Section 3](#3-messaging-architecture-dlx--dlq) for full architecture details.

**Summary:** Without a DLX, `nack` with `requeue: true` caused infinite retry loops that starved other messages. `ack` on failure silently discarded them.

**Fix applied:** Full DLX/DLQ topology implemented. See [Section 3](#3-messaging-architecture-dlx--dlq) for topology diagram, retry protocol, and operational guidance.

---

#### MEDIUM — Dockerfile Copies `.env` Files Into Image
**Severity:** Medium → **Fixed**
**File:** `push-service/Dockerfile`

`COPY .env* ./` was present, baking any `.env` file on the build host into the image layer. Secrets are extractable via `docker history` or layer inspection from any image copy.

**Fix applied:** The `COPY .env* ./` line was removed. The Firebase service account key is now mounted at runtime as a read-only volume:
```yaml
volumes:
  - ./push-service/cred:/app/cred:ro
```
Secrets are injected via environment variables at container start, not baked into the image.

---

#### LOW — Floating Semver (`^`) on All Dependencies
**Severity:** Low → **Open**
**File:** `push-service/package.json`

All dependencies use `^` (compatible range). `package-lock.json` is committed and `npm ci` is used in the Dockerfile, which pins the exact resolved versions at install time. The risk is limited to the window between `npm install` and `npm ci` in CI. Run `npm audit fix` periodically.

---

#### LOW — No Health Endpoint
**Severity:** Low → **Partially Fixed**
**File:** `docker-compose.yml`

A Docker `HEALTHCHECK` is now defined:
```yaml
healthcheck:
  test: ["CMD", "node", "-e", "process.exit(0)"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 15s
```
This verifies the Node.js runtime is alive. It does not verify the RabbitMQ consumer channel or Firebase connectivity. An HTTP health endpoint checking `channel.checkQueue()` would provide deeper observability.

---

### email-service (Node.js/TypeScript)

**Role:** RabbitMQ consumer — receives email jobs from the monolith and delivers them via the Resend API using React Email templates.
**HTTP surface:** None (pure queue consumer). Correct architecture.

---

#### HIGH — `@ts-nocheck` Disables All TypeScript Type Safety
**Severity:** High → **Fixed**
**File:** `email/server.ts`

`// @ts-nocheck` on line 1 disabled all type checking across the file, making all payload properties implicitly `any`.

**Fix applied:** `// @ts-nocheck` removed. Proper type declarations added:

```typescript
interface EmailJobPayload {
    templateId: string
    to: string | string[]
    cc?: string | string[]
    bcc?: string | string[]
    replyTo?: string
    from?: string
    subject?: string
    variables?: Record<string, unknown>
}

type TemplateLookupEntry = {
    key: string
    id: string
    subject: unknown
    component: (props: never) => unknown
}
```

---

#### HIGH — Silent Error Swallowing (same as push-service)
**Severity:** High → **Fixed**

See [push-service section](#high--silent-error-swallowing-in-rabbitmq-connection-handlers). Identical fix applied to `email/server.ts`.

---

#### HIGH — No Dead Letter Queue (same as push-service)
**Severity:** High → **Fixed**

See [Section 3](#3-messaging-architecture-dlx--dlq) for the full DLX/DLQ architecture. The email service uses `dlq.email` (routing key `dead.email`).

---

#### MEDIUM — `tsx` Used at Runtime in Production
**Severity:** Medium → **Fixed**
**Files:** `email/package.json`, `email/Dockerfile`

`tsx` is a development-only TypeScript execution engine. Running it in production adds transpilation overhead on every startup and requires dev toolchain in the production image.

**Fix applied:** Multi-stage Docker build added. `email/Dockerfile`:

```dockerfile
# --- Build stage ---
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci && npm cache clean --force
COPY . .
RUN npm run build          # runs tsc → dist/

# --- Runtime stage ---
FROM node:18-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY --from=builder /app/dist ./dist

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

CMD ["node", "dist/server.js"]
```

`package.json` scripts:
```json
"build": "tsc",
"start": "node dist/server.js"
```
`tsx` moved to `devDependencies`; not present in the production image.

---

#### MEDIUM — `COPY . .` Without `.dockerignore`
**Severity:** Medium → **Fixed**
**File:** `email/.dockerignore`

Updated `.dockerignore` now excludes:
```
.env
.env.*
node_modules
*.test.ts
*.spec.ts
dist
```

---

#### LOW — Floating Semver (`^`) on All Dependencies
**Severity:** Low → **Open**

See [push-service section](#low--floating-semver--on-all-dependencies). Same status.

---

#### LOW — No Health Endpoint
**Severity:** Low → **Partially Fixed**

Same `HEALTHCHECK` as push-service applied in `docker-compose.yml`. Same limitation — no deep channel check.

---

### ai-service (Python/FastAPI)

**Role:** HTTP API — accepts requests from the monolith/Kong, calls OpenAI for AI-powered image generation.
**HTTP surface:** Yes — exposes REST endpoints at `/generate-cover-image`, `/generate-cover-image-v2`, `/generate-event-cover-image`, and `/health`.

---

#### HIGH — Auth Middleware Logic Flaw (Implicit Fallthrough)
**Severity:** High → **Fixed**
**File:** `ai-service/main.py`

The original middleware had a fragile control flow: when `REQUIRE_SECRET=True` and the secret was absent/wrong, execution fell through to JWT verification rather than returning 401 immediately.

**Fix applied:** The middleware now has explicit branches with no fallthrough. Current logic (`verify_auth` in `main.py:172`):

```
REQUIRE_SECRET=true + SHARED_SECRET configured:
  ✓ secret matches → pass
  ✗ secret missing or wrong → return JSONResponse(401)  ← no fallthrough to JWT

REQUIRE_SECRET=true + SHARED_SECRET NOT configured:
  → fall back to JWT verification (configuration warning logged at startup)

REQUIRE_SECRET=false:
  → accept valid shared secret OR valid JWT
```

`secrets.compare_digest()` is used for constant-time comparison to prevent timing attacks on the shared secret.

---

#### HIGH — Exception Detail Leakage in Error Responses
**Severity:** High → **Fixed**
**File:** `ai-service/main.py`

`raise HTTPException(500, detail=str(e))` returned raw Python exception messages (file paths, library internals, OpenAI API error payloads) to API callers.

**Fix applied:** All exception handlers log the exception type internally and return generic messages:
```python
# Before
raise HTTPException(status_code=500, detail=f"Failed to generate image: {str(e)}")

# After
print(f"[ai] Error generating image: {type(e).__name__}")
raise HTTPException(status_code=500, detail="Failed to generate image")
```

---

#### HIGH — `prompt_used` Returned in API Responses
**Severity:** High → **Fixed**
**File:** `ai-service/main.py`

The `ImageGenerationResponse` model previously included `prompt_used`, exposing full system prompt structure, template variables, and prompt engineering techniques to API callers.

**Fix applied:** `prompt_used` field removed from `ImageGenerationResponse` and all three endpoint return statements. Prompts are internal to the service. Debug logging of prompt metadata (length only, not content) is gated behind `AI_DEBUG_LOGGING=true`.

---

#### MEDIUM — Unvalidated Image Dimensions
**Severity:** Medium → **Fixed**
**File:** `ai-service/main.py`

Image dimensions were forwarded to OpenAI without validation. Arbitrary dimensions caused expensive API failures.

**Fix applied:** `ALLOWED_IMAGE_SIZES` constant defined at module level:
```python
ALLOWED_IMAGE_SIZES = {"1024x1024", "1792x1024", "1024x1792"}
```
All three endpoints validate `{width}x{height}` against this set and return HTTP 400 for unsupported sizes before calling OpenAI.

---

#### MEDIUM — `random.seed()` with User-Controlled Value Pollutes Global State
**Severity:** Medium → **Fixed**
**File:** `ai-service/main.py`

`random.seed(variation_seed)` with a caller-controlled value set the global Python random state, affecting all concurrent requests in the same process.

**Fix applied:** Isolated `random.Random` instance used per invocation:
```python
rng = random.Random(variation_seed)   # isolated — does not affect global state
selected_style = rng.choice(artistic_styles)
```

---

#### LOW — Outdated Dependencies / `python-jose`
**Severity:** Low → **Fixed**
**File:** `ai-service/requirements.txt`

`python-jose` was effectively unmaintained. All dependencies were behind current stable.

**Fix applied:** `python-jose` replaced with `PyJWT[cryptography]==2.9.0`. JWT verification now uses `RSAAlgorithm.from_jwk()` to load keys from the JWKS endpoint. All other dependencies updated to current stable:

```
fastapi==0.115.0
uvicorn[standard]==0.32.0
python-dotenv==1.0.1
openai==1.54.0
pillow==10.4.0
httpx==0.27.2
pydantic==2.9.2
pydantic-settings==2.5.2
PyJWT[cryptography]==2.9.0
```

---

#### LOW — Container Runs as Root
**Severity:** Low → **Fixed**

`ai-service/Dockerfile` now includes:
```dockerfile
RUN useradd -r -s /bin/false appuser
USER appuser
```

---

#### Additional Security Controls (ai-service)

**SSRF protection on base64 fetch:** When `RETURN_BASE64=true`, the service fetches the generated image URL. `_is_safe_image_url()` validates that the URL is `https`, restricts the host to known OpenAI CDN hostnames (`oaidalleapiprodscus.blob.core.windows.net`, `dalleproduse.blob.core.windows.net`), and rejects private/link-local addresses.

**JWKS caching:** JWKS keys are cached in memory for `AI_JWKS_CACHE_SECONDS` (default 3600s) to avoid JWKS endpoint hammering on every request.

---

### docker-compose Orchestration

---

#### MEDIUM — RabbitMQ Management UI Exposed on All Interfaces
**Severity:** Medium → **Fixed**
**File:** `docker-compose.yml`

Port 15672 was bound to `0.0.0.0`, exposing full admin access on any non-localhost network interface.

**Fix applied:**
```yaml
ports:
  - "5672:5672"
  - "127.0.0.1:15672:15672"
```

---

#### MEDIUM — Default Credentials on RabbitMQ and MinIO
**Severity:** Medium → **Fixed**
**File:** `docker-compose.yml`

`:-guest` and `:-minioadmin` fallback defaults meant services started with well-known credentials when env vars were absent.

**Fix applied:** Fallback defaults removed entirely. Missing vars cause startup failure:
```yaml
RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}   # required — no default
RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS}   # required — no default
```

---

#### MEDIUM — MinIO Buckets Set to Public Anonymous Download
**Severity:** Medium → **Fixed**
**File:** `docker-compose.yml`

`mc anonymous set download` commands removed from MinIO init. Buckets are private. All object access is via pre-signed URLs (see [Storage: Pre-signed URLs](#storage-pre-signed-urls)).

---

#### LOW — No Resource Limits on Any Container
**Severity:** Low → **Fixed**
**File:** `docker-compose.yml`

Resource limits applied to all services:

| Service | CPUs | Memory |
|---|---|---|
| `rabbitmq` | 1.0 | 512M |
| `email-service` | 0.5 | 256M |
| `push-service` | 0.5 | 256M |
| `ai-service` | 1.0 | 512M |
| `java-app` | 2.0 | 1G |

---

#### LOW — Floating Image Tags
**Severity:** Low → **Open**
**File:** `docker-compose.yml`

Tags like `latest`, `3.13-management`, `3.7` can be silently updated upstream. A `docker compose pull` could pull a breaking change or compromised image.

**Recommended fix (not yet applied):**
```yaml
rabbitmq:3.13-management@sha256:<digest>
```

---

#### LOW — No Health Checks on Microservice Containers
**Severity:** Low → **Fixed**
**File:** `docker-compose.yml`

`push-service` and `email-service` now have health checks. `ai-service` has a Dockerfile-level `HEALTHCHECK` using `curl -f http://localhost:8000/health`:

```yaml
healthcheck:
  test: ["CMD", "node", "-e", "process.exit(0)"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 15s
```

---

### What Is Good (Microservices)

- **No HTTP surface on push-service and email-service** — both are pure RabbitMQ consumers. No auth surface to attack.
- **Email service input validation** — email regex validation, field length limits, `MAX_RECIPIENTS_TOTAL=50`, `MAX_SUBJECT_LENGTH=500`, hardcoded `TEMPLATE_REGISTRY` allowlist preventing arbitrary template loading.
- **Kong admin API bound to `127.0.0.1:8001`** — not published to the host network.
- **ai-service JWT verification** — JWKS-based RS256 verification with `iss`, `aud`, and `exp` validation via PyJWT.
- **Template registry pattern** — all React Email templates go through a typed registry keyed by constants. No dynamic template loading from user input.
- **All microservice secrets via environment variables** — no hardcoded credentials in source. `.gitignore` covers all `.env` files and credential directories.
- **Firebase key mounted as volume** — not baked into the image layer.

---

## 3. Messaging Architecture: DLX & DLQ

This section documents the complete RabbitMQ dead-letter topology implemented in `push-service/server.js` and `email/server.ts`.

### Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRODUCER (Java monolith)                  │
└───────────────────────────┬─────────────────────────────────────┘
                            │ publish
                            ▼
                    ┌───────────────┐
                    │  notifications │  (direct exchange, durable)
                    │   exchange     │
                    └───────┬───────┘
             ┌─────────────┴──────────────┐
             │ routing key:               │ routing key:
             │ push.notifications         │ email.notifications
             ▼                            ▼
    ┌────────────────┐          ┌────────────────┐
    │  push.queue    │          │  email.queue   │  (durable)
    │  x-dlx →       │          │  x-dlx →       │
    │  dlx.notif.    │          │  dlx.notif.    │
    │  x-dlrk →      │          │  x-dlrk →      │
    │  dead.push     │          │  dead.email    │
    └────────┬───────┘          └────────┬───────┘
             │ consume                   │ consume
             ▼                           ▼
    [push-service]             [email-service]
             │                           │
     on failure                  on failure
    (nack, no requeue)          (nack, no requeue)
             │                           │
             └─────────────┬─────────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ dlx.notifications│  (direct exchange, durable)
                  └────────┬────────┘
               ┌───────────┴────────────┐
               │ routing key: dead.push  │ routing key: dead.email
               ▼                         ▼
      ┌────────────────┐       ┌─────────────────┐
      │   dlq.push     │       │   dlq.email      │  (durable)
      └────────────────┘       └─────────────────┘
               │                         │
               └───────────┬─────────────┘
                           │
               Monitor via RabbitMQ Management UI
               (http://localhost:15672 — localhost only)
```

---

### Message Lifecycle

A message goes through the following states:

```
PUBLISHED → DELIVERED → [consumer processing]
                                │
                    ┌───────────┼────────────────────┐
                    │           │                    │
               success    transient error      permanent error
                    │           │            (bad JSON / 400 validation)
               channel.ack      │                    │
                    │           ▼                    ▼
               [done]    retry count              channel.nack
                         < MAX_RETRIES?           (requeue=false)
                         /           \                 │
                       yes           no                │
                        │             │                │
                  republish to    channel.nack          │
                  main exchange   (requeue=false)       │
                  with            │                    │
                  x-retry-count+1  │                   │
                                   └─────────┬─────────┘
                                             │
                                             ▼
                                   DLX routes to DLQ
                                   (dlq.push / dlq.email)
```

---

### Retry Protocol

Both services implement identical retry logic:

| Constant | Value | Purpose |
|---|---|---|
| `MAX_RETRIES` | `3` | Maximum delivery attempts before dead-lettering |
| `DLX_EXCHANGE` | `dlx.notifications` | Dead-letter exchange name (shared) |
| `DLQ` (push) | `dlq.push` | Dead-letter queue for push notifications |
| `DLQ` (email) | `dlq.email` | Dead-letter queue for emails |
| Header | `x-retry-count` | Integer tracking attempt number (0-indexed on first retry) |

**Retry flow (transient errors only):**

1. Message arrives; consumer processes it.
2. If processing throws a non-400 error:
   - Read `x-retry-count` from message headers (default 0).
   - Increment: `retryCount = (x-retry-count || 0) + 1`.
   - If `retryCount >= MAX_RETRIES` (3): `nack(msg, false, false)` → routed to DLQ by RabbitMQ.
   - Otherwise: `ack` the original + `publish` a copy to the main exchange with `x-retry-count: retryCount` header.
3. Republishing to the tail of the queue (rather than `nack + requeue`) prevents head-of-line blocking.

**Permanent failure fast-path:**

| Condition | Action |
|---|---|
| `JSON.parse` throws | `nack(msg, false, false)` immediately (no retry) |
| Error has `statusCode === 400` | `nack(msg, false, false)` immediately (no retry) |

These messages go directly to the DLQ on first failure.

---

### Operating the DLQ

#### Inspecting dead-lettered messages

```bash
# RabbitMQ Management HTTP API (localhost only)
curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
  "http://localhost:15672/api/queues/%2F/dlq.push" | jq '.messages'

curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
  "http://localhost:15672/api/queues/%2F/dlq.email" | jq '.messages'
```

#### Peeking at a dead-lettered message (non-destructive)

```bash
curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
  -X POST "http://localhost:15672/api/queues/%2F/dlq.push/get" \
  -H "Content-Type: application/json" \
  -d '{"count": 1, "ackmode": "ack_requeue_true", "encoding": "auto"}' | jq .
```

The response includes:
- `payload` — the original message body (JSON)
- `properties.headers` — includes `x-retry-count`, `x-death` (RabbitMQ-injected death history), original routing key, original exchange, and reason for dead-lettering.

`x-death` array from RabbitMQ shows the full death history:
```json
"x-death": [{
  "count": 1,
  "exchange": "notifications",
  "queue": "push.queue",
  "reason": "rejected",
  "routing-keys": ["push.notifications"],
  "time": "..."
}]
```

#### Replaying a dead-lettered message

After fixing the root cause (e.g., correcting a downstream dependency), republish from the DLQ to the main exchange:

```bash
# 1. Get the message from the DLQ (destructive — removes it)
curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
  -X POST "http://localhost:15672/api/queues/%2F/dlq.push/get" \
  -H "Content-Type: application/json" \
  -d '{"count": 1, "ackmode": "ack_requeue_false", "encoding": "auto"}' \
  | jq -r '.[0].payload' > /tmp/replayed.json

# 2. Republish to the main exchange (resets retry count)
curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
  -X POST "http://localhost:15672/api/exchanges/%2F/notifications/publish" \
  -H "Content-Type: application/json" \
  -d "{
    \"routing_key\": \"push.notifications\",
    \"payload\": $(cat /tmp/replayed.json | jq -Rs .),
    \"payload_encoding\": \"string\",
    \"properties\": {}
  }"
```

#### Monitoring alert thresholds

| Queue | Alert when |
|---|---|
| `dlq.push` | `messages > 0` (any dead letter is noteworthy) |
| `dlq.email` | `messages > 0` |
| `push.queue` | `messages > 100` sustained (consumer lag) |
| `email.queue` | `messages > 100` sustained (consumer lag) |

Configure these as alerts in your monitoring system (Prometheus RabbitMQ exporter, Datadog, Grafana, etc.).

---

## 4. Fixes Applied

All issues resolved during the review session (2026-02-24):

### Spring Boot Monolith

| # | Finding | Severity | File(s) Changed |
|---|---|---|---|
| 2 | Account takeover via unverified email in JWT auto-provisioning | Critical | `OidcJwtAuthenticationConverter.java` |
| 4 | CORS disabled at application level | High | `WebSecurityConfig.java` |
| 5 | Swagger accessible to all authenticated users | Medium | `WebSecurityConfig.java`, `application.yml` |
| 6 | X-API-Key bypassed when Bearer token present | High | `ServiceApiKeyFilter.java` |
| 7 | Actuator health details publicly exposed | Medium | `application.yml` |
| 8 | HTML injection via email template variables | High | `EmailService.java`, `EmailVariableSanitizer.java` (new) |
| 9 | Arbitrary sort column injection in ticket listing | High | `TicketController.java` |
| 10 | TOCTOU race condition on ticket validation | High | `TicketRepository.java`, `TicketService.java` |
| 11 | RBAC policy file tampering undetectable | Medium | `RbacPolicyStore.java` |
| 12 | Security headers bypassed via User-Agent spoofing | Low | `SecurityHeadersFilter.java` |
| 13 | No server-side logout / JWT revocation | High | `TokenRevocationService.java` (new), `TokenRevocationFilter.java` (new), `AuthInfoController.java`, `WebSecurityConfig.java` |
| 14 | ADMIN accounts linkable via standard OIDC flow | High | `OidcJwtAuthenticationConverter.java` |
| 16 | Database SSL enforcement | Low | Verified correct, no change needed |
| 17 | No API key rotation support | Low | `ServiceAuthProperties.java`, `ServiceApiKeyFilter.java`, `application.yml` |
| 18 | Ticket pagination max too large (was 100) | Low | `TicketController.java` |
| 19 | CSRF incorrectly configured | Low | Verified correct, no change needed |
| 20 | Native SQL queries unparameterized | Low | Verified parameterized, no change needed |
| PS | Pre-signed URLs for S3/MinIO object access | Medium | `S3StorageService.java`, `EventService.java`, `EventMediaService.java`, `ProfileImageService.java`, `UserAccountService.java`, `FeedPostService.java`, `PostCommentService.java`, `UserFollowService.java`, `CommentResponse.java`, `AuthMapper.java`, `UserFollowService.java` |

### Microservices

| # | Finding | Severity | File(s) Changed |
|---|---|---|---|
| MS-H1 | All containers run as root | High | `push-service/Dockerfile`, `email/Dockerfile`, `ai-service/Dockerfile` |
| MS-H2 | `@ts-nocheck` disables all TypeScript safety | High | `email/server.ts` |
| MS-H3 | Silent RabbitMQ error swallowing | High | `push-service/server.js`, `email/server.ts` |
| MS-H4 | No Dead Letter Exchange — bad messages retry forever | High | `push-service/server.js`, `email/server.ts` |
| MS-H5 | Auth middleware implicit fallthrough | High | `ai-service/main.py` |
| MS-H6 | Exception detail leakage in responses | High | `ai-service/main.py` |
| MS-H7 | `prompt_used` exposed in API responses | High | `ai-service/main.py` |
| MS-M1 | `.env` files copied into push-service image | Medium | `push-service/Dockerfile` |
| MS-M2 | `COPY . .` without `.dockerignore` | Medium | `email/.dockerignore` |
| MS-M3 | `tsx` runtime in production | Medium | `email/Dockerfile`, `email/package.json` |
| MS-M4 | Unvalidated image dimensions | Medium | `ai-service/main.py` |
| MS-M5 | `random.seed()` with user-controlled value | Medium | `ai-service/main.py` |
| MS-M6 | RabbitMQ management UI on all interfaces | Medium | `docker-compose.yml` |
| MS-M7 | Default credentials on RabbitMQ and MinIO | Medium | `docker-compose.yml` |
| MS-M8 | MinIO buckets publicly readable | Medium | `docker-compose.yml` |
| MS-L2 | `python-jose` (unmaintained) | Low | `ai-service/requirements.txt`, `ai-service/main.py` |
| MS-L3 | No resource limits on containers | Low | `docker-compose.yml` |
| MS-L5 | No health checks on microservice containers | Low | `docker-compose.yml`, `ai-service/Dockerfile` |

---

## 5. Open Findings

The following findings are documented but **not yet remediated**. They require manual attention or scheduled sprint work.

| Severity | ID | Finding | Service | File | Action |
|---|---|---|---|---|---|
| Low | MS-L1 | Floating semver `^` on Node.js deps | push, email | `package.json` | Run `npm audit fix` periodically; `package-lock.json` committed + `npm ci` in Dockerfile mitigates |
| Low | MS-L4 | Floating Docker image tags (`latest`, `3.13-management`, `3.7`) | infra | `docker-compose.yml` | Pin to SHA digests for production |

---

## 6. Priority Action List

### Completed

- [x] Add `USER` directive to all three microservice Dockerfiles
- [x] Fix auth middleware fallthrough in `ai-service/main.py` (explicit 401 on wrong/missing secret)
- [x] Remove `prompt_used` from AI service response models
- [x] Remove `COPY .env* ./` from `push-service/Dockerfile`; mount credentials as read-only volume
- [x] Add/update `.dockerignore` for both Node services
- [x] Remove `// @ts-nocheck` from `email/server.ts`; add `EmailJobPayload` interface and `TemplateLookupEntry` type
- [x] Add error logging + `process.exit(1)` to `connection.on('error', ...)` in both Node services
- [x] Add reconnect on `connection.on('close', ...)` with `RABBITMQ_RECONNECT_MS` delay
- [x] Replace exception strings in AI service error responses with generic messages; log type internally
- [x] Validate image dimensions against `ALLOWED_IMAGE_SIZES` whitelist before calling OpenAI
- [x] Replace `random.seed(user_value)` with local `random.Random(user_value)` instance
- [x] Restrict RabbitMQ management port to `127.0.0.1:15672:15672`
- [x] Remove `:-guest` / `:-minioadmin` fallback defaults from `docker-compose.yml`
- [x] Remove `mc anonymous set download` commands; buckets are now private
- [x] Compile email service for production: multi-stage Dockerfile with `tsc`; runtime uses `node dist/server.js`
- [x] Implement Dead Letter Exchange (`dlx.notifications`) in both Node services; retry up to 3× via `x-retry-count`, then dead-letter to `dlq.push` / `dlq.email`
- [x] Add resource limits to all containers in `docker-compose.yml`
- [x] Add container health checks to push-service and email-service
- [x] Migrate `python-jose` → `PyJWT[cryptography]==2.9.0`; update all ai-service deps to current stable
- [x] Implement pre-signed URLs for MinIO/S3 at all API read boundaries
- [x] Fix account takeover via unverified email in JWT auto-provisioning
- [x] Re-enable CORS with restrictive configuration in `WebSecurityConfig.java`
- [x] Restrict Swagger to `ROLE_ADMIN`; default `SWAGGER_UI_ENABLED=false`
- [x] Fix X-API-Key bypass when Bearer token present
- [x] Set actuator `show-details: when-authorized` + `ACTUATOR_ADMIN` role
- [x] Sanitize email template variables via `EmailVariableSanitizer`
- [x] Whitelist `sortBy` in `TicketController`; throw 400 on unknown field
- [x] Add pessimistic write lock (`PESSIMISTIC_WRITE`) to ticket validation
- [x] Add SHA-256 digest logging to `RbacPolicyStore`
- [x] Remove User-Agent gate from `SecurityHeadersFilter` — headers now unconditional
- [x] Implement JWT revocation via Redis (`TokenRevocationService`, `TokenRevocationFilter`)
- [x] Block ADMIN type in `linkOrProvisionUser`
- [x] Add secondary API key support for zero-downtime rotation

### Remaining Backlog

- [ ] **MS-L4** Pin Docker image tags to SHA digests in `docker-compose.yml`
- [ ] **MS-L1** Run `npm audit fix` on `push-service` and `email` dependencies; update to latest non-breaking versions
- [ ] Add deep health check for push-service: HTTP endpoint that verifies `channel.checkQueue()` and Firebase SDK connectivity
- [ ] Add deep health check for email-service: HTTP endpoint that verifies channel and Resend API reachability
