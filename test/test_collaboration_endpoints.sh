#!/bin/bash

# Collaboration Endpoints Test Script
# Mirrors the structure/format of test_event_endpoints.sh, but focuses on:
# - Public directory list/search (name or username)
# - Add/update/remove collaborators on an event
#
# Usage:
#   ./test_collaboration_endpoints.sh
#   ./test_collaboration_endpoints.sh local
#   ./test_collaboration_endpoints.sh prod <API_URL>

# Function to show help
show_help() {
    echo "🤝 Collaboration Endpoints Test Script"
    echo "======================================"
    echo ""
    echo "Usage:"
    echo "  $0                    # Interactive mode - choose environment"
    echo "  $0 local              # Test localhost:8080"
    echo "  $0 prod <API_URL>     # Test production URL"
    echo "  $0 help               # Show this help"
    exit 0
}

if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "🤝 Starting Collaboration Endpoints Test"
echo "======================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

get_testing_environment() {
    if [ $# -gt 0 ]; then
        case $1 in
            "local"|"l")
                BASE_URL="http://localhost:8080"
                echo -e "${GREEN}✅ Selected: Local Development (from command line)${NC}"
                ;;
            "prod"|"p")
                if [ -n "$2" ]; then
                    BASE_URL="$2"
                    echo -e "${GREEN}✅ Selected: Production - $BASE_URL (from command line)${NC}"
                else
                    echo -e "${RED}❌ Production URL required. Usage: $0 prod <API_URL>${NC}"
                    exit 1
                fi
                ;;
            *)
                echo -e "${RED}❌ Invalid argument. Use: $0 local | $0 prod <API_URL>${NC}"
                exit 1
                ;;
        esac
    else
        echo -e "${CYAN}🌍 Choose Testing Environment:${NC}"
        echo "1. Local Development (localhost:8080)"
        echo "2. Production (Custom API URL)"
        echo ""
        read -p "Enter your choice (1 or 2): " choice
        case $choice in
            1)
                BASE_URL="http://localhost:8080"
                ;;
            2)
                read -p "API URL (https://...): " custom_url
                BASE_URL="$custom_url"
                ;;
            *)
                echo -e "${RED}❌ Invalid choice${NC}"
                exit 1
                ;;
        esac
    fi
    echo ""
    echo -e "${BLUE}🔗 Testing URL: $BASE_URL${NC}"
    echo ""
}

get_testing_environment "$@"

# Path configuration (always resolve relative to this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"
REPORT_FILE="${REPORTS_DIR}/collaboration_test_report_$(date +%Y%m%d_%H%M%S).md"

TEST_USER_EMAIL="admin@test.com"
TEST_USER_PASSWORD="Admin123!@#"
TEST_USER_NAME="Admin User"
TEST_USER_PHONE="+1234567890"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Session vars
ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""
DEVICE_ID=""
EVENT_ID=""

# Directory-discovered user
DISCOVERED_USER_ID=""
DISCOVERED_USER_USERNAME=""

# Collaborator record id returned by API
COLLABORATOR_ID=""

# Invite-related variables
INVITE_ID=""
INVITE_TOKEN=""
INVITED_USER_EMAIL="invited@test.com"
INVITED_USER_PASSWORD="Invited123!@#"
INVITED_USER_TOKEN=""
INVITED_USER_ID=""
INVITED_USER_DEVICE_ID=""

# Requirements check
if ! command -v jq >/dev/null 2>&1; then
    echo -e "${RED}❌ jq is required but not installed.${NC}"
    echo -e "${YELLOW}💡 Install it with: brew install jq${NC}"
    exit 1
fi

mkdir -p "$REPORTS_DIR"
cat > "$REPORT_FILE" << EOF
# Collaboration Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

---

## Detailed Test Results

EOF

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=40
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Attempt $attempt/$max_attempts...${NC}"
        sleep 3
        ((attempt++))
    done
    echo -e "${RED}❌ Service failed to respond within expected time${NC}"
    return 1
}

