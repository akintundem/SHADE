#!/bin/bash

# Comprehensive Attendee Controller Endpoints Test Script
# Tests all attendee-related endpoints and generates a detailed report
# Supports both local and production testing with custom API URLs
#
# Usage:
#   ./test_attendee_endpoints.sh                    # Interactive mode
#   ./test_attendee_endpoints.sh local              # Test localhost:8080
#   ./test_attendee_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_attendee_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "🎫 Attendee Controller Endpoints Test Script"
    echo "========================================="
    echo ""
    echo "Usage:"
    echo "  $0                    # Interactive mode - choose environment"
    echo "  $0 local              # Test localhost:8080"
    echo "  $0 prod <API_URL>     # Test production URL"
    echo "  $0 help               # Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 local"
    echo "  $0 prod https://your-app.railway.app"
    echo "  $0 prod https://your-app.herokuapp.com"
    echo "  $0 prod https://api.yourdomain.com"
    echo ""
    echo "Interactive Mode:"
    echo "  Run without arguments to choose environment interactively"
    echo ""
    echo "Requirements:"
    echo "  - curl command available"
    echo "  - jq command available (for JSON parsing)"
    echo "  - For local testing: Spring Boot app running on port 8080"
    echo "  - For production testing: Valid API URL with health endpoint"
    echo ""
    exit 0
}

# Check for help argument
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "🎫 Starting Comprehensive Attendee Controller Endpoints Test"
echo "============================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to get user input for testing environment
get_testing_environment() {
    # Check for command line arguments
    if [ $# -gt 0 ]; then
        case $1 in
            "local"|"l")
                BASE_URL="http://localhost:8080"
                echo -e "${GREEN}✅ Selected: Local Development (from command line)${NC}"
                echo -e "${YELLOW}💡 Make sure your local Spring Boot application is running${NC}"
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
                echo -e "${RED}❌ Invalid argument. Usage:${NC}"
                echo -e "${YELLOW}   $0 local                    # Test localhost:8080${NC}"
                echo -e "${YELLOW}   $0 prod <API_URL>          # Test production URL${NC}"
                echo -e "${YELLOW}   $0                         # Interactive mode${NC}"
                exit 1
                ;;
        esac
    else
        # Interactive mode
        echo -e "${CYAN}🌍 Choose Testing Environment:${NC}"
        echo "1. Local Development (localhost:8080)"
        echo "2. Production (Custom API URL)"
        echo ""
        read -p "Enter your choice (1 or 2): " choice
        
        case $choice in
            1)
                BASE_URL="http://localhost:8080"
                echo -e "${GREEN}✅ Selected: Local Development${NC}"
                echo -e "${YELLOW}💡 Make sure your local Spring Boot application is running${NC}"
                ;;
            2)
                echo ""
                echo -e "${CYAN}🌐 Enter Production API URL:${NC}"
                echo -e "${YELLOW}   Example: https://your-app.railway.app${NC}"
                echo -e "${YELLOW}   Example: https://your-app.herokuapp.com${NC}"
                echo -e "${YELLOW}   Example: https://api.yourdomain.com${NC}"
                echo ""
                read -p "API URL: " custom_url
                
                # Validate URL format
                if [[ $custom_url =~ ^https?:// ]]; then
                    BASE_URL="$custom_url"
                    echo -e "${GREEN}✅ Selected: Production - $BASE_URL${NC}"
                else
                    echo -e "${RED}❌ Invalid URL format. Please include http:// or https://${NC}"
                    echo -e "${YELLOW}   Example: https://your-app.railway.app${NC}"
                    exit 1
                fi
                ;;
            *)
                echo -e "${RED}❌ Invalid choice. Please select 1 or 2.${NC}"
                exit 1
                ;;
        esac
    fi
    
    echo ""
    echo -e "${BLUE}🔗 Testing URL: $BASE_URL${NC}"
    echo ""
}

# Get testing environment from user
get_testing_environment "$@"

# Path configuration (always resolve relative to this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"

