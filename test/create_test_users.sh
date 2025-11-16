#!/bin/bash

# Script to create test users (admin and guest) and an event for frontend testing
# This script registers users, verifies emails, sets admin role, and creates an event

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="admin@test.com"
ADMIN_PASSWORD="Admin123!@#"
GUEST_EMAIL="guest@test.com"
GUEST_PASSWORD="Guest123!@#"

echo -e "${CYAN}🚀 Creating Test Users and Event${NC}"
echo "=================================="
echo ""

# Function to verify email in database
verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Verified email: ${email}${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Could not verify email via docker (may need manual verification)${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  Docker not available - email verification skipped${NC}"
        return 1
    fi
}

# Function to set admin role in database
set_admin_role() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET user_type = 'ADMIN' WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Set admin role for: ${email}${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Could not set admin role via docker${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  Docker not available - admin role setting skipped${NC}"
        return 1
    fi
}

# Function to complete onboarding
complete_onboarding() {
    local email="$1"
    local password="$2"
    local name="$3"
    
    # Login first
    local login_data="{
        \"email\": \"${email}\",
        \"password\": \"${password}\",
        \"rememberMe\": false
    }"
    
    local login_response=$(curl -s -w '%{http_code}' -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/v1/auth/login")
    
    local login_http_code="${login_response: -3}"
    local login_response_body="${login_response%???}"
    
    if [ "$login_http_code" != "200" ]; then
        echo -e "${RED}❌ Failed to login: ${email}${NC}"
        return 1
    fi
    
    local access_token=$(echo "$login_response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    local device_id=$(echo "$login_response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$access_token" ]; then
        echo -e "${RED}❌ Failed to get access token${NC}"
        return 1
    fi
    
    # Complete onboarding
    local onboarding_data="{
        \"name\": \"${name}\",
        \"phoneNumber\": \"+1234567890\",
        \"dateOfBirth\": \"1990-01-01\",
        \"acceptTerms\": true,
        \"acceptPrivacy\": true,
        \"marketingOptIn\": false
    }"
    
    local onboarding_response=$(curl -s -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $access_token" \
        -H "X-Device-ID: $device_id" \
        -H "Content-Type: application/json" \
        -d "$onboarding_data" \
        "$BASE_URL/api/v1/auth/complete-onboarding")
    
    local onboarding_http_code="${onboarding_response: -3}"
    
    if [ "$onboarding_http_code" = "200" ]; then
        echo -e "${GREEN}✅ Completed onboarding for: ${email}${NC}" >&2
        echo "$access_token"
        return 0
    else
        echo -e "${YELLOW}⚠️  Onboarding may have already been completed${NC}" >&2
        echo "$access_token"
        return 0
    fi
}

# Step 1: Create Admin User
echo -e "${BLUE}📝 Step 1: Creating Admin User${NC}"
echo "Email: ${ADMIN_EMAIL}"
echo "Password: ${ADMIN_PASSWORD}"
echo ""

admin_registration_data="{
    \"email\": \"${ADMIN_EMAIL}\",
    \"password\": \"${ADMIN_PASSWORD}\",
    \"confirmPassword\": \"${ADMIN_PASSWORD}\"
}"

admin_registration_response=$(curl -s -w '%{http_code}' -X POST \
    -H "Content-Type: application/json" \
    -d "$admin_registration_data" \
    "$BASE_URL/api/v1/auth/register")

admin_registration_code="${admin_registration_response: -3}"

if [ "$admin_registration_code" = "201" ] || [ "$admin_registration_code" = "200" ]; then
    echo -e "${GREEN}✅ Admin user registered${NC}"
else
    echo -e "${YELLOW}⚠️  Admin user may already exist (HTTP: $admin_registration_code)${NC}"
fi

# Verify admin email
verify_email_in_database "$ADMIN_EMAIL"

# Set admin role
set_admin_role "$ADMIN_EMAIL"

# Complete admin onboarding
ADMIN_TOKEN=$(complete_onboarding "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "Admin User" 2>&1 | tail -1)
if [ -z "$ADMIN_TOKEN" ] || [[ "$ADMIN_TOKEN" == *"✅"* ]] || [[ "$ADMIN_TOKEN" == *"⚠️"* ]]; then
    # Retry login to get token
    login_data="{
        \"email\": \"${ADMIN_EMAIL}\",
        \"password\": \"${ADMIN_PASSWORD}\",
        \"rememberMe\": false
    }"
    login_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/v1/auth/login")
    ADMIN_TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}❌ Failed to get admin token${NC}"
    exit 1
fi

echo ""

# Step 2: Create Guest User
echo -e "${BLUE}📝 Step 2: Creating Guest User${NC}"
echo "Email: ${GUEST_EMAIL}"
echo "Password: ${GUEST_PASSWORD}"
echo ""

guest_registration_data="{
    \"email\": \"${GUEST_EMAIL}\",
    \"password\": \"${GUEST_PASSWORD}\",
    \"confirmPassword\": \"${GUEST_PASSWORD}\"
}"