run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local headers="$4"
    local data="$5"
    local expected_status="$6"
    local description="$7"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Running: $test_name${NC}"

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    local temp_data_file=""

    # Auto attach device id when doing authenticated calls
    if [[ -n "$DEVICE_ID" && "$headers" == *"Authorization:"* && "$headers" != *"X-Device-ID"* ]]; then
        headers="$headers -H 'X-Device-ID: $DEVICE_ID'"
    fi

    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi

    if [ -n "$data" ]; then
        temp_data_file=$(mktemp)
        printf '%s' "$data" > "$temp_data_file"
        curl_cmd="$curl_cmd --data-binary @$temp_data_file"
    fi

    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"

    local response
    response=$(eval "$curl_cmd")

    if [ -n "$temp_data_file" ]; then
        rm -f "$temp_data_file"
    fi

    local http_code="${response: -3}"
    local response_body="${response%???}"

    # expose for callers
    LAST_HTTP_CODE="$http_code"
    LAST_BODY="$response_body"

    local status_icon="❌"
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    {
        echo ""
        echo "### $test_name"
        echo "**Status:** $status_icon $http_code (Expected: $expected_status)"
        echo "**Description:** $description"
        echo "**Endpoint:** $method $endpoint"
        echo "**Request Headers:** $headers"
        if [ -n "$data" ]; then
            echo "**Request Body:** $data"
        fi
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$response_body"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"

    # Extract IDs/tokens
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        case "$test_name" in
            "User Login")
                ACCESS_TOKEN=$(echo "$response_body" | jq -r '.accessToken // empty')
                REFRESH_TOKEN=$(echo "$response_body" | jq -r '.refreshToken // empty')
                DEVICE_ID=$(echo "$response_body" | jq -r '.deviceId // empty')
                USER_ID=$(echo "$response_body" | jq -r '.user.id // empty')
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | jq -r '.event.id // .id // empty')
                ;;
            "Add Collaborator")
                COLLABORATOR_ID=$(echo "$response_body" | jq -r '.collaboratorId // empty')
                ;;
            "Create Collaborator Invite")
                INVITE_ID=$(echo "$response_body" | jq -r '.inviteId // empty')
                ;;
            "Create Collaborator Invite By Email")
                INVITE_ID=$(echo "$response_body" | jq -r '.inviteId // empty')
                # Note: Token is not returned in response for security, would be in email
                ;;
            "Invited User Login")
                INVITED_USER_TOKEN=$(echo "$response_body" | jq -r '.accessToken // empty')
                INVITED_USER_ID=$(echo "$response_body" | jq -r '.user.id // empty')
                INVITED_USER_DEVICE_ID=$(echo "$response_body" | jq -r '.deviceId // empty')
                ;;
        esac
    fi

    echo ""
}

authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"

    local reg='{"email":"'"$TEST_USER_EMAIL"'","password":"'"$TEST_USER_PASSWORD"'","confirmPassword":"'"$TEST_USER_PASSWORD"'"}'
    local r=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$reg" "$BASE_URL/api/v1/auth/register")
    local rc="${r: -3}"
    if [ "$rc" = "201" ]; then
        verify_email_in_database "$TEST_USER_EMAIL"
    fi

    local login='{"email":"'"$TEST_USER_EMAIL"'","password":"'"$TEST_USER_PASSWORD"'","rememberMe":false}'
    run_test "User Login" "POST" "/api/v1/auth/login" "-H 'Content-Type: application/json'" "$login" "200" "Login to obtain access token"

    # Complete onboarding if needed
    if [ "$LAST_HTTP_CODE" = "200" ]; then
        local onboarding_required
        onboarding_required=$(echo "$LAST_BODY" | jq -r '.onboardingRequired // false')
        if [ "$onboarding_required" = "true" ]; then
            # Get user ID from /me endpoint
            local me_response=$(curl -s -X GET -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/v1/auth/me")
            local user_id=$(echo "$me_response" | jq -r '.id // empty')
            if [ -n "$user_id" ]; then
                local uname="testcollab_$(date +%s | cut -c1-8)"
                local onboarding='{"name":"'"$TEST_USER_NAME"'","username":"'"$uname"'","phoneNumber":"'"$TEST_USER_PHONE"'","dateOfBirth":"1990-01-01","acceptTerms":true,"acceptPrivacy":true,"marketingOptIn":false}'
                run_test "Complete Onboarding" "PUT" "/api/v1/auth/users/$user_id" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$onboarding" "200" "Complete onboarding (ensure username exists)"
            fi
        fi
    fi
}

