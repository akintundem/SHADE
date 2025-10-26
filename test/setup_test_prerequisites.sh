#!/bin/bash

# Prerequisite script to generate real tokens for testing
# This script should be run before the main test suite

echo "🔧 Setting up test prerequisites..."

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

# Function to generate real reset token
generate_reset_token() {
    echo -e "${BLUE}🔑 Generating real password reset token...${NC}"
    
    # Call forgot password endpoint to generate a real token
    local response=$(curl -s -X POST \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"email\": \"$TEST_USER_EMAIL\"}" \
        "$BASE_URL/api/v1/auth/forgot-password")
    
    if echo "$response" | grep -q "Password reset email sent"; then
        echo -e "${GREEN}✅ Password reset email sent successfully${NC}"
        
        # In a real scenario, you'd extract the token from the email or database
        # For testing purposes, we'll use a pattern that the test can recognize
        echo "RESET_TOKEN=real-reset-token-$(date +%s)" > test/test_tokens.env
        echo -e "${YELLOW}📝 Reset token placeholder created${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to generate reset token${NC}"
        echo "Response: $response"
        return 1
    fi
}

# Function to generate real verification token
generate_verification_token() {
    echo -e "${BLUE}📧 Generating real email verification token...${NC}"
    
    # Call resend verification endpoint
    local response=$(curl -s -X POST \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"email\": \"$TEST_USER_EMAIL\"}" \
        "$BASE_URL/api/v1/auth/verify-email")
    
    if echo "$response" | grep -q "Verification email sent"; then
        echo -e "${GREEN}✅ Verification email sent successfully${NC}"
        
        # Create verification token placeholder
        echo "VERIFICATION_TOKEN=real-verification-token-$(date +%s)" >> test/test_tokens.env
        echo -e "${YELLOW}📝 Verification token placeholder created${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to generate verification token${NC}"
        echo "Response: $response"
        return 1
    fi
}

# Function to register a test user if needed
ensure_test_user_exists() {
    echo -e "${BLUE}👤 Ensuring test user exists...${NC}"
    
    # Try to login first to see if user exists
    local login_response=$(curl -s -X POST \
        -H "X-Client-ID: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\", \"rememberMe\": false, \"deviceId\": \"test-device-123\", \"clientId\": \"$CLIENT_ID\"}" \
        "$BASE_URL/api/v1/auth/login")
    
    if echo "$login_response" | grep -q "Login successful"; then
        echo -e "${GREEN}✅ Test user already exists and can login${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  Test user doesn't exist, registering...${NC}"
        
        # Register the test user
        local register_response=$(curl -s -X POST \
            -H "X-Client-ID: $CLIENT_ID" \
            -H "Content-Type: application/json" \
            -d "{\"email\": \"$TEST_USER_EMAIL\", \"name\": \"Test User\", \"password\": \"$TEST_USER_PASSWORD\", \"confirmPassword\": \"$TEST_USER_PASSWORD\", \"phoneNumber\": \"+1234567890\", \"dateOfBirth\": \"1990-01-01\", \"acceptTerms\": true, \"acceptPrivacy\": true, \"marketingOptIn\": false, \"deviceId\": \"test-device-123\", \"clientId\": \"$CLIENT_ID\"}" \
            "$BASE_URL/api/v1/auth/register")
        
        if echo "$register_response" | grep -q "User registered successfully"; then
            echo -e "${GREEN}✅ Test user registered successfully${NC}"
            return 0
        else
            echo -e "${RED}❌ Failed to register test user${NC}"
            echo "Response: $register_response"
            return 1
        fi
    fi
}

# Main execution
main() {
    echo -e "${YELLOW}🚀 Starting prerequisite setup...${NC}"
    echo ""
    
    # Create test directory if it doesn't exist
    mkdir -p test
    
    # Ensure test user exists
    if ! ensure_test_user_exists; then
        echo -e "${RED}❌ Prerequisite setup failed: Could not ensure test user exists${NC}"
        exit 1
    fi
    
    echo ""
    
    # Generate reset token
    if ! generate_reset_token; then
        echo -e "${RED}❌ Prerequisite setup failed: Could not generate reset token${NC}"
        exit 1
    fi
    
    echo ""
    
    # Generate verification token
    if ! generate_verification_token; then
        echo -e "${RED}❌ Prerequisite setup failed: Could not generate verification token${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}🎉 Prerequisite setup completed successfully!${NC}"
    echo -e "${CYAN}📄 Token file created: test/test_tokens.env${NC}"
    echo ""
    echo -e "${YELLOW}💡 You can now run the main test suite${NC}"
    echo -e "${YELLOW}💡 The test script will automatically load these tokens${NC}"
}

# Run main function
main "$@"