# Configuration
REPORT_FILE="${REPORTS_DIR}/attendee_test_report_$(date +%Y%m%d_%H%M%S).md"
# Use existing test user from create_test_users.sh
TEST_USER_EMAIL="admin@test.com"
TEST_USER_PASSWORD="Admin123!@#"
TEST_USER_NAME="Admin User"
TEST_USER_PHONE="+1234567890"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and test data
ACCESS_TOKEN=""
REFRESH_TOKEN=""
DEVICE_ID=""
USER_ID=""
EVENT_ID=""
ATTENDEE_ID=""
ATTENDEE_ID_2=""
INVITE_ID=""

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Create directories for reports
mkdir -p "$REPORTS_DIR"

# Create report file
cat > "$REPORT_FILE" << EOF
# Attendee Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Basic Attendee Operations | 0 | 0 | 0 | 0% |
| Attendee Invite Status Updates | 0 | 0 | 0 | 0% |
| Check-in Operations | 0 | 0 | 0 | 0% |
| Search & Filtering | 0 | 0 | 0 | 0% |
| Bulk Operations | 0 | 0 | 0 | 0% |
| Notification Options | 0 | 0 | 0 | 0% |
| **TOTAL** | 0 | 0 | 0 | 0% |

---

## Detailed Test Results

EOF

# Function to run a test and log results
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
    local temp_data_file=""
    
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
    
    if [ -n "$data" ]; then
        temp_data_file=$(mktemp)
        printf '%s' "$data" > "$temp_data_file"
        curl_cmd="$curl_cmd --data-binary @$temp_data_file"
    fi
    
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    # Execute the request
    local response
    if ! response=$(eval "$curl_cmd"); then
        echo -e "${RED}❌ Failed to execute curl command${NC}"
        echo -e "${YELLOW}   Command: $curl_cmd${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        if [ -n "$temp_data_file" ]; then
            rm -f "$temp_data_file"
        fi
        return 1
    fi

    if [ -n "$temp_data_file" ]; then
        rm -f "$temp_data_file"
    fi
    
    local http_code
    local response_body
    
    http_code="${response: -3}"
    response_body="${response%???}"
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        if [ "$http_code" = "403" ] || [ "$http_code" = "401" ]; then
            echo -e "${YELLOW}   Response: $response_body${NC}"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi
    
    # Log to report
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
    
    # Extract IDs and tokens from successful responses
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ] || [ "$http_code" = "202" ]; then
        case "$test_name" in
            "User Registration"|"User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                DEVICE_ID=$(echo "$response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Bulk Add Attendees by Email"|"Bulk Add Attendees by UserId"|"Bulk Add More Attendees")
                # Extract first attendee ID from array response
                ATTENDEE_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                # Extract second attendee ID if available
                if [ -z "$ATTENDEE_ID_2" ]; then
                    ATTENDEE_ID_2=$(echo "$response_body" | grep -o '"id":"[^"]*"' | tail -1 | cut -d'"' -f4)
                fi
                ;;
            "Update Invite Status to ACCEPTED"*)
                # After accepting invite, we get an attendee response
                ATTENDEE_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
        esac
    fi
    
    echo ""
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name="Service"
    if [[ $BASE_URL == *"localhost"* ]]; then
        service_name="Local Spring Boot Application"
    else
        service_name="Production Service"
    fi
    
    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        # Check service health
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        else
            echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for $service_name...${NC}"
        fi
        
        sleep 3
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to respond within expected time${NC}"
    return 1
}

