# Getting started

This guide helps you run the Event Planner backend (sade-mono) locally: repository layout, prerequisites, database and service setup, and how to run the full stack or only the monolith.

---

## Repository layout

```
sade-mono/
├── documentation/          # All docs (README, architecture, er-diagram, security, this file, api-overview)
├── src/
│   ├── main/java/           # Spring Boot application (eventplanner.*)
│   └── main/resources/
│       ├── application.yml  # Main configuration
│       ├── db/migration/    # Flyway SQL migrations (when used)
│       └── rbac/            # RBAC_policy.yml
├── infra/kong/              # Kong declarative config (kong.yml)
├── email/                   # Node.js email worker (Resend + React Email)
├── push-service/            # Node.js push worker (Firebase)
├── ai-service/              # Python FastAPI (OpenAI cover image)
├── scripts/
│   ├── setup-local.sh       # PostgreSQL + PostGIS + MinIO (macOS/Homebrew)
│   └── Reset_Clean_Up_DB.sh # Drop schema, re-enable PostGIS
├── docker-compose.yml      # Full stack
├── .env.example             # Env template — copy to .env
└── pom.xml                  # Java 17, Spring Boot 3
```

---

## Prerequisites

- **Java 17+** (OpenJDK or compatible)
- **Maven** (or use wrapper `./mvnw`)
- **PostgreSQL** with **PostGIS** (for geometry columns if used)
- **Redis**
- **Node.js** (for email and push workers when run locally)
- **Python 3.x** (for AI service when run locally)
- **Docker & Docker Compose** (optional but recommended for full stack)

---

## 1. Clone and env

```bash
git clone <repo-url>
cd sade-mono
cp .env.example .env
# Edit .env and set at least: DB_*, SPRING_REDIS_*, OIDC_*, GATEWAY_SERVICE_API_KEY, AI_GATEWAY_SHARED_SECRET, etc.
```

Do not commit `.env`. See [security-and-configuration.md](security-and-configuration.md#environment-variables-reference) for variable descriptions.

---

## 2. Database (PostgreSQL + PostGIS)

- **Option A — Script (macOS/Homebrew):**  
  `./scripts/setup-local.sh`  
  This checks/installs PostgreSQL 16, PostGIS, creates a DB (default `shade_dev`), and can start MinIO via Docker.

- **Option B — Manual:**  
  Create a database, enable PostGIS:  
  `CREATE EXTENSION IF NOT EXISTS postgis;`

Set in `.env`:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- For local dev without SSL: `DB_SSLMODE=disable` (or leave empty if app default is acceptable)

**First-time schema:**  
If you are not using Flyway migrations yet, run the app once with:

```bash
SPRING_JPA_HIBERNATE_DDL_AUTO=create
```

Then remove this or set to `validate` for subsequent runs. Alternatively use `scripts/Reset_Clean_Up_DB.sh` to drop the schema and run again with `create` if needed.

---

## 3. Redis

Install and start Redis (e.g. `brew install redis && brew services start redis` on macOS). Set in `.env`:

- `SPRING_REDIS_HOST=localhost`
- `SPRING_REDIS_PORT=6379`
- `SPRING_REDIS_PASSWORD=` (empty if none)

---

## 4. Running the full stack with Docker Compose

With PostgreSQL and Redis available (on host or elsewhere), you can run the rest via Docker Compose:

```bash
# From repo root; ensure .env is populated (GATEWAY_SERVICE_API_KEY, AI_GATEWAY_SHARED_SECRET, RABBITMQ_*, etc.)
docker compose up -d
```

This starts:

- Kong (ports 8000, 8443)
- Monolith (java-app, internal 8080)
- AI service (internal 8000, exposed as `/ai-service` via Kong)
- Email and push workers
- RabbitMQ (5672, 15672)
- MinIO (9000, 9001) and minio-init

The monolith uses `env_file: .env` and may need `DB_HOST=host.docker.internal` if PostgreSQL runs on the host.

**Health check:**

- Kong: `curl -s http://localhost:8000/actuator/health` (proxied to monolith)
- Monolith directly (if port 8080 exposed for dev): `curl -s http://localhost:8080/actuator/health`

---

## 5. Running only the monolith (no Docker)

Useful for development with hot-reload (e.g. Spring DevTools).

1. Start PostgreSQL and Redis (and optionally RabbitMQ, MinIO, or mocks).
2. Ensure `.env` is loaded (e.g. `export $(grep -v '^#' .env | xargs)` or use an env file in your IDE).
3. Run:

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/event-planner-monolith-*.jar
```

The app listens on port 8080. Call `http://localhost:8080/api/v1/...` and, if service auth is enabled, either send a valid JWT or the configured `X-API-Key` header (as Kong would inject).

---

## 6. Running AI, email, and push services locally (without Docker)

- **AI service:**  
  `cd ai-service && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt`  
  Copy `ai-service/.env.example` to `ai-service/.env`, set `OPENAI_API_KEY`, `AI_SERVICE_SECRET`.  
  `uvicorn main:app --reload --port 8000`

- **Email service:**  
  `cd email && npm install && npm run dev`  
  Set `RESEND_API_KEY`, `RESEND_FROM`, `RABBITMQ_*` in env.

- **Push service:**  
  `cd push-service && npm install && npm start`  
  Set `FIREBASE_SERVICE_ACCOUNT_KEY_PATH`, `FIREBASE_PROJECT_ID`, `RABBITMQ_*` in env.

The monolith publishes email and push jobs to RabbitMQ; ensure the same exchange, queues, and routing keys are configured in both monolith and workers.

---

## 7. Verifying the stack

- **Monolith health:**  
  `GET http://localhost:8080/actuator/health` (or via Kong at `http://localhost:8000/actuator/health`).

- **OpenAPI:**  
  `http://localhost:8080/swagger-ui` and `http://localhost:8080/v3/api-docs` (or through gateway if routed).

- **AI service (via Kong):**  
  `POST http://localhost:8000/ai-service/health` or the generate endpoints with `x-ai-secret` (injected by Kong when using Docker).

- **Kong:**  
  CORS, request-size limiting, and correlation ID are applied; check response headers for `X-Request-ID`, `X-Content-Type-Options`, etc.

---

## 8. Database reset (development)

To drop all tables and re-enable PostGIS (e.g. before re-running with `ddl-auto: create`):

```bash
./scripts/Reset_Clean_Up_DB.sh
```

It reads `DB_*` from `.env` and prompts before dropping the public schema. Then run the app once with `SPRING_JPA_HIBERNATE_DDL_AUTO=create` or apply Flyway migrations.

---

## Next steps

- [Architecture](architecture.md) — Tech stack, request flow, resilience, observability.
- [API overview](api-overview.md) — Base URL, auth, main endpoints.
- [Security and configuration](security-and-configuration.md) — Env reference, RBAC, invite flows, secrets.
