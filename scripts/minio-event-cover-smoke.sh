#!/usr/bin/env bash
# ============================================
# MinIO smoke: Reset DB, create user (session), one event with cover image
# ============================================
# Flow (aligned with FE seed: capsuleapp/test/seed/seed.test.ts):
#   1. Optionally reset DB (RESET_DB=1).
#   2. Ensure app is running (with SPRING_JPA_HIBERNATE_DDL_AUTO=create after reset).
#   3. GET /api/v1/auth/session → provisions user from JWT.
#   4. POST /api/v1/events → create one event.
#   5. POST /api/v1/events/{id}/cover-image → get presigned URL.
#   6. Download image from internet (Picsum), PUT to presigned URL.
#   7. POST /api/v1/events/{id}/cover-image/complete → persist cover.
#   8. GET event and print coverImageUrl (presigned) so you can confirm MinIO.
#
# Requires: curl, jq. BEARER_TOKEN from FE login (or Auth0).
# Usage:
#   RESET_DB=1 ./scripts/minio-event-cover-smoke.sh   # reset DB then run smoke (you must restart app with ddl-auto=create)
#   BEARER_TOKEN=<token> ./scripts/minio-event-cover-smoke.sh   # run smoke only (app already up)
#   SKIP_DB_RESET=1 BEARER_TOKEN=<token> ./scripts/minio-event-cover-smoke.sh

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# Config from env (same as FE: capsuleapp seed)
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
# Picsum image (same idea as getPicsumImageUrl in capsuleapp/test/seed/seedData.ts)
COVER_IMAGE_URL="${COVER_IMAGE_URL:-https://picsum.photos/seed/minio-smoke/800/600}"
COVER_FILE_NAME="cover-minio-smoke.jpg"
COVER_CONTENT_TYPE="image/jpeg"

if ! command -v jq &>/dev/null; then
  error "jq is required. Install with: brew install jq"
  exit 1
fi

# ----- Step 0: Optional DB reset -----
if [[ "${RESET_DB:-}" == "1" ]] && [[ "${SKIP_DB_RESET:-}" != "1" ]]; then
  info "Resetting database (non-interactive)..."
  RESET_DB_NONINTERACTIVE=1 bash "${SCRIPT_DIR}/Reset_Clean_Up_DB.sh"
  info "Database reset done. Start the app with SPRING_JPA_HIBERNATE_DDL_AUTO=create, wait for startup, then run this script again without RESET_DB=1 (or with SKIP_DB_RESET=1) and BEARER_TOKEN set."
  exit 0
fi

# ----- Token required for API steps -----
if [[ -z "${BEARER_TOKEN:-}" ]]; then
  error "BEARER_TOKEN is required. Get it by logging in via the FE app (or Auth0), then run:"
  echo "  BEARER_TOKEN=<your-jwt> ./scripts/minio-event-cover-smoke.sh"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer ${BEARER_TOKEN}"

# ----- Step 1: Session (provision user from JWT) -----
info "1. GET /api/v1/auth/session (provision user)..."
SESSION_RESP=$(curl -sf -H "$AUTH_HEADER" "${API_BASE_URL}/api/v1/auth/session") || {
  error "Session request failed. Is the app running at ${API_BASE_URL}? Is the token valid?"
  exit 1
}
USER_ID=$(echo "$SESSION_RESP" | jq -r '.user.id')
if [[ -z "$USER_ID" ]] || [[ "$USER_ID" == "null" ]]; then
  error "Could not get user from session."
  exit 1
fi
info "   User ID: $USER_ID"

# ----- Step 2: Create one event (no cover yet) -----
info "2. POST /api/v1/events (create event)..."
# Portable future date: tomorrow 09:00 and 11:00 UTC
if date -u -v+1d "+%Y-%m-%d" &>/dev/null; then
  START_ISO=$(date -u -v+1d "+%Y-%m-%dT09:00:00")
  END_ISO=$(date -u -v+1d "+%Y-%m-%dT11:00:00")
elif date -u -d "+1 day" "+%Y-%m-%d" &>/dev/null; then
  START_ISO=$(date -u -d "+1 day" "+%Y-%m-%dT09:00:00")
  END_ISO=$(date -u -d "+1 day" "+%Y-%m-%dT11:00:00")
else
  START_ISO="2026-03-25T09:00:00"
  END_ISO="2026-03-25T11:00:00"
fi

CREATE_PAYLOAD=$(jq -n \
  --arg name "MinIO Smoke Test Event" \
  --arg desc "Event created by minio-event-cover-smoke.sh to verify MinIO cover upload." \
  --arg start "$START_ISO" \
  --arg end "$END_ISO" \
  '{
    event: {
      name: $name,
      description: $desc,
      eventType: "CONFERENCE",
      accessType: "OPEN",
      capacity: 50,
      isPublic: true,
      startDateTime: $start,
      endDateTime: $end
    }
  }')

CREATE_RESP=$(curl -sf -X POST -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$CREATE_PAYLOAD" "${API_BASE_URL}/api/v1/events") || {
  error "Create event failed."
  exit 1
}
EVENT_ID=$(echo "$CREATE_RESP" | jq -r '.event.id')
if [[ -z "$EVENT_ID" ]] || [[ "$EVENT_ID" == "null" ]]; then
  error "Could not get event ID from create response."
  exit 1
