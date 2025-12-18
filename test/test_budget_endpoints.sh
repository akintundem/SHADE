#!/bin/bash

# Comprehensive Budget Controller Endpoints Test Script
# Tests all budget-related endpoints and generates a detailed report
# Supports both local and production testing with custom API URLs
#
# Usage:
#   ./test_budget_endpoints.sh                    # Interactive mode
#   ./test_budget_endpoints.sh local              # Test localhost:8080
#   ./test_budget_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_budget_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "💰 Budget Controller Endpoints Test Script"
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
    echo ""
    exit 0
}

# Check for help argument
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "💰 Starting Comprehensive Budget Controller Endpoints Test"
echo "============================================================"

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
                ;;
            2)
                echo ""
                echo -e "${CYAN}🌐 Enter Production API URL:${NC}"
                read -p "API URL: " custom_url
                
                if [[ $custom_url =~ ^https?:// ]]; then
                    BASE_URL="$custom_url"
                    echo -e "${GREEN}✅ Selected: Production - $BASE_URL${NC}"
                else
                    echo -e "${RED}❌ Invalid URL format${NC}"
                    exit 1
                fi
                ;;
            *)
                echo -e "${RED}❌ Invalid choice${NC}"
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
REPORT_FILE="test/reports/budget_test_report_$(date +%Y%m%d_%H%M%S).md"
TEST_USER_EMAIL="budgettest@example.com"
TEST_USER_PASSWORD="Password123!"
TEST_USER_NAME="Budget Test User"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and test data
ACCESS_TOKEN=""
DEVICE_ID=""
USER_ID=""
EVENT_ID=""
CATEGORY_ID=""
LINE_ITEM_ID=""

# Create report file
mkdir -p test/reports
cat > "$REPORT_FILE" << EOF
# Budget Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Budget Management | 0 | 0 | 0 | 0% |
| Category List | 0 | 0 | 0 | 0% |
| Line Item Flow | 0 | 0 | 0 | 0% |
| Validation Tests | 0 | 0 | 0 | 0% |
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
    if [[ "$http_code" == "$expected_status" ]]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        if [ "$http_code" = "403" ] || [ "$http_code" = "401" ] || [ "$http_code" = "400" ]; then
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
    
    # Extract IDs from successful responses
    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        case "$test_name" in
            "User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                DEVICE_ID=$(echo "$response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Get Categories")
                CATEGORY_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
            "Auto-save New Line Item")
                LINE_ITEM_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
                ;;
        esac
    fi
    
    echo ""
}

# Function to wait for service to be ready
wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        else
            echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting...${NC}"
        fi
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ Service failed to respond${NC}"
    return 1
}

# Function to authenticate user
authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"
    
    # Register user
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "'$TEST_USER_NAME'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "phoneNumber": "+1234567890",
        "dateOfBirth": "1990-01-01",
        "acceptTerms": true,
        "acceptPrivacy": true,
        "marketingOptIn": false
    }'
    
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$registration_data" \
        "$BASE_URL/api/v1/auth/register" > /dev/null 2>&1
    
    # Verify email if Docker is available
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${TEST_USER_EMAIL}');" >/dev/null 2>&1
    fi
    
    # Login
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
        DEVICE_ID=$(echo "$login_response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$ACCESS_TOKEN" ]; then
            local jwt_payload=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
            if command -v python3 &> /dev/null; then
                USER_ID=$(echo "$jwt_payload" | python3 -c "import sys, base64, json; data=sys.stdin.read().strip(); padding=4-len(data)%4; data+=('='*padding if padding<4 else ''); print(json.loads(base64.b64decode(data)).get('sub', ''))" 2>/dev/null)
            fi
        fi
        
        echo -e "${GREEN}✅ User authenticated successfully${NC}"
        return 0
    fi
    
    echo -e "${RED}❌ Failed to authenticate user${NC}"
    return 1
}

