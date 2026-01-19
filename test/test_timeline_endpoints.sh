#!/bin/bash

# ⏰ Tasks & Checklist Endpoints Test Script
# Tests all task and checklist endpoints and generates a detailed report
#
# Endpoints covered:
#   TaskController (/api/v1/events/{eventId}/tasks):
#     - GET    /                    Get all tasks for an event
#     - PATCH  /auto-save           Auto-save task draft
#     - PUT    /{taskId}/finalize   Finalize task
#     - PATCH  /order               Update tasks order
#     - DELETE /{taskId}            Delete task
#
#   ChecklistController (/api/v1/tasks/{taskId}/checklist):
#     - PATCH  /auto-save           Auto-save checklist item draft
#     - PUT    /{itemId}/finalize   Finalize checklist item
#     - PATCH  /order               Update checklist items order
#     - DELETE /{itemId}            Delete checklist item
#
# Usage:
#   ./test_timeline_endpoints.sh                    # Test localhost:8080 (default)
#   ./test_timeline_endpoints.sh local              # Test localhost:8080
#   ./test_timeline_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_timeline_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "⏰ Tasks & Checklist Endpoints Test Script"
    echo "=========================================="
    echo ""
    echo "Usage:"
    echo "  $0                    # Test localhost:8080 (default)"
    echo "  $0 local              # Test localhost:8080"
    echo "  $0 prod <API_URL>     # Test production URL"
    echo "  $0 help               # Show this help"
    echo ""
    echo "Endpoints covered:"
    echo "  Tasks:"
    echo "    GET    /api/v1/events/{eventId}/tasks"
    echo "    PATCH  /api/v1/events/{eventId}/tasks/auto-save"
    echo "    PUT    /api/v1/events/{eventId}/tasks/{taskId}/finalize"
    echo "    PATCH  /api/v1/events/{eventId}/tasks/order"
    echo "    DELETE /api/v1/events/{eventId}/tasks/{taskId}"
    echo ""
    echo "  Checklist:"
    echo "    PATCH  /api/v1/tasks/{taskId}/checklist/auto-save"
    echo "    PUT    /api/v1/tasks/{taskId}/checklist/{itemId}/finalize"
    echo "    PATCH  /api/v1/tasks/{taskId}/checklist/order"
    echo "    DELETE /api/v1/tasks/{taskId}/checklist/{itemId}"
    echo ""
    exit 0
}

if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "⏰ Starting Tasks & Checklist Endpoints Test"
echo "============================================="

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
elif [ "$1" = "local" ] || [ -z "$1" ]; then
    BASE_URL="http://localhost:8080"
else
    BASE_URL="http://localhost:8080"
fi

echo -e "${BLUE}🔗 Testing URL: $BASE_URL${NC}"
echo ""

# Path configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"
REPORT_FILE="${REPORTS_DIR}/timeline_test_report_$(date +%Y%m%d_%H%M%S).md"

TEST_USER_EMAIL="admin@test.com"
TEST_USER_PASSWORD="Admin123!@#"
TEST_USER_NAME="Timeline Admin"

# Globals
ACCESS_TOKEN=""
DEVICE_ID=""
USER_ID=""
EVENT_ID=""
TASK_ID=""
TASK_ID_2=""
CHECKLIST_ID=""
CHECKLIST_ID_2=""
LAST_BODY=""
LAST_HTTP_CODE=""
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Requirements check
if ! command -v jq >/dev/null 2>&1; then
    echo -e "${RED}❌ jq is required but not installed.${NC}"
    echo -e "${YELLOW}💡 Install it with: brew install jq${NC}"
    exit 1
fi

mkdir -p "$REPORTS_DIR"

# Initialize Report
cat > "$REPORT_FILE" << EOF
# Tasks & Checklist Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Category | Total | Passed | Failed | Success Rate |
|----------|-------|--------|--------|--------------|
| Auth     | 0     | 0      | 0      | 0% |
| Tasks    | 0     | 0      | 0      | 0% |
| Checklist| 0     | 0      | 0      | 0% |
| **TOTAL**| 0     | 0      | 0      | 0% |

---

