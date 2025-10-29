#!/bin/bash

# Comprehensive Attendee Management Endpoints Test Script
# Tests all attendee management endpoints and generates a detailed report

echo "👥 Starting Comprehensive Attendee Management Endpoints Test"
echo "============================================================"

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
REPORT_FILE="reports/attendee_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="attendee.test@example.com"
TEST_USER_PASSWORD="TestPassword123!"
TEST_USER_NAME="Attendee Test User"
TEST_EVENT_TITLE="Test Event for Attendee Management"
TEST_EVENT_DESCRIPTION="Testing comprehensive attendee management features"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and user data
ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""
EVENT_ID=""
ATTENDANCE_ID=""

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Attendee Management Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Client ID:** $CLIENT_ID
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Authentication | 0 | 0 | 0 | 0% |
| Event Management | 0 | 0 | 0 | 0% |
| Attendee Registration | 0 | 0 | 0 | 0% |
| Check-in/Check-out | 0 | 0 | 0 | 0% |
| QR Code Management | 0 | 0 | 0 | 0% |
| Analytics | 0 | 0 | 0 | 0% |
| Communication | 0 | 0 | 0 | 0% |
| Export/Import | 0 | 0 | 0 | 0% |
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
    
    # Extract tokens and IDs from successful responses
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        case "$test_name" in
            "User Registration")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Create Test Event")
                EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Register for Event")
                ATTENDANCE_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
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
    echo "1. Stop and restart services"
    echo "2. Test health check endpoint"
    echo "3. Test user registration"
    echo "4. Test user login"
    echo "5. Test event creation"
    echo "6. Test attendee registration"
    echo "7. Test attendee management"
    echo "8. Test check-in/check-out"
    echo "9. Test QR code functionality"
    echo "10. Test analytics and reporting"
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
    run_test "Health Check" "GET" "/actuator/health" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "" "200" "Check if application is healthy"
    echo ""
    
    # Step 3: Authentication Tests
    echo -e "${CYAN}🔐 Step 3: Authentication Tests${NC}"
    echo "================================="
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "'$TEST_USER_NAME'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "acceptTerms": true,
        "acceptPrivacy": true
    }'
    
    run_test "User Registration" "POST" "/api/v1/auth/register" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$registration_data" "201" "Register a new user for attendee testing"
    
    local login_data='{
        "email": "'$TEST_USER_EMAIL'",
        "password": "'$TEST_USER_PASSWORD'"
    }'
    
    run_test "User Login" "POST" "/api/v1/auth/login" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$login_data" "200" "Login with valid credentials"
    echo ""
    
    # Step 4: Event Creation Tests
    echo -e "${CYAN}🎪 Step 4: Event Creation Tests${NC}"
    echo "================================="
    local event_data='{
        "name": "'$TEST_EVENT_TITLE'",
        "description": "'$TEST_EVENT_DESCRIPTION'",
        "startDateTime": "2025-12-01T10:00:00",
        "endDateTime": "2025-12-01T18:00:00",
        "capacity": 100,
        "eventType": "CONFERENCE"
    }'
    
    run_test "Create Test Event" "POST" "/api/v1/events" "-H 'X-Client-ID: $CLIENT_ID' -H 'X-User-Id: $USER_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$event_data" "201" "Create a test event for attendee management"
    echo ""
    
    # Step 5: Attendee Registration Tests
    echo -e "${CYAN}👥 Step 5: Attendee Registration Tests${NC}"
    echo "======================================="
    local attendance_data='{
        "eventId": "'$EVENT_ID'",
        "userId": "'$USER_ID'",
        "name": "'$TEST_USER_NAME'",
        "email": "'$TEST_USER_EMAIL'",
        "phone": "+1234567890",
        "attendanceStatus": "REGISTERED",
        "dietaryRestrictions": "Vegetarian",
        "accessibilityNeeds": "Wheelchair access",
        "emergencyContact": "Emergency Contact",
        "emergencyPhone": "+1987654321",
        "notes": "Test attendee registration"
    }'
    
    run_test "Register for Event" "POST" "/api/v1/events/$EVENT_ID/attendances" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$attendance_data" "201" "Register attendee for the event"
    
    run_test "Get All Attendances" "GET" "/api/v1/events/$EVENT_ID/attendances" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all attendances for the event"
    
    run_test "Get Specific Attendance" "GET" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific attendance details"
    echo ""
    
    # Step 6: Attendee Management Tests
    echo -e "${CYAN}📝 Step 6: Attendee Management Tests${NC}"
    echo "===================================="
    local update_data='{
        "name": "Updated Attendee Name",
        "email": "updated.attendee@example.com",
        "phone": "+1234567891",
        "attendanceStatus": "CONFIRMED",
        "dietaryRestrictions": "Vegan",
        "accessibilityNeeds": "Sign language interpreter",
        "emergencyContact": "Updated Emergency Contact",
        "emergencyPhone": "+1987654322",
        "notes": "Updated attendee information"
    }'
    
    run_test "Update Attendance" "PUT" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_data" "200" "Update attendee information"
    echo ""
    
    # Step 7: Check-in/Check-out Tests
    echo -e "${CYAN}✅ Step 7: Check-in/Check-out Tests${NC}"
    echo "===================================="
    local checkin_data='{
        "qrCode": "QR_TEST_CODE",
        "notes": "Checked in via test"
    }'
    
    run_test "Check In Attendee" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/check-in" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$checkin_data" "200" "Check in attendee"
    
    run_test "Get Checked-in Attendees" "GET" "/api/v1/events/$EVENT_ID/attendances/checked-in" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get list of checked-in attendees"
    
    run_test "Check Out Attendee" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/check-out" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Check out attendee"
    echo ""
    
    # Step 8: QR Code Tests
    echo -e "${CYAN}📱 Step 8: QR Code Tests${NC}"
    echo "========================="
    run_test "Get Attendee QR Code" "GET" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/qr-code" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendee QR code"
    
    run_test "Scan QR Code" "POST" "/api/v1/events/$EVENT_ID/attendances/scan-qr?qrCode=QR_TEST_CODE" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Scan QR code for check-in"
    
    run_test "Regenerate QR Code" "POST" "/api/v1/events/$EVENT_ID/attendances/$ATTENDANCE_ID/regenerate-qr" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Regenerate attendee QR code"
    echo ""
    
    # Step 9: Analytics Tests
    echo -e "${CYAN}📊 Step 9: Analytics Tests${NC}"
    echo "=========================="
    run_test "Get Attendance Statistics" "GET" "/api/v1/events/$EVENT_ID/attendances/attendance-stats" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance statistics"
    
    run_test "Get Attendance Analytics" "GET" "/api/v1/events/$EVENT_ID/analytics/attendance" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get attendance analytics"
    
    run_test "Get Check-in Timeline" "GET" "/api/v1/events/$EVENT_ID/analytics/check-in-timeline" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get check-in timeline"
    echo ""
    
    # Step 10: Export/Import Tests
    echo -e "${CYAN}📤 Step 10: Export/Import Tests${NC}"
    echo "================================="
    run_test "Export Attendees to CSV" "GET" "/api/v1/events/$EVENT_ID/attendances/export/csv" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Export attendees to CSV"
    
    run_test "Export Attendees to Excel" "GET" "/api/v1/events/$EVENT_ID/attendances/export/excel" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Export attendees to Excel"
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
| Event Management | 1 | 0 | 0 | 0% |
| Attendee Registration | 3 | 0 | 0 | 0% |
| Check-in/Check-out | 3 | 0 | 0 | 0% |
| QR Code Management | 3 | 0 | 0 | 0% |
| Analytics | 3 | 0 | 0 | 0% |
| Export/Import | 2 | 0 | 0 | 0% |

### Recommendations

EOF
    
    if [ $success_rate -ge 90 ]; then
        cat >> "$REPORT_FILE" << EOF
✅ **Excellent!** All attendee management tests are passing successfully. The system is working correctly.
EOF
    elif [ $success_rate -ge 70 ]; then
        cat >> "$REPORT_FILE" << EOF
⚠️ **Good** - Most tests are passing, but there are some issues that need attention.
EOF
    else
        cat >> "$REPORT_FILE" << EOF
❌ **Needs Attention** - Multiple test failures indicate significant issues with the attendee management system.
EOF
    fi
    
    cat >> "$REPORT_FILE" << EOF

### Next Steps

1. Review failed tests and fix underlying issues
2. Check server logs for detailed error information
3. Verify database connectivity and data integrity
4. Test with different attendee scenarios
5. Consider adding more edge case tests

---

**Report generated by:** Attendee Management Test Script
**Script version:** 1.0
EOF
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Attendee management system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"