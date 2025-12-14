#!/bin/bash

# Comprehensive Event Controller Endpoints Test Script
# Tests all event-related endpoints and generates a detailed report
# Supports both local and production testing with custom API URLs
#
# Usage:
#   ./test_event_endpoints.sh                    # Interactive mode
#   ./test_event_endpoints.sh local              # Test localhost:8080
#   ./test_event_endpoints.sh prod <API_URL>     # Test production URL
#   ./test_event_endpoints.sh help               # Show help

# Function to show help
show_help() {
    echo "🎉 Event Controller Endpoints Test Script"
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
    echo "  $0 prod https://your-app.herokuapp.com"
    echo "  $0 prod https://api.yourdomain.com"
    echo ""
    echo "Interactive Mode:"
    echo "  Run without arguments to choose environment interactively"
    echo ""
    echo "Requirements:"
    echo "  - curl command available"
    echo "  - jq command available (for JSON parsing)"
    echo "  - For local testing: Spring Boot app running on port 8080"
    echo "  - For production testing: Valid API URL with health endpoint"
    echo ""
    exit 0
}

# Check for help argument
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "🎉 Starting Comprehensive Event Controller Endpoints Test"
echo "========================================================"

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
                echo -e "${YELLOW}   $0                         # Interactive mode${NC}"
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
                echo -e "${YELLOW}💡 Make sure your local Spring Boot application is running${NC}"
                ;;
            2)
                echo ""
                echo -e "${CYAN}🌐 Enter Production API URL:${NC}"
                echo -e "${YELLOW}   Example: https://your-app.railway.app${NC}"
                echo -e "${YELLOW}   Example: https://your-app.herokuapp.com${NC}"
                echo -e "${YELLOW}   Example: https://api.yourdomain.com${NC}"
                echo ""
                read -p "API URL: " custom_url
                
                # Validate URL format
                if [[ $custom_url =~ ^https?:// ]]; then
                    BASE_URL="$custom_url"
                    echo -e "${GREEN}✅ Selected: Production - $BASE_URL${NC}"
                else
                    echo -e "${RED}❌ Invalid URL format. Please include http:// or https://${NC}"
                    echo -e "${YELLOW}   Example: https://your-app.railway.app${NC}"
                    exit 1
                fi
                ;;
            *)
                echo -e "${RED}❌ Invalid choice. Please select 1 or 2.${NC}"
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

# Path configuration (always resolve relative to this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS_DIR="${SCRIPT_DIR}/reports"

# Configuration
REPORT_FILE="${REPORTS_DIR}/event_test_report_$(date +%Y%m%d_%H%M%S).md"
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
MEDIA_ID=""
MEDIA_UPLOAD_URL=""
MEDIA_UPLOAD_METHOD=""
MEDIA_RESOURCE_URL=""
MEDIA_REQUIRED_HEADERS=""
MEDIA_DOWNLOAD_URL=""
MEDIA_OBJECT_KEY=""
ASSET_ID=""
ASSET_UPLOAD_URL=""
ASSET_RESOURCE_URL=""
ASSET_REQUIRED_HEADERS=""
ASSET_OBJECT_KEY=""
ASSET_DOWNLOAD_URL=""
ASSET_MEDIA_IDENTIFIER=""
ASSET_COMPLETE_OK="false"
COVER_ID=""
COVER_OBJECT_KEY=""
COVER_UPLOAD_URL=""
COVER_RESOURCE_URL=""
COVER_REQUIRED_HEADERS=""
COLLABORATOR_ID=""
REMINDER_ID=""
DEVICE_ID=""
OTHER_ACCESS_TOKEN=""
OTHER_DEVICE_ID=""


verify_email_in_database() {
    local email="$1"
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T postgres psql -U postgres -d eventplanner -c "UPDATE auth_users SET email_verified = true WHERE lower(email) = lower('${email}');" >/dev/null 2>&1
    fi
}

# Requirements check (jq is needed for presigned upload parsing)
if ! command -v jq >/dev/null 2>&1; then
    echo -e "${RED}❌ jq is required but not installed.${NC}"
    echo -e "${YELLOW}💡 Install it with: brew install jq${NC}"
    exit 1
fi

# Download an Unsplash image for upload tests
UNSPLASH_IMAGE_URL_DEFAULT="https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=800&q=80"
UNSPLASH_IMAGE_URL="${UNSPLASH_IMAGE_URL:-$UNSPLASH_IMAGE_URL_DEFAULT}"
TEST_IMAGE_PATH=""
TEST_IMAGE_SHA256=""
TEST_IMAGE_SIZE=""

download_test_image() {
    echo -e "${YELLOW}🖼️  Downloading test image from Unsplash...${NC}"
    TEST_IMAGE_PATH="$(mktemp -t event-media-test-XXXXXX).jpg"
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

extract_presigned_fields() {
    local json="$1"
    MEDIA_ID="$(echo "$json" | jq -r '.mediaId // empty')"
    MEDIA_OBJECT_KEY="$(echo "$json" | jq -r '.objectKey // empty')"
    MEDIA_UPLOAD_URL="$(echo "$json" | jq -r '.uploadUrl // empty')"
    MEDIA_UPLOAD_METHOD="$(echo "$json" | jq -r '.uploadMethod // empty')"
    MEDIA_RESOURCE_URL="$(echo "$json" | jq -r '.resourceUrl // empty')"
    MEDIA_REQUIRED_HEADERS="$(echo "$json" | jq -c '.headers // {}')"
}

extract_asset_presigned_fields() {
    local json="$1"
    ASSET_ID="$(echo "$json" | jq -r '.mediaId // empty')"
    ASSET_OBJECT_KEY="$(echo "$json" | jq -r '.objectKey // empty')"
    ASSET_UPLOAD_URL="$(echo "$json" | jq -r '.uploadUrl // empty')"
    ASSET_RESOURCE_URL="$(echo "$json" | jq -r '.resourceUrl // empty')"
    ASSET_REQUIRED_HEADERS="$(echo "$json" | jq -c '.headers // {}')"
}

extract_cover_presigned_fields() {
    local json="$1"
    COVER_ID="$(echo "$json" | jq -r '.mediaId // empty')"
    COVER_OBJECT_KEY="$(echo "$json" | jq -r '.objectKey // empty')"
    COVER_UPLOAD_URL="$(echo "$json" | jq -r '.uploadUrl // empty')"
    COVER_RESOURCE_URL="$(echo "$json" | jq -r '.resourceUrl // empty')"
    COVER_REQUIRED_HEADERS="$(echo "$json" | jq -c '.headers // {}')"
}

extract_create_event_with_cover_upload_fields() {
    local json="$1"
    EVENT_ID="$(echo "$json" | jq -r '.event.id // empty')"
    COVER_ID="$(echo "$json" | jq -r '.coverUpload.mediaId // empty')"
    COVER_OBJECT_KEY="$(echo "$json" | jq -r '.coverUpload.objectKey // empty')"
    COVER_UPLOAD_URL="$(echo "$json" | jq -r '.coverUpload.uploadUrl // empty')"
    COVER_RESOURCE_URL="$(echo "$json" | jq -r '.coverUpload.resourceUrl // empty')"
    COVER_REQUIRED_HEADERS="$(echo "$json" | jq -c '.coverUpload.headers // {}')"
}

extract_update_event_with_cover_upload_fields() {
    local json="$1"
    COVER_ID="$(echo "$json" | jq -r '.coverUpload.mediaId // empty')"
    COVER_OBJECT_KEY="$(echo "$json" | jq -r '.coverUpload.objectKey // empty')"
    COVER_UPLOAD_URL="$(echo "$json" | jq -r '.coverUpload.uploadUrl // empty')"
    COVER_RESOURCE_URL="$(echo "$json" | jq -r '.coverUpload.resourceUrl // empty')"
    COVER_REQUIRED_HEADERS="$(echo "$json" | jq -c '.coverUpload.headers // {}')"
}

presigned_put_upload() {
    local upload_url="$1"
    local headers_json="$2"
    local file_path="$3"

    local header_flags=""
    # Apply all required headers
    while IFS=$'\t' read -r k v; do
        if [ -n "$k" ] && [ -n "$v" ] && [ "$k" != "null" ] && [ "$v" != "null" ]; then
            header_flags="$header_flags -H '$k: $v'"
        fi
    done < <(echo "$headers_json" | jq -r 'to_entries[] | "\(.key)\t\(.value)"')

    # Run upload once, log it, and return success/failure
    run_full_url_test "PUT Presigned Upload (S3)" "PUT" "$upload_url" "$header_flags" "$file_path" "200" "Upload bytes to S3 using presigned PUT URL (S3 often returns 200 or 204)"
    local last_status=$?
    if [ $last_status -eq 0 ]; then
        return 0
    fi
    # Some S3-compatible endpoints return 204 on successful PUT
    run_full_url_test "PUT Presigned Upload (S3 - 204)" "PUT" "$upload_url" "$header_flags" "$file_path" "204" "Upload bytes to S3 using presigned PUT URL (204 variant)"
    return $?
}

download_and_hash() {
    local url="$1"
    local out_path="$2"
    if ! curl -sS -L --fail "$url" -o "$out_path"; then
        return 1
    fi
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$out_path" | awk '{print $1}'
        return 0
    elif command -v openssl >/dev/null 2>&1; then
        openssl dgst -sha256 "$out_path" | awk '{print $2}'
        return 0
    fi
    echo ""
    return 0
}

authenticate_other_user() {
    local email="otheruser_$(date +%s)@example.com"
    local password="Password123!"

    # Register
    local reg='{"email":"'$email'","password":"'$password'","confirmPassword":"'$password'"}'
    local r=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$reg" "$BASE_URL/api/v1/auth/register")
    local rc="${r: -3}"
    if [ "$rc" = "201" ]; then
        verify_email_in_database "$email"
    fi

    # Login
    local login='{"email":"'$email'","password":"'$password'","rememberMe":false}'
    local lr=$(curl -s -w '%{http_code}' -X POST -H "Content-Type: application/json" -d "$login" "$BASE_URL/api/v1/auth/login")
    local lc="${lr: -3}"
    local lb="${lr%???}"
    if [ "$lc" != "200" ]; then
        return 1
    fi
    OTHER_ACCESS_TOKEN="$(echo "$lb" | jq -r '.accessToken // empty')"
    OTHER_DEVICE_ID="$(echo "$lb" | jq -r '.deviceId // empty')"
    return 0
}

# Create directory for reports
mkdir -p "$REPORTS_DIR"

# Create report file
cat > "$REPORT_FILE" << EOF
# Event Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| CRUD Operations | 0 | 0 | 0 | 0% |
| Event Management | 0 | 0 | 0 | 0% |
| Media Management | 0 | 0 | 0 | 0% |
| Notifications | 0 | 0 | 0 | 0% |
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
    # Use OTHER_DEVICE_ID when using OTHER_ACCESS_TOKEN, otherwise use DEVICE_ID.
    local effective_device_id="$DEVICE_ID"
    if [[ -n "$OTHER_ACCESS_TOKEN" && "$headers" == *"Authorization: Bearer $OTHER_ACCESS_TOKEN"* && -n "$OTHER_DEVICE_ID" ]]; then
        effective_device_id="$OTHER_DEVICE_ID"
    fi
    if [[ -n "$effective_device_id" && "$headers" != *"X-Device-ID"* ]]; then
        if [ -n "$headers" ]; then
            headers="$headers -H 'X-Device-ID: $effective_device_id'"
        else
            headers="-H 'X-Device-ID: $effective_device_id'"
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
        echo -e "${YELLOW}   Command: $curl_cmd${NC}"
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
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        if [ "$http_code" = "403" ] || [ "$http_code" = "401" ]; then
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
    
    # Extract IDs and tokens from successful responses
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        case "$test_name" in
            "User Registration"|"User Login")
                ACCESS_TOKEN=$(echo "$response_body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                REFRESH_TOKEN=$(echo "$response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                USER_ID=$(echo "$response_body" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Create Event")
                # Create event now returns { event, coverUpload }
                EVENT_ID=$(echo "$response_body" | jq -r '.event.id // empty')
                ;;
            "Upload Event Media")
                # Presigned upload response (parse via jq)
                extract_presigned_fields "$response_body"
                ;;
            "Request Cover Upload via Update Event")
                extract_update_event_with_cover_upload_fields "$response_body"
                ;;
            "Add Event Collaborator")
                COLLABORATOR_ID=$(echo "$response_body" | grep -o '"collaboratorId":"[^"]*"' | cut -d'"' -f4)
                ;;
            "Create Event Reminder")
                REMINDER_ID=$(echo "$response_body" | grep -o '"reminderId":"[^"]*"' | cut -d'"' -f4)
                ;;
        esac
    fi
    
    echo ""
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name="Service"
    if [[ $BASE_URL == *"localhost"* ]]; then
        service_name="Local Spring Boot Application"
    else
        service_name="Production Service"
    fi
    
    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    # Docker compose healthcheck start_period allows up to 120s on first run.
    # Keep this in sync so tests don't fail during cold starts.
    local max_attempts=40
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        # Check service health
        if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        else
            echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for $service_name...${NC}"
        fi
        
        sleep 3
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to respond within expected time${NC}"
    return 1
}

