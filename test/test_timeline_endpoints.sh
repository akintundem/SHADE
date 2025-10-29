#!/bin/bash

# Comprehensive Timeline Controller Endpoints Test Script
# Tests all timeline-related endpoints and generates a detailed report

echo "⏰ Starting Comprehensive Timeline Controller Endpoints Test"
echo "=========================================================="

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
REPORT_FILE="reports/timeline_test_report_$(date +%Y%m%d_%H%M%S).md"
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
TIMELINE_ITEM_ID=""

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Timeline Controller Endpoints Test Report

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
| Timeline CRUD | 0 | 0 | 0 | 0% |
| Timeline Status | 0 | 0 | 0 | 0% |
| Workback Scheduling | 0 | 0 | 0 | 0% |
| Filtering & Search | 0 | 0 | 0 | 0% |
| Dependencies | 0 | 0 | 0 | 0% |
| Templates | 0 | 0 | 0 | 0% |
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
            "Create Timeline Item")
                TIMELINE_ITEM_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
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
    echo "5. Test timeline CRUD operations"
    echo "6. Test timeline status management"
    echo "7. Test workback scheduling"
    echo "8. Test timeline filtering and search"
    echo "9. Test timeline dependencies"
    echo "10. Test timeline templates"
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
            "name": "Test Wedding Event",
            "description": "A beautiful wedding celebration",
            "eventType": "WEDDING",
            "startDateTime": "2025-12-25T18:00:00",
            "endDateTime": "2025-12-25T23:00:00",
            "capacity": 150,
            "isPublic": false,
            "requiresApproval": false
        }'
        
        run_test "Create Event" "POST" "/api/v1/events" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'X-User-Id: $USER_ID' -H 'Content-Type: application/json'" "$event_data" "201" "Create a new event"
    else
        echo -e "${RED}❌ No access token available for event creation tests${NC}"
    fi
    echo ""
    
    # Step 5: Timeline CRUD Tests
    echo -e "${CYAN}📅 Step 5: Timeline CRUD Tests${NC}"
    echo "==============================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Get timeline
        run_test "Get Timeline" "GET" "/api/v1/timeline/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event timeline"
        
        # Get timeline summary
        run_test "Get Timeline Summary" "GET" "/api/v1/timeline/$EVENT_ID/summary" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get timeline summary"
        
        # Create timeline item
        local timeline_item_data='{
            "eventId": "'$EVENT_ID'",
            "title": "Ceremony Setup",
            "description": "Setup ceremony area with decorations",
            "scheduledAt": "2024-12-25T16:00:00",
            "durationMinutes": 60,
            "itemType": "SETUP",
            "priority": "HIGH",
            "location": "Main Hall",
            "assignedTo": "'$USER_ID'",
            "setupTimeMinutes": 30,
            "teardownTimeMinutes": 15,
            "resourcesRequired": "Decorations, flowers, chairs",
            "notes": "Ensure all decorations are in place"
        }'
        
        run_test "Create Timeline Item" "POST" "/api/v1/timeline" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$timeline_item_data" "200" "Create a new timeline item"
        
        # Create bulk timeline items
        local bulk_timeline_data='{
            "items": [
                {
                    "eventId": "'$EVENT_ID'",
                    "title": "Guest Arrival",
                    "description": "Welcome guests and check-in",
                    "scheduledAt": "2024-12-25T17:30:00",
                    "durationMinutes": 30,
                    "itemType": "REGISTRATION",
                    "priority": "HIGH",
                    "location": "Entrance"
                },
                {
                    "eventId": "'$EVENT_ID'",
                    "title": "Wedding Ceremony",
                    "description": "Main wedding ceremony",
                    "scheduledAt": "2024-12-25T18:00:00",
                    "durationMinutes": 45,
                    "itemType": "PRESENTATION",
                    "priority": "CRITICAL",
                    "location": "Main Hall"
                }
            ]
        }'
        
        run_test "Create Bulk Timeline Items" "POST" "/api/v1/timeline/bulk" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_timeline_data" "200" "Create multiple timeline items"
    else
        echo -e "${RED}❌ No access token or event ID available for timeline tests${NC}"
    fi
    echo ""
    
    # Step 6: Timeline Status Management Tests
    echo -e "${CYAN}📊 Step 6: Timeline Status Management Tests${NC}"
    echo "============================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$TIMELINE_ITEM_ID" ]; then
        # Update timeline item status
        run_test "Update Timeline Item Status" "PUT" "/api/v1/timeline/$TIMELINE_ITEM_ID/status?status=IN_PROGRESS" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Update timeline item status"
        
        # Mark timeline item as complete
        run_test "Mark Timeline Item Complete" "PUT" "/api/v1/timeline/$TIMELINE_ITEM_ID/complete" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Mark timeline item as complete"
        
        # Assign timeline item
        run_test "Assign Timeline Item" "PUT" "/api/v1/timeline/$TIMELINE_ITEM_ID/assign?assignedTo=$USER_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Assign timeline item to user"
    else
        echo -e "${RED}❌ No access token or timeline item ID available for status tests${NC}"
    fi
    echo ""
    
    # Step 7: Workback Scheduling Tests
    echo -e "${CYAN}📋 Step 7: Workback Scheduling Tests${NC}"
    echo "====================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        local workback_data='{
            "eventDate": "2024-12-25",
            "eventType": "WEDDING"
        }'
        
        run_test "Generate Workback Schedule" "POST" "/api/v1/timeline/$EVENT_ID/workback" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$workback_data" "200" "Generate workback schedule"
        
        run_test "Apply Workback Schedule" "POST" "/api/v1/timeline/$EVENT_ID/workback/apply" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$workback_data" "200" "Apply workback schedule to timeline"
    else
        echo -e "${RED}❌ No access token or event ID available for workback tests${NC}"
    fi
    echo ""
    
    # Step 8: Filtering and Search Tests
    echo -e "${CYAN}🔍 Step 8: Filtering and Search Tests${NC}"
    echo "====================================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
        # Filter timeline items
        run_test "Filter Timeline Items" "GET" "/api/v1/timeline/$EVENT_ID/filter?status=PENDING&priority=HIGH" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Filter timeline items by status and priority"
        
        # Get upcoming items
        run_test "Get Upcoming Items" "GET" "/api/v1/timeline/$EVENT_ID/upcoming" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get upcoming timeline items"
        
        # Get overdue items
        run_test "Get Overdue Items" "GET" "/api/v1/timeline/$EVENT_ID/overdue" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get overdue timeline items"
    else
        echo -e "${RED}❌ No access token or event ID available for filtering tests${NC}"
    fi
    echo ""
    
    # Step 9: Dependencies Tests
    echo -e "${CYAN}🔗 Step 9: Dependencies Tests${NC}"
    echo "============================="
    
    if [ -n "$ACCESS_TOKEN" ] && [ -n "$TIMELINE_ITEM_ID" ]; then
        # Get dependencies
        run_test "Get Dependencies" "GET" "/api/v1/timeline/$TIMELINE_ITEM_ID/dependencies" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get timeline item dependencies"
        
        # Add dependency (using a dummy ID for testing)
        run_test "Add Dependency" "POST" "/api/v1/timeline/$TIMELINE_ITEM_ID/dependencies?dependencyId=00000000-0000-0000-0000-000000000000" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Add dependency to timeline item"
        
        # Remove dependency
        run_test "Remove Dependency" "DELETE" "/api/v1/timeline/$TIMELINE_ITEM_ID/dependencies/00000000-0000-0000-0000-000000000000" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Remove dependency from timeline item"
    else
        echo -e "${RED}❌ No access token or timeline item ID available for dependency tests${NC}"
    fi
    echo ""
    
    # Step 10: Templates Tests
    echo -e "${CYAN}📄 Step 10: Templates Tests${NC}"
    echo "============================="
    
    if [ -n "$ACCESS_TOKEN" ]; then
        # Get timeline templates
        run_test "Get Timeline Templates" "GET" "/api/v1/timeline/templates?eventType=WEDDING" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get timeline templates for wedding events"
        
        if [ -n "$EVENT_ID" ]; then
            # Apply timeline template
            run_test "Apply Timeline Template" "POST" "/api/v1/timeline/$EVENT_ID/apply-template?templateName=Traditional Wedding" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Apply timeline template to event"
        fi
    else
        echo -e "${RED}❌ No access token available for template tests${NC}"
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
| Timeline CRUD | 4 | 0 | 0 | 0% |
| Timeline Status | 3 | 0 | 0 | 0% |
| Workback Scheduling | 2 | 0 | 0 | 0% |
| Filtering & Search | 3 | 0 | 0 | 0% |
| Dependencies | 3 | 0 | 0 | 0% |
| Templates | 2 | 0 | 0 | 0% |

### Recommendations

EOF
    
    if [ $success_rate -ge 90 ]; then
        cat >> "$REPORT_FILE" << EOF
✅ **Excellent!** All timeline tests are passing successfully. The timeline system is working correctly.
EOF
    elif [ $success_rate -ge 70 ]; then
        cat >> "$REPORT_FILE" << EOF
⚠️ **Good** - Most timeline tests are passing, but there are some issues that need attention.
EOF
    else
        cat >> "$REPORT_FILE" << EOF
❌ **Needs Attention** - Multiple test failures indicate significant issues with the timeline system.
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

**Report generated by:** Timeline Controller Test Script
**Script version:** 1.0
EOF
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All timeline tests passed! Timeline system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some timeline tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
