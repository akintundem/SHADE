# Security and configuration

This document summarizes **security posture** and **deployment configuration** for the Event Planner backend. It complements the [Architecture](architecture.md) and [ER diagram](er-diagram.md). For the full list of remediations, see **STRICT_SECURITY_REVIEW_REPORT.md** in the repository root.

---

## Configuration model

- **Single config file:** `src/main/resources/application.yml`. No `application-prod.yml` or other profile-specific YAML for core settings. Override with environment variables.
- **Schema:** When using Flyway, it owns all DDL; Hibernate uses `ddl-auto: validate`. For initial setup, `ddl-auto: create` may be used once (see `.env.example`), then removed or set to `validate`.
- **Secrets:** No hardcoded secrets in the repo. Use environment variables or a secret manager; copy `.env.example` to `.env` and do not commit `.env`.

---

## Environment variables reference

Variables below are used by the monolith, Docker Compose, Kong, or the AI/email/push services. Required values must be set in `.env` or the deployment environment.

### Core application

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile (e.g. `dev`, `prod`, `docker`). |
| `APP_BASE_URL` | Base URL of the app (used in emails/links). |
| `APP_TICKET_QR_SECRET` | Secret for signing ticket QR codes. |

### Database (PostgreSQL)

| Variable | Purpose |
|----------|---------|
| `DB_HOST` | Database host (use `host.docker.internal` when app runs in Docker and DB on host). |
| `DB_PORT` | Database port. |
| `DB_NAME` | Database name. |
| `DB_USERNAME` | Database user. |
| `DB_PASSWORD` | Database password. |
| `DB_SSLMODE` | SSL mode (e.g. `require`, `disable` for local). |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Optional one-time `create`; then remove or set to `validate`. |

### Redis

| Variable | Purpose |
|----------|---------|
| `SPRING_REDIS_HOST` | Redis host. |
| `SPRING_REDIS_PORT` | Redis port. |
| `SPRING_REDIS_PASSWORD` | Redis password (optional). |
| `SPRING_REDIS_TIMEOUT` | Connection timeout (default in app). |
| `SPRING_REDIS_DATABASE` | Redis DB index (default 0). |
| `SPRING_REDIS_POOL_*` | Lettuce pool settings (optional). |

### OIDC / Auth0

| Variable | Purpose |
|----------|---------|
| `OIDC_ISSUER_URI` | JWT issuer (e.g. `https://your-tenant.auth0.com/`). |
| `OIDC_JWK_SET_URI` | JWKS URL for JWT validation. |
| `OIDC_AUDIENCE` | JWT audience. |
| `AUTH0_DOMAIN` | Auth0 domain (optional; Management API). |
| `AUTH0_MANAGEMENT_CLIENT_ID` | Auth0 Management API client ID (optional). |
| `AUTH0_MANAGEMENT_CLIENT_SECRET` | Auth0 Management API client secret (optional). |
| `SECURITY_JWT_AUTO_PROVISION` | Create user on first JWT validation (true/false). |
| `RBAC_POLICY_LOCATION` | RBAC policy file (e.g. `classpath:rbac/RBAC_policy.yml`). |

### Service authentication (Kong ↔ Monolith)

| Variable | Purpose |
|----------|---------|
| `SERVICE_AUTH_ENABLED` | Enable service API key check. |
| `SERVICE_AUTH_REQUIRE_HEADER` | Require X-API-Key for non-actuator requests. |
| `SERVICE_API_KEY` | Must match `GATEWAY_SERVICE_API_KEY` (Kong injects this). |
| `SERVICE_ROLE_ALLOWED_PATHS` | Paths that require service key even with JWT. |

### Kong gateway

| Variable | Purpose |
|----------|---------|
| `GATEWAY_SERVICE_API_KEY` | Key Kong injects as `X-API-Key` for monolith. Must match monolith `SERVICE_API_KEY`. |
| `AI_GATEWAY_SHARED_SECRET` | Secret Kong injects as `x-ai-secret` for AI service. |
| `KONG_CORS_ORIGINS` | Comma-separated CORS origins for production. |

### RabbitMQ