create_event() {
    # Minimal event create via existing event controller
    local start_date
    local end_date
    if [[ "$OSTYPE" == "darwin"* ]]; then
        start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S')
    else
        start_date=$(date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -d '+1 day +2 hours' '+%Y-%m-%dT%H:%M:%S')
    fi

    local payload='{
      "event": {
        "name": "Collaboration Test Event",
        "description": "Event created for collaborator endpoints test",
        "eventType": "CONFERENCE",
        "startDateTime": "'"$start_date"'",
        "endDateTime": "'"$end_date"'",
        "venueRequirements": "Test venue",
        "capacity": 50,
        "isPublic": true,
        "requiresApproval": false
      },
      "coverUpload": {
        "fileName": "cover.jpg",
        "contentType": "image/jpeg",
        "category": "cover",
        "isPublic": true,
        "description": "Cover upload (not exercised in this script)"
      }
    }'
    run_test "Create Event" "POST" "/api/v1/events" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$payload" "201" "Create a test event for collaboration endpoints"
}

seed_directory_users() {
    # Best effort: create some directory users locally so list(page,size) returns results.
    if [[ $BASE_URL != *"localhost"* ]]; then
        return 0
    fi
    if ! command -v docker >/dev/null 2>&1; then
        return 0
    fi

    echo -e "${YELLOW}👥 Seeding directory users (local)...${NC}"
    local count=0
    while [ $count -lt 12 ]; do
        local suffix="$(date +%s%N | cut -b1-10)"
        local email="diruser_${suffix}@example.com"
        local password="Password123!"
        local reg="{\"email\":\"$email\",\"password\":\"$password\",\"confirmPassword\":\"$password\"}"
        local r=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$reg" "$BASE_URL/api/v1/auth/register")
        local rc="${r: -3}"
        if [ "$rc" = "201" ] || [ "$rc" = "200" ] || [ "$rc" = "400" ]; then
            verify_email_in_database "$email"
            local login="{\"email\":\"$email\",\"password\":\"$password\",\"rememberMe\":false}"
            local lr=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$login" "$BASE_URL/api/v1/auth/login")
            local lc="${lr: -3}"
            local lb="${lr%???}"
            if [ "$lc" = "200" ]; then
                local tok="$(echo "$lb" | jq -r '.accessToken // empty')"
                local did="$(echo "$lb" | jq -r '.deviceId // empty')"
                local onboarding_required="$(echo "$lb" | jq -r '.onboardingRequired // false')"
                if [ "$onboarding_required" = "true" ] && [ -n "$tok" ] && [ -n "$did" ]; then
                    # Get user ID from /me endpoint
                    local me_resp=$(curl -s -X GET -H "Authorization: Bearer $tok" "$BASE_URL/api/v1/auth/me")
                    local uid=$(echo "$me_resp" | jq -r '.id // empty')
                    if [ -n "$uid" ]; then
                        local uname="diruser_${suffix}"
                        local onboarding="{\"name\":\"Directory User $count\",\"username\":\"$uname\",\"acceptTerms\":true,\"acceptPrivacy\":true}"
                        curl -sS -X PUT \
                          -H "Authorization: Bearer $tok" \
                          -H "X-Device-ID: $did" \
                          -H "Content-Type: application/json" \
                          -d "$onboarding" \
                          "$BASE_URL/api/v1/auth/users/$uid" >/dev/null 2>&1
                    fi
                fi
            fi
        fi
        count=$((count + 1))
    done
    echo -e "${GREEN}✅ Seeding done${NC}"
    echo ""
}

