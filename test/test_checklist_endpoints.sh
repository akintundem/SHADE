#!/bin/bash

# Comprehensive Checklist Controller Endpoints Test Script
# Tests all checklist-related endpoints and generates a detailed report

# Function to show help
show_help() {
    echo "✅ Checklist Controller Endpoints Test Script"
    echo "============================================="
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

echo "✅ Starting Comprehensive Checklist Controller Endpoints Test"
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
CLIENT_ID="web-app"
REPORT_FILE="reports/checklist_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="testuser@example.com"
TEST_USER_PASSWORD="Password123!"
TEST_USER_NAME="Test User"
TEST_USER_PHONE="+1234567890"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and data
ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""
EVENT_ID=""
CHECKLIST_ITEM_ID=""

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Checklist Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Client ID:** $CLIENT_ID
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Authentication | 0 | 0 | 0 | 0% |
| Event Creation | 0 | 0 | 0 | 0% |
| Checklist CRUD | 0 | 0 | 0 | 0% |
| Checklist Status | 0 | 0 | 0 | 0% |
| Filtering & Search | 0 | 0 | 0 | 0% |
| Categories | 0 | 0 | 0 | 0% |
| Templates | 0 | 0 | 0 | 0% |
| Bulk Operations | 0 | 0 | 0 | 0% |
| Analytics | 0 | 0 | 0 | 0% |
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
    local curl_exec_cmd="curl -s -w '%{http_code}' -X $method"
    local curl_display_cmd="curl -s -X $method"
    
    if [ -n "$headers" ]; then
        curl_exec_cmd="$curl_exec_cmd $headers"
        curl_display_cmd="$curl_display_cmd $headers"
    fi
    
    local request_body_display="(empty)"
    if [ -n "$data" ]; then
        curl_exec_cmd="$curl_exec_cmd -d '$data'"
        curl_display_cmd="$curl_display_cmd -d '$data'"
        request_body_display="$data"
    fi
    
    curl_exec_cmd="$curl_exec_cmd '$BASE_URL$endpoint'"
    curl_display_cmd="$curl_display_cmd '$BASE_URL$endpoint'"
    
    # Execute the request
    local response=$(eval "$curl_exec_cmd")
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        echo -e "${RED}   Response: $response_body${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi
    
    local headers_display="$headers"
    if [ -z "$headers_display" ]; then
        headers_display="(none)"
    fi
    
    local response_body_display="$response_body"
    if [ -z "$response_body_display" ]; then
        response_body_display="(empty response body)"
    fi
    
    # Log to report
    cat >> "$REPORT_FILE" << EOF

### $test_name
**Status:** $status_icon $http_code (Expected: $expected_status)
**Description:** $description
**Endpoint:** $method $endpoint
**Request Headers:** $headers_display
**Request Body:** $request_body_display
**Request Command:**
\`\`\`bash
$curl_display_cmd
\`\`\`

**Response Status:** $http_code
**Response Body:**
\`\`\`json
$response_body_display
\`\`\`

---

EOF
    
    # Extract data from successful responses
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
            "Create Checklist Item")
                CHECKLIST_ITEM_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
        esac
    fi
    
    echo ""
}

# Function to wait for service to be ready
wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=60
    local attempt=1
    local java_ready=false
    
    while [ $attempt -le $max_attempts ]; do
        # Check Java Spring Boot service
        if ! $java_ready; then
            if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Java Application is ready!${NC}"
                java_ready=true
            else
                echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for Java Application...${NC}"
            fi
        fi
        
        # If service is ready, return success
        if $java_ready; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        fi
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ Service failed to start within expected time${NC}"
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
        service_name="Remote Service"
        echo -e "${YELLOW}🔍 Checking remote service availability...${NC}"
    fi
    
    if wait_for_service; then
        echo -e "${GREEN}✅ $service_name is available!${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name is not available${NC}"
        if [[ $BASE_URL == *"localhost"* ]]; then
            echo -e "${YELLOW}💡 Troubleshooting tips:${NC}"
            echo -e "${YELLOW}   1. Start Spring Boot with: mvn spring-boot:run${NC}"
            echo -e "${YELLOW}   2. Confirm port 8080 is free${NC}"
            echo -e "${YELLOW}   3. Verify database connectivity${NC}"
        else
            echo -e "${YELLOW}💡 Troubleshooting tips:${NC}"
            echo -e "${YELLOW}   1. Verify the API URL is correct${NC}"
            echo -e "${YELLOW}   2. Confirm the service is deployed and running${NC}"
            echo -e "${YELLOW}   3. Check network connectivity${NC}"
        fi
        return 1
    fi
}

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Verify service availability"
    echo "2. Test health check endpoint"
    echo "3. Test user authentication"
    echo "4. Test event creation"
    echo "5. Test checklist CRUD operations"
    echo "6. Test checklist status management"
    echo "7. Test checklist filtering and search"
    echo "8. Test checklist categories"
    echo "9. Test checklist templates"
    echo "10. Test bulk operations"
    echo "11. Test checklist analytics"
    echo ""
    
    # Step 1: Verify service availability
    echo -e "${CYAN}🔍 Step 1: Checking Service Availability${NC}"
    echo "=========================================="
    if ! check_service; then
        echo -e "${RED}❌ Service is not available. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 2: Health Check Tests
    echo -e "${CYAN}🏥 Step 2: Health Check Tests${NC}"
    echo "============================="
    run_test "Health Check" "GET" "/api/v1/auth/health" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "" "200" "Check if auth service is healthy"
    echo ""
    
    # Step 3: Authentication Tests
    echo -e "${CYAN}🔐 Step 3: Authentication Tests${NC}"
    echo "=================================="
    
    # Register user
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "'$TEST_USER_NAME'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "phoneNumber": "'$TEST_USER_PHONE'",
        "dateOfBirth": "1990-01-01",
        "acceptTerms": true,
        "acceptPrivacy": true,
        "marketingOptIn": false,
        "deviceId": "test-device-123",
        "clientId": "'$CLIENT_ID'"
    }'
    
    run_test "User Registration" "POST" "/api/v1/auth/register" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$registration_data" "201" "Register a new user"
    
    # Login user
    local login_data='{
        "email": "'$TEST_USER_EMAIL'",
        "password": "'$TEST_USER_PASSWORD'",
        "rememberMe": false,
        "deviceId": "test-device-123",
        "clientId": "'$CLIENT_ID'"
    }'
    
    run_test "User Login" "POST" "/api/v1/auth/login" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$login_data" "200" "Login with valid credentials"
    echo ""
    
    # Step 4: Event Creation Tests
    echo -e "${CYAN}🎉 Step 4: Event Creation Tests${NC}"
    echo "================================="
    
    if [ -n "$ACCESS_TOKEN" ]; then
        # Extract user ID from token
        USER_ID=$(curl -s -X GET "$BASE_URL/api/v1/auth/me" \
            -H "X-Client-ID: $CLIENT_ID" \
            -H "Authorization: Bearer $ACCESS_TOKEN" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        
        local event_data='{
            "name": "Test Corporate Event",
            "description": "Annual company conference",
            "eventType": "CORPORATE",
            "startDateTime": "2025-12-15T09:00:00",
            "endDateTime": "2025-12-15T17:00:00",
            "capacity": 500,
            "isPublic": false,
            "requiresApproval": false
        }'
        
        run_test "Create Event" "POST" "/api/v1/events" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'X-User-Id: $USER_ID' -H 'Content-Type: application/json'" "$event_data" "201" "Create a new event"
    else
        echo -e "${RED}❌ No access token available for event creation tests${NC}"
    fi
    echo ""
    
    # Step 5: Checklist CRUD Tests
    echo -e "${CYAN}📝 Step 5: Checklist CRUD Tests${NC}"
    echo "================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Get checklist
        run_test "Get Checklist" "GET" "/api/v1/checklist/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event checklist"
        
        # Get checklist summary
        run_test "Get Checklist Summary" "GET" "/api/v1/checklist/$EVENT_ID/summary" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get checklist summary"
        
        # Create checklist item
        local checklist_item_data='{
            "eventId": "'$EVENT_ID'",
            "title": "Book Venue",
            "description": "Reserve conference venue",
            "isCompleted": false,
            "dueDate": "2024-11-15T23:59:59",
            "priority": "HIGH",
            "assignedTo": "'$USER_ID'",
            "category": "venue"
        }'
        
        run_test "Create Checklist Item" "POST" "/api/v1/checklist" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checklist_item_data" "200" "Create a new checklist item"
        
        # Create bulk checklist items
        local bulk_checklist_data='{
            "items": [
                {
                    "eventId": "'$EVENT_ID'",
                    "title": "Order Catering",
                    "description": "Arrange food and beverages",
                    "isCompleted": false,
                    "dueDate": "2024-11-20T23:59:59",
                    "priority": "HIGH",
                    "category": "catering"
                },
                {
                    "eventId": "'$EVENT_ID'",
                    "title": "Send Invitations",
                    "description": "Send event invitations to attendees",
                    "isCompleted": false,
                    "dueDate": "2024-11-25T23:59:59",
                    "priority": "MEDIUM",
                    "category": "communication"
                },
                {
                    "eventId": "'$EVENT_ID'",
                    "title": "Setup AV Equipment",
                    "description": "Install audio-visual equipment",
                    "isCompleted": false,
                    "dueDate": "2024-12-14T18:00:00",
                    "priority": "CRITICAL",
                    "category": "setup"
                }
            ]
        }'
        
        run_test "Create Bulk Checklist Items" "POST" "/api/v1/checklist/bulk" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_checklist_data" "200" "Create multiple checklist items"
    else
        echo -e "${RED}❌ No access token or event ID available for checklist tests${NC}"
    fi
    echo ""
    
    # Step 6: Checklist Status Management Tests
    echo -e "${CYAN}📊 Step 6: Checklist Status Management Tests${NC}"
    echo "============================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$CHECKLIST_ITEM_ID" ]; then
        # Mark checklist item as complete
        run_test "Mark Checklist Item Complete" "PUT" "/api/v1/checklist/$CHECKLIST_ITEM_ID/complete" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Mark checklist item as complete"
        
        # Mark checklist item as incomplete
        run_test "Mark Checklist Item Incomplete" "PUT" "/api/v1/checklist/$CHECKLIST_ITEM_ID/incomplete" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Mark checklist item as incomplete"
        
        # Toggle checklist item completion
        run_test "Toggle Checklist Item" "PUT" "/api/v1/checklist/$CHECKLIST_ITEM_ID/toggle" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Toggle checklist item completion status"
        
        # Assign checklist item
        run_test "Assign Checklist Item" "PUT" "/api/v1/checklist/$CHECKLIST_ITEM_ID/assign?assignedTo=$USER_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Assign checklist item to user"
    else
        echo -e "${RED}❌ No access token or checklist item ID available for status tests${NC}"
    fi
    echo ""
    
    # Step 7: Filtering and Search Tests
    echo -e "${CYAN}🔍 Step 7: Filtering and Search Tests${NC}"
    echo "====================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Filter checklist items
        run_test "Filter Checklist Items" "GET" "/api/v1/checklist/$EVENT_ID/filter?isCompleted=false&priority=HIGH" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter checklist items by completion status and priority"
        
        # Get completed items
        run_test "Get Completed Items" "GET" "/api/v1/checklist/$EVENT_ID/completed" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get completed checklist items"
        
        # Get pending items
        run_test "Get Pending Items" "GET" "/api/v1/checklist/$EVENT_ID/pending" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get pending checklist items"
        
        # Get overdue items
        run_test "Get Overdue Items" "GET" "/api/v1/checklist/$EVENT_ID/overdue" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get overdue checklist items"
        
        # Get items due soon
        run_test "Get Items Due Soon" "GET" "/api/v1/checklist/$EVENT_ID/due-soon" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get checklist items due within 7 days"
    else
        echo -e "${RED}❌ No access token or event ID available for filtering tests${NC}"
    fi
    echo ""
    
    # Step 8: Categories Tests
    echo -e "${CYAN}📂 Step 8: Categories Tests${NC}"
    echo "============================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Get categories
        run_test "Get Categories" "GET" "/api/v1/checklist/$EVENT_ID/categories" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all checklist categories"
        
        # Get items by category
        run_test "Get Items by Category" "GET" "/api/v1/checklist/$EVENT_ID/category/venue" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get checklist items by category"
    else
        echo -e "${RED}❌ No access token or event ID available for category tests${NC}"
    fi
    echo ""
    
    # Step 9: Templates Tests
    echo -e "${CYAN}📄 Step 9: Templates Tests${NC}"
    echo "============================="
    
    if [ -n "$ACCESS_TOKEN" ]; then
        # Get checklist templates
        run_test "Get Checklist Templates" "GET" "/api/v1/checklist/templates?eventType=CORPORATE" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get checklist templates for corporate events"
        
        # Get template details
        run_test "Get Template Details" "GET" "/api/v1/checklist/templates/Conference" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get details of Conference template"
        
        if [ -n "$EVENT_ID" ]; then
            # Apply checklist template
            local template_data='{
                "templateName": "Conference",
                "eventType": "CORPORATE"
            }'
            run_test "Apply Checklist Template" "POST" "/api/v1/checklist/$EVENT_ID/apply-template" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$template_data" "200" "Apply checklist template to event"
        fi
    else
        echo -e "${RED}❌ No access token available for template tests${NC}"
    fi
    echo ""
    
    # Step 10: Bulk Operations Tests
    echo -e "${CYAN}📦 Step 10: Bulk Operations Tests${NC}"
    echo "====================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Complete all items
        run_test "Complete All Items" "PUT" "/api/v1/checklist/$EVENT_ID/complete-all" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Mark all checklist items as completed"
        
        # Mark all items incomplete
        run_test "Incomplete All Items" "PUT" "/api/v1/checklist/$EVENT_ID/incomplete-all" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Mark all checklist items as incomplete"
        
        # Assign all items
        run_test "Assign All Items" "PUT" "/api/v1/checklist/$EVENT_ID/assign-all?assignedTo=$USER_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Assign all checklist items to user"
    else
        echo -e "${RED}❌ No access token or event ID available for bulk operation tests${NC}"
    fi
    echo ""
    
    # Step 11: Analytics Tests
    echo -e "${CYAN}📈 Step 11: Analytics Tests${NC}"
    echo "============================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Get checklist analytics
        run_test "Get Checklist Analytics" "GET" "/api/v1/checklist/$EVENT_ID/analytics" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get detailed checklist analytics"
        
        # Get completion progress
        run_test "Get Completion Progress" "GET" "/api/v1/checklist/$EVENT_ID/progress" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get completion progress over time"
    else
        echo -e "${RED}❌ No access token or event ID available for analytics tests${NC}"
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
    cat >> "$REPORT_FILE" << EOF

## Final Summary

**Test Execution Completed:** $(date)
**Total Tests:** $TOTAL_TESTS
**Passed:** $PASSED_TESTS
**Failed:** $FAILED_TESTS
**Success Rate:** $success_rate%

### Test Categories Breakdown

| Category | Tests | Passed | Failed | Success Rate |
|----------|-------|--------|--------|--------------|
| Health Check | 1 | $([ $PASSED_TESTS -gt 0 ] && echo "1" || echo "0") | $([ $FAILED_TESTS -gt 0 ] && echo "1" || echo "0") | $([ $PASSED_TESTS -gt 0 ] && echo "100%" || echo "0%") |
| Authentication | 2 | 0 | 0 | 0% |
| Event Creation | 1 | 0 | 0 | 0% |
| Checklist CRUD | 4 | 0 | 0 | 0% |
| Checklist Status | 4 | 0 | 0 | 0% |
| Filtering & Search | 5 | 0 | 0 | 0% |
| Categories | 2 | 0 | 0 | 0% |
| Templates | 3 | 0 | 0 | 0% |
| Bulk Operations | 3 | 0 | 0 | 0% |
| Analytics | 2 | 0 | 0 | 0% |

### Recommendations

EOF
    
    if [ $success_rate -ge 90 ]; then
        cat >> "$REPORT_FILE" << EOF
✅ **Excellent!** All checklist tests are passing successfully. The checklist system is working correctly.
EOF
    elif [ $success_rate -ge 70 ]; then
        cat >> "$REPORT_FILE" << EOF
⚠️ **Good** - Most checklist tests are passing, but there are some issues that need attention.
EOF
    else
        cat >> "$REPORT_FILE" << EOF
❌ **Needs Attention** - Multiple test failures indicate significant issues with the checklist system.
EOF
    fi
    
    cat >> "$REPORT_FILE" << EOF

### Next Steps

1. Review failed tests and fix underlying issues
2. Check server logs for detailed error information
3. Verify database connectivity and data integrity
4. Test with different event scenarios
5. Consider adding more edge case tests

---

**Report generated by:** Checklist Controller Test Script
**Script version:** 1.0
EOF
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All checklist tests passed! Checklist system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some checklist tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