| Variable | Purpose |
|----------|---------|
| `RABBITMQ_USER`, `RABBITMQ_PASS` | Broker credentials. |
| `RABBITMQ_URL` | AMQP URL. |
| `RABBITMQ_EXCHANGE` | Exchange name. |
| `RABBITMQ_EMAIL_QUEUE`, `RABBITMQ_EMAIL_ROUTING_KEY` | Email queue and routing. |
| `RABBITMQ_PUSH_QUEUE`, `RABBITMQ_PUSH_ROUTING_KEY` | Push queue and routing. |
| `RABBITMQ_PREFETCH`, `RABBITMQ_RECONNECT_MS`, `RABBITMQ_REQUEUE_ON_ERROR` | Worker tuning. |

### Email (Resend) and push (Firebase)

| Variable | Purpose |
|----------|---------|
| `RESEND_API_KEY`, `RESEND_FROM` | Resend API key and from address. |
| `ALLOWED_TEMPLATES` | Comma-list of allowed email template IDs. |
| `APP_ASSET_BUCKET`, `LOGO_URL` | Used in email templates. |
| `FIREBASE_SERVICE_ACCOUNT_KEY_PATH` | Path inside push-service container (e.g. `/app/cred/firebase.json`). |
| `FIREBASE_PROJECT_ID` | Firebase project ID. |

### Object storage (S3 / MinIO)

| Variable | Purpose |
|----------|---------|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | S3 or MinIO credentials. |
| `AWS_REGION` | Region (e.g. `us-east-1`). |
| `AWS_S3_USER_BUCKET`, `AWS_S3_EVENT_BUCKET` | Bucket names. |
| `AWS_S3_ENDPOINT` | MinIO: `http://localhost:9000` or `http://host.docker.internal:9000`. |
| `AWS_S3_PATH_STYLE` | Set `true` for MinIO. |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | MinIO container credentials. |

### AI service

| Variable | Purpose |
|----------|---------|
| `AI_SERVICE_SECRET` | Secret for service-to-service auth (Kong uses `AI_GATEWAY_SHARED_SECRET`). |
| `EXTERNAL_AI_SERVICE_SECRET` | Optional alternate secret. |
| `OPENAI_API_KEY` | OpenAI API key (required for image generation). |
| `AI_OIDC_ISSUER`, `AI_OIDC_AUDIENCE` | Optional OIDC for JWT auth to AI service. |

### Feeds and other

| Variable | Purpose |
|----------|---------|
| `EXTERNAL_EMAIL_FROM`, `EXTERNAL_EMAIL_FROM_EVENTS` | Default from addresses for outbound email. |
| `FEEDS_CLEANUP_MAX_AGE_MINUTES` | Max age for incomplete feed uploads before cleanup. |
| `FEEDS_CLEANUP_CRON` | Cron expression for cleanup job. |

---

## Gateway (Kong)

| Item | Detail |
|------|--------|
| **Service API key** | Kong injects `X-API-Key` using `GATEWAY_SERVICE_API_KEY`. Set this in the environment and rotate periodically. The value must not be committed. |
| **Admin API** | Port 8001 is bound to 127.0.0.1 only in the container and is not published in `docker-compose`. For local debug only, you can temporarily expose it. |
| **CORS** | Kong uses an explicit allowlist (no wildcard when credentials are enabled). Defaults in `infra/kong/kong.yml` are dev-oriented; production should set an explicit allowlist via `KONG_CORS_ORIGINS` if supported by your Kong setup. |
| **Request handling** | Kong strips client-supplied `X-API-Key` and `x-ai-secret` and injects server-side values to prevent spoofing. |

---

## Monolith (Spring Boot)

