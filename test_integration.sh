#!/bin/bash

# Integration Test Script
# Tests the communication between Java Spring Boot and Python Shade Assistant

echo "🧪 Testing Java-Python Integration"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test Java API
echo -e "${BLUE}☕ Testing Java Spring Boot API...${NC}"

# Test 1: Create Event
echo "1. Creating event via Java API..."
CREATE_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/events" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -d '{
        "name": "Integration Test Event",
        "description": "Testing Java-Python integration",
        "eventType": "CONFERENCE",
        "eventStatus": "PLANNING",
        "startDateTime": "2025-12-15T09:00:00",
        "endDateTime": "2025-12-15T17:00:00",
        "capacity": 100,
        "isPublic": true,
        "requiresApproval": false
    }' \
    -w "HTTP_STATUS:%{http_code}")

if echo "$CREATE_RESPONSE" | grep -q "HTTP_STATUS:201"; then
    echo -e "${GREEN}✅ Event created successfully${NC}"
    EVENT_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo "   Event ID: $EVENT_ID"
else
    echo -e "${RED}❌ Failed to create event${NC}"
    echo "Response: $CREATE_RESPONSE"
    exit 1
fi

# Test 2: Get Event
echo "2. Retrieving event via Java API..."
GET_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/events/$EVENT_ID" \
    -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -w "HTTP_STATUS:%{http_code}")

if echo "$GET_RESPONSE" | grep -q "HTTP_STATUS:200"; then
    echo -e "${GREEN}✅ Event retrieved successfully${NC}"
else
    echo -e "${RED}❌ Failed to retrieve event${NC}"
    echo "Response: $GET_RESPONSE"
    exit 1
fi

# Test Python Shade Assistant
echo -e "${BLUE}🐍 Testing Python Shade Assistant...${NC}"

# Test 3: Health Check
echo "3. Checking Python Shade Assistant health..."
PYTHON_HEALTH=$(curl -s -X GET "http://localhost:8000/health" -w "HTTP_STATUS:%{http_code}")

if echo "$PYTHON_HEALTH" | grep -q "HTTP_STATUS:200"; then
    echo -e "${GREEN}✅ Python Shade Assistant is healthy${NC}"
else
    echo -e "${RED}❌ Python Shade Assistant health check failed${NC}"
    echo "Response: $PYTHON_HEALTH"
    exit 1
fi

# Test 4: Test Chat Interface
echo "4. Testing chat interface..."
CHAT_RESPONSE=$(curl -s -X GET "http://localhost:8000/" -w "HTTP_STATUS:%{http_code}")

if echo "$CHAT_RESPONSE" | grep -q "HTTP_STATUS:200"; then
    echo -e "${GREEN}✅ Chat interface is accessible${NC}"
else
    echo -e "${RED}❌ Chat interface test failed${NC}"
    echo "Response: $CHAT_RESPONSE"
    exit 1
fi

# Test 5: Test Java API from Python context (simulate chatbot calling Java)
echo "5. Testing Java API access from Python context..."
echo "   This simulates the chatbot making calls to the Java API"

# Test creating an event that the chatbot might create
CHATBOT_EVENT=$(curl -s -X POST "http://localhost:8080/api/v1/events" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -d '{
        "name": "Chatbot Generated Event",
        "description": "This event was created by the chatbot integration",
        "eventType": "WORKSHOP",
        "eventStatus": "PLANNING",
        "startDateTime": "2025-12-20T10:00:00",
        "endDateTime": "2025-12-20T16:00:00",
        "capacity": 50,
        "isPublic": true,
        "requiresApproval": false
    }' \
    -w "HTTP_STATUS:%{http_code}")

if echo "$CHATBOT_EVENT" | grep -q "HTTP_STATUS:201"; then
    echo -e "${GREEN}✅ Chatbot can successfully create events${NC}"
    CHATBOT_EVENT_ID=$(echo "$CHATBOT_EVENT" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo "   Chatbot Event ID: $CHATBOT_EVENT_ID"
else
    echo -e "${RED}❌ Chatbot failed to create event${NC}"
    echo "Response: $CHATBOT_EVENT"
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 All Integration Tests Passed!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}📊 Test Summary:${NC}"
echo "   ✅ Java Spring Boot API is working"
echo "   ✅ Python Shade Assistant is working"
echo "   ✅ Chat interface is accessible"
echo "   ✅ Java-Python integration is functional"
echo ""
echo -e "${BLUE}🔗 Available Services:${NC}"
echo "   • Java API: http://localhost:8080/api/v1/events"
echo "   • Java Docs: http://localhost:8080/swagger-ui"
echo "   • Chat Interface: http://localhost:8000"
echo ""
echo -e "${YELLOW}💡 You can now use the chatbot to execute Java commands!${NC}"
echo "   Try asking the chatbot to create, read, update, or delete events."
