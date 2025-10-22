#!/bin/bash

echo "🚀 COMPREHENSIVE EVENT PLANNER TEST SUITE"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080"
API_URL="$BASE_URL/api/v1"
PYTHON_URL="http://localhost:8000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test counter
TEST_NUM=1

# Function to print test header
print_test_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}TEST $TEST_NUM: $1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    ((TEST_NUM++))
}

# Function to print request details
print_request() {
    echo -e "${YELLOW}📤 REQUEST:${NC}"
    echo -e "${YELLOW}Method: $1${NC}"
    echo -e "${YELLOW}URL: $2${NC}"
    echo -e "${YELLOW}Headers:${NC}"
    echo "$3" | sed 's/^/  /'
    if [ ! -z "$4" ]; then
        echo -e "${YELLOW}Body:${NC}"
        echo "$4" | jq . 2>/dev/null || echo "$4"
    fi
    echo ""
}

# Function to print response details
print_response() {
    echo -e "${GREEN}📥 RESPONSE:${NC}"
    echo -e "${GREEN}Status: $1${NC}"
    echo -e "${GREEN}Headers:${NC}"
    echo "$2" | sed 's/^/  /'
    echo -e "${GREEN}Body:${NC}"
    echo "$3" | jq . 2>/dev/null || echo "$3"
    echo ""
}

# Function to make request and capture details
make_request() {
    local method=$1
    local url=$2
    local headers=$3
    local data=$4
    local description=$5
    
    print_request "$method" "$url" "$headers" "$data"
    
    # Make the request and capture response
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}\n%{header_json}" -X "$method" "$url" $headers)
    else
        response=$(curl -s -w "\n%{http_code}\n%{header_json}" -X "$method" "$url" $headers -d "$data")
    fi
    
    # Extract status code, headers, and body
    status_code=$(echo "$response" | tail -n 2 | head -n 1)
    headers=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -2)
    
    print_response "$status_code" "$headers" "$body"
    
    # Return status code for further processing
    echo "$status_code"
}

# =============================================================================
# 1. AUTHENTICATION TESTS
# =============================================================================

print_test_header "AUTHENTICATION TESTS"

echo -e "${CYAN}1.1 Testing Health Endpoint (No Auth Required)${NC}"
status=$(make_request "GET" "$API_URL/auth/health" "-H 'X-Client-ID: web-app'" "" "Health check with client ID")

echo -e "${CYAN}1.2 Testing Health Endpoint (No Client ID - Should Fail)${NC}"
status=$(make_request "GET" "$API_URL/auth/health" "" "" "Health check without client ID")

echo -e "${CYAN}1.3 Testing User Registration${NC}"
register_data='{
    "email": "testuser@example.com",
    "name": "Test User",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "acceptTerms": true,
    "acceptPrivacy": true,
    "clientId": "web-app",
    "deviceId": "test-device-001"
}'
status=$(make_request "POST" "$API_URL/auth/register" "-H 'X-Client-ID: web-app' -H 'Content-Type: application/json'" "$register_data" "User registration")

# Extract access token if registration successful
if [ "$status" = "201" ] || [ "$status" = "200" ]; then
    ACCESS_TOKEN=$(echo "$body" | jq -r '.accessToken' 2>/dev/null)
    echo -e "${GREEN}✅ Registration successful! Token: ${ACCESS_TOKEN:0:20}...${NC}"
else
    echo -e "${RED}❌ Registration failed!${NC}"
    ACCESS_TOKEN=""
fi

echo -e "${CYAN}1.4 Testing User Login${NC}"
login_data='{
    "email": "testuser@example.com",
    "password": "SecurePass123!",
    "clientId": "web-app",
    "deviceId": "test-device-001"
}'
status=$(make_request "POST" "$API_URL/auth/login" "-H 'X-Client-ID: web-app' -H 'Content-Type: application/json'" "$login_data" "User login")

if [ "$status" = "200" ]; then
    ACCESS_TOKEN=$(echo "$body" | jq -r '.accessToken' 2>/dev/null)
    echo -e "${GREEN}✅ Login successful! Token: ${ACCESS_TOKEN:0:20}...${NC}"
fi

