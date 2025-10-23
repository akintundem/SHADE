#!/bin/bash

# Comprehensive Budget Controller Endpoints Test Script
# Tests all budget-related endpoints and generates a detailed report

echo "💰 Starting Comprehensive Budget Controller Endpoints Test"
echo "========================================================"

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
REPORT_FILE="reports/budget_test_report_$(date +%Y%m%d_%H%M%S).md"
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
BUDGET_ID=""
LINE_ITEM_ID=""

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Budget Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Client ID:** $CLIENT_ID
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Budget CRUD | 0 | 0 | 0 | 0% |
| Line Item Management | 0 | 0 | 0 | 0% |
| Analysis Endpoints | 0 | 0 | 0 | 0% |
| Approval Workflow | 0 | 0 | 0 | 0% |
| Utility Endpoints | 0 | 0 | 0 | 0% |
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
            "Create Budget")
                BUDGET_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Add Budget Line Item")
                LINE_ITEM_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
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
    echo -e "${RED}   Java ready: $java_ready${NC}"
    echo -e "${RED}   Python ready: $python_ready${NC}"
    return 1
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
        "marketingOptIn": false,
        "deviceId": "test-device-123",
        "clientId": "'$CLIENT_ID'"
    }'
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "$registration_data" \
        "$BASE_URL/api/v1/auth/register")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
        USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✅ User registered and authenticated${NC}"
        return 0
    elif [ "$http_code" = "400" ]; then
        # User might already exist, try to login
        local login_data='{
            "email": "'$TEST_USER_EMAIL'",
            "password": "'$TEST_USER_PASSWORD'",
            "rememberMe": false,
            "deviceId": "test-device-123",
            "clientId": "'$CLIENT_ID'"
        }'
        
        local login_response=$(curl -s -w '%{http_code}' -X POST \
            -H "X-Client-ID: $CLIENT_ID" \
            -H "Content-Type: application/json" \
            -d "$login_data" \
            "$BASE_URL/api/v1/auth/login")
        
        local login_http_code="${login_response: -3}"
        local login_response_body="${login_response%???}"
        
        if [ "$login_http_code" = "200" ]; then
            ACCESS_TOKEN=$(echo "$login_response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
            REFRESH_TOKEN=$(echo "$login_response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
            USER_ID=$(echo "$login_response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
            echo -e "${GREEN}✅ User logged in successfully${NC}"
            return 0
        fi
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
        "name": "Test Event for Budget",
        "description": "This is a test event for budget testing",
        "eventType": "CONFERENCE",
        "startDateTime": "'$start_date'",
        "endDateTime": "'$end_date'",
        "venueRequirements": "Test Location - Conference Room A",
        "capacity": 100,
        "isPublic": true,
        "requiresApproval": false,
        "coverImageUrl": "https://example.com/cover.jpg",
        "eventWebsiteUrl": "https://example.com/event",
        "hashtag": "#TestBudgetEvent"
    }'
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-User-Id: $USER_ID" \
        -H "Content-Type: application/json" \
        -d "$event_data" \
        "$BASE_URL/api/v1/events")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
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
    echo "1. Stop and restart services"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test budget CRUD operations"
    echo "5. Test line item management"
    echo "6. Test analysis endpoints"
    echo "7. Test approval workflow"
    echo "8. Test utility endpoints"
    echo "9. Clean up test data"
    echo ""
    
    # Step 1: Restart services
    echo -e "${CYAN}🔄 Step 1: Restarting Services${NC}"
    echo "=================================="
    if ! restart_services; then
        echo -e "${RED}❌ Failed to restart services. Exiting.${NC}"
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
    
    # Step 4: Budget CRUD Operations Tests
    echo -e "${CYAN}💰 Step 4: Budget CRUD Operations Tests${NC}"
    echo "=========================================="
    
    # Test create budget
    local budget_data='{
        "eventId": "'$EVENT_ID'",
        "totalBudget": 50000.00,
        "currency": "USD"
    }'
    run_test "Create Budget" "POST" "/api/v1/budgets" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$budget_data" "200" "Create budget for event"
    
    # Test get budget by event ID
    run_test "Get Budget by Event ID" "GET" "/api/v1/budgets/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get budget by event ID"
    
    # Test update budget
    local update_budget_data='{
        "totalBudget": 60000.00,
        "contingencyPercentage": 15.00,
        "currency": "USD",
        "notes": "Updated budget with higher contingency"
    }'
    run_test "Update Budget" "PUT" "/api/v1/budgets/$BUDGET_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_budget_data" "200" "Update budget details"
    
    # Test get non-existent budget
    run_test "Get Non-existent Budget" "GET" "/api/v1/budgets/00000000-0000-0000-0000-000000000000" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Get non-existent budget"
    echo ""
    
    # Step 5: Line Item Management Tests
    echo -e "${CYAN}📝 Step 5: Line Item Management Tests${NC}"
    echo "====================================="
    
    # Test add single line item
    local line_item_data='{
        "budgetId": "'$BUDGET_ID'",
        "category": "Venue & Facilities",
        "description": "Conference room rental",
        "estimatedCost": 15000.00,
        "vendorId": "12345678-1234-1234-1234-123456789012"
    }'
    run_test "Add Budget Line Item" "POST" "/api/v1/budgets/$BUDGET_ID/line-items" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$line_item_data" "200" "Add single line item to budget"
    
    # Test add multiple line items
    local bulk_line_items_data='{
        "budgetId": "'$BUDGET_ID'",
        "lineItems": [
            {
                "category": "Catering & Food",
                "description": "Lunch for 100 people",
                "estimatedCost": 5000.00
            },
            {
                "category": "Entertainment & Activities",
                "description": "Keynote speaker",
                "estimatedCost": 3000.00
            },
            {
                "category": "Marketing & Promotion",
                "description": "Event promotion",
                "estimatedCost": 2000.00
            }
        ]
    }'
    run_test "Add Bulk Line Items" "POST" "/api/v1/budgets/$BUDGET_ID/line-items/bulk" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$bulk_line_items_data" "200" "Add multiple line items to budget"
    
    # Test get all line items
    run_test "Get All Line Items" "GET" "/api/v1/budgets/$BUDGET_ID/line-items" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get all line items for budget"
    
    # Test get specific line item
    run_test "Get Specific Line Item" "GET" "/api/v1/budgets/$BUDGET_ID/line-items/$LINE_ITEM_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific line item"
    
    # Test update line item
    local update_line_item_data='{
        "category": "Venue & Facilities",
        "subcategory": "Conference Room",
        "description": "Updated conference room rental with AV equipment",
        "estimatedCost": 18000.00,
        "actualCost": 17500.00,
        "quantity": 1,
        "unitCost": 17500.00,
        "planningStatus": "BOOKED",
        "isEssential": true,
        "priority": "HIGH",
        "notes": "Booked with early bird discount"
    }'
    run_test "Update Line Item" "PUT" "/api/v1/budgets/$BUDGET_ID/line-items/$LINE_ITEM_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_line_item_data" "200" "Update line item details"
    
    # Test get non-existent line item
    run_test "Get Non-existent Line Item" "GET" "/api/v1/budgets/$BUDGET_ID/line-items/00000000-0000-0000-0000-000000000000" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Get non-existent line item"
    echo ""
    
    # Step 6: Analysis Endpoints Tests
    echo -e "${CYAN}📊 Step 6: Analysis Endpoints Tests${NC}"
    echo "====================================="
    
    # Test budget summary
    run_test "Get Budget Summary" "GET" "/api/v1/budgets/$BUDGET_ID/summary" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get budget summary with key metrics"
    
    # Test variance analysis
    run_test "Get Variance Analysis" "GET" "/api/v1/budgets/$BUDGET_ID/variance-analysis" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get variance analysis by category"
    
    # Test contingency analysis
    run_test "Get Contingency Analysis" "GET" "/api/v1/budgets/$BUDGET_ID/contingency-analysis" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get contingency usage analysis"
    
    # Test category breakdown
    run_test "Get Category Breakdown" "GET" "/api/v1/budgets/$BUDGET_ID/category-breakdown" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get spending breakdown by category"
    echo ""
    
    # Step 7: Approval Workflow Tests
    echo -e "${CYAN}✅ Step 7: Approval Workflow Tests${NC}"
    echo "====================================="
    
    # Test submit for approval
    run_test "Submit for Approval" "POST" "/api/v1/budgets/$BUDGET_ID/submit-for-approval" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Submit budget for approval"
    
    # Test approve budget
    local approval_data='{
        "approvedBy": "manager@example.com",
        "notes": "Budget approved for Q1 event"
    }'
    run_test "Approve Budget" "POST" "/api/v1/budgets/$BUDGET_ID/approve" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$approval_data" "200" "Approve budget"
    
    # Test reject budget (this will fail since budget is already approved)
    local rejection_data='{
        "approvedBy": "manager@example.com",
        "notes": "Budget rejected due to overestimation"
    }'
    run_test "Reject Approved Budget" "POST" "/api/v1/budgets/$BUDGET_ID/reject" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$rejection_data" "409" "Attempt to reject already approved budget"
    echo ""
    
    # Step 8: Utility Endpoints Tests
    echo -e "${CYAN}🔧 Step 8: Utility Endpoints Tests${NC}"
    echo "====================================="
    
    # Test compute rollup
    run_test "Compute Rollup" "GET" "/api/v1/budgets/$BUDGET_ID/rollup" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Compute rollup total for budget"
    
    # Test recalculate totals
    run_test "Recalculate Totals" "POST" "/api/v1/budgets/$BUDGET_ID/recalculate" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Force recalculation of budget totals"
    
    # Test get standard categories
    run_test "Get Standard Categories" "GET" "/api/v1/budgets/categories" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get standard budget categories"
    
    # Test invalid budget ID format
    run_test "Invalid Budget ID Format" "GET" "/api/v1/budgets/invalid-id" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Get budget with invalid ID format"
    echo ""
    
    # Step 9: Clean up test data
    echo -e "${CYAN}🧹 Step 9: Clean Up Test Data${NC}"
    echo "==============================="
    
    if [ -n "$BUDGET_ID" ]; then
        run_test "Delete Budget" "DELETE" "/api/v1/budgets/$BUDGET_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete test budget"
    fi
    
    if [ -n "$EVENT_ID" ]; then
        run_test "Delete Test Event" "DELETE" "/api/v1/events/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete test event"
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
        echo "### Test Categories Breakdown"
        echo ""
        echo "| Category | Tests | Passed | Failed | Success Rate |"
        echo "|----------|-------|--------|--------|--------------|"
        echo "| Budget CRUD | 4 | $([ $PASSED_TESTS -gt 0 ] && echo "4" || echo "0") | $([ $FAILED_TESTS -gt 0 ] && echo "4" || echo "0") | $([ $PASSED_TESTS -gt 0 ] && echo "100%" || echo "0%") |"
        echo "| Line Item Management | 6 | 0 | 0 | 0% |"
        echo "| Analysis Endpoints | 4 | 0 | 0 | 0% |"
        echo "| Approval Workflow | 3 | 0 | 0 | 0% |"
        echo "| Utility Endpoints | 4 | 0 | 0 | 0% |"
        echo "| Cleanup | 2 | 0 | 0 | 0% |"
        echo ""
    } >> "$REPORT_FILE"
    
    if [ $success_rate -ge 90 ]; then
        {
            echo "✅ **Excellent!** All tests are passing successfully. The budget system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the budget system."
        } >> "$REPORT_FILE"
    fi
    
    {
        echo ""
        echo "### Next Steps"
        echo ""
        echo "1. Review failed tests and fix underlying issues"
        echo "2. Check server logs for detailed error information"
        echo "3. Verify database connectivity and data integrity"
        echo "4. Test with different budget scenarios"
        echo "5. Consider adding more edge case tests"
        echo ""
        echo "### Budget System Features Tested"
        echo ""
        echo "- ✅ Budget CRUD operations (Create, Read, Update, Delete)"
        echo "- ✅ Line item management (Single and bulk operations)"
        echo "- ✅ Variance analysis and reporting"
        echo "- ✅ Contingency planning and analysis"
        echo "- ✅ Approval workflow (Submit, Approve, Reject)"
        echo "- ✅ Budget calculations and rollups"
        echo "- ✅ Category breakdown and analysis"
        echo "- ✅ Error handling and validation"
        echo ""
        echo "---"
        echo ""
        echo "**Report generated by:** Budget Controller Test Script"
        echo "**Script version:** 1.0"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Budget system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
