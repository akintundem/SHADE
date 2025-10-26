#!/bin/bash

# Comprehensive Checklist Controller Endpoints Test Script
# Tests all checklist-related endpoints and generates a detailed report

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

# Configuration
BASE_URL="http://localhost:8080"
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
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi
    
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    # Execute the request
    local response=$(eval $curl_cmd)
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi
    
    # Log to report
    cat >> "$REPORT_FILE" << EOF

### $test_name
**Status:** $status_icon $http_code (Expected: $expected_status)
**Description:** $description
**Endpoint:** $method $endpoint
**Request Headers:** $headers
**Request Body:** $data

**Response:**
\`\`\`json
$response_body
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
    echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
    local max_attempts=60
    local attempt=1
    local java_ready=false
    local python_ready=false
    
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
        
        # Check Python Shade Assistant service
        if ! $python_ready; then
            if curl -s "http://localhost:8000/health" > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Python Shade Assistant is ready!${NC}"
                python_ready=true
            else
                echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for Python Assistant...${NC}"
            fi
        fi
        
        # If both services are ready, return success
        if $java_ready && $python_ready; then
            echo -e "${GREEN}✅ All services are ready!${NC}"
            return 0
        fi
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ Services failed to start within expected time${NC}"
    return 1
}

# Function to stop and start services
restart_services() {
    echo -e "${YELLOW}🛑 Stopping services...${NC}"
    cd .. && ./stop_full_stack.sh
    
    echo -e "${YELLOW}⏳ Waiting 5 seconds...${NC}"
    sleep 5
    
    echo -e "${YELLOW}🚀 Starting services...${NC}"
    ./start_full_stack.sh &
    
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
    echo "1. Stop and restart services"
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
    
    # Step 1: Restart services
    echo -e "${CYAN}🔄 Step 1: Restarting Services${NC}"
    echo "=================================="
    if ! restart_services; then
        echo -e "${RED}❌ Failed to restart services. Exiting.${NC}"
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
