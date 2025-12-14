#!/bin/bash

# Feed Posts Endpoints Test Script
# Tests all feed post-related endpoints (TEXT, IMAGE, VIDEO posts with presigned uploads)
# Mirrors the structure/format of test_event_endpoints.sh
#
# Usage:
#   ./test_feeds_endpoints.sh                    # Interactive mode
#   ./test_feeds_endpoints.sh local              # Test localhost:8080
#   ./test_feeds_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_feeds_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "📰 Feed Posts Endpoints Test Script"
    echo "===================================="
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
    echo "Requirements:"
    echo "  - curl command available"
    echo "  - jq command available (for JSON parsing)"
    echo "  - For local testing: Spring Boot app running on port 8080"
    echo ""
    exit 0
}

# Check for help argument
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "📰 Starting Feed Posts Endpoints Test"
echo "====================================="

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
    if [ $# -gt 0 ]; then
        case $1 in
            "local"|"l")
                BASE_URL="http://localhost:8080"
                echo -e "${GREEN}✅ Selected: Local Development (from command line)${NC}"
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
                echo -e "${RED}❌ Invalid argument. Use: $0 local | $0 prod <API_URL>${NC}"
                exit 1
                ;;
        esac
    else
        echo -e "${CYAN}🌍 Choose Testing Environment:${NC}"
        echo "1. Local Development (localhost:8080)"
        echo "2. Production (Custom API URL)"
        echo ""
        read -p "Enter your choice (1 or 2): " choice
        case $choice in
            1)
                BASE_URL="http://localhost:8080"
                ;;
            2)
                read -p "API URL (https://...): " custom_url
                BASE_URL="$custom_url"
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

get_testing_environment "$@"

# Path configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"

# Configuration
REPORT_FILE="${REPORTS_DIR}/feeds_test_report_$(date +%Y%m%d_%H%M%S).md"
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
DEVICE_ID=""
EVENT_ID=""
POST_ID=""
IMAGE_POST_ID=""
VIDEO_POST_ID=""
POST_MEDIA_ID=""
POST_MEDIA_UPLOAD_URL=""
POST_MEDIA_OBJECT_KEY=""
POST_MEDIA_RESOURCE_URL=""
POST_MEDIA_REQUIRED_HEADERS=""
TEST_IMAGE_PATH=""
TEST_IMAGE_SHA256=""
TEST_IMAGE_SIZE=""

verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Requirements check
if ! command -v jq >/dev/null 2>&1; then
    echo -e "${RED}❌ jq is required but not installed.${NC}"
    echo -e "${YELLOW}💡 Install it with: brew install jq${NC}"
    exit 1
fi

# Download an Unsplash image for upload tests
UNSPLASH_IMAGE_URL_DEFAULT="https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=800&q=80"
UNSPLASH_IMAGE_URL="${UNSPLASH_IMAGE_URL:-$UNSPLASH_IMAGE_URL_DEFAULT}"

download_test_image() {
    echo -e "${YELLOW}🖼️  Downloading test image from Unsplash...${NC}"
    TEST_IMAGE_PATH="$(mktemp -t feed-post-test-XXXXXX).jpg"
    if ! curl -sS -L --fail "$UNSPLASH_IMAGE_URL" -o "$TEST_IMAGE_PATH"; then
        echo -e "${RED}❌ Failed to download test image${NC}"
        return 1
    fi
    TEST_IMAGE_SIZE="$(wc -c < "$TEST_IMAGE_PATH" | tr -d ' ')"
    if command -v shasum >/dev/null 2>&1; then
        TEST_IMAGE_SHA256="$(shasum -a 256 "$TEST_IMAGE_PATH" | awk '{print $1}')"
    elif command -v openssl >/dev/null 2>&1; then
        TEST_IMAGE_SHA256="$(openssl dgst -sha256 "$TEST_IMAGE_PATH" | awk '{print $2}')"
    else
        TEST_IMAGE_SHA256=""
    fi
    echo -e "${GREEN}✅ Test image downloaded (${TEST_IMAGE_SIZE} bytes)${NC}"
    return 0
}

