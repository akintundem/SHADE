#!/bin/bash

# ⏰ Tasks & Checklist Endpoints Test Script
# Tests all task-related endpoints and generates a detailed report
# Matches the format and robustness of test_budget_endpoints.sh
#
# Usage:
#   ./test_timeline_endpoints.sh                    # Test localhost:8080 (default)
#   ./test_timeline_endpoints.sh local              # Test localhost:8080
#   ./test_timeline_endpoints.sh prod <API_URL>     # Test production URL

# Function to show help
show_help() {
    echo "⏰ Tasks & Checklist Endpoints Test Script"
    echo "=========================================="
    echo ""
    echo "Usage:"
    echo "  $0 local              # Test localhost:8080"
    echo "  $0 prod <API_URL>     # Test production URL"
    echo ""
    exit 0
}

if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
if [ "$1" = "prod" ] && [ -n "$2" ]; then
    BASE_URL="$2"
else
    BASE_URL="http://localhost:8080"
fi

echo -e "${BLUE}🔗 Testing URL: $BASE_URL${NC}"

REPORT_FILE="test/reports/timeline_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="admin@test.com"
TEST_USER_PASSWORD="Admin123!@#"
TEST_USER_NAME="Timeline Admin"

# Globals
ACCESS_TOKEN=""
DEVICE_ID=""
USER_ID=""
EVENT_ID=""
TASK_ID=""
CHECKLIST_ID=""
LAST_BODY=""
LAST_HTTP_CODE=""
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

mkdir -p test/reports

# Initialize Report
cat > "$REPORT_FILE" << EOF
# Tasks & Checklist Endpoints Test Report
**Started:** $(date)
**Base URL:** $BASE_URL

## Test Summary
| Category | Total | Passed | Failed |
|----------|-------|--------|--------|
| Auth     | 0     | 0      | 0      |
| Tasks    | 0     | 0      | 0      |
| Checklist| 0     | 0      | 0      |
EOF

# --- Helper Functions ---

verify_email_in_database() {
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${TEST_USER_EMAIL}');" >/dev/null 2>&1
    fi
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

    # Build curl command
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    # Automatically attach X-Device-ID header for authenticated requests
    if [[ -n "$DEVICE_ID" && "$headers" != *"X-Device-ID"* ]]; then
        if [ -n "$headers" ]; then
            headers="$headers -H 'X-Device-ID: $DEVICE_ID'"
        else
            headers="-H 'X-Device-ID: $DEVICE_ID'"
        fi
    fi

    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    local temp_data_file=""
    if [ -n "$data" ]; then
        temp_data_file=$(mktemp)
        printf '%s' "$data" > "$temp_data_file"
        curl_cmd="$curl_cmd --data-binary @$temp_data_file"
    fi
    
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"

    local response
    if ! response=$(eval "$curl_cmd"); then
        echo -e "${RED}❌ Failed to execute curl${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        [ -n "$temp_data_file" ] && rm -f "$temp_data_file"
        return 1
    fi
    [ -n "$temp_data_file" ] && rm -f "$temp_data_file"

    LAST_HTTP_CODE="${response: -3}"
    LAST_BODY="${response%???}"

    if [ "$LAST_HTTP_CODE" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED ($LAST_HTTP_CODE)${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAILED (Expected $expected_status, Got $LAST_HTTP_CODE)${NC}"
        echo -e "${YELLOW}Response: $LAST_BODY${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    {
        echo "### $test_name"
        echo "**Status:** $LAST_HTTP_CODE"
        echo "**Endpoint:** $method $endpoint"
        echo "**Description:** $description"
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$LAST_BODY" | jq '.' 2>/dev/null || echo "$LAST_BODY"
        echo "\`\`\`"
        echo "---"
    } >> "$REPORT_FILE"

    return 0
}

wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=30
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/api/v1/auth/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        fi
        sleep 2
        ((attempt++))
    done
    return 1
}

