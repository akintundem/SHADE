#!/bin/bash

# Vendor Endpoints Test Script
# Validates the vendor discovery and onboarding endpoints introduced in the organization refactor.

set -o pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo -e "${RED}❌ Required command '$1' not found. Please install it before running this script.${NC}"
        exit 1
    fi
}

require_command curl
require_command jq

BASE_URL=""
CLIENT_ID="${CLIENT_ID:-web-app}"

show_help() {
    cat <<EOF
🧪 Vendor Endpoints Test Script
================================

Usage:
  $0                # Interactive mode
  $0 local          # Test against http://localhost:8080
  $0 prod <API_URL> # Test against a custom API host
  $0 help           # Show this message

Environment variables:
  CLIENT_ID                Defaults to 'web-app'
  TEST_ADMIN_EMAIL         Email used to authenticate (prompted if absent)
  TEST_ADMIN_PASSWORD      Password used to authenticate (prompted if absent)

Endpoints covered:
  - POST   /api/v1/vendors
  - GET    /api/v1/vendors?query=...

EOF
    exit 0
}

if [[ "$1" == "help" || "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
fi

choose_environment() {
    if [[ "$1" == "local" || "$1" == "l" ]]; then
        BASE_URL="http://localhost:8080"
    elif [[ "$1" == "prod" || "$1" == "p" ]]; then
        if [[ -z "$2" ]]; then
            echo -e "${RED}❌ Production URL required. Usage: $0 prod <API_URL>${NC}"
            exit 1
        fi
        BASE_URL="$2"
    else
        echo -e "${CYAN}🌍 Choose testing environment:${NC}"
        echo "1. Local Development (http://localhost:8080)"
        echo "2. Custom URL"
        read -rp "Enter choice: " choice
        case "$choice" in
            1) BASE_URL="http://localhost:8080" ;;
            2)
                read -rp "Enter base URL (e.g. https://api.example.com): " custom_url
                if [[ ! "$custom_url" =~ ^https?:// ]]; then
                    echo -e "${RED}❌ Invalid URL. Must begin with http:// or https://${NC}"
                    exit 1
                fi
                BASE_URL="$custom_url"
                ;;
            *) echo -e "${RED}❌ Invalid choice${NC}"; exit 1 ;;
        esac
    fi

    echo -e "${BLUE}🔗 Base URL: $BASE_URL${NC}"
}

choose_environment "$@"

REPORT_DIR="test/reports"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/vendor_test_report_$(date +%Y%m%d_%H%M%S).md"

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log_report_header() {
    cat > "$REPORT_FILE" <<EOF
# Vendor Endpoint Test Report

**Test Timestamp:** $(date)
**Base URL:** $BASE_URL
**Client ID:** $CLIENT_ID

| Test | Status | Details |
|------|--------|---------|
EOF
}

append_report_row() {
    local name="$1"
    local status="$2"
    local details="$3"
    echo "| $name | $status | $details |" >> "$REPORT_FILE"
}

record_pass() {
    PASSED_TESTS=$((PASSED_TESTS + 1))
    echo -e "${GREEN}✅ PASS${NC}"
}

record_fail() {
    FAILED_TESTS=$((FAILED_TESTS + 1))
    echo -e "${RED}❌ FAIL${NC}"
}

AUTH_TOKEN=""

prompt_for_credentials() {
    if [[ -z "$TEST_ADMIN_EMAIL" ]]; then
        read -rp "Enter email with vendor permissions: " TEST_ADMIN_EMAIL
    fi
    if [[ -z "$TEST_ADMIN_PASSWORD" ]]; then
        read -rsp "Enter password: " TEST_ADMIN_PASSWORD
        echo ""
    fi
}

authenticate() {
    prompt_for_credentials
    echo -e "${CYAN}🔐 Authenticating as $TEST_ADMIN_EMAIL...${NC}"

    local payload
    payload=$(jq -n \
        --arg email "$TEST_ADMIN_EMAIL" \
        --arg password "$TEST_ADMIN_PASSWORD" \
        '
        {
            email: $email,
            password: $password,
            rememberMe: false,
            deviceId: "vendor-test-device"
        }
        ')

    local response http_code body
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "X-Client-ID: $CLIENT_ID" \
        -d "$payload" \
        "$BASE_URL/api/v1/auth/login")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [[ "$http_code" -ne 200 ]]; then
        echo -e "${RED}❌ Authentication failed (HTTP $http_code)${NC}"
        echo "$body"
        exit 1
    fi

    AUTH_TOKEN=$(echo "$body" | jq -r '.accessToken // empty')
    DEVICE_ID=$(echo "$body" | jq -r '.deviceId // empty')
    if [[ -z "$AUTH_TOKEN" ]]; then
        echo -e "${RED}❌ Unable to extract access token from login response${NC}"
        exit 1
    fi
    if [[ -z "$DEVICE_ID" ]]; then
        echo -e "${RED}❌ Unable to extract device ID from login response${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ Authentication succeeded${NC}"
}

run_curl_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    local require_auth="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${BLUE}🧪 $test_name${NC}"

    local url="$BASE_URL$endpoint"
    local response http_code body

    if [[ "$require_auth" == "true" ]]; then
        if [[ -n "$data" ]]; then
            response=$(printf '%s' "$data" | curl -s -w '\n%{http_code}' -X "$method" \
                -H "Content-Type: application/json" \
                -H "X-Client-ID: $CLIENT_ID" \
                -H "Authorization: Bearer $AUTH_TOKEN" \
                -H "X-Device-ID: $DEVICE_ID" \
                --data-binary @- \
                "$url")
        else
            response=$(curl -s -w '\n%{http_code}' -X "$method" \
                -H "Content-Type: application/json" \
                -H "X-Client-ID: $CLIENT_ID" \
                -H "Authorization: Bearer $AUTH_TOKEN" \
                -H "X-Device-ID: $DEVICE_ID" \
                "$url")
        fi
    else
        if [[ -n "$data" ]]; then
            response=$(printf '%s' "$data" | curl -s -w '\n%{http_code}' -X "$method" \
                -H "Content-Type: application/json" \
                -H "X-Client-ID: $CLIENT_ID" \
                --data-binary @- \
                "$url")
        else
            response=$(curl -s -w '\n%{http_code}' -X "$method" \
                -H "Content-Type: application/json" \
                -H "X-Client-ID: $CLIENT_ID" \
                "$url")
        fi
    fi
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [[ "$http_code" -eq "$expected_status" ]]; then
        record_pass
        append_report_row "$test_name" "PASS" "HTTP $http_code"
        echo "$body" | jq '.' >/dev/null 2>&1 && echo "$body" | jq '.' || echo "$body"
        return 0
    else
        record_fail
        append_report_row "$test_name" "FAIL" "Expected $expected_status, got $http_code"
        echo "$body" | jq '.' >/dev/null 2>&1 && echo "$body" | jq '.' || echo "$body"
        return 1
    fi
}

