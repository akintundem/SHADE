#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Reset / Clean Up Database
# ============================================
# Drops all tables in the public schema and re-enables PostGIS.
# Loads DB_* from .env in repo root if present.
#
# Usage: ./scripts/Reset_Clean_Up_DB.sh
# Non-interactive: RESET_DB_NONINTERACTIVE=1 ./scripts/Reset_Clean_Up_DB.sh

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# Load only DB_* vars from .env (avoids interpreting cron etc. when sourcing)
if [[ -f "${REPO_ROOT}/.env" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^#.*$ ]] && continue
    [[ -z "${line:=}" ]] && continue
    if [[ "$line" =~ ^DB_HOST= ]]; then DB_HOST="${line#DB_HOST=}"; DB_HOST="${DB_HOST#\"}"; DB_HOST="${DB_HOST%\"}"; fi
    if [[ "$line" =~ ^DB_PORT= ]]; then DB_PORT="${line#DB_PORT=}"; DB_PORT="${DB_PORT#\"}"; DB_PORT="${DB_PORT%\"}"; fi
    if [[ "$line" =~ ^DB_NAME= ]]; then DB_NAME="${line#DB_NAME=}"; DB_NAME="${DB_NAME#\"}"; DB_NAME="${DB_NAME%\"}"; fi
    if [[ "$line" =~ ^DB_USERNAME= ]]; then DB_USERNAME="${line#DB_USERNAME=}"; DB_USERNAME="${DB_USERNAME#\"}"; DB_USERNAME="${DB_USERNAME%\"}"; fi
    if [[ "$line" =~ ^DB_PASSWORD= ]]; then DB_PASSWORD="${line#DB_PASSWORD=}"; DB_PASSWORD="${DB_PASSWORD#\"}"; DB_PASSWORD="${DB_PASSWORD%\"}"; fi
  done < "${REPO_ROOT}/.env"
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-shade_dev}"
DB_USERNAME="${DB_USERNAME:-$(whoami)}"
export PGPASSWORD="${DB_PASSWORD:-}"

# When .env uses host.docker.internal (for app in Docker), use localhost for psql on host
if [[ "${DB_HOST}" == "host.docker.internal" ]]; then
  DB_HOST="localhost"
fi

PSQL_OPTS=(-h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}")

info "Database: ${DB_NAME} @ ${DB_HOST}:${DB_PORT}"
warn "This will DROP all tables and data in the public schema."
if [[ "${RESET_DB_NONINTERACTIVE:-}" != "1" ]]; then
  read -r -p "Continue? [y/N] " reply
  if [[ ! "${reply}" =~ ^[yY]$ ]]; then
    info "Aborted."
    exit 0
  fi
fi

info "Dropping public schema (CASCADE)..."
psql "${PSQL_OPTS[@]}" -v ON_ERROR_STOP=1 -c "DROP SCHEMA public CASCADE;"

info "Recreating public schema..."
psql "${PSQL_OPTS[@]}" -v ON_ERROR_STOP=1 -c "CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO ${DB_USERNAME};"

info "Enabling PostGIS extension..."
psql "${PSQL_OPTS[@]}" -v ON_ERROR_STOP=1 -c "CREATE EXTENSION IF NOT EXISTS postgis;" 2>/dev/null || {
  warn "PostGIS extension not available (install with: brew install postgis). Schema is still reset."
}

info "Database reset complete. Run the app with SPRING_JPA_HIBERNATE_DDL_AUTO=create once to recreate tables, or apply Flyway migrations."

unset PGPASSWORD
