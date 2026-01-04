# Event Planner Monolith

An AI-powered event planning application that consolidates all microservices into a single Spring Boot monolith.
The application provides comprehensive event management including event creation, attendee management, budget tracking, timeline scheduling, vendor sourcing, and payment processing.
Built with Java 17, Spring Boot, PostgreSQL, and Redis, featuring JWT authentication, RBAC authorization, and AI assistant integration for intelligent event planning.
Supports multi-tenant organizations with role-based access control at event, organization, and system levels.
Deployable as a single JAR with Docker support for simplified infrastructure management.

## Gateway & network

- Kong fronts all client traffic on `http://localhost:8000` (TLS optional on `:8443`, admin on `:8001`) using `infra/kong/kong.yml`.
- Routes: `/` → event-planner; `/ai-service` (stripped) → AI service. Push/email are internal only and have no Kong routes.
- Only Kong publishes ports; event-planner, AI, email, and push stay on the internal Docker network. Kong injects `X-API-Key` for event-planner and `x-ai-secret` for AI requests.
- CORS and security headers live at the gateway (global plugins). Spring CORS remains disabled; services are intended to be reached only through Kong.
- Rate limiting is expected at the gateway; the in-app rate limiter has been removed.
- Secrets: `GATEWAY_SERVICE_API_KEY` and `AI_GATEWAY_SHARED_SECRET` default to local values; keep them in sync with `infra/kong/kong.yml` and override for real deployments.
- Event-planner enforces the service API key with `SERVICE_AUTH_REQUIRE_HEADER=true` to ensure traffic originates from Kong.

## Auth config

- Cognito is the sole IdP. Only these envs are needed: `AUTH_COGNITO_ISSUER_URI`, `AUTH_COGNITO_JWK_SET_URI`, `AUTH_COGNITO_AUDIENCE`, and optional `AUTH_COGNITO_AUTO_PROVISION`.
- Legacy Cognito fields (user pool ID, region, app client ID, enabled toggle) are removed from `application.yml` to reduce config noise.

### Env snippets

```
# ============================================
# Cognito ENDPOINTS
# ============================================
AUTH_COGNITO_ISSUER_URI=https://cognito-idp.us-east-2.amazonaws.com/us-east-2_AbcdefGhi
AUTH_COGNITO_AUDIENCE=***REMOVED***
AUTH_COGNITO_JWK_SET_URI=https://cognito-idp.us-east-2.amazonaws.com/us-east-2_AbcdefGhi/.well-known/jwks.json

# ============================================
# Service Auth (gateway -> event-planner)
# ============================================
SERVICE_AUTH_ENABLED=true
SERVICE_AUTH_REQUIRE_HEADER=true
SERVICE_API_KEY=<match Kong injected X-API-Key>
GATEWAY_SERVICE_API_KEY=<same value used by Kong and SERVICE_API_KEY>
```

## Local usage

- Start the stack through Kong: `docker-compose up --build kong` (or `docker-compose up`).
- Call the API via Kong at `http://localhost:8000/api/...`.
- AI endpoints are reachable via Kong at `http://localhost:8000/ai-service/...`.