authenticate_invited_user() {
    echo -e "${YELLOW}🔐 Authenticating invited user...${NC}"

    local reg='{"email":"'"$INVITED_USER_EMAIL"'","password":"'"$INVITED_USER_PASSWORD"'","confirmPassword":"'"$INVITED_USER_PASSWORD"'"}'
    local r=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$reg" "$BASE_URL/api/v1/auth/register")
    local rc="${r: -3}"
    if [ "$rc" = "201" ]; then
        verify_email_in_database "$INVITED_USER_EMAIL"
    fi

    local login='{"email":"'"$INVITED_USER_EMAIL"'","password":"'"$INVITED_USER_PASSWORD"'","rememberMe":false}'
    run_test "Invited User Login" "POST" "/api/v1/auth/login" "-H 'Content-Type: application/json'" "$login" "200" "Login as invited user to test invite acceptance"

    # Complete onboarding if needed
    if [ "$LAST_HTTP_CODE" = "200" ]; then
        local onboarding_required
        onboarding_required=$(echo "$LAST_BODY" | jq -r '.onboardingRequired // false')
        if [ "$onboarding_required" = "true" ]; then
            # Get user ID from /me endpoint
            local me_response=$(curl -s -X GET -H "Authorization: Bearer $INVITED_USER_TOKEN" -H "X-Device-ID: $INVITED_USER_DEVICE_ID" "$BASE_URL/api/v1/auth/me")
            local user_id=$(echo "$me_response" | jq -r '.id // empty')
            if [ -n "$user_id" ]; then
                local uname="invited_$(date +%s | cut -c1-8)"
                local onboarding='{"name":"Invited Test User","username":"'"$uname"'","phoneNumber":"+1234567891","dateOfBirth":"1990-01-01","acceptTerms":true,"acceptPrivacy":true,"marketingOptIn":false}'
                run_test "Complete Invited User Onboarding" "PUT" "/api/v1/auth/users/$user_id" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID' -H 'Content-Type: application/json'" "$onboarding" "200" "Complete onboarding for invited user"
            fi
        fi
    fi
}

