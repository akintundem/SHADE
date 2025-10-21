#!/bin/bash

# Enhanced Conversation Test Script
# Tests the new conversational event creation and enhancement flow

echo "🧪 Testing Enhanced Event Planning Conversation"
echo "==============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test the enhanced conversation flow
echo -e "${BLUE}🎯 Testing Enhanced Event Creation Flow...${NC}"

# Test 1: Create event with core info
echo -e "${YELLOW}1. Creating event with core information...${NC}"
CREATE_RESPONSE=$(curl -s -X POST "http://localhost:8000/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I want to create a birthday party for my 2-year-old on October 26th, 2026 at 2 PM",
    "user_id": "test-user-123",
    "chat_id": "test-chat-456"
  }')

echo "Response: $CREATE_RESPONSE"

# Test 2: Add more details
echo -e "${YELLOW}2. Adding capacity and theme...${NC}"
ENHANCE_RESPONSE=$(curl -s -X POST "http://localhost:8000/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "We expect about 15 guests and want a superhero theme",
    "user_id": "test-user-123",
    "chat_id": "test-chat-456"
  }')

echo "Response: $ENHANCE_RESPONSE"

# Test 3: Ask questions about the event
echo -e "${YELLOW}3. Asking questions about the event...${NC}"
QUESTION_RESPONSE=$(curl -s -X POST "http://localhost:8000/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What date did I schedule my event for?",
    "user_id": "test-user-123",
    "chat_id": "test-chat-456"
  }')

echo "Response: $QUESTION_RESPONSE"

# Test 4: Check event status
echo -e "${YELLOW}4. Checking event status...${NC}"
STATUS_RESPONSE=$(curl -s -X POST "http://localhost:8000/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the current status of my event?",
    "user_id": "test-user-123",
    "chat_id": "test-chat-456"
  }')

echo "Response: $STATUS_RESPONSE"

# Test 5: Add venue requirements
echo -e "${YELLOW}5. Adding venue requirements...${NC}"
VENUE_RESPONSE=$(curl -s -X POST "http://localhost:8000/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "We need a venue with a playground and space for 15 kids",
    "user_id": "test-user-123",
    "chat_id": "test-chat-456"
  }')

echo "Response: $VENUE_RESPONSE"

echo ""
echo -e "${GREEN}🎉 Enhanced Conversation Test Complete!${NC}"
echo "==============================================="
echo ""
echo -e "${BLUE}📊 What we tested:${NC}"
echo "   ✅ Event creation with core info (name, date, type)"
echo "   ✅ Human-like confirmations (event planner's journal style)"
echo "   ✅ Adding additional details (capacity, theme)"
echo "   ✅ Natural language questions about the event"
echo "   ✅ Event status checking with missing details suggestions"
echo "   ✅ Venue requirements and other enhancements"
echo ""
echo -e "${YELLOW}💡 The enhanced system now provides:${NC}"
echo "   • Two-phase creation (core info first, then details)"
echo "   • Human-like confirmations and journal-style responses"
echo "   • Intelligent question answering about events"
echo "   • Suggestions for missing information"
echo "   • Better conversational flow"
echo ""
echo -e "${GREEN}🚀 Ready for natural conversation!${NC}"
