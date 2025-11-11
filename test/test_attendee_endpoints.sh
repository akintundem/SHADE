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

# Configuration
REPORT_FILE="reports/attendee_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="attendeetest@example.com"
TEST_USER_PASSWORD="Password123!"
TEST_USER_NAME="Attendee Test User"
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
ATTENDANCE_ID=""
ATTENDANCE_ID_2=""
QR_CODE=""

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Create report file
mkdir -p reports
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
| Attendance Management | 0 | 0 | 0 | 0% |
| Check-in/Check-out | 0 | 0 | 0 | 0% |
| QR Code Management | 0 | 0 | 0 | 0% |
| Analytics & Reporting | 0 | 0 | 0 | 0% |
| Search & Filtering | 0 | 0 | 0 | 0% |
| Bulk Operations | 0 | 0 | 0 | 0% |
| Export & Import | 0 | 0 | 0 | 0% |
| Communication | 0 | 0 | 0 | 0% |
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
            "Bulk Add Attendees")
                ATTENDEE_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Register for Event")
                ATTENDANCE_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Bulk Register Attendees")
                if [ -z "$ATTENDANCE_ID_2" ]; then
                    ATTENDANCE_ID_2=$(echo "$response_body" | grep -o '"id":"[^"]*"' | tail -1 | cut -d'"' -f4)
                fi
                ;;
            "Get Attendee QR Code")
                QR_CODE=$(echo "$response_body" | tr -d '"')
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
        "name": "Test Event for Attendees",
        "description": "This is a test event for attendee endpoint testing",
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
    echo "4. Test basic attendee operations"
    echo "5. Test attendance management"
    echo "6. Test check-in/check-out operations"
    echo "7. Test QR code management"
    echo "8. Test analytics and reporting"
    echo "9. Test search and filtering"
    echo "10. Test bulk operations"
    echo "11. Test export and import"
    echo "12. Test communication features"
    echo "13. Clean up test data"
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
    
    # Test bulk add attendees
    local bulk_attendee_data='{
        "eventId": "'$EVENT_ID'",
        "attendees": [
            {
                "name": "John Doe",
                "email": "john.doe@example.com",
                "phone": "+1234567891"
            },
            {
                "name": "Jane Smith",
                "email": "jane.smith@example.com",
                "phone": "+1234567892"
            }
        ]
    }'
    run_test "Bulk Add Attendees" "POST" "/api/v1/attendees" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendee_data" "200" "Bulk add attendees to event"
    
    # Test list attendees by event
    run_test "List Attendees by Event" "GET" "/api/v1/attendees/event/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all attendees for event"
    
    # Test update RSVP status
    if [ -n "$ATTENDEE_ID" ]; then
        run_test "Update RSVP Status" "PATCH" "/api/v1/attendees/events/$EVENT_ID/attendees/$ATTENDEE_ID/rsvp?status=CONFIRMED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Update attendee RSVP status"
    fi
    
    # Test check-in attendee (basic)
    if [ -n "$ATTENDEE_ID" ]; then
        run_test "Check-in Attendee (Basic)" "POST" "/api/v1/attendees/events/$EVENT_ID/attendees/$ATTENDEE_ID/check-in" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Check-in attendee using basic endpoint"
    fi
    echo ""
    
    # Step 5: Attendance Management Tests
    echo -e "${CYAN}📋 Step 5: Attendance Management Tests${NC}"
    echo "========================================"
    
    # Test register for event
    local attendance_data='{
        "userId": "'$USER_ID'",
        "name": "Test Attendee",
        "email": "test.attendee@example.com",
        "phone": "+1234567893",
        "attendanceStatus": "REGISTERED",
        "ticketType": "VIP",
        "dietaryRestrictions": "Vegetarian",
        "accessibilityNeeds": "Wheelchair access",
        "emergencyContact": "Emergency Contact Name",
        "emergencyPhone": "+1234567894",
        "notes": "Test notes"
    }'
    run_test "Register for Event" "POST" "/api/v1/events/$EVENT_ID/attendances" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$attendance_data" "201" "Register user for event"
    
    # Test bulk register attendees
    local bulk_attendance_data='{
        "attendances": [
            {
                "userId": "'$USER_ID'",
                "name": "Bulk Attendee 1",
                "email": "bulk1@example.com",
                "phone": "+1234567895",
                "ticketType": "REGULAR"
            },
            {
                "userId": "'$USER_ID'",
                "name": "Bulk Attendee 2",
                "email": "bulk2@example.com",
                "phone": "+1234567896",
                "ticketType": "REGULAR"
            }
        ]
    }'
    run_test "Bulk Register Attendees" "POST" "/api/v1/events/$EVENT_ID/attendances/bulk" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_attendance_data" "201" "Bulk register attendees"
    
    # Test get all attendees
    run_test "Get All Attendees" "GET" "/api/v1/events/$EVENT_ID/attendances" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all attendees for event"
    
    # Test get specific attendance
    if [ -n "$ATTENDANCE_ID" ]; then
        run_test "Get Specific Attendance" "GET" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific attendance details"
    fi
    
    # Test update attendance
    if [ -n "$ATTENDANCE_ID" ]; then
        local update_attendance_data='{
            "name": "Updated Test Attendee",
            "email": "updated.attendee@example.com",
            "phone": "+1234567897",
            "ticketType": "PREMIUM",
            "dietaryRestrictions": "Vegan",
            "notes": "Updated notes"
        }'
        run_test "Update Attendance" "PUT" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_attendance_data" "200" "Update attendance details"
    fi
    echo ""
    
    # Step 6: Check-in/Check-out Tests
    echo -e "${CYAN}✅ Step 6: Check-in/Check-out Tests${NC}"
    echo "===================================="
    
    # Test check-in attendee
    if [ -n "$ATTENDANCE_ID" ]; then
        local checkin_data='{
            "checkInMethod": "MANUAL",
            "checkInLocation": "Main Entrance",
            "notes": "Checked in manually"
        }'
        run_test "Check-in Attendee" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/check-in" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checkin_data" "200" "Check-in attendee with details"
    fi
    
    # Test get checked-in attendees
    run_test "Get Checked-in Attendees" "GET" "/api/v1/events/$EVENT_ID/attendances/checked-in" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all checked-in attendees"
    
    # Test get attendance stats
    run_test "Get Attendance Statistics" "GET" "/api/v1/events/$EVENT_ID/attendances/attendance-stats" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance statistics"
    
    # Test check-out attendee
    if [ -n "$ATTENDANCE_ID" ]; then
        run_test "Check-out Attendee" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/check-out" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Check-out attendee"
    fi
    echo ""
    
    # Step 7: QR Code Management Tests
    echo -e "${CYAN}📱 Step 7: QR Code Management Tests${NC}"
    echo "===================================="
    
    # Test get attendee QR code
    if [ -n "$ATTENDANCE_ID" ]; then
        run_test "Get Attendee QR Code" "GET" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/qr-code" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get QR code for attendee"
    fi
    
    # Test scan QR code
    if [ -n "$QR_CODE" ]; then
        run_test "Scan QR Code" "POST" "/api/v1/events/$EVENT_ID/attendances/scan-qr?qrCode=$QR_CODE" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Scan QR code for check-in"
    fi
    
    # Test regenerate QR code
    if [ -n "$ATTENDANCE_ID" ]; then
        run_test "Regenerate QR Code" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/regenerate-qr" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Regenerate QR code"
    fi
    echo ""
    
    # Step 8: Analytics and Reporting Tests
    echo -e "${CYAN}📊 Step 8: Analytics and Reporting Tests${NC}"
    echo "========================================="
    
    # Test get attendance analytics
    run_test "Get Attendance Analytics" "GET" "/api/v1/events/$EVENT_ID/analytics/attendance" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance analytics"
    
    # Test get check-in timeline
    run_test "Get Check-in Timeline" "GET" "/api/v1/events/$EVENT_ID/analytics/check-in-timeline" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get check-in timeline"
    
    # Test get attendance by type
    run_test "Get Attendance by Type" "GET" "/api/v1/events/$EVENT_ID/analytics/attendance-by-type" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance breakdown by type"
    
    # Test get no-show analytics
    run_test "Get No-show Analytics" "GET" "/api/v1/events/$EVENT_ID/analytics/no-shows" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get no-show analytics"
    
    # Test get registration timeline
    run_test "Get Registration Timeline" "GET" "/api/v1/events/$EVENT_ID/analytics/registration-timeline" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get registration timeline"
    echo ""
    
    # Step 9: Search and Filtering Tests
    echo -e "${CYAN}🔍 Step 9: Search and Filtering Tests${NC}"
    echo "======================================"
    
    # Test search attendees
    run_test "Search Attendees by Name" "GET" "/api/v1/events/$EVENT_ID/attendances/search?name=Test" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search attendees by name"
    
    run_test "Search Attendees by Email" "GET" "/api/v1/events/$EVENT_ID/attendances/search?email=example.com" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search attendees by email"
    
    # Test filter attendees
    run_test "Filter Attendees by Status" "GET" "/api/v1/events/$EVENT_ID/attendances/filter?status=REGISTERED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by status"
    
    run_test "Filter Attendees by Ticket Type" "GET" "/api/v1/events/$EVENT_ID/attendances/filter?ticketType=VIP" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees by ticket type"
    
    run_test "Filter Attendees with Dietary Restrictions" "GET" "/api/v1/events/$EVENT_ID/attendances/filter?hasDietaryRestrictions=true" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter attendees with dietary restrictions"
    echo ""
    
    # Step 10: Bulk Operations Tests
    echo -e "${CYAN}🔢 Step 10: Bulk Operations Tests${NC}"
    echo "=================================="
    
    # Test bulk update attendees
    if [ -n "$ATTENDANCE_ID" ] && [ -n "$ATTENDANCE_ID_2" ]; then
        local bulk_update_data='{
            "attendanceIds": ["'$ATTENDANCE_ID'", "'$ATTENDANCE_ID_2'"],
            "updates": {
                "ticketType": "UPDATED",
                "notes": "Bulk updated"
            }
        }'
        run_test "Bulk Update Attendees" "POST" "/api/v1/events/$EVENT_ID/attendances/bulk-update" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_update_data" "200" "Bulk update attendees"
    fi
    
    # Test validate attendee data
    run_test "Validate Attendee Data" "GET" "/api/v1/events/$EVENT_ID/attendances/validate" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Validate attendee data"
    
    # Test find duplicate attendees
    run_test "Find Duplicate Attendees" "GET" "/api/v1/events/$EVENT_ID/attendances/duplicates" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Find duplicate attendees"
    
    # Test find incomplete profiles
    run_test "Find Incomplete Profiles" "GET" "/api/v1/events/$EVENT_ID/attendances/incomplete" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Find incomplete attendee profiles"
    
    # Test get capacity status
    run_test "Get Capacity Status" "GET" "/api/v1/events/$EVENT_ID/attendances/capacity-status" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event capacity status"
    
    # Test get waitlist status
    run_test "Get Waitlist Status" "GET" "/api/v1/events/$EVENT_ID/attendances/waitlist-status" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get waitlist status"
    echo ""
    
    # Step 11: Export and Import Tests
    echo -e "${CYAN}💾 Step 11: Export and Import Tests${NC}"
    echo "===================================="
    
    # Test export to CSV
    run_test "Export Attendees to CSV" "GET" "/api/v1/events/$EVENT_ID/attendances/export/csv" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Export attendee list to CSV"
    
    # Test export to Excel
    run_test "Export Attendees to Excel" "GET" "/api/v1/events/$EVENT_ID/attendances/export/excel" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Export attendee list to Excel"
    
    # Test import from CSV
    local csv_data='name,email,phone,ticketType