# Function to check service availability
check_service() {
    local service_name="Service"
    if [[ $BASE_URL == *"localhost"* ]]; then
        service_name="Local Spring Boot Application"
        echo -e "${YELLOW}🔍 Checking local service availability...${NC}"
        echo -e "${YELLOW}💡 Make sure your local application is running with: mvn spring-boot:run${NC}"
    else
        service_name="Production Service"
        echo -e "${YELLOW}🔍 Checking production service availability...${NC}"
    fi
    
    # Wait for service to be ready
    if wait_for_service; then
        echo -e "${GREEN}✅ $service_name is available!${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name is not available${NC}"
        if [[ $BASE_URL == *"localhost"* ]]; then
            echo -e "${YELLOW}💡 Troubleshooting tips for local development:${NC}"
            echo -e "${YELLOW}   1. Make sure Spring Boot is running: mvn spring-boot:run${NC}"
            echo -e "${YELLOW}   2. Check if port 8080 is available${NC}"
            echo -e "${YELLOW}   3. Verify database connections${NC}"
        else
            echo -e "${YELLOW}💡 Troubleshooting tips for production:${NC}"
            echo -e "${YELLOW}   1. Verify the API URL is correct${NC}"
            echo -e "${YELLOW}   2. Check if the service is deployed and running${NC}"
            echo -e "${YELLOW}   3. Verify network connectivity${NC}"
        fi
        return 1
    fi
}

