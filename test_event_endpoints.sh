#!/bin/bash

# Event Controller API Testing Script
# This script tests all endpoints in the EventCrudController

BASE_URL="http://localhost:8080/api/v1/events"
echo "Testing Event Controller Endpoints"
echo "=================================="

# Test 1: Create a new event
echo -e "\n1. Testing POST /api/v1/events - Create Event"
echo "-----------------------------------------------"

CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Test Conference 2024",
    "description": "A test conference for API testing",
    "eventType": "CONFERENCE",
    "eventStatus": "PLANNING",
    "startDateTime": "2025-12-15T09:00:00",
    "endDateTime": "2025-12-15T17:00:00",
    "registrationDeadline": "2025-12-10T23:59:59",
    "capacity": 100,
    "currentAttendeeCount": 0,
    "isPublic": true,
    "requiresApproval": false,
    "qrCodeEnabled": true,
    "coverImageUrl": "https://example.com/image.jpg",
    "eventWebsiteUrl": "https://example.com/event",
    "hashtag": "#TestConf2024",
    "theme": "Innovation and Technology",
    "objectives": "Test the API endpoints",
    "targetAudience": "Developers and Testers",
    "successMetrics": "All tests pass",
    "brandingGuidelines": "Keep it simple",
    "venueRequirements": "Conference room with AV",
    "technicalRequirements": "WiFi and projectors",
    "accessibilityFeatures": "Wheelchair accessible",
    "emergencyPlan": "Standard emergency procedures",
    "backupPlan": "Virtual event option",
    "postEventTasks": "Send feedback surveys"
  }')

echo "Response: $CREATE_RESPONSE"

# Extract the event ID from the response
EVENT_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Created Event ID: $EVENT_ID"

# Test 2: Get the created event
echo -e "\n2. Testing GET /api/v1/events/{id} - Get Event by ID"
echo "----------------------------------------------------"

if [ ! -z "$EVENT_ID" ]; then
    GET_RESPONSE=$(curl -s -X GET "$BASE_URL/$EVENT_ID" \
      -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000")
    echo "Response: $GET_RESPONSE"
else
    echo "ERROR: Could not extract event ID from create response"
fi

# Test 3: Try to get a non-existent event (should return 404)
echo -e "\n3. Testing GET /api/v1/events/{id} - Get Non-existent Event (404 test)"
echo "----------------------------------------------------------------------"

NON_EXISTENT_ID="550e8400-e29b-41d4-a716-446655440001"
GET_404_RESPONSE=$(curl -s -w "HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$NON_EXISTENT_ID" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000")
echo "Response: $GET_404_RESPONSE"

# Test 4: Create another event for deletion test
echo -e "\n4. Testing POST /api/v1/events - Create Second Event for Deletion"
echo "----------------------------------------------------------------"

CREATE_RESPONSE_2=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Test Workshop 2024",
    "description": "A test workshop for deletion testing",
    "eventType": "WORKSHOP",
    "eventStatus": "DRAFT",
    "startDateTime": "2025-12-20T10:00:00",
    "endDateTime": "2025-12-20T16:00:00",
    "capacity": 50,
    "isPublic": false,
    "requiresApproval": true
  }')

echo "Response: $CREATE_RESPONSE_2"

# Extract the second event ID
EVENT_ID_2=$(echo "$CREATE_RESPONSE_2" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Created Second Event ID: $EVENT_ID_2"

# Test 5: Delete the second event
echo -e "\n5. Testing DELETE /api/v1/events/{id} - Delete Event"
echo "---------------------------------------------------"

if [ ! -z "$EVENT_ID_2" ]; then
    DELETE_RESPONSE=$(curl -s -w "HTTP_STATUS:%{http_code}" -X DELETE "$BASE_URL/$EVENT_ID_2" \
      -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000")
    echo "Response: $DELETE_RESPONSE"
else
    echo "ERROR: Could not extract second event ID from create response"
fi

# Test 6: Try to get the deleted event (should return 404)
echo -e "\n6. Testing GET /api/v1/events/{id} - Get Deleted Event (404 test)"
echo "----------------------------------------------------------------"

if [ ! -z "$EVENT_ID_2" ]; then
    GET_DELETED_RESPONSE=$(curl -s -w "HTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$EVENT_ID_2" \
      -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000")
    echo "Response: $GET_DELETED_RESPONSE"
fi

# Test 7: Test validation errors
echo -e "\n7. Testing POST /api/v1/events - Validation Error Test"
echo "-----------------------------------------------------"

VALIDATION_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "",
    "eventType": "INVALID_TYPE",
    "startDateTime": "2020-01-01T09:00:00"
  }')

echo "Response: $VALIDATION_RESPONSE"

# Test 8: Test without required headers
echo -e "\n8. Testing POST /api/v1/events - Missing User ID Header"
echo "------------------------------------------------------"

NO_HEADER_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Event",
    "eventType": "CONFERENCE",
    "startDateTime": "2025-12-15T09:00:00"
  }')

echo "Response: $NO_HEADER_RESPONSE"

echo -e "\n=================================="
echo "Event Controller API Testing Complete"
echo "=================================="