verify_vendor_in_results() {
    local test_name="$1"
    local json="$2"
    local vendor_name="$3"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if echo "$json" | jq -e . >/dev/null 2>&1; then
        if echo "$json" | jq --arg name "$vendor_name" '[.[] | select(.name == $name)] | length > 0' | grep -q "true"; then
            record_pass
            append_report_row "$test_name" "PASS" "Vendor '$vendor_name' present in results"
            return
        fi
    fi

    record_fail
    if [[ -n "$json" ]]; then
        append_report_row "$test_name" "FAIL" "Vendor '$vendor_name' not found in results"
    else
        append_report_row "$test_name" "FAIL" "Vendor '$vendor_name' not found (empty response)"
    fi
}

log_report_header
authenticate

UNIQUE_TOKEN=$(date +%s)
VENDOR_NAME="Automation Vendor $UNIQUE_TOKEN"
VENDOR_EMAIL="vendor${UNIQUE_TOKEN}@example.com"
GOOGLE_PLACE_ID="automation-place-$UNIQUE_TOKEN"

VENDOR_PAYLOAD=$(jq -n \
    --arg name "$VENDOR_NAME" \
    --arg description "Automated test vendor onboarding" \
    --arg website "https://vendor-$UNIQUE_TOKEN.example.com" \
    --arg phone "+1555000$UNIQUE_TOKEN" \
    --arg email "$VENDOR_EMAIL" \
    --arg taxId "TAX-$UNIQUE_TOKEN" \
    --arg reg "REG-$UNIQUE_TOKEN" \
    --arg place "$GOOGLE_PLACE_ID" \
    '{
        name: $name,
        description: $description,
        type: "CATERING",
        website: $website,
        phoneNumber: $phone,
        address: {
            street: "123 Test Street",
            city: "Testville",
            state: "CA",
            zipCode: "90001",
            country: "USA"
        },
        contactEmail: $email,
        taxId: $taxId,
        registrationNumber: $reg,
        googlePlaceId: $place,
        platformVendor: true,
        vendorTier: "BASIC",
        vendorStatus: "APPROVED"
    }')

run_curl_test "Vendor Registration" "POST" "/api/v1/vendors" "$VENDOR_PAYLOAD" 201 true

ESCAPED_QUERY=$(printf "%s" "$VENDOR_NAME" | jq -s -R -r @uri)
SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
    -H "Content-Type: application/json" \
    -H "X-Client-ID: $CLIENT_ID" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -H "X-Device-ID: $DEVICE_ID" \
    "$BASE_URL/api/v1/vendors?query=$ESCAPED_QUERY")

SEARCH_HTTP_CODE=$(echo "$SEARCH_RESPONSE" | tail -n1)
SEARCH_BODY=$(echo "$SEARCH_RESPONSE" | sed '$d')

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}🧪 Vendor Search${NC}"
if [[ "$SEARCH_HTTP_CODE" -eq 200 ]]; then
    record_pass
    append_report_row "Vendor Search" "PASS" "HTTP 200"
else
    record_fail
    append_report_row "Vendor Search" "FAIL" "Expected 200, got $SEARCH_HTTP_CODE"
fi

echo "$SEARCH_BODY" | jq '.' >/dev/null 2>&1 && echo "$SEARCH_BODY" | jq '.' || echo "$SEARCH_BODY"

verify_vendor_in_results "Vendor Appears In Search Results" "$SEARCH_BODY" "$VENDOR_NAME"

echo "" >> "$REPORT_FILE"
echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Metric | Value |" >> "$REPORT_FILE"
echo "|--------|-------|" >> "$REPORT_FILE"
echo "| Total Tests | $TOTAL_TESTS |" >> "$REPORT_FILE"
echo "| Passed | $PASSED_TESTS |" >> "$REPORT_FILE"
echo "| Failed | $FAILED_TESTS |" >> "$REPORT_FILE"

echo ""
echo -e "${CYAN}📄 Report generated at: $REPORT_FILE${NC}"
echo -e "${GREEN}✅ $PASSED_TESTS tests passed${NC}"
if [[ "$FAILED_TESTS" -gt 0 ]]; then
    echo -e "${RED}❌ $FAILED_TESTS tests failed${NC}"
else
    echo -e "${GREEN}🎉 All vendor endpoint tests passed${NC}"
fi

