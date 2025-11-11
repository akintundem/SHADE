#!/bin/bash

# Comprehensive Event Controller Endpoints Test Script
# Tests all event-related endpoints and generates a detailed report
# Supports both local and production testing with custom API URLs
#
# Usage:
#   ./test_event_endpoints.sh                    # Interactive mode
#   ./test_event_endpoints.sh local              # Test localhost:8080
#   ./test_event_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_event_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "🎉 Event Controller Endpoints Test Script"
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

echo "🎉 Starting Comprehensive Event Controller Endpoints Test"
echo "========================================================"

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

# Configuration
REPORT_FILE="reports/event_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="testuser@example.com"
TEST_USER_PASSWORD="Password123!"
TEST_USER_NAME="Test User"
TEST_USER_PHONE="+1234567890"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and test data
ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""
EVENT_ID=""
MEDIA_ID=""
COLLABORATOR_ID=""
REMINDER_ID=""
DEVICE_ID=""

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Event Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| CRUD Operations | 0 | 0 | 0 | 0% |
| Event Management | 0 | 0 | 0 | 0% |
| Media Management | 0 | 0 | 0 | 0% |
| Collaboration | 0 | 0 | 0 | 0% |
| Notifications | 0 | 0 | 0 | 0% |
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
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
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
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        case "$test_name" in
            "User Registration"|"User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Upload Event Media")
                MEDIA_ID=$(echo "$response_body" | grep -o '"mediaId":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Add Event Collaborator")
                COLLABORATOR_ID=$(echo "$response_body" | grep -o '"collaboratorId":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Create Event Reminder")
                REMINDER_ID=$(echo "$response_body" | grep -o '"reminderId":"[^"]*"' | cut -d'"' -f4)
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
    
    # First, try to register a new user
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "'$TEST_USER_NAME'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "phoneNumber": "'$TEST_USER_PHONE'",
        "dateOfBirth": "1990-01-01",
        "acceptTerms": true,
        "acceptPrivacy": true,
        "marketingOptIn": false
    }'
    
    local response=$(curl -s -w '%{http_code}' -X POST \
                -H "Content-Type: application/json" \
        -d "$registration_data" \
        "$BASE_URL/api/v1/auth/register")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✅ User registered${NC}"
        verify_email_in_database "$TEST_USER_EMAIL"
    elif [ "$http_code" != "400" ]; then
        echo -e "${RED}❌ Failed to register user${NC}"
        return 1
    fi
    
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
    
    local event_data=$(cat <<EOF
{
    "name": "Test Event",
    "description": "This is a test event for endpoint testing",
    "eventType": "CONFERENCE",
    "startDateTime": "$start_date",
    "endDateTime": "$end_date",
    "venueRequirements": "Test Location - Conference Room A",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false,
    "coverImageUrl": "https://example.com/cover.jpg",
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
}
EOF
)
    
    # Debug: Show what we're sending
    echo -e "${CYAN}   Debug: Creating event with token: ${ACCESS_TOKEN:0:20}...${NC}"
    echo -e "${CYAN}   Debug: User ID: $USER_ID${NC}"
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Device-ID: $DEVICE_ID" \
        -H "Content-Type: application/json" \
        -d "$event_data" \
        "$BASE_URL/api/v1/events")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
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

# Function to stop and start services
restart_services() {
    echo -e "${YELLOW}🛑 Stopping services...${NC}"
    cd .. && ./stop_full_stack.sh && cd test
    
    echo -e "${YELLOW}⏳ Waiting 5 seconds...${NC}"
    sleep 5
    
    echo -e "${YELLOW}🚀 Starting services...${NC}"
    cd .. && ./start_full_stack.sh & cd test
    
    # Wait for services to be ready
    if wait_for_service; then
        echo -e "${GREEN}✅ Services restarted successfully!${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to restart services${NC}"
        return 1
    fi
}

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test CRUD operations"
    echo "5. Test event management endpoints"
    echo "6. Test media management endpoints"
    echo "7. Test collaboration endpoints"
    echo "8. Test notification endpoints"
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
    
    # Step 4: CRUD Operations Tests
    echo -e "${CYAN}📝 Step 4: CRUD Operations Tests${NC}"
    echo "=================================="
    
    # Test get event by ID
    run_test "Get Event by ID" "GET" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event by ID"
    
    # Test update event
    local update_start_date=$(date -u -v+2d '+%Y-%m-%dT%H:%M:%S')
    local update_end_date=$(date -u -v+2d -v+3H '+%Y-%m-%dT%H:%M:%S')
    local update_data=$(cat <<EOF
{
    "name": "Updated Test Event",
    "description": "Updated description",
    "eventType": "WORKSHOP",
    "startDateTime": "$update_start_date",
    "endDateTime": "$update_end_date",
    "venueRequirements": "Updated Location - Workshop Room B",
    "capacity": 150,
    "isPublic": false,
    "requiresApproval": true,
    "venue": {
        "address": "456 Market Street",
        "city": "San Francisco",
        "state": "California",
        "country": "United States",
        "zipCode": "94105",
        "latitude": 37.7849,
        "longitude": -122.4094,
        "googlePlaceId": "ChIJUpdatedPlaceID123",
        "googlePlaceData": "{\"name\":\"Updated Venue\",\"rating\":4.8}"
    }
}
EOF
)
    run_test "Update Event" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_data" "200" "Update event details"
    
    # Test get non-existent event
    run_test "Get Non-existent Event" "GET" "/api/v1/events/00000000-0000-0000-0000-000000000000" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Get non-existent event"
    echo ""
    
    # Step 5: Event Management Tests
    echo -e "${CYAN}🎯 Step 5: Event Management Tests${NC}"
    echo "=================================="
    
    # User-Event Relationship Tests
    run_test "Get User Events" "GET" "/api/v1/events/user/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all events for user"
    run_test "Get User Owned Events" "GET" "/api/v1/events/user/$USER_ID/owned" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get events owned by user"
    run_test "Get User Upcoming Events" "GET" "/api/v1/events/user/$USER_ID/upcoming" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get upcoming events for user"
    run_test "Get User Past Events" "GET" "/api/v1/events/user/$USER_ID/past" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get past events for user"
    
    # My Events Tests
    run_test "Get My Events" "GET" "/api/v1/events/my-events" "-H 'Authorization: Bearer $ACCESS_TOKEN' " "" "200" "Get current user's events"
    run_test "Get My Owned Events" "GET" "/api/v1/events/my-events/owned" "-H 'Authorization: Bearer $ACCESS_TOKEN' " "" "200" "Get current user's owned events"
    run_test "Get My Upcoming Events" "GET" "/api/v1/events/my-events/upcoming" "-H 'Authorization: Bearer $ACCESS_TOKEN' " "" "200" "Get current user's upcoming events"
    run_test "Get My Past Events" "GET" "/api/v1/events/my-events/past" "-H 'Authorization: Bearer $ACCESS_TOKEN' " "" "200" "Get current user's past events"
    
    # Event Status & Lifecycle Tests
    run_test "Get Event Status" "GET" "/api/v1/events/$EVENT_ID/status" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event status"
    
    local status_update_data='{
        "eventStatus": "PUBLISHED"
    }'
    run_test "Update Event Status" "PUT" "/api/v1/events/$EVENT_ID/status" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$status_update_data" "200" "Update event status"
    
    run_test "Publish Event" "POST" "/api/v1/events/$EVENT_ID/publish" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Publish event"
    run_test "Open Registration" "POST" "/api/v1/events/$EVENT_ID/open-registration" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Open event registration"
    run_test "Close Registration" "POST" "/api/v1/events/$EVENT_ID/close-registration" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Close event registration"
    
    # Event Discovery & Search Tests
    run_test "Search Events" "GET" "/api/v1/events/search?q=test" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search events with query"
    run_test "Get Public Events" "GET" "/api/v1/events/public" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get public events"
    run_test "Get Featured Events" "GET" "/api/v1/events/featured" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get featured events"
    run_test "Get Trending Events" "GET" "/api/v1/events/trending" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get trending events"
    run_test "Get Upcoming Events" "GET" "/api/v1/events/upcoming" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get upcoming events"
    run_test "Get Events by Type" "GET" "/api/v1/events/by-type/WORKSHOP" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get events by type"
    run_test "Get Events by Status" "GET" "/api/v1/events/by-status/PUBLISHED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get events by status"
    
    # Event Capacity & Registration Tests
    run_test "Get Event Capacity" "GET" "/api/v1/events/$EVENT_ID/capacity" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event capacity information"
    
    local capacity_update_data='{
        "capacity": 200
    }'
    run_test "Update Event Capacity" "PUT" "/api/v1/events/$EVENT_ID/capacity" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$capacity_update_data" "200" "Update event capacity"
    
    run_test "Get Available Capacity" "GET" "/api/v1/events/$EVENT_ID/capacity/available" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get available capacity"
    
    local deadline_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local deadline_data='{
        "deadline": "'$deadline_date'"
    }'
    run_test "Update Registration Deadline" "PUT" "/api/v1/events/$EVENT_ID/registration-deadline" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$deadline_data" "200" "Update registration deadline"
    
    # QR Code Tests
    run_test "Get Event QR Code" "GET" "/api/v1/events/$EVENT_ID/qr-code" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event QR code"
    run_test "Generate QR Code" "POST" "/api/v1/events/$EVENT_ID/qr-code/generate" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Generate QR code for event"
    run_test "Regenerate QR Code" "POST" "/api/v1/events/$EVENT_ID/qr-code/regenerate" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Regenerate QR code"
    run_test "Disable QR Code" "DELETE" "/api/v1/events/$EVENT_ID/qr-code" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Disable QR code"
    
    # Visibility & Access Control Tests
    run_test "Get Event Visibility" "GET" "/api/v1/events/$EVENT_ID/visibility" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event visibility settings"
    
    local visibility_data='{
        "isPublic": true
    }'
    run_test "Update Event Visibility" "PUT" "/api/v1/events/$EVENT_ID/visibility" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$visibility_data" "200" "Update event visibility"
    
    run_test "Make Event Public" "POST" "/api/v1/events/$EVENT_ID/make-public" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Make event public"
    run_test "Make Event Private" "POST" "/api/v1/events/$EVENT_ID/make-private" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Make event private"
    
    # Analytics Tests
    run_test "Get Event Analytics" "GET" "/api/v1/events/$EVENT_ID/analytics" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event analytics"
    run_test "Get Attendance Analytics" "GET" "/api/v1/events/$EVENT_ID/analytics/attendance" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance analytics"
    
    # Duplication Tests
    local duplicate_data='{
        "newEventName": "Duplicated Test Event"
    }'
    run_test "Duplicate Event" "POST" "/api/v1/events/$EVENT_ID/duplicate" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$duplicate_data" "200" "Duplicate event"
    
    # Validation & Health Check Tests
    run_test "Validate Event" "GET" "/api/v1/events/$EVENT_ID/validation" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Validate event"
    run_test "Event Health Check" "GET" "/api/v1/events/$EVENT_ID/health" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Event health check"
    echo ""
    
    # Step 6: Media Management Tests
    echo -e "${CYAN}📸 Step 6: Media Management Tests${NC}"
    echo "=================================="
    
    # Media Tests (Note: These will return mock responses since upload is presigned)
    run_test "Get Event Media" "GET" "/api/v1/events/$EVENT_ID/media" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event media"
    
    # Request presigned media upload
    local media_upload_request='{
        "fileName": "test-image.jpg",
        "contentType": "image/jpeg",
        "category": "gallery",
        "isPublic": true,
        "description": "Test image upload"
    }'
    run_test "Upload Event Media" "POST" "/api/v1/events/$EVENT_ID/media" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$media_upload_request" "200" "Upload event media"
    
    # Get specific media (using captured media ID when available)
    local mock_media_id="12345678-1234-1234-1234-123456789012"
    local media_identifier="${MEDIA_ID:-$mock_media_id}"
    run_test "Get Specific Media" "GET" "/api/v1/events/$EVENT_ID/media/$media_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific media"
    
    # Update media
    local media_update_data='{
        "mediaType": "image",
        "mediaName": "Updated Test Image",
        "description": "Updated description",
        "category": "gallery",
        "isPublic": false,
        "tags": "test,event"
    }'
    run_test "Update Media" "PUT" "/api/v1/events/$EVENT_ID/media/$media_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$media_update_data" "200" "Update media information"
    
    # Delete media
    run_test "Delete Media" "DELETE" "/api/v1/events/$EVENT_ID/media/$media_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete media"
    
    # Assets Tests
    run_test "Get Event Assets" "GET" "/api/v1/events/$EVENT_ID/assets" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event assets"
    local asset_upload_request='{
        "fileName": "test-document.pdf",
        "contentType": "application/pdf",
        "category": "documents",
        "isPublic": false,
        "description": "Test document upload"
    }'
    run_test "Upload Event Asset" "POST" "/api/v1/events/$EVENT_ID/assets" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$asset_upload_request" "200" "Upload event asset"
    
    # Cover Image Tests
    local cover_image_request='{
        "fileName": "cover-image.png",
        "contentType": "image/png",
        "category": "cover",
        "isPublic": true,
        "description": "Cover image upload"
    }'
    run_test "Update Cover Image" "PUT" "/api/v1/events/$EVENT_ID/cover-image" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$cover_image_request" "200" "Update cover image"
    run_test "Remove Cover Image" "DELETE" "/api/v1/events/$EVENT_ID/cover-image" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Remove cover image"
    echo ""
    
    # Step 7: Collaboration Tests
    echo -e "${CYAN}🤝 Step 7: Collaboration Tests${NC}"
    echo "==============================="
    
    # Sharing Tests
    run_test "Get Sharing Options" "GET" "/api/v1/events/$EVENT_ID/share" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event sharing options"
    
    local share_data='{
        "channel": "email",
        "recipients": ["test@example.com", "user@example.com"],
        "message": "Check out this event!",
        "includeEventDetails": true,
        "includeQRCode": false
    }'
    run_test "Share Event" "POST" "/api/v1/events/$EVENT_ID/share" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$share_data" "200" "Share event via email"
    
    # Collaboration Tests
    run_test "Get Event Collaborators" "GET" "/api/v1/events/$EVENT_ID/collaborators" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event collaborators"
    
    local collaborator_data='{
        "userId": "87654321-4321-4321-4321-210987654321",
        "email": "collaborator@example.com",
        "role": "COLLABORATOR",
        "permissions": ["read", "write"],
        "notes": "Test collaborator",
        "sendInvitation": true
    }'
    run_test "Add Event Collaborator" "POST" "/api/v1/events/$EVENT_ID/collaborators" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$collaborator_data" "200" "Add event collaborator"
    
    # Update collaborator (using captured ID when available)
    local mock_collaborator_id="87654321-4321-4321-4321-210987654321"
    local collaborator_identifier="${COLLABORATOR_ID:-$mock_collaborator_id}"
    local collaborator_update_data='{
        "userId": "87654321-4321-4321-4321-210987654321",
        "email": "updated-collaborator@example.com",
        "role": "ADMIN",
        "permissions": ["read", "write", "delete"],
        "notes": "Updated collaborator"
    }'
    run_test "Update Event Collaborator" "PUT" "/api/v1/events/$EVENT_ID/collaborators/$collaborator_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$collaborator_update_data" "200" "Update event collaborator"
    
    run_test "Remove Event Collaborator" "DELETE" "/api/v1/events/$EVENT_ID/collaborators/$collaborator_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Remove event collaborator"
    echo ""
    
    # Step 8: Notification Tests
    echo -e "${CYAN}🔔 Step 8: Notification Tests${NC}"
    echo "==============================="
    
    # Notification Settings Tests
    run_test "Get Notification Settings" "GET" "/api/v1/events/$EVENT_ID/notifications" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event notification settings"
    
    local notification_settings='{
        "enabledChannels": ["EMAIL", "PUSH"],
        "reminderEnabled": true,
        "defaultReminderMinutes": 1440
    }'
    run_test "Update Notification Settings" "PUT" "/api/v1/events/$EVENT_ID/notifications" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$notification_settings" "200" "Update notification settings"
    
    # Send Notification Tests
    local notification_data='{
        "channel": "EMAIL",
        "subject": "Event Update",
        "content": "This is a test notification",
        "recipientUserIds": ["'$USER_ID'"],
        "recipientEmails": ["test@example.com"],
        "scheduledAt": null,
        "priority": "NORMAL"
    }'
    run_test "Send Event Notification" "POST" "/api/v1/events/$EVENT_ID/notifications/send" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$notification_data" "200" "Send event notification"
    
    # Reminder Tests
    run_test "Get Event Reminders" "GET" "/api/v1/events/$EVENT_ID/reminders" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event reminders"
    
    local reminder_time=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local reminder_data
    reminder_data=$(cat <<EOF
{
    "title": "Event Reminder",
    "description": "Don't forget about the event!",
    "reminderTime": "$reminder_time",
    "channel": "email",
    "reminderType": "custom",
    "isActive": true,
    "customMessage": "Custom reminder message",
    "recipientUserIds": ["$USER_ID"],
    "recipientEmails": ["test@example.com"]
}
EOF
)
    run_test "Create Event Reminder" "POST" "/api/v1/events/$EVENT_ID/reminders" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$reminder_data" "200" "Create event reminder"
    
    # Update reminder (using captured ID when available)
    local mock_reminder_id="12345678-1234-1234-1234-123456789012"
    local reminder_identifier="${REMINDER_ID:-$mock_reminder_id}"
    local reminder_update_time=$(date -u -v+2d '+%Y-%m-%dT%H:%M:%S')
    local reminder_update_data
    reminder_update_data=$(cat <<EOF
{
    "title": "Updated Event Reminder",
    "description": "Updated reminder description",
    "reminderTime": "$reminder_update_time",
    "channel": "sms",
    "reminderType": "custom",
    "isActive": true,
    "customMessage": "Updated reminder message",
    "recipientUserIds": ["$USER_ID"],
    "recipientEmails": ["test@example.com"]
}
EOF
)
    run_test "Update Event Reminder" "PUT" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$reminder_update_data" "200" "Update event reminder"
    
    run_test "Get Specific Reminder" "GET" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific reminder"
    
    run_test "Delete Event Reminder" "DELETE" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete event reminder"
    echo ""
    
    # Step 9: Clean up test data
    echo -e "${CYAN}🧹 Step 9: Clean Up Test Data${NC}"
    echo "==============================="
    
    if [ -n "$EVENT_ID" ]; then
        run_test "Delete Test Event" "DELETE" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete test event"
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
            echo "✅ **Excellent!** All tests are passing successfully. The event system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the event system."
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
        echo "**Report generated by:** Event Controller Test Script"
        echo "**Script version:** 1.0"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Event system is working correctly.${NC}"
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