guest_registration_response=$(curl -s -w '%{http_code}' -X POST \
    -H "Content-Type: application/json" \
    -d "$guest_registration_data" \
    "$BASE_URL/api/v1/auth/register")

guest_registration_code="${guest_registration_response: -3}"

if [ "$guest_registration_code" = "201" ] || [ "$guest_registration_code" = "200" ]; then
    echo -e "${GREEN}✅ Guest user registered${NC}"
else
    echo -e "${YELLOW}⚠️  Guest user may already exist (HTTP: $guest_registration_code)${NC}"
fi

# Verify guest email
verify_email_in_database "$GUEST_EMAIL"

# Complete guest onboarding
GUEST_TOKEN=$(complete_onboarding "$GUEST_EMAIL" "$GUEST_PASSWORD" "Guest User" 2>&1 | tail -1)
if [ -z "$GUEST_TOKEN" ] || [[ "$GUEST_TOKEN" == *"✅"* ]] || [[ "$GUEST_TOKEN" == *"⚠️"* ]]; then
    # Retry login to get token
    login_data="{
        \"email\": \"${GUEST_EMAIL}\",
        \"password\": \"${GUEST_PASSWORD}\",
        \"rememberMe\": false
    }"
    login_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/v1/auth/login")
    GUEST_TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$GUEST_TOKEN" ]; then
    echo -e "${RED}❌ Failed to get guest token${NC}"
    exit 1
fi

echo ""

# Step 3: Create Event (as Admin)
echo -e "${BLUE}📅 Step 3: Creating Test Event${NC}"
echo ""

# Get admin user ID from token
ADMIN_USER_ID=""
if command -v python3 &> /dev/null; then
    ADMIN_JWT_PAYLOAD=$(echo "$ADMIN_TOKEN" | cut -d'.' -f2)
    ADMIN_USER_ID=$(echo "$ADMIN_JWT_PAYLOAD" | python3 -c "import sys, base64, json; data=sys.stdin.read().strip(); padding=4-len(data)%4; data+=('='*padding if padding<4 else ''); print(json.loads(base64.b64decode(data)).get('sub', ''))" 2>/dev/null)
