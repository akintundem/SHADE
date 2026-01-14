# Event Planner Monolith

An AI-powered event planning application that consolidates all microservices into a single Spring Boot monolith.
The application provides comprehensive event management including event creation, attendee management, budget tracking, timeline scheduling, vendor sourcing, and payment processing.
Built with Java 17, Spring Boot, PostgreSQL, and Redis, featuring JWT authentication, RBAC authorization, and AI assistant integration for intelligent event planning.
Supports multi-tenant organizations with role-based access control at event, organization, and system levels.
Deployable as a single JAR with Docker support for simplified infrastructure management.

## Gateway & network

- Kong fronts all client traffic on `http://localhost:8000` (TLS optional on `:8443`, admin on `:8001`) using `infra/kong/kong.yml`.
- Routes: `/` → event-planner; `/ai-service` (stripped) → AI service. Push/email are internal only and have no Kong routes.
- Kong is the only public entry; event-planner, AI, email, and push stay on the internal Docker network. Kong injects `X-API-Key` for event-planner and `x-ai-secret` for AI requests.
- CORS and security headers live at the gateway (global plugins). Spring CORS remains disabled; services are intended to be reached only through Kong.
- Rate limiting is expected at the gateway; the in-app rate limiter has been removed.
- Secrets: `GATEWAY_SERVICE_API_KEY` (default `shade_service_api_key_12345`) and `AI_GATEWAY_SHARED_SECRET` default to local values; keep them in sync with `infra/kong/kong.yml` and override for real deployments.
- Event-planner enforces the service API key with `SERVICE_AUTH_REQUIRE_HEADER=true` to ensure traffic originates from Kong.

## Auth config

- Event-planner and AI service validate Cognito JWTs directly. Set `COGNITO_ISSUER_URI` (e.g., `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_AbcdefGhi`) and `COGNITO_AUDIENCE` (app client ID) in the app environment; set `AI_COGNITO_ISSUER`/`AI_COGNITO_AUDIENCE` for the AI service. Kong simply forwards the `Authorization: Bearer` header.
- Configure gateway-forwarded identity header if needed: `GATEWAY_USER_ID_HEADER` (default `X-User-Id`) and `GATEWAY_AUTO_PROVISION` (default `true`) to auto-create users when unseen.
- Service-to-service proof stays enforced via `SERVICE_AUTH_REQUIRE_HEADER=true` and matching `SERVICE_API_KEY`/`GATEWAY_SERVICE_API_KEY`.

## Account management

- `DELETE /api/v1/auth/users/{userId}` (requires `user.delete` with self-scope) deactivates an account by marking it `DELETED`; directory listings return only `ACTIVE` users. When `AWS_COGNITO_USER_POOL_ID`/`AWS_COGNITO_REGION` are set, the Cognito user is removed too.

## Local usage

- Start the stack through Kong: `docker-compose up --build kong` (or `docker-compose up`).
- Call the API via Kong at `http://localhost:8000/api/...`.
- AI endpoints are reachable via Kong at `http://localhost:8000/ai-service/...`.
