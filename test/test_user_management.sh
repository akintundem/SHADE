#!/bin/bash

# User Management and Profile Image Test Script
# Tests user profile management and profile image upload endpoints
# Uses pre-registered admin user from create_test_users.sh
#
# Usage:
#   ./test_user_management.sh                    # Interactive mode
#   ./test_user_management.sh local              # Test localhost:8080
#   ./test_user_management.sh prod <API_URL>     # Test production URL
#   ./test_user_management.sh help               # Show help

# Function to show help
show_help() {
    echo "👤 User Management and Profile Image Test Script"
    echo "=================================================="
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
    echo "  - Pre-registered admin user (admin@test.com)"
    echo "  - For local testing: Spring Boot app running on port 8080"
    echo ""
    exit 0
}

# Check for help argument
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
fi

echo "👤 Starting User Management and Profile Image Tests"
echo "===================================================="

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
                echo ""
                read -p "API URL: " custom_url
                
                # Validate URL format
                if [[ $custom_url =~ ^https?:// ]]; then
                    BASE_URL="$custom_url"
                    echo -e "${GREEN}✅ Selected: Production - $BASE_URL${NC}"
                else
                    echo -e "${RED}❌ Invalid URL format. Please include http:// or https://${NC}"
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

# Configuration - Using admin user from create_test_users.sh
ADMIN_EMAIL="admin@test.com"
ADMIN_PASSWORD="Admin123!@#"
REPORT_FILE="test/reports/user_management_test_report_$(date +%Y%m%d_%H%M%S).md"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store tokens and user data
ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""
DEVICE_ID=""
LAST_HTTP_CODE=""
RESPONSE_BODY=""
PROFILE_IMAGE_OBJECT_KEY=""
PROFILE_IMAGE_RESOURCE_URL=""
PROFILE_IMAGE_UPLOAD_URL=""

# Create report file
mkdir -p test/reports
cat > "$REPORT_FILE" << EOF
# User Management and Profile Image Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Test User:** $ADMIN_EMAIL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Authentication | 0 | 0 | 0 | 0% |
| Profile Update | 0 | 0 | 0 | 0% |
| Profile Image Upload | 0 | 0 | 0 | 0% |
| User Search | 0 | 0 | 0 | 0% |
| User Directory | 0 | 0 | 0 | 0% |
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
    
    # Automatically attach X-Device-ID header for authenticated requests
    if [[ -n "$DEVICE_ID" && "$headers" != *"X-Device-ID"* ]]; then
        if [[ "$headers" == *"Authorization:"* ]]; then
            if [ -n "$headers" ]; then
                headers="$headers -H 'X-Device-ID: $DEVICE_ID'"
            else
                headers="-H 'X-Device-ID: $DEVICE_ID'"
            fi
        fi
    fi

    # Build curl command
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi
    
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    # Execute the request
    local response=$(eval $curl_cmd)
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    # Store response body globally for extraction in calling code
    RESPONSE_BODY="$response_body"
    
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
    LAST_HTTP_CODE="$http_code"
    
    # Log to report
    cat >> "$REPORT_FILE" << EOF

### $test_name
**Status:** $status_icon $http_code (Expected: $expected_status)
**Description:** $description
**Endpoint:** $method $endpoint
**Request Headers:** $headers
**Request Body:** $data

**Response:**
\`\`\`json
$response_body
\`\`\`

---

EOF
    
    # Extract tokens and user data from successful responses
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
         case "$test_name" in
             "Admin Login")
                 ACCESS_TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
                 REFRESH_TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
                 USER_ID=$(echo "$RESPONSE_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                 DEVICE_ID=$(echo "$RESPONSE_BODY" | grep -o '"deviceId":"[^"]*"' | cut -d'"' -f4)
                 ;;
             "Get Current User")
                 if [ -z "$USER_ID" ]; then
                     USER_ID=$(echo "$RESPONSE_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
                 fi
                 ;;
             "Create Profile Image Upload URL")
                 PROFILE_IMAGE_OBJECT_KEY=$(echo "$RESPONSE_BODY" | grep -o '"objectKey":"[^"]*"' | cut -d'"' -f4)
                 PROFILE_IMAGE_RESOURCE_URL=$(echo "$RESPONSE_BODY" | grep -o '"resourceUrl":"[^"]*"' | cut -d'"' -f4)
                 PROFILE_IMAGE_UPLOAD_URL=$(echo "$RESPONSE_BODY" | grep -o '"uploadUrl":"[^"]*"' | cut -d'"' -f4)
                 # Also extract Content-Type header if present
                 PROFILE_IMAGE_CONTENT_TYPE=$(echo "$RESPONSE_BODY" | grep -o '"Content-Type":"[^"]*"' | cut -d'"' -f4)
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
    local max_attempts=10
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
        return 1
    fi
}

# Main test execution
main() {
    echo -e "${PURPLE}📋 Test Plan:${NC}"
    echo "1. Check service availability"
    echo "2. Login as admin user"
    echo "3. Get current user profile"
    echo "4. Test profile updates (name, username, phone, etc.)"
    echo "5. Test profile image upload (upload-url)"
    echo "6. Test profile image upload completion"
    echo "7. Test user search (admin only)"
    echo "8. Test user directory"
    echo "9. Test get user by ID (admin only)"
    echo ""
    echo -e "${CYAN}👤 Test User: ${ADMIN_EMAIL}${NC}"
    echo ""
    
    # Step 1: Check service availability
    echo -e "${CYAN}🔍 Step 1: Checking Service Availability${NC}"
    echo "============================================="
    if ! check_service; then
        echo -e "${RED}❌ Service is not available. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 2: Login as Admin
    echo -e "${CYAN}🔑 Step 2: Admin Authentication${NC}"
    echo "============================="
    local login_data='{
        "email": "'$ADMIN_EMAIL'",
        "password": "'$ADMIN_PASSWORD'",
        "rememberMe": false
    }'
    
    run_test "Admin Login" "POST" "/api/v1/auth/login" "-H 'Content-Type: application/json'" "$login_data" "200" "Login as admin user"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        echo -e "${RED}❌ Failed to get access token. Cannot proceed with tests.${NC}"
        echo -e "${YELLOW}💡 Make sure the admin user exists. Run ./test/create_test_users.sh first${NC}"
        exit 1
    fi
    
    # Get user ID if not set from login
    if [ -z "$USER_ID" ]; then
        echo -e "${YELLOW}📋 Getting user ID from /me endpoint...${NC}"
        run_test "Get Current User" "GET" "/api/v1/auth/me" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get current user profile"
    fi
    
    if [ -z "$USER_ID" ]; then
        echo -e "${RED}❌ Failed to get user ID. Cannot proceed with tests.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ User ID: $USER_ID${NC}"
    echo ""
    
    # Step 3: Profile Update Tests
    echo -e "${CYAN}👤 Step 3: Profile Update Tests${NC}"
    echo "============================="
    
    # Test updating name
    local update_name_data='{
        "name": "Updated Admin Name",
        "username": "adminuser",
        "phoneNumber": "+1234567890",
        "dateOfBirth": "1990-01-15",
        "marketingOptIn": true
    }'
    run_test "Update Profile - Name and Details" "PUT" "/api/v1/auth/users/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_name_data" "200" "Update user profile with name, username, phone, and date of birth"
    
    # Test updating username only
    local update_username_data='{
        "name": "Updated Admin Name",
        "username": "adminuser2"
    }'
    run_test "Update Profile - Username Only" "PUT" "/api/v1/auth/users/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$update_username_data" "200" "Update username only"
    
    # Test invalid profile update (empty name)
    local invalid_update_data='{
        "name": "",
        "username": "test"
    }'
    run_test "Invalid Profile Update - Empty Name" "PUT" "/api/v1/auth/users/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invalid_update_data" "400" "Update profile with invalid data (empty name)"
    
    # Test invalid username format
    local invalid_username_data='{
        "name": "Test User",
        "username": "ab"
    }'
    run_test "Invalid Profile Update - Short Username" "PUT" "/api/v1/auth/users/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invalid_username_data" "400" "Update profile with invalid username (too short)"
    
    echo ""
    
    # Step 4: Profile Image Upload Tests
    echo -e "${CYAN}🖼️  Step 4: Profile Image Upload Tests${NC}"
    echo "====================================="
    
    # Test creating upload URL
    local upload_url_data='{
        "fileName": "profile-avatar.png",
        "contentType": "image/png"
    }'
    run_test "Create Profile Image Upload URL" "POST" "/api/v1/auth/profile-image/upload-url" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$upload_url_data" "200" "Create presigned URL for profile image upload"
    
    if [ -n "$PROFILE_IMAGE_OBJECT_KEY" ] && [ -n "$PROFILE_IMAGE_RESOURCE_URL" ] && [ -n "$PROFILE_IMAGE_UPLOAD_URL" ]; then
        echo -e "${GREEN}✅ Got upload URL - Object Key: $PROFILE_IMAGE_OBJECT_KEY${NC}"
        echo -e "${GREEN}✅ Resource URL: $PROFILE_IMAGE_RESOURCE_URL${NC}"
        echo -e "${GREEN}✅ Upload URL: $PROFILE_IMAGE_UPLOAD_URL${NC}"
        
        # Create a small test PNG image (1x1 pixel PNG)
        # Base64 encoded 1x1 transparent PNG
        local test_image_base64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        
        # Decode and upload to S3 using the presigned URL
        echo -e "${YELLOW}📤 Uploading test image to S3...${NC}"
        local upload_response=$(echo "$test_image_base64" | base64 -d | curl -s -w '%{http_code}' -X PUT \
            -H "Content-Type: image/png" \
            --data-binary @- \
            "$PROFILE_IMAGE_UPLOAD_URL")
        
        local upload_http_code="${upload_response: -3}"
        
        if [ "$upload_http_code" = "200" ] || [ "$upload_http_code" = "204" ]; then
            echo -e "${GREEN}✅ Successfully uploaded image to S3 (Status: $upload_http_code)${NC}"
            
            # Test completing the upload
            local complete_upload_data='{
                "objectKey": "'$PROFILE_IMAGE_OBJECT_KEY'",
                "resourceUrl": "'$PROFILE_IMAGE_RESOURCE_URL'"
            }'
            run_test "Complete Profile Image Upload" "POST" "/api/v1/auth/profile-image/complete" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$complete_upload_data" "200" "Complete profile image upload after S3 upload"
        else
            echo -e "${RED}❌ Failed to upload image to S3 (Status: $upload_http_code)${NC}"
            echo -e "${YELLOW}   Response: ${upload_response%???}${NC}"
            echo -e "${YELLOW}   Skipping complete upload test${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Could not extract upload details from response${NC}"
        echo -e "${YELLOW}   Object Key: ${PROFILE_IMAGE_OBJECT_KEY:-missing}${NC}"
        echo -e "${YELLOW}   Resource URL: ${PROFILE_IMAGE_RESOURCE_URL:-missing}${NC}"
        echo -e "${YELLOW}   Upload URL: ${PROFILE_IMAGE_UPLOAD_URL:-missing}${NC}"
        echo -e "${YELLOW}   Skipping S3 upload and complete test${NC}"
    fi
    
    # Test invalid upload request (empty fileName)
    local invalid_upload_data='{
        "fileName": "",
        "contentType": "image/png"
    }'
    run_test "Invalid Profile Image Upload - Empty File Name" "POST" "/api/v1/auth/profile-image/upload-url" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invalid_upload_data" "400" "Create upload URL with invalid data (empty fileName)"
    
    # Test invalid upload request (missing contentType)
    local invalid_content_type_data='{
        "fileName": "test.png"
    }'
    run_test "Invalid Profile Image Upload - Missing Content Type" "POST" "/api/v1/auth/profile-image/upload-url" "-H 'Authorization: Bearer $ACCESS_TOKEN' -H 'Content-Type: application/json'" "$invalid_content_type_data" "400" "Create upload URL with invalid data (missing contentType)"
    
    echo ""
    
    # Step 5: User Search Tests (Admin Only)
    echo -e "${CYAN}🔍 Step 5: User Search Tests (Admin Only)${NC}"
    echo "========================================="
    
    run_test "Search Users - Admin" "GET" "/api/v1/auth/users/search?searchTerm=admin" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search users by term (admin only)"
    
    run_test "Search Users - Invalid (Empty Term)" "GET" "/api/v1/auth/users/search?searchTerm=" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "400" "Search users with empty search term"
    
    echo ""
    
    # Step 6: User Directory Tests
    echo -e "${CYAN}📂 Step 6: User Directory Tests${NC}"
    echo "============================="
    
    run_test "User Directory - List All" "GET" "/api/v1/auth/users/directory" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "List all public users in directory"
    
    run_test "User Directory - Search" "GET" "/api/v1/auth/users/directory?searchTerm=admin" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Search public users directory"
    
    echo ""
    
    # Step 7: Get User by ID (Admin Only)
    echo -e "${CYAN}👤 Step 7: Get User by ID Tests (Admin Only)${NC}"
    echo "=========================================="
    
    run_test "Get User by ID - Admin" "GET" "/api/v1/auth/users/$USER_ID" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "200" "Get user profile by ID (admin only)"
    
    # Test getting non-existent user
    run_test "Get User by ID - Non-existent" "GET" "/api/v1/auth/users/00000000-0000-0000-0000-000000000000" "-H 'Authorization: Bearer $ACCESS_TOKEN'" "" "404" "Get non-existent user by ID"
    
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
    cat >> "$REPORT_FILE" << EOF

## Final Summary

**Test Execution Completed:** $(date)
**Total Tests:** $TOTAL_TESTS
**Passed:** $PASSED_TESTS
**Failed:** $FAILED_TESTS
**Success Rate:** $success_rate%

### Test Categories Breakdown

| Category | Tests | Passed | Failed | Success Rate |
|----------|-------|--------|--------|--------------|
| Authentication | 2 | 0 | 0 | 0% |
| Profile Update | 4 | 0 | 0 | 0% |
| Profile Image Upload | 4 | 0 | 0 | 0% |
| User Search | 2 | 0 | 0 | 0% |
| User Directory | 2 | 0 | 0 | 0% |
| Get User by ID | 2 | 0 | 0 | 0% |

### Recommendations

EOF
    
    if [ $success_rate -ge 90 ]; then
        cat >> "$REPORT_FILE" << EOF
✅ **Excellent!** All tests are passing successfully. User management and profile image features are working correctly.
EOF
    elif [ $success_rate -ge 70 ]; then
        cat >> "$REPORT_FILE" << EOF
⚠️ **Good** - Most tests are passing, but there are some issues that need attention.
EOF
    else
        cat >> "$REPORT_FILE" << EOF
❌ **Needs Attention** - Multiple test failures indicate significant issues with user management features.
EOF
    fi
    
    cat >> "$REPORT_FILE" << EOF

### Next Steps

1. Review failed tests and fix underlying issues
2. Check server logs for detailed error information
3. Verify database connectivity and data integrity
4. Test with different user scenarios
5. Verify S3 configuration for profile image uploads

---

**Report generated by:** User Management Test Script
**Script version:** 1.0
EOF
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! User management features are working correctly.${NC}"
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