extract_post_presigned_fields() {
    local json="$1"
    POST_MEDIA_ID="$(echo "$json" | jq -r '.mediaUpload.mediaId // empty')"
    POST_MEDIA_OBJECT_KEY="$(echo "$json" | jq -r '.mediaUpload.objectKey // empty')"
    POST_MEDIA_UPLOAD_URL="$(echo "$json" | jq -r '.mediaUpload.uploadUrl // empty')"
    POST_MEDIA_RESOURCE_URL="$(echo "$json" | jq -r '.mediaUpload.resourceUrl // empty')"
    POST_MEDIA_REQUIRED_HEADERS="$(echo "$json" | jq -c '.mediaUpload.headers // {}')"
}

presigned_put_upload() {
    local upload_url="$1"
    local headers_json="$2"
    local file_path="$3"

    local header_flags=""
    while IFS=$'\t' read -r k v; do
        if [ -n "$k" ] && [ -n "$v" ] && [ "$k" != "null" ] && [ "$v" != "null" ]; then
            header_flags="$header_flags -H '$k: $v'"
        fi
    done < <(echo "$headers_json" | jq -r 'to_entries[] | "\(.key)\t\(.value)"')

    run_full_url_test "PUT Presigned Upload (S3)" "PUT" "$upload_url" "$header_flags" "$file_path" "200" "Upload bytes to S3 using presigned PUT URL"
    local last_status=$?
    if [ $last_status -eq 0 ]; then
        return 0
    fi
    run_full_url_test "PUT Presigned Upload (S3 - 204)" "PUT" "$upload_url" "$header_flags" "$file_path" "204" "Upload bytes to S3 using presigned PUT URL (204 variant)"
    return $?
}

run_full_url_test() {
    local test_name="$1"
    local method="$2"
    local url="$3"
    local headers="$4"
    local data_file="$5"
    local expected_status="$6"
    local description="$7"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Running: $test_name${NC}"

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    if [ -n "$data_file" ]; then
        curl_cmd="$curl_cmd --data-binary @$data_file"
    fi
    curl_cmd="$curl_cmd '$url'"

    local response
    if ! response=$(eval "$curl_cmd"); then
        echo -e "${RED}❌ Failed to execute curl command${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi

    local http_code="${response: -3}"
    local response_body="${response%???}"

    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi

    {
        echo ""
        echo "### $test_name"
        echo "**Status:** $status_icon $http_code (Expected: $expected_status)"
        echo "**Description:** $description"
        echo "**Endpoint:** $method $url"
        echo "**Request Headers:** $headers"
        if [ -n "$data_file" ]; then
            echo "**Request Body:** (binary file: $data_file)"
        fi
        echo ""
        echo "**Response:**"
        echo "\`\`\`"
        echo "$response_body"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"

    echo ""
}

# Create directory for reports
mkdir -p "$REPORTS_DIR"

# Create report file
cat > "$REPORT_FILE" << EOF
# Feed Posts Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Text Posts | 0 | 0 | 0 | 0% |
| Image Posts | 0 | 0 | 0 | 0% |
| Video Posts | 0 | 0 | 0 | 0% |
| Post Management | 0 | 0 | 0 | 0% |
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
    
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    local temp_data_file=""
    
    if [[ -n "$DEVICE_ID" && "$headers" == *"Authorization:"* && "$headers" != *"X-Device-ID"* ]]; then
        headers="$headers -H 'X-Device-ID: $DEVICE_ID'"
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
    
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi
    
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
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        case "$test_name" in
            "User Login")
                ACCESS_TOKEN=$(echo "$response_body" | jq -r '.accessToken // empty')
                REFRESH_TOKEN=$(echo "$response_body" | jq -r '.refreshToken // empty')
                DEVICE_ID=$(echo "$response_body" | jq -r '.deviceId // empty')
                USER_ID=$(echo "$response_body" | jq -r '.user.id // empty')
                ;;
            "Create Event")
                EVENT_ID=$(echo "$response_body" | jq -r '.event.id // empty')
                ;;
            "Create Text Post")
                POST_ID=$(echo "$response_body" | jq -r '.post.id // empty')
                ;;
            "Create Image Post")
                IMAGE_POST_ID=$(echo "$response_body" | jq -r '.post.id // empty')
                extract_post_presigned_fields "$response_body"
                ;;
            "Create Video Post")
                VIDEO_POST_ID=$(echo "$response_body" | jq -r '.post.id // empty')
                extract_post_presigned_fields "$response_body"
                ;;
        esac
    fi
    
    echo ""
}

wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for service to be ready...${NC}"
    local max_attempts=40
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Service is ready!${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Attempt $attempt/$max_attempts...${NC}"
        sleep 3
        ((attempt++))
    done
    echo -e "${RED}❌ Service failed to respond within expected time${NC}"
    return 1
}

authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"
    
    local registration_data='{
        "email": "'$TEST_USER_EMAIL'",
        "password": "'$TEST_USER_PASSWORD'",
        "confirmPassword": "'$TEST_USER_PASSWORD'"
    }'
    
    local response=$(curl -s -w '%{http_code}' -X POST \
                -H "Content-Type: application/json" \
        -d "$registration_data" \
        "$BASE_URL/api/v1/auth/register")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✅ User registered${NC}"
        verify_email_in_database "$TEST_USER_EMAIL"
    fi
    
    local login_data='{
        "email": "'$TEST_USER_EMAIL'",
        "password": "'$TEST_USER_PASSWORD'",
        "rememberMe": false
    }'
    
    run_test "User Login" "POST" "/api/v1/auth/login" "-H 'Content-Type: application/json'" "$login_data" "200" "Login to obtain access token"
    
    # Complete onboarding if needed
    if [ -n "$ACCESS_TOKEN" ]; then
        local onboarding_check=$(curl -s -X GET \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            "$BASE_URL/api/v1/auth/me")
        local onboarding_required=$(echo "$onboarding_check" | jq -r '.onboardingRequired // false')
        
        if [ "$onboarding_required" = "true" ]; then
            echo -e "${YELLOW}⚠️  Onboarding required - completing profile...${NC}"
            local onboarding_data='{
                "name": "'$TEST_USER_NAME'",
                "username": "testfeeds_'"$(date +%s | cut -c1-8)"'",
                "phoneNumber": "'$TEST_USER_PHONE'",
                "dateOfBirth": "1990-01-01",
                "acceptTerms": true,
                "acceptPrivacy": true,
                "marketingOptIn": false
            }'
            run_test "Complete Onboarding" "POST" "/api/v1/auth/complete-onboarding" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$onboarding_data" "200" "Complete user onboarding"
        fi
    fi
    
    if [ -n "$ACCESS_TOKEN" ]; then
        echo -e "${GREEN}✅ User authenticated${NC}"
        return 0
    fi
    
    echo -e "${RED}❌ Failed to authenticate user${NC}"
    return 1
}