| Item | Detail |
|------|--------|
| **Service auth** | Requests without a Bearer JWT must present a valid `X-API-Key` (injected by Kong). Internal (service-role) paths always require the API key even when a JWT is present. |
| **User auth** | JWT from OIDC (e.g. Auth0); validated by the monolith. Signup requires a verified email in the token; request-body email is not used as identity. Account linking does not attach an IdP sub to an existing account by email alone. |
| **RBAC** | Policy in `src/main/resources/rbac/RBAC_policy.yml`. Permissions that declare `conditions` are currently **denied** until condition evaluators are implemented. Event roles (COORDINATOR, STAFF, VOLUNTEER, etc.) use least privilege. See [RBAC summary](#rbac-summary) below. |
| **Logout** | Allowed only via **POST** `/api/v1/auth/logout`. |
| **Actuator** | Health, metrics, prometheus, circuitbreakers, retry exposed; `env` is not exposed. Health details configurable (`show-details`). |
| **CSRF** | CSRF cookie is set with HttpOnly. CSRF is disabled for `/api/**` (JWT is used). |

---

## RBAC summary

- **Policy file:** `src/main/resources/rbac/RBAC_policy.yml`. Evaluation order: resource resolution → is_authenticated → role_grant → owns_scope → is_member → conditions. **Deny by default.**
- **Scopes:** Permissions are scoped as **SYSTEM** (user-level), **EVENT** (event-scoped), **ORGANIZATION**, or **PUBLIC** (unauthenticated allowed).
- **System roles (user-level):** `SUPER_ADMIN`, `ORGANIZER`, `USER`. They grant permissions like `event.create`, `event.read`, `user.update`, `admin.*` (SUPER_ADMIN only), etc.
- **Event roles (per-event):** `ORGANIZER` (full event control), `COORDINATOR` (no event delete, no role assign/remove, no budget approve), `STAFF` (no event delete, no budget approve/reject), `VOLUNTEER` (check-in, read, limited timeline), plus specialized roles (SECURITY, TECHNICAL, CATERING, CLEANUP, REGISTRATION, PHOTOGRAPHER, VIDEOGRAPHER, DJ, MC, SPEAKER, MODERATOR, MEDIA, GUEST). **ATTENDEE** is a virtual role for invite/ticket-based access.
- **Conditions:** Permissions that list `conditions` in the policy (e.g. “Owner only”, “SoD”, “audited”) are **denied** until condition evaluators are implemented. Avoid relying on such permissions for access until then.

---

## Attendee invites

- **Token in URL:** Invite acceptance by token does **not** use the query string. Use **POST** `/api/v1/attendees/invites/accept` with body `{ "token": "...", "status": "ACCEPTED" }` or `"DECLINED"`.
- **Email links:** Invite emails should link to the app with the token in the **fragment** (e.g. `.../invite/accept#<token>`), so the token is not sent in query or referrer. The front end reads the fragment and calls the POST endpoint with the token in the body.

---

## AI service

- **Gateway secret:** Validated with constant-time comparison (`secrets.compare_digest`). Set `AI_SERVICE_SECRET` (or the env var used by Kong, `AI_GATEWAY_SHARED_SECRET`) in the environment.
- **Image URLs:** Fetches are restricted to an allowlisted set of HTTPS hosts (e.g. OpenAI CDN). Timeouts apply.
- **Logging:** Sensitive prompt/error content is only logged when `AI_DEBUG_LOGGING=true`.

---

## Email and push workers

- **Email (Node):** Max 50 total recipients per message; basic email format and length checks; subject/from length limits.
- **Push (Node):** Max 500 tokens per message; title/body/data length and key-count limits.
- **Queue signing:** Not implemented. Rely on broker ACL and network controls; consider adding HMAC/JWS signing from the monolith and verification in workers as a follow-up.

---

## User directory and search

- Directory and user-post list endpoints cap page size at **50** to limit enumeration and scraping.

---

## Environment and .env files

- **Do not commit** `.env`, `push-service/.env`, or `ai-service/.env`. Use `.env.example`, `push-service/.env.example`, and `ai-service/.env.example` as templates.
- `docker-compose` loads monolith env from `${ENV_FILE:-.env}`. The AI service is configured via environment variables passed to the container (no `env_file` for `ai-service`).
- If `.env` files were ever committed, remove them from tracking:  
  `git rm --cached .env push-service/.env ai-service/.env`

---

## Reference

- Full list of remediations: see **STRICT_SECURITY_REVIEW_REPORT.md** in the repository root (section “Remediation Applied”).