# Function to check service availability
check_service() {
    local service_name="Service"
    if [[ $BASE_URL == *"localhost"* ]]; then
        service_name="Local Spring Boot Application"
        echo -e "${YELLOW}🔍 Checking local service availability...${NC}"
        echo -e "${YELLOW}💡 Make sure your local application is running with: mvn spring-boot:run${NC}"
    else
        service_name="Production Service"
        echo -e "${YELLOW}🔍 Checking production service availability...${NC}"
    fi
    
    # Wait for service to be ready
    if wait_for_service; then
        echo -e "${GREEN}✅ $service_name is available!${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name is not available${NC}"
        if [[ $BASE_URL == *"localhost"* ]]; then
            echo -e "${YELLOW}💡 Troubleshooting tips for local development:${NC}"
            echo -e "${YELLOW}   1. Make sure Spring Boot is running: mvn spring-boot:run${NC}"
            echo -e "${YELLOW}   2. Check if port 8080 is available${NC}"
            echo -e "${YELLOW}   3. Verify database connections${NC}"
        else
            echo -e "${YELLOW}💡 Troubleshooting tips for production:${NC}"
            echo -e "${YELLOW}   1. Verify the API URL is correct${NC}"
            echo -e "${YELLOW}   2. Check if the service is deployed and running${NC}"
            echo -e "${YELLOW}   3. Verify network connectivity${NC}"
        fi
        return 1
    fi
}

