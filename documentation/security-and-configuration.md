# Security and configuration

This document summarizes **security posture** and **deployment configuration** for the Event Planner backend. It complements the [Architecture](architecture.md) and [ER diagram](er-diagram.md). For the full list of remediations, see **STRICT_SECURITY_REVIEW_REPORT.md** in the repository root.

## Configuration model

- **Single config file:** `src/main/resources/application.yml`. No `application-prod.yml` or other profile-specific YAML for core settings.
- **Schema:** Flyway owns all DDL. Hibernate uses `ddl-auto: validate` and does not create or alter tables.
- **Secrets:** No hardcoded secrets in the repo. Use environment variables or a secret manager.

## Gateway (Kong)

| Item | Detail |
|------|--------|
| **Service API key** | Kong injects `X-API-Key` using `GATEWAY_SERVICE_API_KEY`. Set this in the environment and rotate periodically. The value must not be committed. |
| **Admin API** | Port 8001 is not published in `docker-compose`. For local debug only, you can temporarily expose it. |
| **CORS** | Kong uses an explicit allowlist (no wildcard when credentials are enabled). Defaults in `infra/kong/kong.yml` are dev-oriented; production must set an explicit allowlist (e.g. via `KONG_CORS_ORIGINS` if supported by your deployment). |

## Monolith (Spring Boot)

| Item | Detail |
|------|--------|
| **Service auth** | Requests without a Bearer JWT must present a valid `X-API-Key` (injected by Kong). Internal (service-role) paths always require the API key even when a JWT is present. |
| **User auth** | JWT from OIDC (e.g. Auth0); validated by the monolith. Signup requires a verified email in the token; request-body email is not used as identity. Account linking does not attach an IdP sub to an existing account by email alone. |
| **RBAC** | Policy in `src/main/resources/rbac/RBAC_policy.yml`. Permissions that declare `conditions` are currently **denied** until condition evaluators are implemented. Event roles (COORDINATOR, STAFF, VOLUNTEER) have been reduced to least privilege. |
| **Logout** | Allowed only via **POST** `/api/v1/auth/logout`. |
| **Actuator** | Health details are shown only when authorized (`show-details: when_authorized`). |
| **CSRF** | CSRF cookie is set with HttpOnly. CSRF is disabled for `/api/**` (JWT is used). |

## Attendee invites

- **Token in URL:** Invite acceptance by token does **not** use the query string. Use **POST** `/api/v1/attendees/invites/accept` with body `{ "token": "...", "status": "ACCEPTED" }` or `"DECLINED"`.
- **Email links:** Invite emails link to the app with the token in the **fragment** (e.g. `.../invite/accept#<token>`), so the token is not sent in query or referrer. The front end should read the fragment and call the POST endpoint with the token in the body.

## AI service

- **Gateway secret:** Validated with constant-time comparison (`secrets.compare_digest`). Set `AI_SERVICE_SECRET` (or the env var used by Kong) in the environment.
- **Image URLs:** Fetches are restricted to an allowlisted set of HTTPS hosts (e.g. OpenAI CDN). Timeouts apply.
- **Logging:** Sensitive prompt/error content is only logged when `AI_DEBUG_LOGGING=true`.

## Email and push workers

- **Email (Node):** Max 50 total recipients per message; basic email format and length checks; subject/from length limits.
- **Push (Node):** Max 500 tokens per message; title/body/data length and key-count limits.
- **Queue signing:** Not implemented. Rely on broker ACL and network controls; consider adding HMAC/JWS signing from the monolith and verification in workers as a follow-up.

## User directory and search

- Directory and user-post list endpoints cap page size at **50** to limit enumeration and scraping.

## Environment and .env files

- **Do not commit** `push-service/.env` or `ai-service/.env`. Use `push-service/.env.example` and `ai-service/.env.example` as templates.
- `docker-compose` does not reference `ai-service/.env`; configure the AI service via environment variables passed to the container.
- If `.env` files were ever committed, remove them from tracking:  
  `git rm --cached push-service/.env ai-service/.env`

## Reference

- Full list of remediations: see **STRICT_SECURITY_REVIEW_REPORT.md** in the repository root (section “Remediation Applied”).