# Function to create a test event
create_test_event() {
    echo -e "${YELLOW}📅 Creating test event...${NC}"
    
    local start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S' 2>/dev/null || date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%S')
    local end_date=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S' 2>/dev/null || date -u -d '+1 day +2 hours' '+%Y-%m-%dT%H:%M:%S')
    
    local event_data='{
        "event": {
            "name": "Test Event for Budget",
            "description": "Budget testing event",
            "eventType": "CONFERENCE",
            "startDateTime": "'$start_date'",
            "endDateTime": "'$end_date'",
            "capacity": 100,
            "isPublic": true,
            "requiresApproval": false
        },
        "coverUpload": {
            "fileName": "test_cover.jpg",
            "contentType": "image/jpeg"
        }
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
        echo -e "${GREEN}✅ Test event created: $EVENT_ID${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to create test event - HTTP: $http_code${NC}"
        return 1
    fi
}

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event (auto-creates budget)"
    echo "4. Test budget retrieval and update"
    echo "5. Test category listing"
    echo "6. Test line item flow (Auto-save, Finalize, Delete)"
    echo "7. Test validation"
    echo "8. Clean up"
    echo ""
    
    # Step 1: Check service
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
    
    # Step 3: Create event
    echo -e "${CYAN}📅 Step 3: Create Test Event${NC}"
    echo "============================="
    if ! create_test_event; then
        echo -e "${RED}❌ Failed to create event. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 4: Budget Retrieval and Update
    echo -e "${CYAN}💰 Step 4: Budget Retrieval and Update${NC}"
    echo "======================================="
    
    # Get budget
    run_test "Get Budget" "GET" "/api/v1/events/$EVENT_ID/budget" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Retrieve the auto-created budget for the event"
    
    # Update budget
    local update_budget_data='{
        "totalBudget": 60000.00,
        "contingencyPercentage": 10.0,
        "currency": "USD",
        "notes": "Updated budget via test script"
    }'
    run_test "Update Budget" "PUT" "/api/v1/events/$EVENT_ID/budget" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_budget_data" "200" "Update budget details"
    echo ""
    
    # Step 5: Category Listing
    echo -e "${CYAN}📂 Step 5: Category Listing${NC}"
    echo "============================"
    
    run_test "Get Categories" "GET" "/api/v1/events/$EVENT_ID/budget/categories" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all auto-seeded categories"
    echo ""
    
    # Step 6: Line Item Flow
    echo -e "${CYAN}📝 Step 6: Line Item Flow${NC}"
    echo "=========================="
    
    # Auto-save new line item (Draft)
    if [ -n "$CATEGORY_ID" ]; then
        local autosave_new_data='{
            "budgetCategoryId": "'$CATEGORY_ID'",
            "description": "Draft item from test",
            "estimatedCost": 1000.00,
            "quantity": 1
        }'
        run_test "Auto-save New Line Item" "PATCH" "/api/v1/events/$EVENT_ID/budget/line-items/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$autosave_new_data" "200" "Create a new line item in draft mode"
    fi
    
    # Auto-save update existing line item
    if [ -n "$LINE_ITEM_ID" ]; then
        local autosave_update_data='{
            "id": "'$LINE_ITEM_ID'",
            "description": "Updated draft item",
            "estimatedCost": 1200.00,
            "actualCost": 1100.00
        }'
        run_test "Auto-save Update Line Item" "PATCH" "/api/v1/events/$EVENT_ID/budget/line-items/auto-save" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$autosave_update_data" "200" "Update an existing draft line item"
    fi
    
    # List line items
    run_test "Get All Line Items" "GET" "/api/v1/events/$EVENT_ID/budget/line-items" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all line items for the budget"
    
    # Finalize line item
    if [ -n "$LINE_ITEM_ID" ]; then
        local finalize_data='{
            "description": "Finalized item",
            "estimatedCost": 1200.00,
            "actualCost": 1150.00
        }'
        run_test "Finalize Line Item" "PUT" "/api/v1/events/$EVENT_ID/budget/line-items/$LINE_ITEM_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$finalize_data" "200" "Mark the line item as non-draft and update totals"
    fi
    
    # Finalize empty line item (Should result in deletion/204)
    # First create another draft
    local empty_draft_data='{
        "budgetCategoryId": "'$CATEGORY_ID'",
        "description": "To be emptied"
    }'
    local empty_draft_response=$(curl -s -X PATCH -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Device-ID: $DEVICE_ID" -H "Content-Type: application/json" -d "$empty_draft_data" "$BASE_URL/api/v1/events/$EVENT_ID/budget/line-items/auto-save")
    local TEMP_ITEM_ID=$(echo "$empty_draft_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -n "$TEMP_ITEM_ID" ]; then
        local empty_finalize_data='{
            "description": "",
            "estimatedCost": 0,
            "actualCost": 0
        }'
        run_test "Finalize Empty Item (Auto-delete)" "PUT" "/api/v1/events/$EVENT_ID/budget/line-items/$TEMP_ITEM_ID/finalize" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$empty_finalize_data" "204" "Finalizing an empty item should delete it"
    fi
    echo ""
    
    # Step 7: Validation Tests
    echo -e "${CYAN}🔒 Step 7: Validation Tests${NC}"
    echo "============================"
    
    # Test negative budget (should fail)
    local negative_budget='{
        "totalBudget": -1000.00
    }'
    run_test "Update Negative Budget (Should Fail)" "PUT" "/api/v1/events/$EVENT_ID/budget" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$negative_budget" "400" "Attempt to set negative budget"
    
    # Test invalid contingency percentage (should fail - max 50%)
    local invalid_contingency='{
        "totalBudget": 5000.00,
        "contingencyPercentage": 75.00
    }'
    run_test "Update with Invalid Contingency (Should Fail)" "PUT" "/api/v1/events/$EVENT_ID/budget" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invalid_contingency" "400" "Attempt to set contingency > 50%"
    
    # Test unauthenticated access (should fail)
    run_test "Get Budget Without Auth (Should Fail)" "GET" "/api/v1/events/$EVENT_ID/budget" "" "" "401" "Attempt to access budget without authentication"
    echo ""
    
    # Step 8: Clean Up
    echo -e "${CYAN}🧹 Step 8: Clean Up${NC}"
    echo "===================="
    
    # Delete line item
    if [ -n "$LINE_ITEM_ID" ]; then
        run_test "Delete Line Item" "DELETE" "/api/v1/events/$EVENT_ID/budget/line-items/$LINE_ITEM_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete the finalized line item"
    fi
    
    # Archive event (which would normally lead to budget deletion if we had it, but for now we just archive)
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
    
    # Update report
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
