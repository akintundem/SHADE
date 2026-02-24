#!/usr/bin/env bash
# ============================================
# Run full MinIO smoke: reset DB → start backend → get token → create event + cover → print URL
# ============================================
# Requires: .env (backend), capsuleapp/.env (for Auth0 + SEED_USER1_*), jq, curl, docker
# Optional: SEED_FE_DIR=/path/to/capsuleapp (default: ../capsuleapp or $PWD)
# Requires: Docker running (for compose). For backend-on-host, use run-minio-smoke-with-token.sh instead.
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# FE dir for Auth0 token (seed user credentials)
SEED_FE_DIR="${SEED_FE_DIR:-$BACKEND_DIR/../capsuleapp}"
if [[ ! -d "$SEED_FE_DIR" ]]; then
  SEED_FE_DIR="${SEED_FE_DIR:-$(pwd)}"
fi

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ----- 1. Reset DB -----
info "1/5 Resetting database..."
export RESET_DB_NONINTERACTIVE=1
bash "${SCRIPT_DIR}/Reset_Clean_Up_DB.sh"
unset RESET_DB_NONINTERACTIVE

# ----- 2. Ensure DDL create for next startup -----
ENV_FILE="${BACKEND_DIR}/.env"
DDL_LINE="SPRING_JPA_HIBERNATE_DDL_AUTO=create"
if [[ -f "$ENV_FILE" ]] && ! grep -q "^SPRING_JPA_HIBERNATE_DDL_AUTO=" "$ENV_FILE"; then
  info "2/5 Adding SPRING_JPA_HIBERNATE_DDL_AUTO=create to .env for this run..."
  echo "$DDL_LINE" >> "$ENV_FILE"
  DDL_ADDED=1
else
  DDL_ADDED=0
fi

# ----- 3. Start backend and wait for healthy -----
if ! docker info &>/dev/null; then
  error "Docker is not running. Start Docker Desktop (or the daemon), then run this script again."
  exit 1
fi
info "3/5 Starting backend (Docker Compose)..."
cd "$BACKEND_DIR"
docker compose -f docker-compose.yml --env-file "${ENV_FILE}" down 2>/dev/null || true
docker compose -f docker-compose.yml --env-file "${ENV_FILE}" up -d
info "Waiting for event-planner-app to be healthy (up to ~3 min)..."
for i in $(seq 1 60); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' event-planner-app 2>/dev/null || echo "unknown")
  if [[ "$STATUS" == "healthy" ]]; then
    info "App healthy after ~$((i * 3))s"
    break
  fi
  printf "."
  sleep 3
  if [[ $i -eq 60 ]]; then
    echo ""
    error "Health check timed out. Proceeding anyway (STATUS=$STATUS)."
  fi
done
sleep 2

# ----- 4. Get JWT from Auth0 (FE seed user) -----
info "4/5 Getting JWT from Auth0 (SEED_FE_DIR=$SEED_FE_DIR)..."
export SEED_FE_DIR
BEARER_TOKEN=$("${SCRIPT_DIR}/get-auth0-token.sh") || {
  error "Could not get Auth0 token. Ensure capsuleapp/.env has AUTH0_DOMAIN, AUTH0_CLIENT_ID, SEED_USER1_EMAIL, SEED_USER1_PASSWORD and the seed user exists in Auth0 (run FE seed once to create it)."
  exit 1
}

# ----- 5. Run MinIO smoke (create event + cover, print URL) -----
info "5/5 Running MinIO event cover smoke..."
# Use Kong (8000) when backend runs in Docker; use 8080 if backend runs on host
API_BASE_URL="${API_BASE_URL:-http://localhost:8000}" BEARER_TOKEN="$BEARER_TOKEN" SKIP_DB_RESET=1 \
  bash "${SCRIPT_DIR}/minio-event-cover-smoke.sh"

# ----- Cleanup: remove DDL_AUTO so next start doesn't recreate tables -----
if [[ "$DDL_ADDED" == "1" ]] && [[ -f "$ENV_FILE" ]]; then
  info "Removing SPRING_JPA_HIBERNATE_DDL_AUTO=create from .env..."
  grep -v "^SPRING_JPA_HIBERNATE_DDL_AUTO=create" "$ENV_FILE" > "${ENV_FILE}.tmp" && mv "${ENV_FILE}.tmp" "$ENV_FILE"
fi

info "Done. Use the Cover URL printed above to confirm MinIO in the browser."