authenticate_user() {
    echo -e "${PURPLE}🔐 Step 1: User Authentication${NC}"
    
    # Try registration first (ignore failure if user exists)
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "'$TEST_USER_NAME'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "phoneNumber": "+1234567890",
        "dateOfBirth": "1990-01-01",
        "acceptTerms": true,
        "acceptPrivacy": true
    }'
    curl -s -X POST -H "Content-Type: application/json" -d "$registration_data" "$BASE_URL/api/v1/auth/register" > /dev/null 2>&1
    verify_email_in_database
    
    # Login
    local login_data="{\"email\":\"$TEST_USER_EMAIL\",\"password\":\"$TEST_USER_PASSWORD\"}"
    local login_resp=$(curl -s -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$login_data" "$BASE_URL/api/v1/auth/login")
    local login_code="${login_resp: -3}"
    local login_body="${login_resp%???}"

    if [ "$login_code" = "200" ]; then
        ACCESS_TOKEN=$(echo "$login_body" | jq -r '.accessToken // empty')
        DEVICE_ID=$(echo "$login_body" | jq -r '.deviceId // empty')
        
        # Check onboarding
        local onboarding_required=$(echo "$login_body" | jq -r '.onboardingRequired // false')
        if [ "$onboarding_required" = "true" ]; then
            echo -e "${YELLOW}📝 Completing onboarding...${NC}"
            # Get user ID from /me endpoint
            local me_response=$(curl -s -X GET -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/api/v1/auth/me")
            local user_id=$(echo "$me_response" | jq -r '.id // empty')
            if [ -n "$user_id" ]; then
                local onboard_data="{\"name\":\"$TEST_USER_NAME\",\"username\":\"admin_$(date +%s)\",\"phoneNumber\":\"+1234567890\",\"dateOfBirth\":\"1990-01-01\",\"acceptTerms\":true,\"acceptPrivacy\":true}"
                curl -s -X PUT -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Device-ID: $DEVICE_ID" -H "Content-Type: application/json" -d "$onboard_data" "$BASE_URL/api/v1/auth/users/$user_id" > /dev/null
            fi
        fi
        echo -e "${GREEN}✅ Authenticated${NC}"
        return 0
    fi
    echo -e "${RED}❌ Auth Failed ($login_code)${NC}"
    echo -e "${YELLOW}Response: $login_body${NC}"
    return 1
}

# --- Main Logic ---

main() {
    if ! wait_for_service || ! authenticate_user; then exit 1; fi

    echo -e "\n${PURPLE}📅 Step 2: Event Setup${NC}"
    local event_payload='{
        "event": {
            "name": "Timeline Integration Test",
            "eventType": "CONFERENCE",
            "startDateTime": "2025-12-20T10:00:00",
            "endDateTime": "2025-12-20T18:00:00"
        },
        "coverUpload": { "fileName": "cover.jpg", "contentType": "image/jpeg" }
    }'
    run_test "Create Event" "POST" "/api/v1/events" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$event_payload" "201" "Setup parent event for tasks"
    EVENT_ID=$(echo "$LAST_BODY" | jq -r '.event.id // empty')

    if [ -z "$EVENT_ID" ]; then
        echo -e "${RED}❌ Could not capture EVENT_ID${NC}"
        exit 1
    fi

    echo -e "\n${PURPLE}✅ Step 3: Task Operations${NC}"
    local task_data="{\"title\":\"Venue Setup\",\"description\":\"Draft setup plan\"}"
    run_test "Auto-save Task" "PATCH" "/api/v1/events/$EVENT_ID/tasks/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task_data" "200" "Create task draft"
    TASK_ID=$(echo "$LAST_BODY" | jq -r '.id // empty')

    if [ -n "$TASK_ID" ]; then
        run_test "Finalize Task" "PUT" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "{\"title\":\"Venue Setup FINAL\"}" "200" "Finalize task"
        run_test "Get Tasks" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all event tasks"
    fi

    echo -e "\n${PURPLE}📝 Step 4: Checklist Operations${NC}"
    if [ -n "$TASK_ID" ]; then
        run_test "Auto-save Checklist" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "{\"title\":\"Confirm Floor Plan\"}" "200" "Add checklist item"
        CHECKLIST_ID=$(echo "$LAST_BODY" | jq -r '.id // empty')

        if [ -n "$CHECKLIST_ID" ]; then
            run_test "Finalize Checklist" "PUT" "/api/v1/tasks/$TASK_ID/checklist/$CHECKLIST_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "{\"title\":\"Floor Plan Confirmed\"}" "200" "Finalize sub-item"
            run_test "Reorder Checklist" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/order" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "[\"$CHECKLIST_ID\"]" "204" "Update ordering"
        fi
    fi

    echo -e "\n${PURPLE}🧹 Step 5: Cleanup${NC}"
    if [ -n "$TASK_ID" ]; then
        run_test "Delete Task" "DELETE" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Remove test task and its checklists"
    fi
    if [ -n "$EVENT_ID" ]; then
        run_test "Archive Event" "POST" "/api/v1/events/$EVENT_ID/archive" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Cleanup parent event"
    fi

    echo -e "\n${GREEN}🎉 All tests completed. Detailed report: $REPORT_FILE${NC}"
}

main "$@"