main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Directory list (no search) + search"
    echo "5. Add collaborator using discovered userId"
    echo "6. Update collaborator"
    echo "7. Remove collaborator"
    echo "8. Create collaborator invite"
    echo "9. List event invites"
    echo "10. Authenticate as invited user"
    echo "11. List incoming invites"
    echo "12. Accept invite"
    echo "13. Decline invite (if applicable)"
    echo ""

    if ! wait_for_service; then
        exit 1
    fi

    authenticate_user
    if [ -z "$ACCESS_TOKEN" ]; then
        echo -e "${RED}❌ No access token. Exiting.${NC}"
        exit 1
    fi

    create_event
    if [ -z "$EVENT_ID" ]; then
        echo -e "${RED}❌ No event id. Exiting.${NC}"
        exit 1
    fi

    seed_directory_users

    # Directory list without searchTerm
    run_test "Directory List Users (No Search)" "GET" "/api/v1/auth/users/directory?page=0&size=10" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List first 10 users from public directory (no searchTerm)"
    if [ "$LAST_HTTP_CODE" = "200" ]; then
        DISCOVERED_USER_ID="$(echo "$LAST_BODY" | jq -r --arg me "$USER_ID" '.content[] | select(.id != $me) | .id' | head -n 1)"
        DISCOVERED_USER_USERNAME="$(echo "$LAST_BODY" | jq -r --arg me "$USER_ID" '.content[] | select(.id != $me) | .username // empty' | head -n 1)"
    fi

    # Try searching by username or name
    if [ -n "$DISCOVERED_USER_USERNAME" ]; then
        run_test "Directory Search Users (By Username)" "GET" "/api/v1/auth/users/directory?searchTerm=$DISCOVERED_USER_USERNAME&page=0&size=10" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search directory by username"
    else
        run_test "Directory Search Users (By Name)" "GET" "/api/v1/auth/users/directory?searchTerm=Directory&page=0&size=10" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search directory by name"
        if [ -z "$DISCOVERED_USER_ID" ] && [ "$LAST_HTTP_CODE" = "200" ]; then
            DISCOVERED_USER_ID="$(echo "$LAST_BODY" | jq -r --arg me "$USER_ID" '.content[] | select(.id != $me) | .id' | head -n 1)"
        fi
    fi

    if [ -z "$DISCOVERED_USER_ID" ]; then
        echo -e "${YELLOW}⚠️  No discovered user from directory. Skipping collaborator tests.${NC}"
        exit 0
    fi

    local add_payload
    add_payload=$(cat <<EOF
{
  "userId": "$DISCOVERED_USER_ID",
  "role": "COLLABORATOR",
  "permissions": ["read", "write"],
  "sendInvitation": false
}
EOF
)
    run_test "Add Collaborator" "POST" "/api/v1/events/$EVENT_ID/collaborators" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$add_payload" "200" "Add collaborator by userId (selected from directory)"

    local collab_id="${COLLABORATOR_ID}"
    if [ -z "$collab_id" ]; then
        # fallback: list collaborators and pick the one matching our user
        run_test "List Collaborators" "GET" "/api/v1/events/$EVENT_ID/collaborators?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List collaborators for event"
        collab_id="$(echo "$LAST_BODY" | jq -r --arg uid "$DISCOVERED_USER_ID" '.[] | select(.userId==$uid) | .collaboratorId' | head -n 1)"
    fi

    if [ -z "$collab_id" ]; then
        echo -e "${YELLOW}⚠️  Could not resolve collaboratorId; skipping update/remove.${NC}"
        exit 0
    fi

    local update_payload
    update_payload=$(cat <<EOF
{
  "userId": "$DISCOVERED_USER_ID",
  "role": "ADMIN",
  "permissions": ["read", "write", "delete"]
}
EOF
)
    run_test "Update Collaborator" "PUT" "/api/v1/events/$EVENT_ID/collaborators/$collab_id" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_payload" "200" "Update collaborator role/permissions"

    run_test "Remove Collaborator" "DELETE" "/api/v1/events/$EVENT_ID/collaborators/$collab_id" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Remove collaborator from event"

    # ===== Invite Tests =====
    echo -e "${CYAN}📨 Testing Collaborator Invites${NC}"
    echo ""

    # First, authenticate as invited user so we can create invites for them
    authenticate_invited_user
    if [ -z "$INVITED_USER_TOKEN" ] || [ -z "$INVITED_USER_ID" ]; then
        echo -e "${YELLOW}⚠️  Could not authenticate invited user; skipping invite tests.${NC}"
    else
        # Create an invite for the invited user (by userId)
        local invite_payload
        invite_payload=$(cat <<EOF
{
  "inviteeUserId": "$INVITED_USER_ID",
  "role": "COLLABORATOR",
  "message": "You are invited to collaborate on this event",
  "sendEmail": false,
  "sendPush": false
}
EOF
)
        run_test "Create Collaborator Invite" "POST" "/api/v1/events/$EVENT_ID/collaborator-invites" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invite_payload" "201" "Create collaborator invite by userId for invited user"

        local invite_id="${INVITE_ID}"
        if [ -z "$invite_id" ]; then
            # fallback: list invites and pick the one for invited user
            run_test "List Event Invites" "GET" "/api/v1/events/$EVENT_ID/collaborator-invites?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all invites for the event"
            invite_id="$(echo "$LAST_BODY" | jq -r --arg uid "$INVITED_USER_ID" '.content[] | select(.inviteeUserId==$uid and .status=="PENDING") | .inviteId' | head -n 1)"
        fi

        # Test that the event owner CANNOT accept someone else's invite
        if [ -n "$invite_id" ]; then
            run_test "Event Owner Cannot Accept Others Invite" "POST" "/api/v1/collaborator-invites/$invite_id/accept" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "{}" "400" "Verify event owner cannot accept invite meant for another user"
        fi

        # List incoming invites for the invited user
        run_test "List My Incoming Invites" "GET" "/api/v1/collaborator-invites/incoming" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID'" "" "200" "List pending invites for authenticated user"

        # Get invite ID from incoming invites if we don't have one
        if [ -z "$invite_id" ] && [ "$LAST_HTTP_CODE" = "200" ]; then
            invite_id="$(echo "$LAST_BODY" | jq -r '.[] | select(.status=="PENDING") | .inviteId' | head -n 1)"
        fi

        # Accept invite as the invited user (should succeed)
        if [ -n "$invite_id" ]; then
            run_test "Accept Collaborator Invite" "POST" "/api/v1/collaborator-invites/$invite_id/accept" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID' -H 'Content-Type: application/json'" "{}" "200" "Accept a collaborator invite as the invited user"
        fi

        # Create invite by email for the invited user (to test token-based acceptance)
        local invite_by_email_payload
        invite_by_email_payload=$(cat <<EOF
{
  "inviteeEmail": "$INVITED_USER_EMAIL",
  "role": "STAFF",
  "message": "You are invited to collaborate via email",
  "sendEmail": false,
  "sendPush": false
}
EOF
)
        run_test "Create Collaborator Invite By Email" "POST" "/api/v1/events/$EVENT_ID/collaborator-invites" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invite_by_email_payload" "201" "Create collaborator invite by email"

        local email_invite_id="${INVITE_ID}"
        if [ -z "$email_invite_id" ]; then
            # Get from list
            run_test "List Event Invites For Email" "GET" "/api/v1/events/$EVENT_ID/collaborator-invites?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List invites to find email-based invite"
            email_invite_id="$(echo "$LAST_BODY" | jq -r --arg email "$INVITED_USER_EMAIL" '.content[] | select(.inviteeEmail==$email and .status=="PENDING") | .inviteId' | head -n 1)"
        fi

        # For email invites, token-based acceptance would use: POST /api/v1/collaborator-invites/accept?token=<token>
        # Note: In production, the token is sent via email. For testing, we can't easily retrieve the raw token
        # (it's stored as a hash in the database). However, we can test that accepting by ID works for email invites,
        # which verifies that the logged-in user's email matches the inviteeEmail.
        if [ -n "$email_invite_id" ]; then
            run_test "Accept Email Invite By ID" "POST" "/api/v1/collaborator-invites/$email_invite_id/accept" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID' -H 'Content-Type: application/json'" "{}" "200" "Accept email-based invite by ID (verifies email matches logged-in user)"
            
            # Note: Token-based acceptance would work like this (if we had the token):
            # POST /api/v1/collaborator-invites/accept?token=<token_from_email>
            # The token is hashed in the database, so we can't easily extract it for testing.
            # In production, users click a link in their email with the token parameter.
        fi

        # Create another invite to test decline
        local decline_invite_payload
        decline_invite_payload=$(cat <<EOF
{
  "inviteeUserId": "$INVITED_USER_ID",
  "role": "STAFF",
  "message": "Test invite for decline",
  "sendEmail": false,
  "sendPush": false
}
EOF
)
        run_test "Create Invite For Decline Test" "POST" "/api/v1/events/$EVENT_ID/collaborator-invites" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$decline_invite_payload" "201" "Create invite to test decline functionality"

        local decline_invite_id=""
        if [ "$LAST_HTTP_CODE" = "201" ]; then
            decline_invite_id="$(echo "$LAST_BODY" | jq -r '.inviteId // empty')"
        fi
        if [ -z "$decline_invite_id" ]; then
            # Get from list
            run_test "List Event Invites For Decline" "GET" "/api/v1/events/$EVENT_ID/collaborator-invites?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List invites to find one for decline test"
            decline_invite_id="$(echo "$LAST_BODY" | jq -r --arg uid "$INVITED_USER_ID" '.content[] | select(.inviteeUserId==$uid and .status=="PENDING") | .inviteId' | head -n 1)"
        fi

        if [ -n "$decline_invite_id" ]; then
            run_test "Decline Collaborator Invite" "POST" "/api/v1/collaborator-invites/$decline_invite_id/decline" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID' -H 'Content-Type: application/json'" "{}" "204" "Decline a collaborator invite"
        fi

        # Test that invited user cannot accept someone else's invite
        # Create an invite for the discovered user
        local other_invite_payload
        other_invite_payload=$(cat <<EOF
{
  "inviteeUserId": "$DISCOVERED_USER_ID",
  "role": "COLLABORATOR",
  "message": "Invite for another user",
  "sendEmail": false,
  "sendPush": false
}
EOF
)
        run_test "Create Invite For Other User" "POST" "/api/v1/events/$EVENT_ID/collaborator-invites" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$other_invite_payload" "201" "Create invite for a different user to test access control"

        local other_invite_id=""
        if [ "$LAST_HTTP_CODE" = "201" ]; then
            other_invite_id="$(echo "$LAST_BODY" | jq -r '.inviteId // empty')"
        fi
        if [ -z "$other_invite_id" ]; then
            run_test "List Event Invites For Other User" "GET" "/api/v1/events/$EVENT_ID/collaborator-invites?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List invites to find other user's invite"
            other_invite_id="$(echo "$LAST_BODY" | jq -r --arg uid "$DISCOVERED_USER_ID" '.content[] | select(.inviteeUserId==$uid and .status=="PENDING") | .inviteId' | head -n 1)"
        fi

        # Try to accept someone else's invite (should fail)
        if [ -n "$other_invite_id" ]; then
            run_test "Cannot Accept Others Invite" "POST" "/api/v1/collaborator-invites/$other_invite_id/accept" "-H 'Authorization: Bearer $INVITED_USER_TOKEN' -H 'X-Device-ID: $INVITED_USER_DEVICE_ID' -H 'Content-Type: application/json'" "{}" "400" "Verify invited user cannot accept invite meant for another user"
        fi
    fi

    # List all event invites (as event owner)
    run_test "List All Event Invites" "GET" "/api/v1/events/$EVENT_ID/collaborator-invites?page=0&size=50" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all invites for the event as owner"

    # Revoke a pending invite (as event owner)
    if [ -n "$INVITE_ID" ]; then
        # Get a pending invite to revoke
        local revoke_invite_id=""
        if [ "$LAST_HTTP_CODE" = "200" ]; then
            revoke_invite_id="$(echo "$LAST_BODY" | jq -r '.content[] | select(.status=="PENDING") | .inviteId' | head -n 1)"
        fi
        if [ -n "$revoke_invite_id" ]; then
            run_test "Revoke Collaborator Invite" "DELETE" "/api/v1/events/$EVENT_ID/collaborator-invites/$revoke_invite_id" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Revoke a pending collaborator invite as event owner"
        fi
    fi

    echo -e "${PURPLE}📊 Test Summary${NC}"
    echo "==============="
    echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

    local success_rate=0
    if [ $TOTAL_TESTS -gt 0 ]; then
        success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    fi
    echo -e "Success Rate: ${CYAN}$success_rate%${NC}"

    {
        echo ""
        echo "## Final Summary"
        echo ""
        echo "**Test Execution Completed:** $(date)"
        echo "**Total Tests:** $TOTAL_TESTS"
        echo "**Passed:** $PASSED_TESTS"
        echo "**Failed:** $FAILED_TESTS"
        echo "**Success Rate:** $success_rate%"
        echo ""
    } >> "$REPORT_FILE"

    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"

    if [ $FAILED_TESTS -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Test interrupted...${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM
main "$@"