echo -e "${CYAN}1.5 Testing Get Current User (Authenticated)${NC}"
if [ ! -z "$ACCESS_TOKEN" ]; then
    status=$(make_request "GET" "$API_URL/auth/me" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Get current user")
else
    echo -e "${RED}❌ No access token available for authenticated test${NC}"
fi

# =============================================================================
# 2. SECURITY TESTS
# =============================================================================

print_test_header "SECURITY TESTS"

echo -e "${CYAN}2.1 Testing Invalid Client ID${NC}"
status=$(make_request "GET" "$API_URL/auth/me" "-H 'X-Client-ID: invalid-client' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Invalid client ID")

echo -e "${CYAN}2.2 Testing Missing Client ID${NC}"
status=$(make_request "GET" "$API_URL/auth/me" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Missing client ID")

echo -e "${CYAN}2.3 Testing Invalid JWT Token${NC}"
status=$(make_request "GET" "$API_URL/auth/me" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer invalid-token'" "" "Invalid JWT token")

echo -e "${CYAN}2.4 Testing Python Service Direct Access (Should Fail)${NC}"
status=$(make_request "GET" "$PYTHON_URL/health" "" "" "Direct Python service access")

echo -e "${CYAN}2.5 Testing Python Service Chat (Should Fail)${NC}"
chat_data='{
    "message": "Hello, test message",
    "sessionId": "test-session-001"
}'
status=$(make_request "POST" "$PYTHON_URL/chat" "-H 'Content-Type: application/json'" "$chat_data" "Direct Python chat access")

echo -e "${CYAN}2.6 Testing Rate Limiting${NC}"
echo "Making multiple requests to test rate limiting..."
for i in {1..6}; do
    echo -e "${YELLOW}Request $i:${NC}"
    status=$(make_request "GET" "$API_URL/auth/me" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Rate limit test $i")
    if [ "$status" = "429" ]; then
        echo -e "${RED}✅ Rate limiting working! Request $i was blocked.${NC}"
        break
    fi
    sleep 1
done

# =============================================================================
# 3. EVENT CRUD TESTS
# =============================================================================

print_test_header "EVENT CRUD TESTS"

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ No access token available for event tests. Skipping...${NC}"
else
    echo -e "${CYAN}3.1 Testing Create Event${NC}"
    create_event_data='{
        "name": "Test Conference 2024",
        "description": "A comprehensive test conference for our Event Planner system",
        "eventType": "CONFERENCE",
        "eventStatus": "PLANNING",
        "startDateTime": "2024-12-15T09:00:00",
        "endDateTime": "2024-12-15T17:00:00",
        "registrationDeadline": "2024-12-10T23:59:59",
        "capacity": 200,
        "isPublic": true,
        "requiresApproval": false,
        "qrCodeEnabled": true,
        "venueRequirements": "Large conference hall with AV equipment",
        "technicalRequirements": "WiFi, projectors, microphones"
    }'
    status=$(make_request "POST" "$API_URL/events" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$create_event_data" "Create event")
    
    # Extract event ID if creation successful
    if [ "$status" = "201" ] || [ "$status" = "200" ]; then
        EVENT_ID=$(echo "$body" | jq -r '.id' 2>/dev/null)
        echo -e "${GREEN}✅ Event created! ID: $EVENT_ID${NC}"
    else
        echo -e "${RED}❌ Event creation failed!${NC}"
        EVENT_ID=""
    fi
    
    echo -e "${CYAN}3.2 Testing Get Event by ID${NC}"
    if [ ! -z "$EVENT_ID" ]; then
        status=$(make_request "GET" "$API_URL/events/$EVENT_ID" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Get event by ID")
    else
        echo -e "${RED}❌ No event ID available for get test${NC}"
    fi
    
    echo -e "${CYAN}3.3 Testing Update Event${NC}"
    if [ ! -z "$EVENT_ID" ]; then
        update_event_data='{
            "name": "Updated Test Conference 2024",
            "description": "Updated description for our test conference",
            "eventStatus": "PUBLISHED",
            "capacity": 250
        }'
        status=$(make_request "PUT" "$API_URL/events/$EVENT_ID" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_event_data" "Update event")
    else
        echo -e "${RED}❌ No event ID available for update test${NC}"
    fi
    
    echo -e "${CYAN}3.4 Testing Get All Events${NC}"
    status=$(make_request "GET" "$API_URL/events" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Get all events")
    
    echo -e "${CYAN}3.5 Testing Delete Event${NC}"
    if [ ! -z "$EVENT_ID" ]; then
        status=$(make_request "DELETE" "$API_URL/events/$EVENT_ID" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "Delete event")
    else
        echo -e "${RED}❌ No event ID available for delete test${NC}"
    fi
fi

# =============================================================================
# 4. CHAT TESTS
# =============================================================================

print_test_header "CHAT TESTS"

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ No access token available for chat tests. Skipping...${NC}"
else
    echo -e "${CYAN}4.1 Testing Assistant Chat (BFF Endpoint)${NC}"
    chat_data='{
        "message": "Hello! I need help planning a corporate event for 100 people. Can you help me get started?",
        "chatId": "test-chat-001",
        "eventId": "'$EVENT_ID'"
    }'
    status=$(make_request "POST" "$API_URL/assistant/chat" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$chat_data" "Assistant chat")
    
    echo -e "${CYAN}4.2 Testing Assistant Chat with Event Context${NC}"
    chat_data='{
        "message": "What are the key things I should consider for venue selection?",
        "chatId": "test-chat-002",
        "eventId": "'$EVENT_ID'"
    }'
    status=$(make_request "POST" "$API_URL/assistant/chat" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$chat_data" "Assistant chat with event context")
    
    echo -e "${CYAN}4.3 Testing Assistant Chat with Different Client${NC}"
    chat_data='{
        "message": "I want to plan a birthday party",
        "chatId": "test-chat-003"
    }'
    status=$(make_request "POST" "$API_URL/assistant/chat" "-H 'X-Client-ID: mobile-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$chat_data" "Assistant chat with mobile client")
    
    echo -e "${CYAN}4.4 Testing Assistant Chat without Client ID (Should Fail)${NC}"
    chat_data='{
        "message": "This should fail",
        "chatId": "test-chat-004"
    }'
    status=$(make_request "POST" "$API_URL/assistant/chat" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$chat_data" "Assistant chat without client ID")
fi

# =============================================================================
# 5. ADDITIONAL SECURITY TESTS
# =============================================================================

print_test_header "ADDITIONAL SECURITY TESTS"

echo -e "${CYAN}5.1 Testing SQL Injection Attempt${NC}"
sql_injection_data='{
    "name": "Test Event\"; DROP TABLE events; --",
    "description": "SQL injection test",
    "eventType": "CONFERENCE",
    "startDateTime": "2024-12-15T09:00:00",
    "endDateTime": "2024-12-15T17:00:00"
}'
if [ ! -z "$ACCESS_TOKEN" ]; then
    status=$(make_request "POST" "$API_URL/events" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$sql_injection_data" "SQL injection test")
fi

echo -e "${CYAN}5.2 Testing XSS Attempt${NC}"
xss_data='{
    "name": "<script>alert(\"XSS\")</script>",
    "description": "XSS test",
    "eventType": "CONFERENCE",
    "startDateTime": "2024-12-15T09:00:00",
    "endDateTime": "2024-12-15T17:00:00"
}'
if [ ! -z "$ACCESS_TOKEN" ]; then
    status=$(make_request "POST" "$API_URL/events" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$xss_data" "XSS test")
fi

echo -e "${CYAN}5.3 Testing Large Payload${NC}"
large_data='{
    "name": "'$(printf 'A%.0s' {1..1000})'",
    "description": "'$(printf 'B%.0s' {1..5000})'",
    "eventType": "CONFERENCE",
    "startDateTime": "2024-12-15T09:00:00",
    "endDateTime": "2024-12-15T17:00:00"
}'
if [ ! -z "$ACCESS_TOKEN" ]; then
    status=$(make_request "POST" "$API_URL/events" "-H 'X-Client-ID: web-app' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$large_data" "Large payload test")
fi

# =============================================================================
# 6. SYSTEM HEALTH TESTS
# =============================================================================

print_test_header "SYSTEM HEALTH TESTS"

echo -e "${CYAN}6.1 Testing Java Service Health${NC}"
status=$(make_request "GET" "$BASE_URL/actuator/health" "" "" "Java service health")

echo -e "${CYAN}6.2 Testing Python Service Health (Direct)${NC}"
status=$(make_request "GET" "$PYTHON_URL/health" "" "" "Python service health")

echo -e "${CYAN}6.3 Testing API Documentation Access${NC}"
status=$(make_request "GET" "$BASE_URL/swagger-ui.html" "" "" "API documentation")

# =============================================================================
# SUMMARY
# =============================================================================

echo -e "\n${PURPLE}========================================${NC}"
echo -e "${PURPLE}TEST SUITE COMPLETED${NC}"
echo -e "${PURPLE}========================================${NC}"
echo ""
echo -e "${GREEN}✅ Authentication tests completed${NC}"
echo -e "${GREEN}✅ Security tests completed${NC}"
echo -e "${GREEN}✅ Event CRUD tests completed${NC}"
echo -e "${GREEN}✅ Chat tests completed${NC}"
echo -e "${GREEN}✅ Additional security tests completed${NC}"
echo -e "${GREEN}✅ System health tests completed${NC}"
echo ""
echo -e "${CYAN}🎉 All tests have been executed!${NC}"
echo -e "${CYAN}Check the output above for detailed request/response information.${NC}"
