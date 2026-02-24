#!/usr/bin/env bash
# Get Auth0 token from capsuleapp .env and run MinIO event-cover smoke. Backend must already be running.
# Usage: API_BASE_URL=http://localhost:8080 ./scripts/run-minio-smoke-with-token.sh
#        (default API_BASE_URL when using Kong: http://localhost:8000)
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SEED_FE_DIR="${SEED_FE_DIR:-$BACKEND_DIR/../capsuleapp}"
export SEED_FE_DIR
API_BASE_URL="${API_BASE_URL:-http://localhost:8000}"
BEARER_TOKEN=$("${SCRIPT_DIR}/get-auth0-token.sh") || exit 1
API_BASE_URL="$API_BASE_URL" BEARER_TOKEN="$BEARER_TOKEN" SKIP_DB_RESET=1 \
  bash "${SCRIPT_DIR}/minio-event-cover-smoke.sh"