fi
info "   Event ID: $EVENT_ID"

# ----- Step 3: Get presigned cover upload URL -----
info "3. POST /api/v1/events/{id}/cover-image (presigned URL)..."
PRESIGN_PAYLOAD=$(jq -n \
  --arg fileName "$COVER_FILE_NAME" \
  --arg contentType "$COVER_CONTENT_TYPE" \
  '{ fileName: $fileName, contentType: $contentType, category: "cover", isPublic: true }')
PRESIGN_RESP=$(curl -sf -X POST -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PRESIGN_PAYLOAD" "${API_BASE_URL}/api/v1/events/${EVENT_ID}/cover-image") || {
  error "Presigned cover request failed."
  exit 1
}
UPLOAD_URL=$(echo "$PRESIGN_RESP" | jq -r '.uploadUrl')
COVER_ID=$(echo "$PRESIGN_RESP" | jq -r '.mediaId')
OBJECT_KEY=$(echo "$PRESIGN_RESP" | jq -r '.objectKey')
RESOURCE_URL=$(echo "$PRESIGN_RESP" | jq -r '.resourceUrl')
if [[ -z "$UPLOAD_URL" ]] || [[ "$UPLOAD_URL" == "null" ]]; then
  error "Could not get uploadUrl from presigned response."
  exit 1
fi
info "   Upload URL host: $(echo "$UPLOAD_URL" | sed -n 's|.*://\([^/]*\).*|\1|p')"
# When running from host, presigned URL may use host.docker.internal — rewrite to localhost so curl can reach MinIO
PUT_URL="$UPLOAD_URL"
if [[ "$PUT_URL" == *"host.docker.internal"* ]]; then
  PUT_URL="${PUT_URL//host.docker.internal/localhost}"
  info "   (rewrote upload URL to localhost for host-side PUT)"
fi

# ----- Step 4: Download image and PUT to MinIO -----
info "4. Download image from $COVER_IMAGE_URL and PUT to presigned URL..."
TMP_IMG=$(mktemp)
curl -sfL -o "$TMP_IMG" "$COVER_IMAGE_URL" || {
  error "Failed to download cover image."
  exit 1
}
CONTENT_TYPE_HEADER=$(echo "$PRESIGN_RESP" | jq -r '.headers["Content-Type"] // "image/jpeg"')
HTTP_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" -X PUT -H "Content-Type: $CONTENT_TYPE_HEADER" --data-binary "@${TMP_IMG}" "$PUT_URL")
rm -f "$TMP_IMG"
if [[ "$HTTP_STATUS" != "200" ]]; then
  error "PUT to presigned URL failed with HTTP $HTTP_STATUS. Is MinIO running and AWS_S3_ENDPOINT correct (e.g. http://localhost:9000)?"
  exit 1
fi
info "   PUT returned HTTP $HTTP_STATUS"

# ----- Step 5: Complete cover upload -----
info "5. POST /api/v1/events/{id}/cover-image/complete..."
COMPLETE_PAYLOAD=$(jq -n \
  --arg coverId "$COVER_ID" \
  --arg objectKey "$OBJECT_KEY" \
  --arg resourceUrl "$RESOURCE_URL" \
  --arg fileName "$COVER_FILE_NAME" \
  --arg contentType "$COVER_CONTENT_TYPE" \
  '{
    coverId: $coverId,
    upload: {
      objectKey: $objectKey,
      resourceUrl: $resourceUrl,
      fileName: $fileName,
      contentType: $contentType,
      isPublic: true
    }
  }')
COMPLETE_RESP=$(curl -sf -X POST -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$COMPLETE_PAYLOAD" "${API_BASE_URL}/api/v1/events/${EVENT_ID}/cover-image/complete") || {
  error "Complete cover upload failed."
  exit 1
}
COVER_IMAGE_URL_RESP=$(echo "$COMPLETE_RESP" | jq -r '.coverImageUrl')
info "   Cover persisted."

# ----- Step 6: Confirm via GET event -----
info "6. GET /api/v1/events/{id} (confirm coverImageUrl)..."
EVENT_GET=$(curl -sf -H "$AUTH_HEADER" "${API_BASE_URL}/api/v1/events/${EVENT_ID}")
COVER_URL_FINAL=$(echo "$EVENT_GET" | jq -r '.coverImageUrl')
if [[ -z "$COVER_URL_FINAL" ]] || [[ "$COVER_URL_FINAL" == "null" ]]; then
  warn "Event coverImageUrl is empty; using URL from complete response."
  COVER_URL_FINAL="$COVER_IMAGE_URL_RESP"
fi

echo ""
echo "=========================================="
echo "  MinIO smoke test result"
echo "=========================================="
echo "  Event ID:    $EVENT_ID"
echo "  Cover URL:   $COVER_URL_FINAL"
echo "=========================================="
echo ""
echo "Open the Cover URL in a browser to confirm the image loads (MinIO presigned GET)."
echo ""
