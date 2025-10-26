#!/bin/bash

# Quick Timeline Test Script
# Tests basic timeline functionality to verify fixes

echo "🧪 Quick Timeline Test"
echo "======================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
CLIENT_ID="web-app"
TEST_USER_EMAIL="testuser@example.com"
TEST_USER_PASSWORD="Password123!"

# Variables
ACCESS_TOKEN=""
EVENT_ID=""

# Function to run a test
run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local headers="$4"
    local data="$5"
    local expected_status="$6"
    
    echo -e "${BLUE}🧪 Running: $test_name${NC}"
    
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    local response=$(eval $curl_cmd)
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        return 0
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        echo "Response: $response_body"
        return 1
    fi
}

# Test 1: Health Check
echo -e "${YELLOW}Step 1: Health Check${NC}"
run_test "Health Check" "GET" "/api/v1/auth/health" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "" "200"
echo ""

# Test 2: User Login (try login first, then register if needed)
echo -e "${YELLOW}Step 2: User Authentication${NC}"
login_data='{
    "email": "'$TEST_USER_EMAIL'",
    "password": "'$TEST_USER_PASSWORD'",
    "rememberMe": false,
    "deviceId": "test-device-123",
    "clientId": "'$CLIENT_ID'"
}'

run_test "User Login" "POST" "/api/v1/auth/login" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$login_data" "200"

# Extract token from login
if [ $? -eq 0 ]; then
    ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "$login_data" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✅ Access token extracted: ${ACCESS_TOKEN:0:20}...${NC}"
else
    # If login fails, try registration
    echo -e "${YELLOW}Login failed, trying registration...${NC}"
    registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "name": "Test User",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'",
        "phoneNumber": "+1234567890",
        "dateOfBirth": "1990-01-01",
        "acceptTerms": true,
        "acceptPrivacy": true,
        "marketingOptIn": false,
        "deviceId": "test-device-123",
        "clientId": "'$CLIENT_ID'"
    }'
    
    run_test "User Registration" "POST" "/api/v1/auth/register" "-H 'X-Client-ID: $CLIENT_ID' -H 'Content-Type: application/json'" "$registration_data" "201"
    
    # Extract token from registration
    if [ $? -eq 0 ]; then
        ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
            -H "X-Client-ID: $CLIENT_ID" \
            -H "Content-Type: application/json" \
            -d "$login_data" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✅ Access token extracted: ${ACCESS_TOKEN:0:20}...${NC}"
    fi
fi
echo ""

# Test 3: Create Event
echo -e "${YELLOW}Step 3: Create Event${NC}"
if [ -n "$ACCESS_TOKEN" ]; then
    event_data='{
        "name": "Test Wedding Event",
        "description": "A beautiful wedding celebration",
        "eventType": "WEDDING",
        "startDateTime": "2025-12-25T18:00:00",
        "endDateTime": "2025-12-25T23:00:00",
        "capacity": 150,
        "isPublic": false,
        "requiresApproval": false
    }'
    
    # Extract user ID from token (this is a simplified approach)
    USER_ID=$(curl -s -X GET "$BASE_URL/api/v1/auth/me" \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Authorization: Bearer $ACCESS_TOKEN" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    
    run_test "Create Event" "POST" "/api/v1/events" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'X-User-Id: $USER_ID' -H 'Content-Type: application/json'" "$event_data" "201"
    
    # Extract event ID
    if [ $? -eq 0 ]; then
        EVENT_ID=$(curl -s -X POST "$BASE_URL/api/v1/events" \
            -H "X-Client-ID: $CLIENT_ID" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H "X-User-Id: $USER_ID" \
            -H "Content-Type: application/json" \
            -d "$event_data" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✅ Event ID extracted: $EVENT_ID${NC}"
    fi
else
    echo -e "${RED}❌ No access token available${NC}"
fi
echo ""

# Test 4: Create Timeline Item
echo -e "${YELLOW}Step 4: Create Timeline Item${NC}"
if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
    timeline_item_data='{
        "eventId": "'$EVENT_ID'",
        "title": "Ceremony Setup",
        "description": "Setup ceremony area with decorations",
        "scheduledAt": "2025-12-25T16:00:00",
        "durationMinutes": 60,
        "itemType": "SETUP",
        "priority": "HIGH",
        "location": "Main Hall",
        "assignedTo": null,
        "setupTimeMinutes": 30,
        "teardownTimeMinutes": 15,
        "resourcesRequired": "Decorations, flowers, chairs",
        "notes": "Ensure all decorations are in place"
    }'
    
    run_test "Create Timeline Item" "POST" "/api/v1/timeline" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$timeline_item_data" "200"
else
    echo -e "${RED}❌ No access token or event ID available${NC}"
fi
echo ""

# Test 5: Get Timeline
echo -e "${YELLOW}Step 5: Get Timeline${NC}"
if [ -n "$ACCESS_TOKEN" ] && [ -n "$EVENT_ID" ]; then
    run_test "Get Timeline" "GET" "/api/v1/timeline/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200"
else
    echo -e "${RED}❌ No access token or event ID available${NC}"
fi
echo ""

echo -e "${GREEN}🎉 Quick Timeline Test Complete!${NC}"
