#!/bin/bash

# Test script to verify Java -> Python flow
# This simulates a curl request from localhost:8080 (Java) to Python

echo "🧪 Testing Java -> Python Flow"
echo "================================"
echo ""

# Configuration
JAVA_BACKEND_URL="http://localhost:8080"
PYTHON_SHADE_URL="http://localhost:8000"
INTERNAL_SECRET="${INTERNAL_ASSISTANT_SECRET:-dev-internal-secret}"

echo "📋 Configuration:"
echo "  Java Backend: $JAVA_BACKEND_URL"
echo "  Python Shade: $PYTHON_SHADE_URL"
echo "  Internal Secret: [REDACTED]"
echo ""

# Test 1: Direct curl to Java endpoint (simulating external request)
echo "🔹 Test 1: Curl to Java endpoint /api/v1/assistant/chat"
echo "   (This should forward to Python Shade service)"
echo ""
echo "Request:"
echo "  POST $JAVA_BACKEND_URL/api/v1/assistant/chat"
echo "  Body: {\"message\": \"Create a wedding event\", \"userId\": \"test-user-123\", \"chatId\": \"chat-123\"}"
echo ""

RESPONSE=$(curl -s -X POST "$JAVA_BACKEND_URL/api/v1/assistant/chat" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "message": "Create a wedding event",
    "userId": "test-user-123",
    "chatId": "chat-123"
  }')

echo "Response:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""
echo ""

# Test 2: Direct curl to Python endpoint (bypassing Java)
echo "🔹 Test 2: Direct curl to Python Shade endpoint /chat"
echo "   (This tests Python directly)"
echo ""
echo "Request:"
echo "  POST $PYTHON_SHADE_URL/chat"
echo "  Headers: X-Internal-Service-Auth: $INTERNAL_SECRET"
echo "  Body: {\"message\": \"Hello, can you help me plan an event?\", \"user_id\": \"test-user-123\", \"chat_id\": \"chat-123\"}"
echo ""

PYTHON_RESPONSE=$(curl -s -X POST "$PYTHON_SHADE_URL/chat" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Service-Auth: $INTERNAL_SECRET" \
  -d '{
    "message": "Hello, can you help me plan an event?",
    "user_id": "test-user-123",
    "chat_id": "chat-123"
  }')

echo "Response:"
echo "$PYTHON_RESPONSE" | jq '.' 2>/dev/null || echo "$PYTHON_RESPONSE"
echo ""
echo ""

# Test 3: Health check
echo "🔹 Test 3: Health check endpoints"
echo ""

JAVA_HEALTH=$(curl -s "$JAVA_BACKEND_URL/actuator/health" || echo "Java backend not reachable")
PYTHON_HEALTH=$(curl -s "$PYTHON_SHADE_URL/health" || echo "Python Shade not reachable")

echo "Java Backend Health: $JAVA_HEALTH"
echo "Python Shade Health: $PYTHON_HEALTH"
echo ""

echo "✅ Flow test completed!"
echo ""
echo "💡 To test manually:"
echo "   1. Ensure Java backend is running on localhost:8080"
echo "   2. Ensure Python Shade is running on localhost:8000"
echo "   3. Set INTERNAL_ASSISTANT_SECRET env var (or use default: dev-internal-secret)"
echo "   4. Run: curl -X POST http://localhost:8080/api/v1/assistant/chat \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"message\": \"Create a wedding event\", \"userId\": \"user-123\"}'"