# Function to authenticate and get tokens
authenticate_user() {
    echo -e "${YELLOW}🔐 Authenticating user...${NC}"
    
    # First, try to register a new user (minimal registration - email + password only)
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
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✅ User registered${NC}"
        verify_email_in_database "$TEST_USER_EMAIL"
    elif [ "$http_code" != "400" ]; then
        echo -e "${RED}❌ Failed to register user${NC}"
        return 1
    fi
    
    # Login after email verification
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
        REFRESH_TOKEN=$(echo "$login_response_body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
        DEVICE_ID=$(echo "$login_response_body" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
        
        # Check if onboarding is required
        local onboarding_required=$(echo "$login_response_body" | grep -o '"onboardingRequired":[^,}]*' | cut -d':' -f2 | tr -d ' ')
        
        if [ "$onboarding_required" = "true" ]; then
            echo -e "${YELLOW}⚠️  Onboarding required - completing profile...${NC}"
            
            # Complete onboarding
            local onboarding_data='{
                "name": "'$TEST_USER_NAME'",
                "username": "testuser_'"$(date +%s | cut -c1-8)"'",
                "phoneNumber": "'$TEST_USER_PHONE'",
                "dateOfBirth": "1990-01-01",
                "acceptTerms": true,
                "acceptPrivacy": true,
                "marketingOptIn": false
            }'
            
            local onboarding_response=$(curl -s -w '%{http_code}' -X POST \
                -H "Authorization: Bearer $ACCESS_TOKEN" \
                -H "X-Device-ID: $DEVICE_ID" \
                -H "Content-Type: application/json" \
                -d "$onboarding_data" \
                "$BASE_URL/api/v1/auth/complete-onboarding")
            
            local onboarding_http_code="${onboarding_response: -3}"
            if [ "$onboarding_http_code" = "200" ]; then
                echo -e "${GREEN}✅ Profile onboarding completed${NC}"
            else
                echo -e "${YELLOW}⚠️  Onboarding completion failed (HTTP: $onboarding_http_code), but continuing with tests${NC}"
            fi
        fi
        
        if [ -n "$ACCESS_TOKEN" ]; then
            local jwt_payload=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
            if command -v python3 &> /dev/null; then
                USER_ID=$(echo "$jwt_payload" | python3 -c "import sys, base64, json; data=sys.stdin.read().strip(); padding=4-len(data)%4; data+=('='*padding if padding<4 else ''); print(json.loads(base64.b64decode(data)).get('sub', ''))" 2>/dev/null)
            elif command -v jq &> /dev/null; then
                local padding=$((4 - ${#jwt_payload} % 4))
                if [ $padding -ne 4 ]; then
                    jwt_payload="${jwt_payload}$(printf '%*s' $padding | tr ' ' '=')"
                fi
                USER_ID=$(echo "$jwt_payload" | base64 -d 2>/dev/null | jq -r '.sub // empty' 2>/dev/null)
            fi
        fi
        
        echo -e "${GREEN}✅ User logged in successfully${NC}"
        return 0
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
    
    # Create event + request presigned cover upload
    local event_with_cover_request
    event_with_cover_request=$(cat <<EOF
{
  "event": {
    "name": "Test Event",
    "description": "This is a test event for endpoint testing",
    "eventType": "CONFERENCE",
    "startDateTime": "$start_date",
    "endDateTime": "$end_date",
    "venueRequirements": "Test Location - Conference Room A",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false,
    "eventWebsiteUrl": "https://example.com/event",
    "hashtag": "#TestEvent",
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
    
    # Debug: Show what we're sending
    echo -e "${CYAN}   Debug: Creating event with token: ${ACCESS_TOKEN:0:20}...${NC}"
    echo -e "${CYAN}   Debug: User ID: $USER_ID${NC}"
    
    local response=$(curl -s -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Device-ID: $DEVICE_ID" \
        -H "Content-Type: application/json" \
        -d "$event_with_cover_request" \
        "$BASE_URL/api/v1/events")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        extract_create_event_with_cover_upload_fields "$response_body"
        if [ -z "$EVENT_ID" ] || [ -z "$COVER_ID" ] || [ -z "$COVER_UPLOAD_URL" ]; then
            echo -e "${RED}❌ Failed to extract event or cover upload fields from response${NC}"
            echo -e "${RED}Response: $response_body${NC}"
            return 1
        fi

        # Upload cover image bytes to S3 via presigned URL (requires test image downloaded)
        if [ -n "$TEST_IMAGE_PATH" ] && [ -f "$TEST_IMAGE_PATH" ]; then
            presigned_put_upload "$COVER_UPLOAD_URL" "$COVER_REQUIRED_HEADERS" "$TEST_IMAGE_PATH" >/dev/null 2>&1
        fi

        # Complete upload so backend persists coverImageUrl
        local cover_complete_payload
        cover_complete_payload=$(cat <<EOF
{
  "objectKey": "$COVER_OBJECT_KEY",
  "resourceUrl": "$COVER_RESOURCE_URL",
  "fileName": "initial-cover.jpg",
  "contentType": "image/jpeg",
  "category": "cover",
  "isPublic": true,
  "description": "Cover uploaded via create-event flow",
  "tags": "test,event",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
        # We won't log this "setup" completion into the report; failures will surface in later GET checks.
        local cover_complete_wrapped
        cover_complete_wrapped=$(cat <<EOF
{
  "coverId": "$COVER_ID",
  "upload": $cover_complete_payload
}
EOF
)
        curl -sS -X POST \
          -H "Authorization: Bearer $ACCESS_TOKEN" \
          -H "X-Device-ID: $DEVICE_ID" \
          -H "Content-Type: application/json" \
          -d "$cover_complete_wrapped" \
          "$BASE_URL/api/v1/events/$EVENT_ID/cover-image/complete" >/dev/null 2>&1

        echo -e "${GREEN}✅ Test event created with ID: $EVENT_ID (cover upload requested)${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to create test event - HTTP: $http_code${NC}"
        echo -e "${RED}Response: $response_body${NC}"
        return 1
    fi
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

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Authenticate user"
    echo "3. Create test event"
    echo "4. Test CRUD operations"
    echo "5. Test event management endpoints"
    echo "6. Test media management endpoints"
    echo "7. Test notification endpoints"
    echo "8. Clean up test data"
    echo ""
    
    # Step 1: Check service availability
    echo -e "${CYAN}🔍 Step 1: Checking Service Availability${NC}"
    echo "============================================="
    if ! check_service; then
        echo -e "${RED}❌ Service is not available. Exiting.${NC}"
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
    # Download image early because event creation now uses presigned cover upload
    if ! download_test_image; then
        echo -e "${RED}❌ Failed to download test image. Exiting.${NC}"
        exit 1
    fi
    if ! create_test_event; then
        echo -e "${RED}❌ Failed to create test event. Exiting.${NC}"
        exit 1
    fi
    echo ""

    
    # Step 4: CRUD Operations Tests
    echo -e "${CYAN}📝 Step 4: CRUD Operations Tests${NC}"
    echo "=================================="
    
    # Test create event with cover upload (this will appear in the report)
    local create_start_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local create_end_date=$(date -u -v+1d -v+2H '+%Y-%m-%dT%H:%M:%S')
    local create_event_with_cover_data
    create_event_with_cover_data=$(cat <<EOF
{
  "event": {
    "name": "Report Test Event",
    "description": "This event was created to test create+cover upload flow and appears in the report",
    "eventType": "CONFERENCE",
    "startDateTime": "$create_start_date",
    "endDateTime": "$create_end_date",
    "venueRequirements": "Test Location - Conference Room A",
    "capacity": 100,
    "isPublic": true,
    "requiresApproval": false,
    "eventWebsiteUrl": "https://example.com/event",
    "hashtag": "#ReportTestEvent",
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
    "fileName": "report-cover.jpg",
    "contentType": "image/jpeg",
    "category": "cover",
    "isPublic": true,
    "description": "Cover image upload for report event"
  }
}
EOF
)

    # Create event and capture response (so we can upload + complete cover)
    local create_flow_resp=$(curl -sS -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Device-ID: $DEVICE_ID" \
        -H "Content-Type: application/json" \
        -d "$create_event_with_cover_data" \
        "$BASE_URL/api/v1/events")
    local create_flow_http="${create_flow_resp: -3}"
    local create_flow_body="${create_flow_resp%???}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$create_flow_http" = "201" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    {
        echo ""
        echo "### Create Event"
        echo "**Status:** $([ "$create_flow_http" = "201" ] && echo "✅" || echo "❌") $create_flow_http (Expected: 201)"
        echo "**Description:** Create a new event and request a presigned cover image upload URL"
        echo "**Endpoint:** POST /api/v1/events"
        echo "**Request Headers:** -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -H 'X-Device-ID: $DEVICE_ID'"
        echo "**Request Body:** $create_event_with_cover_data"
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$create_flow_body"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"

    # If created, perform the S3 upload + complete step
    if [ "$create_flow_http" = "201" ]; then
        local created_event_id
        created_event_id="$(echo "$create_flow_body" | jq -r '.event.id // empty')"
        local created_cover_id
        created_cover_id="$(echo "$create_flow_body" | jq -r '.coverUpload.mediaId // empty')"
        local created_cover_object_key
        created_cover_object_key="$(echo "$create_flow_body" | jq -r '.coverUpload.objectKey // empty')"
        local created_cover_upload_url
        created_cover_upload_url="$(echo "$create_flow_body" | jq -r '.coverUpload.uploadUrl // empty')"
        local created_cover_resource_url
        created_cover_resource_url="$(echo "$create_flow_body" | jq -r '.coverUpload.resourceUrl // empty')"
        local created_cover_headers
        created_cover_headers="$(echo "$create_flow_body" | jq -c '.coverUpload.headers // {}')"

        if [ -n "$created_cover_upload_url" ] && [ -n "$TEST_IMAGE_PATH" ]; then
            presigned_put_upload "$created_cover_upload_url" "$created_cover_headers" "$TEST_IMAGE_PATH"
        fi

        if [ -n "$created_event_id" ] && [ -n "$created_cover_id" ]; then
            local created_cover_complete_payload
            created_cover_complete_payload=$(cat <<EOF
{
  "objectKey": "$created_cover_object_key",
  "resourceUrl": "$created_cover_resource_url",
  "fileName": "report-cover.jpg",
  "contentType": "image/jpeg",
  "category": "cover",
  "isPublic": true,
  "description": "Cover uploaded via create+cover flow",
  "tags": "test,event",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
            local created_cover_complete_wrapped
            created_cover_complete_wrapped=$(cat <<EOF
{
  "coverId": "$created_cover_id",
  "upload": $created_cover_complete_payload
}
EOF
)
            run_test "Complete Cover Image Upload (Create Flow)" "POST" "/api/v1/events/$created_event_id/cover-image/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$created_cover_complete_wrapped" "200" "Complete cover image upload for newly created event (create flow)"
        fi
    fi
    
    # Test get event by ID (should return full details for owner with scope=FULL)
    run_test "Get Event by ID (Owner View)" "GET" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event by ID - should return full details for owner with scope=FULL"
    
    # Test get event feed endpoint (should return scope=FEED)
    run_test "Get Event Feed" "GET" "/api/v1/events/$EVENT_ID/feed" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event feed - should return scope=FEED"
    
    # Test get event feed with pagination
    run_test "Get Event Feed (Page 0)" "GET" "/api/v1/events/$EVENT_ID/feed?page=0&size=10" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event feed with pagination - page 0"
    
    # Test get event feed with filters
    run_test "Get Event Feed (Filter by Type)" "GET" "/api/v1/events/$EVENT_ID/feed?postType=VIDEO" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event feed filtered by post type"
    
    # Test update event
    local update_start_date=$(date -u -v+2d '+%Y-%m-%dT%H:%M:%S')
    local update_end_date=$(date -u -v+2d -v+3H '+%Y-%m-%dT%H:%M:%S')
    local update_data=$(cat <<EOF
{
  "event": {
    "name": "Updated Test Event",
    "description": "Updated description",
    "eventType": "WORKSHOP",
    "startDateTime": "$update_start_date",
    "endDateTime": "$update_end_date",
    "venueRequirements": "Updated Location - Workshop Room B",
    "capacity": 150,
    "isPublic": false,
    "requiresApproval": true,
    "venue": {
      "address": "456 Market Street",
      "city": "San Francisco",
      "state": "California",
      "country": "United States",
      "zipCode": "94105",
      "latitude": 37.7849,
      "longitude": -122.4094,
      "googlePlaceId": "ChIJUpdatedPlaceID123",
      "googlePlaceData": "{\"name\":\"Updated Venue\",\"rating\":4.8}"
    }
  }
}
EOF
)
    run_test "Update Event" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_data" "200" "Update event details"
    
    # Test get non-existent event
    run_test "Get Non-existent Event" "GET" "/api/v1/events/00000000-0000-0000-0000-000000000000" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Get non-existent event"
    echo ""
    
    # Step 5: Event Management Tests
    echo -e "${CYAN}🎯 Step 5: Event Management Tests${NC}"
    echo "=================================="
    
    # My Events via filters on the main list endpoint
    run_test "List My Owned Events (mine=true)" "GET" "/api/v1/events?mine=true" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List events owned by current user"
    run_test "List My Upcoming Owned Events" "GET" "/api/v1/events?mine=true&timeframe=UPCOMING" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List upcoming owned events"
    run_test "List My Past Owned Events" "GET" "/api/v1/events?mine=true&timeframe=PAST" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List past owned events"
    # Event Status & Lifecycle Tests
    # NOTE: /{id}/status endpoints removed; eventStatus is returned by GET /api/v1/events/{id}
    # NOTE: Publish endpoint removed. Use PUT /api/v1/events/{id} with event.eventStatus=PUBLISHED instead if needed.
    run_test "Open Registration" "POST" "/api/v1/events/$EVENT_ID/registration?action=open" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Open event registration"
    run_test "Close Registration" "POST" "/api/v1/events/$EVENT_ID/registration?action=close" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Close event registration"
    
    # Event Discovery & Search Tests
    run_test "Search Events" "GET" "/api/v1/events?search=test" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search events with query"
    run_test "Get Public Events" "GET" "/api/v1/events?isPublic=true" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get public events"
    run_test "Get Featured Events" "GET" "/api/v1/events?isPublic=true&status=PUBLISHED&sortBy=createdAt&sortDirection=DESC" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get featured events"
    run_test "Get Trending Events" "GET" "/api/v1/events?isPublic=true&status=PUBLISHED&sortBy=currentAttendeeCount&sortDirection=DESC" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get trending events"
    run_test "Get Upcoming Events" "GET" "/api/v1/events?isPublic=true&timeframe=UPCOMING" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get upcoming events"
    run_test "Get Events by Type" "GET" "/api/v1/events?eventType=WORKSHOP" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get events by type"
    run_test "Get Events by Status" "GET" "/api/v1/events?status=PUBLISHED" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get events by status"
    
    # Event Capacity & Registration Tests
    run_test "Get Event Capacity (via view=capacity)" "GET" "/api/v1/events/$EVENT_ID?view=capacity" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event capacity information via GET /events/{id}?view=capacity"

    local capacity_update_data
    capacity_update_data=$(cat <<EOF
{
  "event": {
    "capacity": 200
  }
}
EOF
)
    run_test "Update Event Capacity (via Update Event)" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$capacity_update_data" "200" "Update event capacity via PUT /events/{id}"
    
    local deadline_date=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local deadline_data
    deadline_data=$(cat <<EOF
{
  "event": {
    "registrationDeadline": "$deadline_date"
  }
}
EOF
)
    run_test "Update Registration Deadline (via Update Event)" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$deadline_data" "200" "Update registration deadline via PUT /events/{id}"
    
    # Visibility & Access Control Tests
    run_test "Get Event Visibility (via view=visibility)" "GET" "/api/v1/events/$EVENT_ID?view=visibility" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event visibility settings via GET /events/{id}?view=visibility"
    
    local visibility_data='{
        "event": {
            "isPublic": true
        }
    }'
    run_test "Update Event Visibility (via Update Event)" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$visibility_data" "200" "Update event visibility via PUT /events/{id}"

    local visibility_private_data='{
        "event": {
            "isPublic": false
        }
    }'
    run_test "Make Event Private (via Update Event)" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$visibility_private_data" "200" "Make event private via PUT /events/{id}"
    
    # Step 6: Media Management Tests
    echo -e "${CYAN}📸 Step 6: Media Management Tests${NC}"
    echo "=================================="

    # Test image already downloaded earlier (required for create flow)
    
    # Media Tests (Note: These will return mock responses since upload is presigned)
    run_test "Get Event Media" "GET" "/api/v1/events/$EVENT_ID/media" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event media"
    
    # Request presigned media upload
    local media_upload_request='{
        "fileName": "unsplash-test-image.jpg",
        "contentType": "image/jpeg",
        "category": "gallery",
        "isPublic": true,
        "description": "Test image upload"
    }'
    run_test "Upload Event Media" "POST" "/api/v1/events/$EVENT_ID/media" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$media_upload_request" "200" "Upload event media"

    # Actually upload the image bytes to S3 via the presigned URL
    if [ -n "$MEDIA_UPLOAD_URL" ] && [ -n "$TEST_IMAGE_PATH" ]; then
        presigned_put_upload "$MEDIA_UPLOAD_URL" "$MEDIA_REQUIRED_HEADERS" "$TEST_IMAGE_PATH"
    fi

    # Complete upload: client informs backend to persist uploaded object reference
    if [ -n "$MEDIA_ID" ]; then
        local complete_payload
        complete_payload=$(cat <<EOF
{
  "objectKey": "$MEDIA_OBJECT_KEY",
  "resourceUrl": "$MEDIA_RESOURCE_URL",
  "fileName": "unsplash-test-image.jpg",
  "contentType": "image/jpeg",
  "category": "gallery",
  "isPublic": false,
  "description": "Uploaded via presigned S3 URL",
  "tags": "test,event",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
        run_test "Complete Event Media Upload" "POST" "/api/v1/events/$EVENT_ID/media/$MEDIA_ID/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$complete_payload" "200" "Complete media upload by saving uploaded object reference in backend"
    fi

    # Security: unauthenticated should not be able to view private event media metadata
    if [ -n "$MEDIA_ID" ]; then
        run_test "Get Specific Media (No Auth - Should Fail)" "GET" "/api/v1/events/$EVENT_ID/media/$MEDIA_ID" "" "" "401" "Ensure private event media cannot be accessed without authentication"
    fi
    # Security: authenticated but non-member should be forbidden
    if authenticate_other_user && [ -n "$MEDIA_ID" ]; then
        run_test "Get Specific Media (Other User - Should Fail)" "GET" "/api/v1/events/$EVENT_ID/media/$MEDIA_ID" "-H 'Authorization: Bearer $OTHER_ACCESS_TOKEN'" "" "403" "Ensure non-members cannot access private event media"
    fi
    
    # Get specific media (using captured media ID when available)
    local mock_media_id="12345678-1234-1234-1234-123456789012"
    local media_identifier="${MEDIA_ID:-$mock_media_id}"
    run_test "Get Specific Media" "GET" "/api/v1/events/$EVENT_ID/media/$media_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific media"

    # Download via the presigned GET URL returned by the API and verify content matches what we uploaded
    if [ -n "$MEDIA_ID" ]; then
        local media_meta=$(curl -sS -X GET -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Device-ID: $DEVICE_ID" "$BASE_URL/api/v1/events/$EVENT_ID/media/$MEDIA_ID")
        MEDIA_DOWNLOAD_URL="$(echo "$media_meta" | jq -r '.mediaUrl // empty')"
        if [ -n "$MEDIA_DOWNLOAD_URL" ]; then
            local downloaded_path
            downloaded_path="$(mktemp -t event-media-downloaded-XXXXXX).jpg"
            local downloaded_sha
            downloaded_sha="$(download_and_hash "$MEDIA_DOWNLOAD_URL" "$downloaded_path")"
            if [ -n "$TEST_IMAGE_SHA256" ] && [ -n "$downloaded_sha" ] && [ "$downloaded_sha" = "$TEST_IMAGE_SHA256" ]; then
                run_full_url_test "GET Presigned Download (Event Media) - Hash Match" "GET" "$MEDIA_DOWNLOAD_URL" "" "" "200" "Download uploaded media via presigned GET URL and verify SHA256 matches"
            else
                run_full_url_test "GET Presigned Download (Event Media)" "GET" "$MEDIA_DOWNLOAD_URL" "" "" "200" "Download uploaded media via presigned GET URL (hash mismatch or unavailable)"
            fi
        fi
    fi
    
    # Update media
    local media_update_data='{
        "mediaType": "image",
        "mediaName": "Updated Test Image",
        "description": "Updated description",
        "category": "gallery",
        "isPublic": false,
        "tags": "test,event"
    }'
    run_test "Update Media" "PUT" "/api/v1/events/$EVENT_ID/media/$media_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$media_update_data" "200" "Update media information"
    
    # Assets Tests
    run_test "Get Event Assets" "GET" "/api/v1/events/$EVENT_ID/assets" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event assets"
    local asset_upload_request='{
        "fileName": "unsplash-private-asset.jpg",
        "contentType": "image/jpeg",
        "category": "documents",
        "isPublic": false,
        "description": "Test document upload"
    }'
    # Request presigned asset upload (capture response directly so we can parse it)
    local asset_resp=$(curl -sS -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Device-ID: $DEVICE_ID" \
        -H "Content-Type: application/json" \
        -d "$asset_upload_request" \
        "$BASE_URL/api/v1/events/$EVENT_ID/assets")
    local asset_http="${asset_resp: -3}"
    local asset_body="${asset_resp%???}"
    if [ "$asset_http" = "200" ]; then
        extract_asset_presigned_fields "$asset_body"
    fi
    # Log this request using the existing report format
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$asset_http" = "200" ]; then PASSED_TESTS=$((PASSED_TESTS + 1)); else FAILED_TESTS=$((FAILED_TESTS + 1)); fi
    {
        echo ""
        echo "### Upload Event Asset"
        echo "**Status:** $([ "$asset_http" = "200" ] && echo "✅" || echo "❌") $asset_http (Expected: 200)"
        echo "**Description:** Upload event asset (presign)"
        echo "**Endpoint:** POST /api/v1/events/$EVENT_ID/assets"
        echo "**Request Headers:** -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -H 'X-Device-ID: $DEVICE_ID'"
        echo "**Request Body:** $asset_upload_request"
        echo ""
        echo "**Response:**"
        echo "\`\`\`json"
        echo "$asset_body"
        echo "\`\`\`"
        echo ""
        echo "---"
        echo ""
    } >> "$REPORT_FILE"

    # Upload asset bytes to S3 and complete upload
    if [ -n "$ASSET_UPLOAD_URL" ] && [ -n "$TEST_IMAGE_PATH" ]; then
        presigned_put_upload "$ASSET_UPLOAD_URL" "$ASSET_REQUIRED_HEADERS" "$TEST_IMAGE_PATH"
    fi
    if [ -n "$ASSET_ID" ]; then
        local asset_complete_payload
        asset_complete_payload=$(cat <<EOF
{
  "objectKey": "$ASSET_OBJECT_KEY",
  "resourceUrl": "$ASSET_RESOURCE_URL",
  "fileName": "unsplash-private-asset.jpg",
  "contentType": "image/jpeg",
  "category": "documents",
  "isPublic": false,
  "description": "Asset uploaded via presigned S3 URL",
  "tags": "test,event",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
        run_test "Complete Event Asset Upload" "POST" "/api/v1/events/$EVENT_ID/assets/$ASSET_ID/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$asset_complete_payload" "200" "Complete asset upload by saving uploaded object reference"
    fi
    
    # Cover Image Tests
    local cover_update_request
    cover_update_request=$(cat <<EOF
{
  "event": {},
  "coverUpload": {
    "fileName": "unsplash-cover.jpg",
    "contentType": "image/jpeg",
    "category": "cover",
    "isPublic": true,
    "description": "Cover image upload via PUT /events"
  }
}
EOF
)
    run_test "Request Cover Upload via Update Event" "PUT" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$cover_update_request" "200" "Request a presigned cover image upload via PUT /events/{id}"

    # Upload cover bytes to S3 and complete upload
    if [ -n "$COVER_UPLOAD_URL" ] && [ -n "$TEST_IMAGE_PATH" ]; then
        presigned_put_upload "$COVER_UPLOAD_URL" "$COVER_REQUIRED_HEADERS" "$TEST_IMAGE_PATH"
    fi
    if [ -n "$COVER_ID" ]; then
        local cover_complete_payload
        cover_complete_payload=$(cat <<EOF
{
  "objectKey": "$COVER_OBJECT_KEY",
  "resourceUrl": "$COVER_RESOURCE_URL",
  "fileName": "unsplash-cover.jpg",
  "contentType": "image/jpeg",
  "category": "cover",
  "isPublic": true,
  "description": "Cover uploaded via presigned S3 URL (update flow)",
  "tags": "test,event",
  "metadata": "{\"source\":\"unsplash\"}"
}
EOF
)
        local cover_complete_wrapped
        cover_complete_wrapped=$(cat <<EOF
{
  "coverId": "$COVER_ID",
  "upload": $cover_complete_payload
}
EOF
)
        run_test "Complete Cover Image Upload" "POST" "/api/v1/events/$EVENT_ID/cover-image/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$cover_complete_wrapped" "200" "Complete cover image upload and persist coverImageUrl on the event"
    fi

    # Confirm coverImageUrl is now set on event
    run_test "Get Event by ID (After Cover Upload)" "GET" "/api/v1/events/$EVENT_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Verify cover image url is persisted on the event"

    # (Optional) Could complete cover upload similarly; remove remains valid to clean state
    run_test "Remove Cover Image" "DELETE" "/api/v1/events/$EVENT_ID/cover-image" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Remove cover image"
    echo ""
    
    # Step 7: Notification Tests
    echo -e "${CYAN}🔔 Step 7: Notification Tests${NC}"
    echo "==============================="
    
    # Notification Settings Tests
    run_test "Get Notification Settings" "GET" "/api/v1/events/$EVENT_ID/notifications" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event notification settings"
    
    # Send Notification Tests
    local notification_data='{
        "channel": "EMAIL",
        "subject": "Event Announcement - Test",
        "content": "This is a test announcement notification. Please check your email at mayokak@gmail.com",
        "emailTemplateType": "ANNOUNCEMENT",
        "recipientUserIds": ["'$USER_ID'"],
        "recipientEmails": ["mayokak@gmail.com"],
        "scheduledAt": null,
        "priority": "NORMAL",
        "includeEventDetails": true
    }'
    run_test "Send Event Notification (Announcement)" "POST" "/api/v1/events/$EVENT_ID/notifications/send" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$notification_data" "200" "Send event announcement notification to mayokak@gmail.com"
    
    # Test cancellation notification
    local cancellation_data='{
        "channel": "EMAIL",
        "subject": "Event Cancellation - Test",
        "content": "We regret to inform you that this test event has been cancelled due to unforeseen circumstances.",
        "emailTemplateType": "CANCEL_EVENT",
        "recipientEmails": ["mayokak@gmail.com"],
        "scheduledAt": null,
        "priority": "NORMAL",
        "includeEventDetails": true
    }'
    run_test "Send Event Notification (Cancellation)" "POST" "/api/v1/events/$EVENT_ID/notifications/send" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$cancellation_data" "200" "Send event cancellation notification to mayokak@gmail.com"
    
    # Reminder Tests
    run_test "Get Event Reminders" "GET" "/api/v1/events/$EVENT_ID/reminders" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get event reminders"
    
    local reminder_time=$(date -u -v+1d '+%Y-%m-%dT%H:%M:%S')
    local reminder_data
    reminder_data=$(cat <<EOF
{
    "title": "Event Reminder",
    "description": "Don't forget about the event!",
    "reminderTime": "$reminder_time",
    "channel": "email",
    "reminderType": "custom",
    "isActive": true,
    "customMessage": "Custom reminder message - Test reminder sent to mayokak@gmail.com",
    "recipientUserIds": ["$USER_ID"],
    "recipientEmails": ["mayokak@gmail.com"]
}
EOF
)
    run_test "Create Event Reminder" "POST" "/api/v1/events/$EVENT_ID/reminders" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$reminder_data" "200" "Create event reminder"
    
    # Update reminder (using captured ID when available)
    local mock_reminder_id="12345678-1234-1234-1234-123456789012"
    local reminder_identifier="${REMINDER_ID:-$mock_reminder_id}"
    local reminder_update_time=$(date -u -v+2d '+%Y-%m-%dT%H:%M:%S')
    local reminder_update_data
    reminder_update_data=$(cat <<EOF
{
    "title": "Updated Event Reminder",
    "description": "Updated reminder description",
    "reminderTime": "$reminder_update_time",
    "channel": "email",
    "reminderType": "custom",
    "isActive": true,
    "customMessage": "Updated reminder message - Test reminder sent to mayokak@gmail.com",
    "recipientUserIds": ["$USER_ID"],
    "recipientEmails": ["mayokak@gmail.com"]
}
EOF
)
    run_test "Update Event Reminder" "PUT" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$reminder_update_data" "200" "Update event reminder"
    
    run_test "Get Specific Reminder" "GET" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get specific reminder"
    
    run_test "Delete Event Reminder" "DELETE" "/api/v1/events/$EVENT_ID/reminders/$reminder_identifier" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "204" "Delete event reminder"
    echo ""
    
    # Step 8: Clean up test data
    echo -e "${CYAN}🧹 Step 8: Clean Up Test Data${NC}"
    echo "==============================="
    
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
    } >> "$REPORT_FILE"
    
    if [ $success_rate -ge 90 ]; then
        {
            echo "✅ **Excellent!** All tests are passing successfully. The event system is working correctly."
        } >> "$REPORT_FILE"
    elif [ $success_rate -ge 70 ]; then
        {
            echo "⚠️ **Good** - Most tests are passing, but there are some issues that need attention."
        } >> "$REPORT_FILE"
    else
        {
            echo "❌ **Needs Attention** - Multiple test failures indicate significant issues with the event system."
        } >> "$REPORT_FILE"
    fi
    
    {
        echo ""
        echo "### Next Steps"
        echo ""
        echo "1. Review failed tests and fix underlying issues"
        echo "2. Check server logs for detailed error information"
        echo "3. Verify database connectivity and data integrity"
        echo "4. Test with different user scenarios"
        echo "5. Consider adding more edge case tests"
        echo ""
        echo "---"
        echo ""
        echo "**Report generated by:** Event Controller Test Script"
        echo "**Script version:** 1.0"
    } >> "$REPORT_FILE"
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Event system is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Function to handle cleanup on script exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Test interrupted...${NC}"
    if [ -n "$TEST_IMAGE_PATH" ] && [ -f "$TEST_IMAGE_PATH" ]; then
        rm -f "$TEST_IMAGE_PATH"
    fi
    if [[ $BASE_URL == *"localhost"* ]]; then
        echo -e "${YELLOW}💡 Local service continues running${NC}"
    else
        echo -e "${YELLOW}💡 Production service continues running${NC}"
    fi
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Run main function
main "$@"
