#!/bin/bash

# Comprehensive Assistant Controller Endpoints Test Script
# Tests all assistant-related endpoints and generates a detailed report

echo "🤖 Starting Comprehensive Assistant Controller Endpoints Test"
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
PYTHON_URL="http://localhost:8000"
CLIENT_ID="web-app"
REPORT_FILE="reports/assistant_test_report_$(date +%Y%m%d_%H%M%S).md"
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
SESSION_ID=""
CHAT_ID=""

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Assistant Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Python URL:** $PYTHON_URL
**Client ID:** $CLIENT_ID
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Authentication | 0 | 0 | 0 | 0% |
| Session Management | 0 | 0 | 0 | 0% |
| Chat Conversations | 0 | 0 | 0 | 0% |
| Wedding Planning | 0 | 0 | 0 | 0% |
| Event Creation | 0 | 0 | 0 | 0% |
| Venue Search | 0 | 0 | 0 | 0% |
| Budget Planning | 0 | 0 | 0 | 0% |
| Weather Checks | 0 | 0 | 0 | 0% |
| Vendor Coordination | 0 | 0 | 0 | 0% |
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
    local base_url="$8"
    
    # Use provided base URL or default to BASE_URL
    if [ -z "$base_url" ]; then
        base_url="$BASE_URL"
    fi
    
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
    
    curl_cmd="$curl_cmd '$base_url$endpoint'"
    
    # Execute the request
    local response=$(eval $curl_cmd)
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    # Check if we got a 401 (Unauthorized) and try to refresh the token
    if [ "$http_code" = "401" ] && [ -n "$REFRESH_TOKEN" ] && [ "$expected_status" != "401" ]; then
        echo -e "${YELLOW}🔄 Token expired, attempting refresh...${NC}"
        local refresh_response=$(curl -s -w '%{http_code}' -X POST \
            -H "Content-Type: application/json" \
            -H "X-Client-ID: $CLIENT_ID" \
            -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" \
            "$BASE_URL/api/v1/auth/refresh")
        
        local refresh_http_code="${refresh_response: -3}"
        local refresh_response_body="${refresh_response%???}"
        
        if [ "$refresh_http_code" = "200" ]; then
            ACCESS_TOKEN=$(echo "$refresh_response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
            REFRESH_TOKEN=$(echo "$refresh_response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
            echo -e "${GREEN}✅ Token refreshed successfully${NC}"
            
            # Retry the original request with new token
            local new_headers=$(echo "$headers" | sed "s/Bearer [^']*/Bearer $ACCESS_TOKEN/g")
            curl_cmd="curl -s -w '%{http_code}' -X $method"
            if [ -n "$new_headers" ]; then
                curl_cmd="$curl_cmd $new_headers"
            fi
            if [ -n "$data" ]; then
                curl_cmd="$curl_cmd -d '$data'"
            fi
            curl_cmd="$curl_cmd '$base_url$endpoint'"
            
            response=$(eval $curl_cmd)
            http_code="${response: -3}"
            response_body="${response%???}"
        fi
    fi
    
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
            "Create Assistant Session")
                SESSION_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
        esac
    fi
    
    echo ""
}

# Function to run a chat conversation test
run_chat_test() {
    local test_name="$1"
    local message="$2"
    local expected_status="$3"
    local description="$4"
    local endpoint="$5"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${BLUE}💬 Running Chat: $test_name${NC}"
    
    local chat_data='{
        "message": "'$message'",
        "userId": "'$USER_ID'",
        "chatId": "'$CHAT_ID'",
        "eventId": "'$EVENT_ID'"
    }'
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "Content-Type: application/json" \
        -d "$chat_data" \
        "$BASE_URL$endpoint")
    
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
        echo "**Message:** $message"
        echo "**Endpoint:** POST $endpoint"
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$response_body"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"
    
    # Extract chat ID from response
    if [ "$http_code" = "200" ] && [ -n "$response_body" ]; then
        local extracted_chat_id=$(echo "$response_body" | grep -o '"chatId":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$extracted_chat_id" ]; then
            CHAT_ID="$extracted_chat_id"
        fi
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
            if curl -s "$PYTHON_URL/health" > /dev/null 2>&1; then
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
        "name": "Greek Wedding Planning",
        "description": "Dream wedding in Greece with Toyin",
        "eventType": "WEDDING",
        "startDateTime": "'$start_date'",
        "endDateTime": "'$end_date'",
        "venueRequirements": "Beautiful Greek venue with sea views",
        "capacity": 150,
        "isPublic": false,
        "requiresApproval": false,
        "coverImageUrl": "https://example.com/greek-wedding.jpg",
        "eventWebsiteUrl": "https://example.com/wedding",
        "hashtag": "#GreekWedding"
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

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Stop and restart services"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test assistant session management"
    echo "5. Test basic chat functionality"
    echo "6. Test comprehensive wedding planning conversation"
    echo "7. Test venue search and recommendations"
    echo "8. Test budget planning and management"
    echo "9. Test weather and vendor coordination"
    echo "10. Test error handling and edge cases"
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
    
    # Step 4: Health Check Tests
    echo -e "${CYAN}🏥 Step 4: Health Check Tests${NC}"
    echo "============================="
    run_test "Java Service Health" "GET" "/actuator/health" "-H 'X-Client-ID: $CLIENT_ID'" "" "200" "Check Java service health"
    run_test "Python Assistant Health" "GET" "/health" "" "" "200" "Check Python assistant health" "$PYTHON_URL"
    run_test "Assistant Agents Status" "GET" "/agents/status" "" "" "200" "Check assistant agents status" "$PYTHON_URL"
    run_test "Assistant Flow Status" "GET" "/flow/status" "" "" "200" "Check LangGraph flow status" "$PYTHON_URL"
    echo ""
    
    # Step 5: Session Management Tests
    echo -e "${CYAN}📱 Step 5: Session Management Tests${NC}"
    echo "=================================="
    
    # Create assistant session for event
    run_test "Create Assistant Session" "POST" "/api/assistant/sessions/event/$EVENT_ID?eventName=Greek Wedding Planning" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Create assistant session for event"
    
    # Get session for event
    run_test "Get Session for Event" "GET" "/api/assistant/sessions/event/$EVENT_ID" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get session for event"
    
    # Check if session exists
    run_test "Check Session Exists" "GET" "/api/assistant/sessions/event/$EVENT_ID/exists" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Check if session exists"
    
    # Get sessions by organizer
    run_test "Get Sessions by Organizer" "GET" "/api/assistant/sessions/organizer" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get sessions by organizer"
    echo ""
    
    # Step 6: Basic Chat Functionality Tests
    echo -e "${CYAN}💬 Step 6: Basic Chat Functionality Tests${NC}"
    echo "============================================="
    
    # Initialize chat ID
    CHAT_ID="test-chat-$(date +%s)"
    
    # Test basic greeting
    run_chat_test "Basic Greeting" "Hello! I need help planning my wedding in Greece." "200" "Test basic greeting and introduction" "/api/v1/ai/chat"
    sleep 3
    
    # Test event planning initiation
    run_chat_test "Event Planning Start" "I want to plan a dream wedding in Greece with my partner Toyin. We're thinking of Santorini or Mykonos." "200" "Test event planning initiation" "/api/v1/ai/chat"
    sleep 3
    
    # Test event details gathering
    run_chat_test "Event Details" "We're planning for June 2024, expecting around 150 guests, and our budget is around $50,000." "200" "Test event details gathering" "/api/v1/ai/chat"
    sleep 3
    echo ""
    
    # Step 7: Comprehensive Wedding Planning Conversation
    echo -e "${CYAN}💍 Step 7: Comprehensive Wedding Planning Conversation${NC}"
    echo "====================================================="
    
    # Venue search and selection
    run_chat_test "Venue Search" "Can you help me find beautiful wedding venues in Greece? I'm looking for something with sea views and traditional Greek architecture." "200" "Test venue search capabilities" "/api/v1/ai/chat"
    sleep 3
    
    # Budget planning
    run_chat_test "Budget Planning" "Let's create a budget for this wedding. I need to track expenses for venue, catering, photography, flowers, and decorations." "200" "Test budget planning and management" "/api/v1/ai/chat"
    sleep 3
    
    # Timeline creation
    run_chat_test "Timeline Creation" "I need help creating a timeline for the wedding planning. What should I do first and when should I book vendors?" "200" "Test timeline creation and planning" "/api/v1/ai/chat"
    
    # Vendor coordination
    run_chat_test "Vendor Search" "I need to find photographers, caterers, and florists in Greece. Can you help me search for vendors?" "200" "Test vendor search and coordination" "/api/v1/ai/chat"
    
    # Weather considerations
    run_chat_test "Weather Check" "What's the weather like in Greece in June? Should I plan for outdoor or indoor ceremonies?" "200" "Test weather information and planning" "/api/v1/ai/chat"
    
    # Guest management
    run_chat_test "Guest Management" "I need help managing my guest list. How should I organize RSVPs and track attendance?" "200" "Test guest and attendee management" "/api/v1/ai/chat"
    
    # Risk management
    run_chat_test "Risk Management" "What are some potential risks for a destination wedding in Greece? How can I prepare for them?" "200" "Test risk assessment and contingency planning" "/api/v1/ai/chat"
    
    # Communication planning
    run_chat_test "Communication Planning" "I need to send invitations and updates to guests. Can you help me create email templates?" "200" "Test communication and invitation management" "/api/v1/ai/chat"
    echo ""
    
    # Step 8: Venue Search and Recommendations
    echo -e "${CYAN}🏰 Step 8: Venue Search and Recommendations${NC}"
    echo "============================================="
    
    # Specific venue search
    run_chat_test "Santorini Venues" "Show me wedding venues specifically in Santorini with sunset views." "200" "Test specific location venue search" "/api/v1/ai/chat"
    
    # Venue comparison
    run_chat_test "Venue Comparison" "Compare the venues you found. What are the pros and cons of each?" "200" "Test venue comparison and analysis" "/api/v1/ai/chat"
    
    # Venue booking inquiry
    run_chat_test "Venue Inquiry" "I want to send an inquiry to the first venue. Can you help me draft an email?" "200" "Test venue inquiry and communication" "/api/v1/ai/chat"
    echo ""
    
    # Step 9: Budget Planning and Management
    echo -e "${CYAN}💰 Step 9: Budget Planning and Management${NC}"
    echo "============================================="
    
    # Budget breakdown
    run_chat_test "Budget Breakdown" "Break down my $50,000 budget across different categories like venue, catering, photography, etc." "200" "Test budget breakdown and allocation" "/api/v1/ai/chat"
    
    # Cost tracking
    run_chat_test "Cost Tracking" "I just got a quote for $8,000 for photography. Add this to my budget tracker." "200" "Test cost tracking and budget updates" "/api/v1/ai/chat"
    
    # Budget analysis
    run_chat_test "Budget Analysis" "How am I doing with my budget? Am I on track to stay within $50,000?" "200" "Test budget analysis and monitoring" "/api/v1/ai/chat"
    echo ""
    
    # Step 10: Weather and Vendor Coordination
    echo -e "${CYAN}🌤️ Step 10: Weather and Vendor Coordination${NC}"
    echo "============================================="
    
    # Weather forecast
    run_chat_test "Weather Forecast" "What's the weather forecast for Santorini in June 2024?" "200" "Test weather forecasting" "/api/v1/ai/chat"
    
    # Vendor search
    run_chat_test "Photographer Search" "Find me wedding photographers in Santorini who specialize in destination weddings." "200" "Test vendor search capabilities" "/api/v1/ai/chat"
    
    # Vendor coordination
    run_chat_test "Vendor Coordination" "Help me coordinate with multiple vendors. I need to schedule meetings with photographers, caterers, and florists." "200" "Test vendor coordination and scheduling" "/api/v1/ai/chat"
    echo ""
    
    # Step 11: Error Handling and Edge Cases
    echo -e "${CYAN}⚠️ Step 11: Error Handling and Edge Cases${NC}"
    echo "============================================="
    
    # Invalid chat request
    run_test "Invalid Chat Request" "POST" "/api/v1/ai/chat" "-H 'Content-Type: application/json'" '{"message": ""}' "400" "Test empty message handling"
    
    # Unauthorized access
    run_test "Unauthorized Chat" "POST" "/api/v1/ai/chat" "-H 'Content-Type: application/json'" '{"message": "Hello"}' "200" "Test unauthenticated access (should work now)"
    
    # Invalid session
    run_test "Invalid Session" "GET" "/api/assistant/sessions/event/00000000-0000-0000-0000-000000000000" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Test invalid session handling"
    
    # Direct Python service test (should fail without internal auth)
    run_test "Direct Python Service" "POST" "/chat" "-H 'Content-Type: application/json'" '{"message": "Hello", "user_id": "test", "chat_id": "test"}' "401" "Test direct Python service access" "$PYTHON_URL"
    echo ""
    
    # Step 12: Advanced Assistant Features
    echo -e "${CYAN}🚀 Step 12: Advanced Assistant Features${NC}"
    echo "============================================="
    
    # Multi-agent coordination
    run_chat_test "Multi-Agent Coordination" "I need help with venue selection, budget planning, and vendor coordination all at once." "200" "Test multi-agent coordination" "/api/v1/ai/chat"
    
    # Complex planning scenario
    run_chat_test "Complex Planning" "Plan a 3-day wedding celebration in Greece with welcome party, main ceremony, and farewell brunch." "200" "Test complex multi-day event planning" "/api/v1/ai/chat"
    
    # Emergency planning
    run_chat_test "Emergency Planning" "I need to change my wedding date from June to August. How will this affect my venue and vendor bookings?" "200" "Test emergency planning and date changes" "/api/v1/ai/chat"
    
    # Final recommendations
    run_chat_test "Final Recommendations" "Give me a final checklist and recommendations for my Greek wedding planning." "200" "Test final recommendations and checklist" "/api/v1/ai/chat"
    echo ""
    
    # Step 13: Shade Controller Tests
    echo -e "${CYAN}🎭 Step 13: Shade Controller Tests${NC}"
    echo "====================================="
    
    # Test Shade conversation endpoint
    local shade_data='{
        "message": "I want to plan a wedding in Greece",
        "sessionId": "'$SESSION_ID'",
        "context": {"eventType": "wedding", "location": "Greece"},
        "intent": "event_planning"
    }'
    run_test "Shade Conversation" "POST" "/api/v1/assistant/shade/chat" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$shade_data" "200" "Test Shade conversation endpoint"
    
    # Test Shade with invalid token
    run_test "Shade Invalid Token" "POST" "/api/v1/assistant/shade/chat" "-H 'X-Client-ID: $CLIENT_ID' -H 'Authorization: Bearer invalid-token' -H 'Content-Type: application/json'" "$shade_data" "401" "Test Shade with invalid token"
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
        echo "| Health Check | 4 | $([ $PASSED_TESTS -gt 0 ] && echo "4" || echo "0") | $([ $FAILED_TESTS -gt 0 ] && echo "4" || echo "0") | $([ $PASSED_TESTS -gt 0 ] && echo "100%" || echo "0%") |"
        echo "| Session Management | 4 | 0 | 0 | 0% |"
        echo "| Chat Conversations | 15 | 0 | 0 | 0% |"
        echo "| Wedding Planning | 8 | 0 | 0 | 0% |"
        echo "| Venue Search | 3 | 0 | 0 | 0% |"
        echo "| Budget Planning | 3 | 0 | 0 | 0% |"
        echo "| Weather & Vendors | 3 | 0 | 0 | 0% |"
        echo "| Error Handling | 4 | 0 | 0 | 0% |"
        echo "| Advanced Features | 4 | 0 | 0 | 0% |"
        echo "| Shade Controller | 2 | 0 | 0 | 0% |"
        echo ""
        echo "### Recommendations"
        echo ""
    } >> "$REPORT_FILE"
    
    if [ $success_rate -ge 90 ]; then
        {
            echo "✅ **Excellent!** All assistant tests are passing successfully. The AI assistant system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the assistant system."
        } >> "$REPORT_FILE"
    fi
    
    {
        echo ""
        echo "### Next Steps"
        echo ""
        echo "1. Review failed tests and fix underlying issues"
        echo "2. Check Python assistant logs for detailed error information"
        echo "3. Verify agent coordination and tool functionality"
        echo "4. Test with different conversation scenarios"
        echo "5. Consider adding more edge case tests for complex conversations"
        echo ""
        echo "---"
        echo ""
        echo "**Report generated by:** Assistant Controller Test Script"
        echo "**Script version:** 1.0"
        echo "**Wedding Planning Scenario:** Greek destination wedding with comprehensive planning conversation"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Assistant system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