## Detailed Test Results

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
        echo -e "${GREEN}✅ PASSED${NC} - Status: $LAST_HTTP_CODE"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $LAST_HTTP_CODE"
        echo -e "${YELLOW}   Response: $LAST_BODY${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi

    {
        echo ""
        echo "### $test_name"
        echo "**Status:** $status_icon $LAST_HTTP_CODE (Expected: $expected_status)"
        echo "**Description:** $description"
        echo "**Endpoint:** $method $endpoint"
        echo "**Request Headers:** $headers"
        if [ -n "$data" ]; then
            echo "**Request Body:** $data"
        fi
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$LAST_BODY" | jq '.' 2>/dev/null || echo "$LAST_BODY"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"

    echo ""
    return 0
}

wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=30
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting...${NC}"
        sleep 2
        ((attempt++))
    done
    echo -e "${RED}❌ Service failed to respond within expected time${NC}"
    return 1
}

authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"
    
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
    local login_data="{\"email\":\"$TEST_USER_EMAIL\",\"password\":\"$TEST_USER_PASSWORD\",\"rememberMe\":false}"
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
                local uname="timeline_$(date +%s | cut -c1-8)"
                local onboard_data="{\"name\":\"$TEST_USER_NAME\",\"username\":\"$uname\",\"phoneNumber\":\"+1234567890\",\"dateOfBirth\":\"1990-01-01\",\"acceptTerms\":true,\"acceptPrivacy\":true}"
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
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test Task CRUD operations"
    echo "5. Test Task ordering"
    echo "6. Test Checklist CRUD operations"
    echo "7. Test Checklist ordering"
    echo "8. Clean up test data"
    echo ""

    # Step 1: Check service availability
    echo -e "${CYAN}🔍 Step 1: Checking Service Availability${NC}"
    echo "============================================="
    if ! wait_for_service; then
        echo -e "${RED}❌ Service not available. Exiting.${NC}"
        exit 1
    fi
    echo ""

    # Step 2: Authenticate
    echo -e "${CYAN}🔐 Step 2: User Authentication${NC}"
    echo "=================================="
    if ! authenticate_user; then
        echo -e "${RED}❌ Authentication failed. Exiting.${NC}"
        exit 1
    fi
    echo ""

    # Step 3: Create Event
    echo -e "${CYAN}📅 Step 3: Create Test Event${NC}"
    echo "============================="
    
    # Calculate dates
    local start_date
    local end_date
    if [[ "$OSTYPE" == "darwin"* ]]; then
        start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -v+1d -v+8H '+%Y-%m-%dT%H:%M:%S')
    else
        start_date=$(date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -d '+1 day +8 hours' '+%Y-%m-%dT%H:%M:%S')
    fi
    
    local event_payload
    event_payload=$(cat <<EOF
{
  "event": {
    "name": "Timeline Integration Test Event",
    "description": "Event created for timeline/task/checklist endpoint testing",
    "eventType": "CONFERENCE",
    "startDateTime": "$start_date",
    "endDateTime": "$end_date",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false
  },
  "coverUpload": {
    "fileName": "cover.jpg",
    "contentType": "image/jpeg",
    "category": "cover",
    "isPublic": true
  }
}
EOF
)
    run_test "Create Event" "POST" "/api/v1/events" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$event_payload" "201" "Create parent event for timeline tasks"
    EVENT_ID=$(echo "$LAST_BODY" | jq -r '.event.id // empty')

    if [ -z "$EVENT_ID" ]; then
        echo -e "${RED}❌ Could not capture EVENT_ID. Exiting.${NC}"
        exit 1
    fi
    echo -e "${GREEN}   Event ID: $EVENT_ID${NC}"
    echo ""

    # Step 4: Task CRUD Operations
    echo -e "${CYAN}📝 Step 4: Task CRUD Operations${NC}"
    echo "=================================="
    
    # Get tasks (should be empty initially)
    run_test "Get Tasks (Empty)" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all tasks for event (should be empty)"

    # Auto-save first task (create draft)
    local task1_data
    task1_data=$(cat <<EOF
{
  "title": "Venue Setup",
  "description": "Prepare the venue for the conference",
  "priority": "HIGH",
  "category": "Logistics",
  "status": "TO_DO"
}
EOF
)
    run_test "Auto-save Task 1 (Create Draft)" "PATCH" "/api/v1/events/$EVENT_ID/tasks/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task1_data" "200" "Create first task draft"
    TASK_ID=$(echo "$LAST_BODY" | jq -r '.id // empty')
    
    if [ -n "$TASK_ID" ]; then
        echo -e "${GREEN}   Task 1 ID: $TASK_ID${NC}"
        
        # Update existing task draft
        local task1_update
        task1_update=$(cat <<EOF
{
  "id": "$TASK_ID",
  "title": "Venue Setup - Updated",
  "description": "Prepare the venue for the conference - updated description",
  "priority": "HIGH",
  "category": "Logistics",
  "status": "TO_DO"
}
EOF
)
        run_test "Auto-save Task 1 (Update Draft)" "PATCH" "/api/v1/events/$EVENT_ID/tasks/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task1_update" "200" "Update existing task draft"
        
        # Finalize task
        local task1_finalize
        task1_finalize=$(cat <<EOF
{
  "title": "Venue Setup - Final",
  "description": "Prepare the venue for the conference - finalized",
  "priority": "HIGH",
  "category": "Logistics",
  "status": "IN_PROGRESS"
}
EOF
)
        run_test "Finalize Task 1" "PUT" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task1_finalize" "200" "Finalize task (remove draft status)"
    fi

    # Create second task
    local task2_data
    task2_data=$(cat <<EOF
{
  "title": "Catering Coordination",
  "description": "Coordinate with catering vendors",
  "priority": "MEDIUM",
  "category": "Food & Beverage",
  "status": "TO_DO"
}
EOF
)
    run_test "Auto-save Task 2 (Create)" "PATCH" "/api/v1/events/$EVENT_ID/tasks/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task2_data" "200" "Create second task"
    TASK_ID_2=$(echo "$LAST_BODY" | jq -r '.id // empty')
    
    if [ -n "$TASK_ID_2" ]; then
        echo -e "${GREEN}   Task 2 ID: $TASK_ID_2${NC}"
        
        # Finalize task 2
        run_test "Finalize Task 2" "PUT" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID_2/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task2_data" "200" "Finalize second task"
    fi

    # Get all tasks
    run_test "Get All Tasks" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all tasks for event"
    echo ""

    # Step 5: Task Ordering
    echo -e "${CYAN}🔢 Step 5: Task Ordering${NC}"
    echo "=========================="
    
    if [ -n "$TASK_ID" ] && [ -n "$TASK_ID_2" ]; then
        # Reorder tasks (swap order)
        local task_order="[\"$TASK_ID_2\", \"$TASK_ID\"]"
        run_test "Update Task Order" "PATCH" "/api/v1/events/$EVENT_ID/tasks/order" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task_order" "204" "Reorder tasks (swap task 1 and 2)"
        
        # Verify new order
        run_test "Get Tasks (After Reorder)" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Verify task order after reordering"
    fi
    echo ""

    # Step 6: Checklist CRUD Operations
    echo -e "${CYAN}✅ Step 6: Checklist CRUD Operations${NC}"
    echo "======================================"
    
    if [ -n "$TASK_ID" ]; then
        # Auto-save first checklist item
        local checklist1_data
        checklist1_data=$(cat <<EOF
{
  "title": "Confirm floor plan with venue",
  "description": "Get final approval on the floor plan layout",
  "status": "TO_DO"
}
EOF
)
        run_test "Auto-save Checklist Item 1" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist1_data" "200" "Create first checklist item draft"
        CHECKLIST_ID=$(echo "$LAST_BODY" | jq -r '.id // empty')
        
        if [ -n "$CHECKLIST_ID" ]; then
            echo -e "${GREEN}   Checklist 1 ID: $CHECKLIST_ID${NC}"
            
            # Update checklist item
            local checklist1_update
            checklist1_update=$(cat <<EOF
{
  "id": "$CHECKLIST_ID",
  "title": "Confirm floor plan with venue - Updated",
  "description": "Get final approval on the floor plan layout - updated",
  "status": "TO_DO"
}
EOF
)
            run_test "Auto-save Checklist Item 1 (Update)" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist1_update" "200" "Update existing checklist item draft"
            
            # Finalize checklist item
            local checklist1_finalize
            checklist1_finalize=$(cat <<EOF
{
  "title": "Floor plan confirmed",
  "description": "Floor plan approved by venue manager",
  "status": "COMPLETED"
}
EOF
)
            run_test "Finalize Checklist Item 1" "PUT" "/api/v1/tasks/$TASK_ID/checklist/$CHECKLIST_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist1_finalize" "200" "Finalize checklist item"
        fi

        # Create second checklist item
        local checklist2_data
        checklist2_data=$(cat <<EOF
{
  "title": "Order tables and chairs",
  "description": "Arrange rental of furniture",
  "status": "TO_DO"
}
EOF
)
        run_test "Auto-save Checklist Item 2" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist2_data" "200" "Create second checklist item"
        CHECKLIST_ID_2=$(echo "$LAST_BODY" | jq -r '.id // empty')
        
        if [ -n "$CHECKLIST_ID_2" ]; then
            echo -e "${GREEN}   Checklist 2 ID: $CHECKLIST_ID_2${NC}"
            
            # Finalize checklist item 2
            run_test "Finalize Checklist Item 2" "PUT" "/api/v1/tasks/$TASK_ID/checklist/$CHECKLIST_ID_2/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist2_data" "200" "Finalize second checklist item"
        fi
        
        # Get tasks to see checklist items
        run_test "Get Tasks (With Checklists)" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get tasks with nested checklist items"
    fi
    echo ""

    # Step 7: Checklist Ordering
    echo -e "${CYAN}🔢 Step 7: Checklist Ordering${NC}"
    echo "==============================="
    
    if [ -n "$TASK_ID" ] && [ -n "$CHECKLIST_ID" ] && [ -n "$CHECKLIST_ID_2" ]; then
        # Reorder checklist items
        local checklist_order="[\"$CHECKLIST_ID_2\", \"$CHECKLIST_ID\"]"
        run_test "Update Checklist Order" "PATCH" "/api/v1/tasks/$TASK_ID/checklist/order" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist_order" "204" "Reorder checklist items"
        
        # Verify new order
        run_test "Get Tasks (After Checklist Reorder)" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Verify checklist order after reordering"
    fi
    echo ""

    # Step 8: Delete Operations
    echo -e "${CYAN}🗑️  Step 8: Delete Operations${NC}"
    echo "==============================="
    
    # Delete a checklist item
    if [ -n "$TASK_ID" ] && [ -n "$CHECKLIST_ID_2" ]; then
        run_test "Delete Checklist Item 2" "DELETE" "/api/v1/tasks/$TASK_ID/checklist/$CHECKLIST_ID_2" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete a checklist item"
    fi
    
    # Delete second task
    if [ -n "$TASK_ID_2" ]; then
        run_test "Delete Task 2" "DELETE" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID_2" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete task and its checklists"
    fi
    
    # Delete first task (with remaining checklist)
    if [ -n "$TASK_ID" ]; then
        run_test "Delete Task 1 (With Checklists)" "DELETE" "/api/v1/events/$EVENT_ID/tasks/$TASK_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete task along with its checklist items"
    fi
    
    # Verify tasks are deleted
    run_test "Get Tasks (After Deletion)" "GET" "/api/v1/events/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Verify all tasks are deleted"
    echo ""

    # Step 9: Cleanup
    echo -e "${CYAN}🧹 Step 9: Cleanup${NC}"
    echo "==================="
    
    if [ -n "$EVENT_ID" ]; then
        run_test "Archive Test Event" "POST" "/api/v1/events/$EVENT_ID/archive" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Archive test event"
    fi
    echo ""

    # Generate final summary
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
    echo ""
    
    # Update report with final summary
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
    
    if [ $success_rate -ge 90 ]; then
        {
            echo "✅ **Excellent!** All tests are passing successfully. The timeline system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the timeline system."
        } >> "$REPORT_FILE"
    fi
    
    {
        echo ""
        echo "### Endpoints Tested"
        echo ""
        echo "#### TaskController (/api/v1/events/{eventId}/tasks)"
        echo "- GET / - Get all tasks for an event"
        echo "- PATCH /auto-save - Auto-save task draft"
        echo "- PUT /{taskId}/finalize - Finalize task"
        echo "- PATCH /order - Update tasks order"
        echo "- DELETE /{taskId} - Delete task"
        echo ""
        echo "#### ChecklistController (/api/v1/tasks/{taskId}/checklist)"
        echo "- PATCH /auto-save - Auto-save checklist item draft"
        echo "- PUT /{itemId}/finalize - Finalize checklist item"
        echo "- PATCH /order - Update checklist items order"
        echo "- DELETE /{itemId} - Delete checklist item"
        echo ""
        echo "---"
        echo ""
        echo "**Report generated by:** Timeline Endpoints Test Script"
        echo "**Script version:** 2.0"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Timeline system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Function to handle cleanup on script exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Test interrupted...${NC}"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Run main function
main "$@"