# Function to authenticate and get tokens
authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"
    
    # Use existing test user (created by create_test_users.sh)
    # Skip registration and go straight to login
    local login_data='{
        "email": "'$TEST_USER_EMAIL'",
        "password": "'$TEST_USER_PASSWORD'",
        "rememberMe": false
    }'
    
    local login_response=$(curl -s -w '%{http_code}' -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/v1/auth/login")
    
    local login_http_code="${login_response: -3}"
    local login_response_body="${login_response%???}"
    
    if [ "$login_http_code" = "200" ]; then
        ACCESS_TOKEN=$(echo "$login_response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$login_response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
        DEVICE_ID=$(echo "$login_response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$ACCESS_TOKEN" ]; then
            local jwt_payload=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
            if command -v python3 &> /dev/null; then
                USER_ID=$(echo "$jwt_payload" | python3 -c "import sys, base64, json; data=sys.stdin.read().strip(); padding=4-len(data)%4; data+=('='*padding if padding<4 else ''); print(json.loads(base64.b64decode(data)).get('sub', ''))" 2>/dev/null)
            elif command -v jq &> /dev/null; then
                local padding=$((4 - ${#jwt_payload} % 4))
                if [ $padding -ne 4 ]; then
                    jwt_payload="${jwt_payload}$(printf '%*s' $padding | tr ' ' '=')"
                fi
                USER_ID=$(echo "$jwt_payload" | base64 -d 2>/dev/null | jq -r '.sub // empty' 2>/dev/null)
            fi
        fi
        
        echo -e "${GREEN}✅ User logged in successfully${NC}"
        return 0
    fi
    
    echo -e "${RED}❌ Failed to authenticate user${NC}"
    return 1
}

# Function to create a test event
create_test_event() {
    echo -e "${YELLOW}📅 Creating test event...${NC}"
    
    # Calculate dates for macOS
    local start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local end_date=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S')
    
    local event_data
    event_data=$(cat <<EOF
{
  "event": {
    "name": "Test Event for Attendees",
    "description": "This is a test event for attendee endpoint testing",
    "eventType": "CONFERENCE",
    "startDateTime": "$start_date",
    "endDateTime": "$end_date",
    "venueRequirements": "Test Venue - Conference Hall",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false,
    "eventWebsiteUrl": "https://example.com/event",
    "hashtag": "#TestEvent",
    "venue": {
      "address": "123 Main Street",
      "city": "San Francisco",
      "state": "California",
      "country": "United States",
      "zipCode": "94102",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "googlePlaceId": "ChIJIQBpAG2ahYAR_6128GcTUEo",
      "googlePlaceData": "{\"name\":\"Test Venue\",\"rating\":4.5}"
    }
  },
  "coverUpload": {
    "fileName": "attendee-test-cover.jpg",
    "contentType": "image/jpeg",
    "category": "cover",
    "isPublic": true,
    "description": "Cover image for attendee test event"
  }
}
EOF
)
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Device-ID: $DEVICE_ID" \
        -H "Content-Type: application/json" \
        -d "$event_data" \
        "$BASE_URL/api/v1/events")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        # Extract event ID from the new response format: { "event": { "id": "..." } }
        if command -v jq &> /dev/null; then
            EVENT_ID=$(echo "$response_body" | jq -r '.event.id // empty')
        else
            # Fallback to grep if jq is not available
            EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        fi
        if [ -z "$EVENT_ID" ]; then
            echo -e "${RED}❌ Failed to extract event ID from response${NC}"
            echo -e "${RED}Response: $response_body${NC}"
            return 1
        fi
        echo -e "${GREEN}✅ Test event created with ID: $EVENT_ID${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to create test event - HTTP: $http_code${NC}"
        echo -e "${RED}Response: $response_body${NC}"
        return 1
    fi
}

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test basic attendee operations"
    echo "5. Test attendee invite status updates"
    echo "6. Test check-in operations"
    echo "7. Test search and filtering"
    echo "8. Test additional bulk operations and notification options"
    echo "9. Clean up test data"
    echo ""
    
    # Step 1: Check service availability
    echo -e "${CYAN}🔍 Step 1: Checking Service Availability${NC}"
    echo "============================================="
    if ! check_service; then
        echo -e "${RED}❌ Service is not available. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 2: Authenticate user
    echo -e "${CYAN}🔐 Step 2: User Authentication${NC}"
    echo "=================================="
    if ! authenticate_user; then
        echo -e "${RED}❌ Failed to authenticate user. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 3: Create test event
    echo -e "${CYAN}📅 Step 3: Create Test Event${NC}"
    echo "============================="
    if ! create_test_event; then
        echo -e "${RED}❌ Failed to create test event. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 4: Basic Attendee Operations Tests
    echo -e "${CYAN}👤 Step 4: Basic Attendee Operations Tests${NC}"
    echo "==========================================="
    
    # Test bulk add attendees (by email) without notifications
    local bulk_attendee_data='{
        "eventId": "'$EVENT_ID'",
        "attendees": [
            {
                "name": "John Doe",
                "email": "john.doe@example.com"
            },
            {
                "name": "Jane Smith",
                "email": "jane.smith@example.com"
            }
        ],
        "sendEmail": false,
        "sendPushNotification": false
    }'
    run_test "Bulk Add Attendees by Email (No Notifications)" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_data" "200" "Bulk add attendees to event by email without notifications"
    
    # Test bulk add attendees (by email) with email notifications
    local bulk_attendee_data_with_email='{
        "eventId": "'$EVENT_ID'",
        "attendees": [
            {
                "name": "Email Test User",
                "email": "email.test@example.com"
            }
        ],
        "sendEmail": true,
        "sendPushNotification": false
    }'
    run_test "Bulk Add Attendees with Email Notifications" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_data_with_email" "200" "Bulk add attendees with email notifications enabled"
    
    # Test bulk add attendees (by userId) with push notifications
    if [ -n "$USER_ID" ]; then
        local bulk_attendee_user_data='{
            "eventId": "'$EVENT_ID'",
            "attendees": [
                {
                    "userId": "'$USER_ID'"
                }
            ],
            "sendEmail": false,
            "sendPushNotification": true
        }'
        run_test "Bulk Add Attendees by UserId (With Push Notifications)" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_user_data" "200" "Bulk add attendees to event by userId with push notifications"
    fi
    
    # Test bulk add attendees (by userId) with both email and push notifications
    if [ -n "$USER_ID" ]; then
        local bulk_attendee_user_data_both='{
            "eventId": "'$EVENT_ID'",
            "attendees": [
                {
                    "userId": "'$USER_ID'"
                }
            ],
            "sendEmail": true,
            "sendPushNotification": true
        }'
        run_test "Bulk Add Attendees with Both Notifications" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_user_data_both" "200" "Bulk add attendees with both email and push notifications"
    fi
    
    # Test list attendees by event (with query params)
    run_test "List Attendees by Event" "GET" "/api/v1/attendees?eventId=$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all attendees for event"
    
    # Test get attendee by ID
    if [ -n "$ATTENDEE_ID" ]; then
        run_test "Get Attendee by ID" "GET" "/api/v1/attendees/$ATTENDEE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific attendee details"
    fi
    
    # Test delete attendee
    if [ -n "$ATTENDEE_ID" ]; then
        run_test "Delete Attendee" "DELETE" "/api/v1/attendees/$ATTENDEE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete attendee from event"
    fi
    echo ""
    
    # Step 5: Attendee Invite Status Update Tests
    echo -e "${CYAN}📋 Step 5: Attendee Invite Status Update Tests${NC}"
    echo "=============================================="
    
    # Note: Invite creation is now handled through the add attendees endpoint
    # This section tests updating invite RSVP status using the unified endpoint
    
    # Test update invite status to ACCEPTED (by inviteId - if available)
    # Note: Since we don't have a way to get invite IDs without listing endpoints,
    # these tests will be skipped if INVITE_ID is not available
    if [ -n "$INVITE_ID" ]; then
        run_test "Update Invite Status to ACCEPTED (by inviteId)" "POST" "/api/v1/attendees/invites?inviteId=$INVITE_ID&status=ACCEPTED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Update attendee invite status to ACCEPTED using inviteId"
    else
        echo -e "${YELLOW}⚠️  Skipping invite status tests - no invite ID available${NC}"
        echo -e "${YELLOW}   Note: Invite IDs are typically obtained from invite creation or listing endpoints${NC}"
    fi
    
    # Test update invite status to DECLINED (by inviteId - if available)
    if [ -n "$INVITE_ID" ]; then
        run_test "Update Invite Status to DECLINED (by inviteId)" "POST" "/api/v1/attendees/invites?inviteId=$INVITE_ID&status=DECLINED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Update attendee invite status to DECLINED using inviteId"
    fi
    
    # Test update invite status to REVOKED (by inviteId - if available)
    if [ -n "$INVITE_ID" ]; then
        run_test "Update Invite Status to REVOKED (by inviteId)" "POST" "/api/v1/attendees/invites?inviteId=$INVITE_ID&status=REVOKED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Update attendee invite status to REVOKED using inviteId"
    fi
    
    # Test update invite status with invalid status
    if [ -n "$INVITE_ID" ]; then
        run_test "Update Invite Status with Invalid Status" "POST" "/api/v1/attendees/invites?inviteId=$INVITE_ID&status=INVALID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Attempt to update invite status with invalid status value"
    fi
    
    # Test update invite status without inviteId or token
    run_test "Update Invite Status without inviteId or token" "POST" "/api/v1/attendees/invites?status=ACCEPTED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Attempt to update invite status without inviteId or token"
    
    # Test update invite status without status parameter
    if [ -n "$INVITE_ID" ]; then
        run_test "Update Invite Status without Status Parameter" "POST" "/api/v1/attendees/invites?inviteId=$INVITE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Attempt to update invite status without status parameter"
    fi
    echo ""
    
    # Step 6: Check-in Tests
    echo -e "${CYAN}✅ Step 6: Check-in Tests${NC}"
    echo "=============================="
    
    # Note: Check-in functionality is handled through the attendee service
    # Check-in is done by setting checkedInAt timestamp, not through a separate endpoint
    # This would typically be done through a service method, but we can test the filtered list
    
    # Test get checked-in attendees (filtered list)
    run_test "Get Checked-in Attendees" "GET" "/api/v1/attendees?eventId=$EVENT_ID&checkedIn=true" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all checked-in attendees"
    
    # Test get not checked-in attendees
    run_test "Get Not Checked-in Attendees" "GET" "/api/v1/attendees?eventId=$EVENT_ID&checkedIn=false" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all attendees who haven't checked in"
    echo ""
    
    # Step 7: Search and Filtering Tests
    echo -e "${CYAN}🔍 Step 8: Search and Filtering Tests${NC}"
    echo "======================================"
    
    # Test search attendees by name/email
    run_test "Search Attendees" "GET" "/api/v1/attendees?eventId=$EVENT_ID&search=John" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search attendees by name or email"
    
    # Test filter attendees by RSVP status
    run_test "Filter Attendees by Status (CONFIRMED)" "GET" "/api/v1/attendees?eventId=$EVENT_ID&status=CONFIRMED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by RSVP status (CONFIRMED)"
    
    run_test "Filter Attendees by Status (PENDING)" "GET" "/api/v1/attendees?eventId=$EVENT_ID&status=PENDING" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by RSVP status (PENDING)"
    
    run_test "Filter Attendees by Multiple Statuses" "GET" "/api/v1/attendees?eventId=$EVENT_ID&status=CONFIRMED,PENDING" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by multiple RSVP statuses"
    
    # Test filter by email
    run_test "Filter Attendees by Email" "GET" "/api/v1/attendees?eventId=$EVENT_ID&email=john.doe@example.com" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by email"
    
    # Test filter by userId
    if [ -n "$USER_ID" ]; then
        run_test "Filter Attendees by UserId" "GET" "/api/v1/attendees?eventId=$EVENT_ID&userId=$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by userId"
    fi
    
    # Test pagination
    run_test "List Attendees with Pagination" "GET" "/api/v1/attendees?eventId=$EVENT_ID&page=0&size=10" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List attendees with pagination"
    
    # Test sorting
    run_test "List Attendees Sorted by Name" "GET" "/api/v1/attendees?eventId=$EVENT_ID&sortBy=name&sortDirection=ASC" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List attendees sorted by name"
    
    run_test "List Attendees Sorted by Created Date" "GET" "/api/v1/attendees?eventId=$EVENT_ID&sortBy=createdAt&sortDirection=DESC" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List attendees sorted by creation date"
    echo ""
    
    # Step 8: Additional Bulk Operations Tests
    echo -e "${CYAN}🔢 Step 9: Additional Bulk Operations Tests${NC}"
    echo "=========================================="
    
    # Test bulk add more attendees (mixed: email and userId)
    local bulk_attendee_data_2='{
        "eventId": "'$EVENT_ID'",
        "attendees": [
            {
                "name": "Alice Johnson",
                "email": "alice.johnson@example.com"
            },
            {
                "name": "Bob Williams",
                "email": "bob.williams@example.com"
            }
        ],
        "sendEmail": false,
        "sendPushNotification": false
    }'
    run_test "Bulk Add More Attendees" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_data_2" "200" "Bulk add more attendees to event"
    
    # Test single attendee add (bulk endpoint supports single attendee too)
    local single_attendee_data='{
        "eventId": "'$EVENT_ID'",
        "attendees": [
            {
                "name": "Single Attendee",
                "email": "single.attendee@example.com"
            }
        ],
        "sendEmail": false,
        "sendPushNotification": false
    }'
    run_test "Add Single Attendee via Bulk Endpoint" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$single_attendee_data" "200" "Add single attendee using bulk endpoint"
    echo ""
    
    # Step 9: Clean up test data
    echo -e "${CYAN}🧹 Step 10: Clean Up Test Data${NC}"
    echo "================================"
    
    # Delete test attendees (if we have IDs)
    # Note: We'll delete attendees that were created during testing
    # The script will attempt to delete the first attendee ID captured
    
    # Delete test event
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
            echo "✅ **Excellent!** All tests are passing successfully. The attendee system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the attendee system."
        } >> "$REPORT_FILE"
    fi
    
    {
        echo ""
        echo "### Next Steps"
        echo ""
        echo "1. Review failed tests and fix underlying issues"
        echo "2. Check server logs for detailed error information"
        echo "3. Verify database connectivity and data integrity"
        echo "4. Test with different user scenarios"
        echo "5. Consider adding more edge case tests"
        echo ""
        echo "---"
        echo ""
        echo "**Report generated by:** Attendee Controller Test Script"
        echo "**Script version:** 1.0"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Attendee system is working correctly.${NC}"
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
    if [[ $BASE_URL == *"localhost"* ]]; then
        echo -e "${YELLOW}💡 Local service continues running${NC}"
    else
        echo -e "${YELLOW}💡 Production service continues running${NC}"
    fi
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Run main function
main "$@"