create_test_event() {
    echo -e "${YELLOW}📅 Creating test event...${NC}"
    
    local start_date
    local end_date
    if [[ "$OSTYPE" == "darwin"* ]]; then
        start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S')
    else
        start_date=$(date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%S')
        end_date=$(date -u -d '+1 day +2 hours' '+%Y-%m-%dT%H:%M:%S')
    fi
    
    local event_data
    event_data=$(cat <<EOF
{
  "event": {
    "name": "Feed Test Event",
    "description": "Event created for feed post testing",
    "eventType": "CONFERENCE",
    "startDateTime": "$start_date",
    "endDateTime": "$end_date",
    "venueRequirements": "Test Location - Conference Room A",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false,
    "eventWebsiteUrl": "https://example.com/event",
    "hashtag": "#FeedTestEvent",
    "venue": {
      "address": "123 Main Street",
      "city": "San Francisco",
      "state": "California",
      "country": "United States",
      "zipCode": "94102",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "googlePlaceId": "ChIJIQBpAG2ahYAR_6128GcTUEo",
      "googlePlaceData": "{\"name\":\"Test Venue\",\"rating\":4.5}"
    }
  },
  "coverUpload": {
    "fileName": "initial-cover.jpg",
    "contentType": "image/jpeg",
    "category": "cover",
    "isPublic": true,
    "description": "Initial cover image upload (create flow)"
  }
}
EOF
)
    
    run_test "Create Event" "POST" "/api/v1/events" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json' -H 'X-Device-ID: $DEVICE_ID'" "$event_data" "201" "Create a test event for feed posts"
    
    if [ -n "$EVENT_ID" ]; then
        echo -e "${GREEN}✅ Test event created with ID: $EVENT_ID${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to create test event${NC}"
        return 1
    fi
}

main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test TEXT posts"
    echo "5. Test IMAGE posts (with presigned upload)"
    echo "6. Test VIDEO posts (with presigned upload)"
    echo "7. Test post management (list, get, delete)"
    echo ""
    
    if ! wait_for_service; then
        exit 1
    fi
    
    if ! authenticate_user; then
        exit 1
    fi
    
    if ! download_test_image; then
        echo -e "${RED}❌ Failed to download test image. Exiting.${NC}"
        exit 1
    fi
    
    if ! create_test_event; then
        exit 1
    fi
    
    # Step 1: TEXT Posts
    echo -e "${CYAN}📝 Step 1: TEXT Posts Tests${NC}"
    echo "============================="
    
    local create_text_post='{
        "type": "TEXT",
        "content": "Hello from automated feed test post! This is a text-only post."
    }'
    run_test "Create Text Post" "POST" "/api/v1/events/$EVENT_ID/posts" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$create_text_post" "200" "Create a simple text post"
    
    if [ -n "$POST_ID" ]; then
        run_test "Get Text Post" "GET" "/api/v1/events/$EVENT_ID/posts/$POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get a text post by id"
    fi
    
    echo ""
    
    # Step 2: IMAGE Posts
    echo -e "${CYAN}🖼️  Step 2: IMAGE Posts Tests${NC}"
    echo "=============================="
    
    local create_image_post
    create_image_post=$(cat <<EOF
{
  "type": "IMAGE",
  "content": "Check out this amazing image!",
  "mediaUpload": {
    "fileName": "feed-post-image.jpg",
    "contentType": "image/jpeg",
    "category": "post",
    "isPublic": true,
    "description": "Image post media upload"
  }
}
EOF
)
    run_test "Create Image Post" "POST" "/api/v1/events/$EVENT_ID/posts" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$create_image_post" "200" "Create an image post with presigned upload request"
    
    # Upload image to S3 via presigned URL
    if [ -n "$POST_MEDIA_UPLOAD_URL" ] && [ -n "$TEST_IMAGE_PATH" ] && [ -n "$IMAGE_POST_ID" ]; then
        presigned_put_upload "$POST_MEDIA_UPLOAD_URL" "$POST_MEDIA_REQUIRED_HEADERS" "$TEST_IMAGE_PATH"
        
        # Complete media upload
        if [ -n "$POST_MEDIA_ID" ]; then
            local complete_payload
            complete_payload=$(cat <<EOF
{
  "objectKey": "$POST_MEDIA_OBJECT_KEY",
  "resourceUrl": "$POST_MEDIA_RESOURCE_URL",
  "fileName": "feed-post-image.jpg",
  "contentType": "image/jpeg",
  "category": "post",
  "isPublic": true,
  "description": "Image post media uploaded via presigned S3 URL",
  "tags": "test,feed,image",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
            run_test "Complete Image Post Media Upload" "POST" "/api/v1/events/$EVENT_ID/posts/$IMAGE_POST_ID/media/$POST_MEDIA_ID/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$complete_payload" "200" "Complete image post media upload after S3 upload"
        fi
        
        if [ -n "$IMAGE_POST_ID" ]; then
            run_test "Get Image Post" "GET" "/api/v1/events/$EVENT_ID/posts/$IMAGE_POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get an image post by id"
        fi
    fi
    
    echo ""
    
    # Step 3: VIDEO Posts
    echo -e "${CYAN}🎥 Step 3: VIDEO Posts Tests${NC}"
    echo "=============================="
    
    local create_video_post
    create_video_post=$(cat <<EOF
{
  "type": "VIDEO",
  "content": "Watch this awesome video!",
  "mediaUpload": {
    "fileName": "feed-post-video.mp4",
    "contentType": "video/mp4",
    "category": "post",
    "isPublic": true,
    "description": "Video post media upload"
  }
}
EOF
)
    run_test "Create Video Post" "POST" "/api/v1/events/$EVENT_ID/posts" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$create_video_post" "200" "Create a video post with presigned upload request"
    
    # For video, we'll use the same test image (in real scenario, would be a video file)
    if [ -n "$POST_MEDIA_UPLOAD_URL" ] && [ -n "$TEST_IMAGE_PATH" ] && [ -n "$VIDEO_POST_ID" ]; then
        presigned_put_upload "$POST_MEDIA_UPLOAD_URL" "$POST_MEDIA_REQUIRED_HEADERS" "$TEST_IMAGE_PATH"
        
        # Complete media upload
        if [ -n "$POST_MEDIA_ID" ]; then
            local complete_payload
            complete_payload=$(cat <<EOF
{
  "objectKey": "$POST_MEDIA_OBJECT_KEY",
  "resourceUrl": "$POST_MEDIA_RESOURCE_URL",
  "fileName": "feed-post-video.mp4",
  "contentType": "video/mp4",
  "category": "post",
  "isPublic": true,
  "description": "Video post media uploaded via presigned S3 URL",
  "tags": "test,feed,video",
  "metadata": "{\"source\":\"test\"}"
}
EOF
)
            run_test "Complete Video Post Media Upload" "POST" "/api/v1/events/$EVENT_ID/posts/$VIDEO_POST_ID/media/$POST_MEDIA_ID/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$complete_payload" "200" "Complete video post media upload after S3 upload"
        fi
        
        if [ -n "$VIDEO_POST_ID" ]; then
            run_test "Get Video Post" "GET" "/api/v1/events/$EVENT_ID/posts/$VIDEO_POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get a video post by id"
        fi
    fi
    
    echo ""
    
    # Step 4: Post Management
    echo -e "${CYAN}📋 Step 4: Post Management Tests${NC}"
    echo "=================================="
    
    run_test "List All Posts" "GET" "/api/v1/events/$EVENT_ID/posts" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all posts for event"
    
    # Delete posts (cleanup)
    if [ -n "$VIDEO_POST_ID" ]; then
        run_test "Delete Video Post" "DELETE" "/api/v1/events/$EVENT_ID/posts/$VIDEO_POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete video post"
    fi
    
    if [ -n "$IMAGE_POST_ID" ]; then
        run_test "Delete Image Post" "DELETE" "/api/v1/events/$EVENT_ID/posts/$IMAGE_POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete image post"
    fi
    
    if [ -n "$POST_ID" ]; then
        run_test "Delete Text Post" "DELETE" "/api/v1/events/$EVENT_ID/posts/$POST_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete text post"
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
        echo -e "${GREEN}🎉 All tests passed! Feed posts system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Test interrupted...${NC}"
    if [ -n "$TEST_IMAGE_PATH" ] && [ -f "$TEST_IMAGE_PATH" ]; then
        rm -f "$TEST_IMAGE_PATH"
    fi
    exit 0
}

trap cleanup SIGINT SIGTERM
main "$@"