elif command -v jq &> /dev/null; then
    ADMIN_JWT_PAYLOAD=$(echo "$ADMIN_TOKEN" | cut -d'.' -f2)
    local padding=$((4 - ${#ADMIN_JWT_PAYLOAD} % 4))
    if [ $padding -ne 4 ]; then
        ADMIN_JWT_PAYLOAD="${ADMIN_JWT_PAYLOAD}$(printf '%*s' $padding | tr ' ' '=')"
    fi
    ADMIN_USER_ID=$(echo "$ADMIN_JWT_PAYLOAD" | base64 -d 2>/dev/null | jq -r '.sub // empty' 2>/dev/null)
fi

# Calculate dates (1 day from now)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    START_DATE=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    END_DATE=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S')
else
    # Linux
    START_DATE=$(date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%S')
    END_DATE=$(date -u -d '+1 day +2 hours' '+%Y-%m-%dT%H:%M:%S')
fi

# Get device ID from login response
ADMIN_DEVICE_ID=""
login_data="{
    \"email\": \"${ADMIN_EMAIL}\",
    \"password\": \"${ADMIN_PASSWORD}\",
    \"rememberMe\": false
}"
login_response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$login_data" \
    "$BASE_URL/api/v1/auth/login")
ADMIN_DEVICE_ID=$(echo "$login_response" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_DEVICE_ID" ]; then
    ADMIN_DEVICE_ID="test-device-$(date +%s | cut -c1-8)"
fi

# Unsplash image URLs for event and feed posts
COVER_IMAGE_URL="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1200&h=600&fit=crop&auto=format"
FEED_IMAGE_1="https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800&h=600&fit=crop&auto=format"
FEED_IMAGE_2="https://images.unsplash.com/photo-1511578314322-379afb476865?w=800&h=600&fit=crop&auto=format"
FEED_IMAGE_3="https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800&h=600&fit=crop&auto=format"
FEED_VIDEO_THUMBNAIL="https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=800&h=600&fit=crop&auto=format"

event_data="{
    \"name\": \"Test Event for Scope Feature\",
    \"description\": \"This is a test event to demonstrate the scope feature. Admin users see full details, guests see feed view.\",
    \"eventType\": \"CONFERENCE\",
    \"startDateTime\": \"${START_DATE}\",
    \"endDateTime\": \"${END_DATE}\",
    \"venueRequirements\": \"Test Venue - Conference Room A\",
    \"capacity\": 100,
    \"isPublic\": true,
    \"requiresApproval\": false,
    \"coverImageUrl\": \"${COVER_IMAGE_URL}\",
    \"eventWebsiteUrl\": \"https://example.com/event\",
    \"hashtag\": \"#TestEvent2024\",
    \"venue\": {
        \"address\": \"123 Main Street\",
        \"city\": \"San Francisco\",
        \"state\": \"California\",
        \"country\": \"United States\",
        \"zipCode\": \"94102\",
        \"latitude\": 37.7749,
        \"longitude\": -122.4194,
        \"googlePlaceId\": \"ChIJIQBpAG2ahYAR_6128GcTUEo\",
        \"googlePlaceData\": \"{\\\"name\\\":\\\"Test Venue\\\",\\\"rating\\\":4.5}\"
    }
}"

event_response=$(curl -s -w '%{http_code}' -X POST \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "X-Device-ID: $ADMIN_DEVICE_ID" \
    -H "Content-Type: application/json" \
    -d "$event_data" \
    "$BASE_URL/api/v1/events")

event_http_code="${event_response: -3}"
event_response_body="${event_response%???}"

if [ "$event_http_code" = "201" ] || [ "$event_http_code" = "200" ]; then
    EVENT_ID=$(echo "$event_response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✅ Event created successfully${NC}"
    echo -e "${CYAN}   Event ID: ${EVENT_ID}${NC}"
else
    echo -e "${RED}❌ Failed to create event (HTTP: $event_http_code)${NC}"
    echo -e "${RED}Response: $event_response_body${NC}"
    EVENT_ID=""
fi

echo ""
echo -e "${PURPLE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Test Users and Event Created Successfully!${NC}"
echo -e "${PURPLE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}📋 Test Credentials:${NC}"
echo ""
echo -e "${YELLOW}👑 ADMIN USER (Full Event Details - scope: FULL)${NC}"
echo -e "   Email:    ${GREEN}${ADMIN_EMAIL}${NC}"
echo -e "   Password: ${GREEN}${ADMIN_PASSWORD}${NC}"
echo -e "   Role:     ${GREEN}ADMIN (ROLE_ADMIN)${NC}"
echo -e "   Access:   ${GREEN}Full event management dashboard${NC}"
echo ""
echo -e "${YELLOW}👤 GUEST USER (Feed View - scope: FEED)${NC}"
echo -e "   Email:    ${GREEN}${GUEST_EMAIL}${NC}"
echo -e "   Password: ${GREEN}${GUEST_PASSWORD}${NC}"
echo -e "   Role:     ${GREEN}USER (Regular user)${NC}"
echo -e "   Access:   ${GREEN}Event feed view with posts${NC}"
echo ""

if [ -n "$EVENT_ID" ]; then
    echo -e "${CYAN}📅 TEST EVENT:${NC}"
    echo -e "   Event ID: ${GREEN}${EVENT_ID}${NC}"
    echo -e "   Name:     ${GREEN}Test Event for Scope Feature${NC}"
    echo -e "   Owner:    ${GREEN}${ADMIN_EMAIL}${NC}"
    echo -e "   Cover:    ${GREEN}${COVER_IMAGE_URL}${NC}"
    echo ""
    echo -e "${CYAN}🔗 Test URLs:${NC}"
    echo -e "   Admin View:  ${GREEN}GET /api/v1/events/${EVENT_ID}${NC} (returns scope: FULL)"
    echo -e "   Guest View:  ${GREEN}GET /api/v1/events/${EVENT_ID}${NC} (returns scope: FEED)"
    echo -e "   Feed View:   ${GREEN}GET /api/v1/events/${EVENT_ID}/feed${NC} (always returns scope: FEED)"
    echo ""
    echo -e "${CYAN}🖼️  Sample Unsplash Images for Feed Posts:${NC}"
    echo -e "   Image 1:   ${GREEN}${FEED_IMAGE_1}${NC}"
    echo -e "   Image 2:   ${GREEN}${FEED_IMAGE_2}${NC}"
    echo -e "   Image 3:   ${GREEN}${FEED_IMAGE_3}${NC}"
    echo -e "   Video Thumb: ${GREEN}${FEED_VIDEO_THUMBNAIL}${NC}"
    echo ""
fi

echo -e "${CYAN}🧪 Testing Instructions:${NC}"
echo ""
echo "1. Login as Admin (${ADMIN_EMAIL}):"
echo "   - Should see FULL event details"
echo "   - Can edit, manage, view analytics"
echo ""
echo "2. Login as Guest (${GUEST_EMAIL}):"
echo "   - Should see FEED view"
echo "   - Can view posts, swipe for more"
echo "   - Limited event information"
echo ""
echo "3. Test Feed Endpoint:"
echo "   - GET /api/v1/events/${EVENT_ID}/feed?page=0&size=20"
echo "   - Always returns feed view regardless of user role"
echo ""
echo -e "${GREEN}✨ Ready for frontend testing!${NC}"
echo ""