Import Test 1,import1@example.com,+1234567897,REGULAR
Import Test 2,import2@example.com,+1234567898,VIP'
    run_test "Import Attendees from CSV" "POST" "/api/v1/events/$EVENT_ID/attendances/import/csv" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: text/plain'" "$csv_data" "201" "Import attendees from CSV"
    echo ""
    
    # Step 12: Communication Tests
    echo -e "${CYAN}📧 Step 12: Communication Tests${NC}"
    echo "================================"
    
    # Test send invitations
    local invitation_data='{
        "recipients": ["test1@example.com", "test2@example.com"],
        "subject": "Event Invitation",
        "message": "You are invited to our event!",
        "includeQRCode": true
    }'
    run_test "Send Invitations" "POST" "/api/v1/events/$EVENT_ID/invitations/send" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invitation_data" "200" "Send event invitations"
    
    # Test get sent invitations
    run_test "Get Sent Invitations" "GET" "/api/v1/events/$EVENT_ID/invitations" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all sent invitations"
    
    # Test send bulk email
    local bulk_email_data='{
        "subject": "Event Update",
        "message": "Important event update for all attendees",
        "recipientType": "ALL"
    }'
    run_test "Send Bulk Email" "POST" "/api/v1/events/$EVENT_ID/attendances/bulk-email" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_email_data" "200" "Send bulk email to attendees"
    
    # Test send notification to specific attendee
    if [ -n "$ATTENDANCE_ID" ]; then
        local notification_data='{
            "subject": "Personal Notification",
            "message": "This is a personal notification",
            "channels": ["EMAIL"]
        }'
        run_test "Send Notification to Attendee" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/notify" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$notification_data" "200" "Send notification to specific attendee"
    fi
    
    # Test get communication history
    run_test "Get Communication History" "GET" "/api/v1/events/$EVENT_ID/attendances/communication-history" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get communication history"
    
    # Test send invites (async)
    if [ -n "$ATTENDEE_ID" ]; then
        local send_invites_data='{
            "eventId": "'$EVENT_ID'",
            "attendeeIds": ["'$ATTENDEE_ID'"],
            "sendEmail": true,
            "sendPush": false,
            "customMessage": "Looking forward to seeing you at the event!"
        }'
        run_test "Send Invites (Async)" "POST" "/api/v1/attendees/invites/send" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$send_invites_data" "202" "Send invites asynchronously"
    fi
    echo ""
    
    # Step 13: Clean up test data
    echo -e "${CYAN}🧹 Step 13: Clean Up Test Data${NC}"
    echo "================================"
    
    # Test cancel attendance
    if [ -n "$ATTENDANCE_ID" ]; then
        run_test "Cancel Attendance" "DELETE" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Cancel user attendance"
    fi
    
    # Test bulk delete attendees
    if [ -n "$ATTENDANCE_ID_2" ]; then
        local bulk_delete_data='{
            "attendanceIds": ["'$ATTENDANCE_ID_2'"]
        }'
        run_test "Bulk Delete Attendees" "POST" "/api/v1/events/$EVENT_ID/attendances/bulk-delete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_delete_data" "204" "Bulk delete attendees"
    fi
    
    # Delete test event
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
