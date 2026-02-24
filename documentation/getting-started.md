# Getting started

This guide helps you run the Event Planner backend (sade-mono) locally: repository layout, prerequisites, database and service setup, MinIO object storage, how to run the full stack, and how to seed the database.

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
├── email/                   # Node.js/TypeScript email worker (Resend + React Email)
├── push-service/            # Node.js push worker (Firebase)
├── ai-service/              # Python FastAPI (OpenAI cover image)
├── scripts/
│   ├── setup-local.sh       # PostgreSQL + PostGIS + Redis setup (macOS/Homebrew)
│   ├── seed-full-reset.sh   # Full reseed: Auth0 cleanup → DB reset → restart → FE seed suite
│   └── Reset_Clean_Up_DB.sh # Drop schema, re-enable PostGIS
├── docker-compose.yml       # Full stack (Kong, monolith, AI, email, push, RabbitMQ)
├── .env.example             # Env template — copy to .env
└── pom.xml                  # Java 17, Spring Boot 3
```

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java | 17+ | OpenJDK or compatible |
| Maven | 3.8+ | Or use `./mvnw` |
| PostgreSQL | 14+ with PostGIS | For geometry columns |
| Redis | 6+ | Caching and JWT revocation |
| Node.js | 18+ | Email and push workers (if run locally) |
| Python | 3.11+ | AI service (if run locally) |
| Docker & Docker Compose | v2+ | Recommended for full stack |
| MinIO + mc | Latest | Object storage (native Homebrew) |

---

## 1. Clone and env

```bash
git clone <repo-url>
cd sade-mono
cp .env.example .env
# Edit .env — at minimum set: DB_*, SPRING_REDIS_*, OIDC_*,
# GATEWAY_SERVICE_API_KEY, AI_GATEWAY_SHARED_SECRET, RABBITMQ_*,
# MINIO_ROOT_USER, MINIO_ROOT_PASSWORD, AWS_*
```

Do not commit `.env`. See [security-and-configuration.md](security-and-configuration.md#environment-variables-reference) for all variable descriptions.

---

## 2. Database (PostgreSQL + PostGIS)

- **Option A — Script (macOS/Homebrew):**
  ```bash
  ./scripts/setup-local.sh
  ```
  This checks/installs PostgreSQL 16, PostGIS, creates the `shade_dev` database, enables the PostGIS extension, and starts Redis.

- **Option B — Manual:**
  ```bash
  createdb shade_dev
  psql shade_dev -c "CREATE EXTENSION IF NOT EXISTS postgis;"
  ```

Set in `.env`:
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shade_dev
DB_USERNAME=<your-user>
DB_PASSWORD=<your-password>
DB_SSLMODE=disable   # local dev only
```

> **When the app runs in Docker:** set `DB_HOST=host.docker.internal` so the container can reach the host PostgreSQL.

**First-time schema creation:**

```bash
# In .env, set:
SPRING_JPA_HIBERNATE_DDL_AUTO=create
# Start the app once — tables are created.
# Then remove this line (or set to "validate") for all subsequent runs.
```

Alternatively, use `scripts/Reset_Clean_Up_DB.sh` to drop and recreate the schema for a clean reset.

---

## 3. Redis

```bash
# macOS
brew install redis
brew services start redis
```

Set in `.env`:
```
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=   # leave empty if none
```

Redis is required for:
- In-process caching (fallback to Caffeine)
- **JWT revocation blocklist** (`token:revoked:{jti}` keys) — required for logout to work

---

## 4. MinIO (object storage — native Homebrew)

MinIO runs **natively on the host machine**, not in Docker. All media (profile photos, event covers) is stored here. Buckets are private; all access uses **pre-signed URLs**.

### Install and start

```bash
brew install minio/stable/minio minio/stable/mc

# Start (data stored in ~/minio-data)
minio server ~/minio-data --console-address :9001

# Or run as a background service
brew services start minio
```

Access:
- **S3 API:** `http://localhost:9000`
- **Console:** `http://localhost:9001`
- **Default credentials:** set `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` in `.env`

### Create buckets

```bash
# Configure mc alias
mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Create the two application buckets
mc mb local/shade-user-assets
mc mb local/shade-event-assets

# Verify (buckets should be private — no anonymous policy)
mc anonymous get local/shade-user-assets   # should return "Access permission for ... is none"
mc anonymous get local/shade-event-assets  # same
```

> **Do NOT run** `mc anonymous set download local/<bucket>` — buckets must remain private.

### Verify MinIO health

```bash
curl -s http://localhost:9000/minio/health/live
# Expected: HTTP 200 (empty body)
```

### Configure the app

In `.env`:
```
AWS_ACCESS_KEY_ID=<MINIO_ROOT_USER value>
AWS_SECRET_ACCESS_KEY=<MINIO_ROOT_PASSWORD value>
AWS_REGION=us-east-1
AWS_S3_USER_BUCKET=shade-user-assets
AWS_S3_EVENT_BUCKET=shade-event-assets
# For local (Java on host):
AWS_S3_ENDPOINT=http://localhost:9000
# For local (Java in Docker):
# AWS_S3_ENDPOINT=http://host.docker.internal:9000
AWS_S3_PATH_STYLE=true
```

---

## 5. Running the full stack with Docker Compose

With PostgreSQL, Redis, and MinIO running on the host:

```bash
# Ensure .env is populated
docker compose up -d
# Or use the Makefile:
make compose-up
```

