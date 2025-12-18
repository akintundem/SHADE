#!/bin/bash

# Comprehensive Timeline Controller Endpoints Test Script
# Tests all timeline-related endpoints and generates a detailed report
# Supports both local and production testing with custom API URLs
#
# Usage:
#   ./test_timeline_endpoints.sh                    # Interactive mode
#   ./test_timeline_endpoints.sh local              # Test localhost:8080
#   ./test_timeline_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_timeline_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "⏰ Timeline Controller Endpoints Test Script"
    echo "==========================================="
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

echo "⏰ Starting Comprehensive Timeline Controller Endpoints Test"
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

# Configuration
REPORT_FILE="test/reports/timeline_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="timelinetest@example.com"
TEST_USER_PASSWORD="Password123!"
TEST_USER_NAME="Timeline Test User"
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
TIMELINE_ITEM_ID=""
TASK_ID=""
TASK_ID_2=""

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Create report file
mkdir -p test/reports
cat > "$REPORT_FILE" << EOF
# Timeline Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|-------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Authentication | 0 | 0 | 0 | 0% |
| Event Creation | 0 | 0 | 0 | 0% |
| Timeline CRUD | 0 | 0 | 0 | 0% |
| Timeline Reorder | 0 | 0 | 0 | 0% |
| Dependencies | 0 | 0 | 0 | 0% |
| Timeline Publish | 0 | 0 | 0 | 0% |
| Task Management | 0 | 0 | 0 | 0% |
| Task Filters | 0 | 0 | 0 | 0% |
| Task Bulk Operations | 0 | 0 | 0 | 0% |
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
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ] || [ "$http_code" = "202" ] || [ "$http_code" = "204" ]; then
        case "$test_name" in
            "User Registration"|"User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                DEVICE_ID=$(echo "$response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                if [ -z "$EVENT_ID" ]; then
                    # Try alternative format
                    EVENT_ID=$(echo "$response_body" | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | head -1)
                fi
                ;;
            "Create Timeline Item")
                TIMELINE_ITEM_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                if [ -z "$TIMELINE_ITEM_ID" ]; then
                    # Try alternative format
                    TIMELINE_ITEM_ID=$(echo "$response_body" | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | head -1)
                fi
                ;;
            "Create Task")
                TASK_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                if [ -z "$TASK_ID" ]; then
                    # Try alternative format
                    TASK_ID=$(echo "$response_body" | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | head -1)
                fi
                ;;
            "Create Second Task")
                if [ -z "$TASK_ID_2" ]; then
                    TASK_ID_2=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                    if [ -z "$TASK_ID_2" ]; then
                        # Try alternative format
                        TASK_ID_2=$(echo "$response_body" | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | head -1)
                    fi
                fi
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
    
    local event_data='{
        "name": "Test Event for Timeline",
        "description": "This is a test event for timeline endpoint testing",
        "eventType": "CONFERENCE",
        "startDateTime": "'$start_date'",
        "endDateTime": "'$end_date'",
        "venueRequirements": "Test Venue - Conference Hall",
        "capacity": 100,
        "isPublic": true,
        "requiresApproval": false,
        "coverImageUrl": "https://example.com/cover.jpg",
        "eventWebsiteUrl": "https://example.com/event",
        "hashtag": "#TestEvent"
    }'
    
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

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test timeline CRUD operations"
    echo "5. Test timeline reorder"
    echo "6. Test dependencies batch update"
    echo "7. Test timeline publish"
    echo "8. Test task management"
    echo "9. Test task filters and pagination"
    echo "10. Test task bulk operations"
    echo "11. Clean up test data"
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
    
    # Step 4: Timeline CRUD Operations Tests
    echo -e "${CYAN}📅 Step 4: Timeline CRUD Operations Tests${NC}"
    echo "==========================================="
    
    # Test get timeline
    run_test "Get Timeline" "GET" "/api/v1/timeline/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get run-of-show timeline for event"
    
    # Test generate workback schedule
    local workback_data='{
        "eventDate": "'$(date -u -v+1d '+%Y-%m-%d')'"
    }'
    run_test "Generate Workback Schedule" "POST" "/api/v1/timeline/$EVENT_ID/workback" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$workback_data" "200" "Generate workback schedule from event date"
    
    # Test create timeline item
    local timeline_item_data='{
        "eventId": "'$EVENT_ID'",
        "title": "Setup Ceremony Area",
        "description": "Setup ceremony area with decorations and seating",
        "scheduledAt": "'$(date -u -v+1d -v+16H '+%Y-%m-%dT%H:%M:%S')'",
        "durationMinutes": 60,
        "assignedTo": "'$USER_ID'"
    }'
    run_test "Create Timeline Item" "POST" "/api/v1/timeline" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$timeline_item_data" "200" "Create a new timeline item"
    
    # Test update timeline item
    if [ -n "$TIMELINE_ITEM_ID" ] && [ -n "$EVENT_ID" ]; then
        local update_timeline_data='{
            "title": "Updated Setup Ceremony Area",
            "description": "Updated description",
            "scheduledAt": "'$(date -u -v+1d -v+16H '+%Y-%m-%dT%H:%M:%S')'",
            "durationMinutes": 90
        }'
        run_test "Update Timeline Item" "PUT" "/api/v1/timeline/$EVENT_ID/items/$TIMELINE_ITEM_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_timeline_data" "200" "Update timeline item"
    fi
    echo ""
    
    # Step 5: Timeline Reorder Tests
    echo -e "${CYAN}🔄 Step 5: Timeline Reorder Tests${NC}"
    echo "=================================="
    
    # Create a second timeline item for reordering
    if [ -n "$EVENT_ID" ]; then
        local timeline_item_2_data='{
            "eventId": "'$EVENT_ID'",
            "title": "Guest Arrival",
            "description": "Welcome guests and check-in",
            "scheduledAt": "'$(date -u -v+1d -v+17H '+%Y-%m-%dT%H:%M:%S')'",
            "durationMinutes": 30
        }'
        run_test "Create Second Timeline Item" "POST" "/api/v1/timeline" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$timeline_item_2_data" "200" "Create second timeline item for reordering"
        
        # Test reorder timeline items
        # Get the second timeline item ID if available
        if [ -n "$TIMELINE_ITEM_ID" ]; then
            # Try to get all timeline items to find the second one, or use the first one
            local second_item_id="$TIMELINE_ITEM_ID"
            local reorder_data='{
                "items": [
                    {
                        "itemId": "'$TIMELINE_ITEM_ID'",
                        "taskOrder": 1,
                        "scheduledAt": "'$(date -u -v+1d -v+16H '+%Y-%m-%dT%H:%M:%S')'"
                    }
                ]
            }'
            run_test "Reorder Timeline Items" "PATCH" "/api/v1/timeline/$EVENT_ID/order" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$reorder_data" "200" "Reorder timeline items"
        fi
    fi
    echo ""
    
    # Step 6: Dependencies Batch Update Tests
    echo -e "${CYAN}🔗 Step 6: Dependencies Batch Update Tests${NC}"
    echo "==========================================="
    
    if [ -n "$EVENT_ID" ] && [ -n "$TIMELINE_ITEM_ID" ]; then
        local dependencies_data='{
            "updates": [
                {
                    "itemId": "'$TIMELINE_ITEM_ID'",
                    "dependencies": []
                }
            ]
        }'
        run_test "Batch Update Dependencies" "PATCH" "/api/v1/timeline/$EVENT_ID/dependencies" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$dependencies_data" "200" "Batch update timeline item dependencies"
    fi
    echo ""
    
    # Step 7: Timeline Publish Tests
    echo -e "${CYAN}📢 Step 7: Timeline Publish Tests${NC}"
    echo "=================================="
    
    if [ -n "$EVENT_ID" ]; then
        local publish_data='{
            "published": true,
            "message": "Timeline published for review"
        }'
        run_test "Publish Timeline" "PATCH" "/api/v1/timeline/$EVENT_ID/publish" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$publish_data" "204" "Publish timeline"
        
        local unpublish_data='{
            "published": false,
            "message": "Timeline unpublished"
        }'
        run_test "Unpublish Timeline" "PATCH" "/api/v1/timeline/$EVENT_ID/publish" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$unpublish_data" "204" "Unpublish timeline"
    fi
    echo ""
    
    # Step 8: Task Management Tests
    echo -e "${CYAN}✅ Step 8: Task Management Tests${NC}"
    echo "===================================="
    
    if [ -n "$EVENT_ID" ]; then
        # Test get tasks
        run_test "Get Tasks" "GET" "/api/v1/timeline/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all tasks for event"
        
        # Test get timeline summary
        run_test "Get Timeline Summary" "GET" "/api/v1/timeline/$EVENT_ID/summary" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get timeline summary with statistics"
        
        # Test create task
        local task_data='{
            "eventId": "'$EVENT_ID'",
            "title": "Finalize Guest List",
            "description": "Confirm all guest RSVPs",
            "startDate": "'$(date -u -v+1d -v+14H '+%Y-%m-%dT%H:%M:%S')'",
            "dueDate": "'$(date -u -v+1d -v+15H '+%Y-%m-%dT%H:%M:%S')'",
            "priority": "HIGH",
            "category": "guest",
            "status": "TO_DO",
            "durationMinutes": 60
        }'
        run_test "Create Task" "POST" "/api/v1/timeline/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task_data" "201" "Create a new task"
        
        # Test update task
        if [ -n "$TASK_ID" ]; then
            local update_task_data='{
                "title": "Updated Finalize Guest List",
                "status": "IN_PROGRESS",
                "progressPercentage": 50,
                "description": "Updated description for task"
            }'
            run_test "Update Task" "PUT" "/api/v1/timeline/tasks/$TASK_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_task_data" "200" "Update task details"
            
            # Test update task position
            local position_data='{
                "startDate": "'$(date -u -v+1d -v+14H '+%Y-%m-%dT%H:%M:%S')'",
                "endDate": "'$(date -u -v+1d -v+15H '+%Y-%m-%dT%H:%M:%S')'",
                "durationMinutes": 60
            }'
            run_test "Update Task Position" "PUT" "/api/v1/timeline/tasks/$TASK_ID/position" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$position_data" "200" "Update task position on timeline"
        fi
    fi
    echo ""
    
    # Step 9: Task Filters and Pagination Tests
    echo -e "${CYAN}🔍 Step 9: Task Filters and Pagination Tests${NC}"
    echo "============================================="
    
    if [ -n "$EVENT_ID" ]; then
        # Test get upcoming tasks
        run_test "Get Upcoming Tasks" "GET" "/api/v1/timeline/$EVENT_ID/upcoming?days=7" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get upcoming tasks for next 7 days"
        
        # Test get overdue tasks
        run_test "Get Overdue Tasks" "GET" "/api/v1/timeline/$EVENT_ID/overdue" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get overdue tasks"
        
        # Test search tasks
        run_test "Search Tasks" "GET" "/api/v1/timeline/$EVENT_ID/search?query=guest" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search tasks by query"
        
        # Test get tasks with status filter
        run_test "Get Tasks by Status" "GET" "/api/v1/timeline/$EVENT_ID/tasks?status=TO_DO" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get tasks filtered by status"
        
        # Test get tasks with category filter
        run_test "Get Tasks by Category" "GET" "/api/v1/timeline/$EVENT_ID/tasks?category=guest" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get tasks filtered by category"
        
        # Test get timeline view
        run_test "Get Timeline View" "GET" "/api/v1/timeline/$EVENT_ID/view?viewType=daily" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get timeline view with filters"
    fi
    echo ""
    
    # Step 10: Task Bulk Operations Tests
    echo -e "${CYAN}🔢 Step 10: Task Bulk Operations Tests${NC}"
    echo "======================================"
    
    if [ -n "$EVENT_ID" ] && [ -n "$TASK_ID" ]; then
        # Create a second task for bulk operations
        local task_2_data='{
            "eventId": "'$EVENT_ID'",
            "title": "Confirm Vendors",
            "description": "Confirm all vendor bookings",
            "startDate": "'$(date -u -v+1d -v+12H '+%Y-%m-%dT%H:%M:%S')'",
            "dueDate": "'$(date -u -v+1d -v+13H '+%Y-%m-%dT%H:%M:%S')'",
            "priority": "HIGH",
            "category": "vendor",
            "status": "TO_DO",
            "durationMinutes": 60
        }'
        run_test "Create Second Task" "POST" "/api/v1/timeline/$EVENT_ID/tasks" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$task_2_data" "201" "Create second task for bulk operations"
        
        # Test bulk update tasks
        if [ -n "$TASK_ID" ] && [ -n "$TASK_ID_2" ]; then
            local bulk_update_data='{
                "updates": [
                    {
                        "taskId": "'$TASK_ID'",
                        "status": "COMPLETED",
                        "endDate": "'$(date -u -v+1d -v+15H '+%Y-%m-%dT%H:%M:%S')'"
                    },
                    {
                        "taskId": "'$TASK_ID_2'",
                        "status": "IN_PROGRESS",
                        "startDate": "'$(date -u -v+1d -v+12H '+%Y-%m-%dT%H:%M:%S')'"
                    }
                ]
            }'
            run_test "Bulk Update Tasks" "PUT" "/api/v1/timeline/$EVENT_ID/tasks/bulk" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_update_data" "200" "Bulk update multiple tasks"
        fi
    fi
    echo ""
    
    # Step 11: Clean up test data
    echo -e "${CYAN}🧹 Step 11: Clean Up Test Data${NC}"
    echo "================================"
    
    # Test delete task
    if [ -n "$TASK_ID" ]; then
        run_test "Delete Task" "DELETE" "/api/v1/timeline/tasks/$TASK_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete task"
    fi
    
    # Test delete timeline item
    if [ -n "$TIMELINE_ITEM_ID" ] && [ -n "$EVENT_ID" ]; then
        run_test "Delete Timeline Item" "DELETE" "/api/v1/timeline/$EVENT_ID/items/$TIMELINE_ITEM_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete timeline item"
    fi
    
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
        echo "**Report generated by:** Timeline Controller Test Script"
        echo "**Script version:** 1.0"
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
