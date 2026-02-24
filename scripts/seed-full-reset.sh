#!/usr/bin/env bash
# seed-full-reset.sh
# Full environment reset for seeding.
# Steps:
#   1. Clean Auth0 IDP (remove seed users)
#   2. Wipe DB (drop schema, recreate PostGIS via Reset_Clean_Up_DB.sh)
#   3. Restart backend (compose-down + compose-up)
#   4. Wait for app to be healthy
#   5. Run FE seed suite (via npx vitest)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

# ── Resolve FE dir ────────────────────────────────────────────────────────────
# SEED_FE_DIR is set by the npm script to $PWD (capsuleapp root).
SEED_FE_DIR="${SEED_FE_DIR:-$PWD}"

# ── Load FE .env for Auth0 / seed vars ───────────────────────────────────────
if [ -f "$SEED_FE_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$SEED_FE_DIR/.env"
  set +a
fi

COMPOSE_DOWN_CMD="${SEED_COMPOSE_DOWN_CMD:-make compose-down}"
COMPOSE_UP_CMD="${SEED_COMPOSE_UP_CMD:-make compose-up ENV_FILE=.env}"

echo "==> Seed full reset"
echo ""

# ── 1. Clean Auth0 ───────────────────────────────────────────────────────────
if [ "${SEED_SKIP_AUTH0_CLEANUP:-}" != "1" ]; then
  echo "==> 1/4 Cleaning Auth0 IDP (removing seed users)..."
  (cd "$SEED_FE_DIR" && npx vitest run test/seed/clean-auth0.test.ts --reporter=verbose 2>/dev/null) || {
    echo "[Warn] Auth0 cleanup failed (backend may be down). Proceeding..."
  }
  echo ""
else
  echo "==> 1/4 Skipping Auth0 cleanup (SEED_SKIP_AUTH0_CLEANUP=1)"
  echo ""
fi

# ── 2. Wipe DB ───────────────────────────────────────────────────────────────
if [ "${SEED_SKIP_DB_RESET:-}" != "1" ]; then
  echo "==> 2/4 Wiping database (Reset_Clean_Up_DB.sh)..."
  echo y | (cd "$BACKEND_DIR" && bash "$SCRIPT_DIR/Reset_Clean_Up_DB.sh") || {
    echo "[Error] DB reset failed."
    exit 1
  }
  echo ""
else
  echo "==> 2/4 Skipping DB reset (SEED_SKIP_DB_RESET=1)"
  echo ""
fi

# ── 3. Restart backend ───────────────────────────────────────────────────────
if [ "${SEED_SKIP_COMPOSE:-}" != "1" ]; then
  echo "==> 3/4 Restarting backend..."
  (cd "$BACKEND_DIR" && $COMPOSE_DOWN_CMD 2>/dev/null || true)
  (cd "$BACKEND_DIR" && $COMPOSE_UP_CMD)
  echo "==> Waiting for event-planner-app to be healthy..."
  for i in $(seq 1 60); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' event-planner-app 2>/dev/null || echo "unknown")
    if [ "$STATUS" = "healthy" ]; then
      echo "==> App is healthy after ~$((i * 3))s"
      sleep 2
      break
    fi
    printf "."
    sleep 3
    if [ "$i" -eq 60 ]; then
      echo ""
      echo "[Warn] Health check timed out after 180s — proceeding (STATUS=$STATUS)"
    fi
  done
  echo ""
else
  echo "==> 3/4 Skipping compose cycle (SEED_SKIP_COMPOSE=1)"
  echo ""
fi

# ── 4. Run seed suite ────────────────────────────────────────────────────────
echo "==> 4/4 Running seed suite..."
(cd "$SEED_FE_DIR" && npx vitest run test/seed/seed.test.ts --reporter=verbose)
echo ""
echo "==> Seed complete."