This starts:
- Kong (ports 8000, 8443)
- Monolith `java-app` (internal 8080, source hot-reload via DevTools)
- AI service (internal 8000, exposed as `/ai-service/` via Kong)
- Email and push workers
- RabbitMQ (5672, management UI at `127.0.0.1:15672`)

The monolith uses `DB_HOST=host.docker.internal` to reach the host PostgreSQL, and `AWS_S3_ENDPOINT=http://host.docker.internal:9000` for MinIO.

**Health check (wait for healthy):**
```bash
# Poll until healthy (up to ~2 minutes for Maven on first run)
docker inspect --format='{{.State.Health.Status}}' event-planner-app

# Or via actuator
curl -s http://localhost:8000/actuator/health | python3 -m json.tool
```

---

## 6. Running only the monolith (no Docker)

```bash
# Load env (or set in IDE)
export $(grep -v '^#' .env | xargs)

# Run with Maven hot-reload
./mvnw spring-boot:run

# Or build JAR
./mvnw package -DskipTests
java -jar target/event-planner-monolith-*.jar
```

The app listens on port 8080. Call `http://localhost:8080/api/v1/...` directly. If `SERVICE_AUTH_ENABLED=true`, send either a valid JWT or the configured `X-API-Key`.

---

## 7. Running AI, email, and push services locally (without Docker)

**AI service:**
```bash
cd ai-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # set OPENAI_API_KEY, AI_SERVICE_SECRET
uvicorn main:app --reload --port 8000
```

**Email service:**
```bash
cd email
npm install
npm run dev   # runs tsx for dev (compiles on the fly)
```

**Push service:**
```bash
cd push-service
npm install
npm start
```

All three services need `RABBITMQ_*` vars pointing to the running RabbitMQ instance.

---

## 8. Verifying the stack

```bash
# 1. Monolith health (via Kong)
curl -s http://localhost:8000/actuator/health | python3 -m json.tool

# 2. MinIO health
curl -s http://localhost:9000/minio/health/live && echo "MinIO OK"

# 3. RabbitMQ management (localhost only)
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS http://localhost:15672/api/overview | python3 -m json.tool

# 4. AI service (via Kong, requires valid JWT or test with x-ai-secret directly)
curl -s http://localhost:8000/ai-service/health | python3 -m json.tool

# 5. OpenAPI docs (Swagger — requires ROLE_ADMIN or admin JWT)
open http://localhost:8080/swagger-ui
```

---

## 9. MinIO smoke test (before seeding)

Before running a full reseed, verify MinIO object storage is working end-to-end:

```bash
# Step 1 — MinIO is alive
curl -sf http://localhost:9000/minio/health/live && echo "✓ MinIO live"

# Step 2 — Buckets exist
mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD
mc ls local/shade-user-assets && echo "✓ user bucket exists"
mc ls local/shade-event-assets && echo "✓ event bucket exists"

# Step 3 — Upload a test object
curl -sL "https://httpbin.org/image/png" -o /tmp/test-smoke.png
mc cp /tmp/test-smoke.png local/shade-user-assets/smoke-test/test.png
echo "✓ upload OK"

# Step 4 — Verify object exists
mc stat local/shade-user-assets/smoke-test/test.png && echo "✓ object found in MinIO"

# Step 5 — Generate a pre-signed URL and fetch the image
mc share download --expire 1m local/shade-user-assets/smoke-test/test.png
# Copy the URL from output and curl it:
# curl -sI "<presigned-url>" | grep HTTP   # should return 200

# Step 6 — Clean up smoke-test object
mc rm local/shade-user-assets/smoke-test/test.png
echo "✓ smoke test cleanup done"
```

All 6 steps should pass before proceeding to reseed.

---

## 10. Database reset

Drop all tables and re-enable PostGIS (e.g. before re-running with `ddl-auto: create`):

```bash
./scripts/Reset_Clean_Up_DB.sh
```

Reads `DB_*` from `.env`, prompts before dropping the public schema. Then run the app once with `SPRING_JPA_HIBERNATE_DDL_AUTO=create` or apply Flyway migrations.

---

## 11. Full reseed

The full reseed script coordinates: Auth0 cleanup → DB reset → backend restart → frontend seed suite.

```bash
# From the frontend repo (capsuleapp / shimmering-generosity) root:
SEED_FE_DIR=$(pwd) bash /path/to/sade-mono/scripts/seed-full-reset.sh

# Skip individual steps if needed:
SEED_SKIP_AUTH0_CLEANUP=1 \
SEED_FE_DIR=$(pwd) \
bash /path/to/sade-mono/scripts/seed-full-reset.sh
```

Steps performed:
1. **Auth0 cleanup** — runs `test/seed/clean-auth0.test.ts` via vitest (removes seed users from Auth0)
2. **DB reset** — drops and recreates the public schema, re-enables PostGIS
3. **Backend restart** — `make compose-down` + `make compose-up`, waits for health check (up to 3 min)
4. **Seed suite** — runs `test/seed/seed.test.ts` via vitest (creates users, events, tickets, etc.)

> **Before running reseed:** complete the [MinIO smoke test](#9-minio-smoke-test-before-seeding) above to confirm storage is working. The seed suite uploads images as part of seeding.

---

## Next steps

- [Architecture](architecture.md) — Tech stack, request flow, MinIO setup, DLX/DLQ, resilience.
- [API overview](api-overview.md) — Base URL, auth, main endpoints, example flows.
- [Security and configuration](security-and-configuration.md) — Env reference, RBAC, secrets, JWT revocation.
- [ER diagram](er-diagram.md) — Database schema and relationships.
