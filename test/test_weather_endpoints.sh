#!/bin/bash

# Comprehensive Weather Controller Endpoints Test Script
# Tests all weather-related endpoints and generates a detailed report

echo "🌤️  Starting Comprehensive Weather Controller Endpoints Test"
echo "============================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
REPORT_FILE="reports/weather_test_report_$(date +%Y%m%d_%H%M%S).md"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test coordinates and locations
NEW_YORK_LAT="40.7128"
NEW_YORK_LON="-74.0060"
LONDON_LAT="51.5074"
LONDON_LON="-0.1278"
MIAMI_LAT="25.7617"
MIAMI_LON="-80.1918"
SEATTLE_LAT="47.6062"
SEATTLE_LON="-122.3321"

# Create report file
mkdir -p reports
cat > "$REPORT_FILE" << EOF
# Weather Controller Endpoints Test Report

**Test Started:** $(date)
**Base URL:** $BASE_URL
**Report File:** $REPORT_FILE

## Test Summary

| Test Category | Total | Passed | Failed | Success Rate |
|---------------|-------|--------|--------|--------------|
| Health Check | 0 | 0 | 0 | 0% |
| Forecast by Coordinates | 0 | 0 | 0 | 0% |
| Forecast by Location | 0 | 0 | 0 | 0% |
| Event Viability | 0 | 0 | 0 | 0% |
| Geocoding | 0 | 0 | 0 | 0% |
| Error Handling | 0 | 0 | 0 | 0% |
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
    
    # Check if test passed
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} - Status: $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        local status_icon="✅"
    else
        echo -e "${RED}❌ FAILED${NC} - Expected: $expected_status, Got: $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        local status_icon="❌"
    fi
    
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
    
    echo ""
}

# Function to wait for service to be ready
wait_for_service() {
    echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
    local max_attempts=60
    local attempt=1
    local java_ready=false
    local python_ready=false
    
    while [ $attempt -le $max_attempts ]; do
        # Check Java Spring Boot service
        if ! $java_ready; then
            if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Java Application is ready!${NC}"
                java_ready=true
            else
                echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for Java Application...${NC}"
            fi
        fi
        
        # Check Python Shade Assistant service
        if ! $python_ready; then
            if curl -s "http://localhost:8000/health" > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Python Shade Assistant is ready!${NC}"
                python_ready=true
            else
                echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for Python Assistant...${NC}"
            fi
        fi
        
        # If both services are ready, return success
        if $java_ready && $python_ready; then
            echo -e "${GREEN}✅ All services are ready!${NC}"
            return 0
        fi
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ Services failed to start within expected time${NC}"
    echo -e "${RED}   Java ready: $java_ready${NC}"
    echo -e "${RED}   Python ready: $python_ready${NC}"
    return 1
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
    echo "1. Stop and restart services"
    echo "2. Test health check endpoint"
    echo "3. Test weather forecast by coordinates"
    echo "4. Test weather forecast by location name"
    echo "5. Test outdoor event viability"
    echo "6. Test geocoding service"
    echo "7. Test error handling"
    echo ""
    
    # Step 1: Restart services
    echo -e "${CYAN}🔄 Step 1: Restarting Services${NC}"
    echo "=================================="
    if ! restart_services; then
        echo -e "${RED}❌ Failed to restart services. Exiting.${NC}"
        exit 1
    fi
    echo ""
    
    # Step 2: Health Check Tests
    echo -e "${CYAN}🏥 Step 2: Health Check Tests${NC}"
    echo "============================="
    run_test "Health Check" "GET" "/actuator/health" "-H 'Content-Type: application/json'" "" "200" "Check if application is healthy"
    echo ""
    
    # Step 3: Forecast by Coordinates Tests
    echo -e "${CYAN}📍 Step 3: Forecast by Coordinates Tests${NC}"
    echo "============================================="
    run_test "New York Forecast" "GET" "/api/v1/weather/forecast?lat=$NEW_YORK_LAT&lon=$NEW_YORK_LON" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for New York coordinates"
    
    run_test "London Forecast" "GET" "/api/v1/weather/forecast?lat=$LONDON_LAT&lon=$LONDON_LON" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for London coordinates"
    
    run_test "Miami Forecast" "GET" "/api/v1/weather/forecast?lat=$MIAMI_LAT&lon=$MIAMI_LON" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for Miami coordinates"
    
    run_test "Seattle Forecast" "GET" "/api/v1/weather/forecast?lat=$SEATTLE_LAT&lon=$SEATTLE_LON" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for Seattle coordinates"
    echo ""
    
    # Step 4: Forecast by Location Name Tests
    echo -e "${CYAN}🏙️  Step 4: Forecast by Location Name Tests${NC}"
    echo "============================================="
    run_test "New York by Name" "GET" "/api/v1/weather/forecast/location?location=New York, NY" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for New York by name"
    
    run_test "London by Name" "GET" "/api/v1/weather/forecast/location?location=London, UK" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for London by name"
    
    run_test "Miami by Name" "GET" "/api/v1/weather/forecast/location?location=Miami, FL" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for Miami by name"
    
    run_test "Seattle by Name" "GET" "/api/v1/weather/forecast/location?location=Seattle, WA" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for Seattle by name"
    
    run_test "Toronto by Name" "GET" "/api/v1/weather/forecast/location?location=Toronto, ON" "-H 'Content-Type: application/json'" "" "200" "Get weather forecast for Toronto by name"
    echo ""
    
    # Step 5: Event Viability Tests
    echo -e "${CYAN}🎪 Step 5: Event Viability Tests${NC}"
    echo "=================================="
    local event_date=$(date -d "+3 days" +%Y-%m-%d 2>/dev/null || date -v+3d +%Y-%m-%d 2>/dev/null || echo "2024-01-15")
    
    run_test "New York Event Viability" "GET" "/api/v1/weather/event-viability?lat=$NEW_YORK_LAT&lon=$NEW_YORK_LON&eventDate=$event_date" "-H 'Content-Type: application/json'" "" "200" "Check outdoor event viability for New York"
    
    run_test "London Event Viability" "GET" "/api/v1/weather/event-viability?lat=$LONDON_LAT&lon=$LONDON_LON&eventDate=$event_date" "-H 'Content-Type: application/json'" "" "200" "Check outdoor event viability for London"
    
    run_test "Miami Event Viability" "GET" "/api/v1/weather/event-viability?lat=$MIAMI_LAT&lon=$MIAMI_LON&eventDate=$event_date" "-H 'Content-Type: application/json'" "" "200" "Check outdoor event viability for Miami"
    
    run_test "Seattle Event Viability" "GET" "/api/v1/weather/event-viability?lat=$SEATTLE_LAT&lon=$SEATTLE_LON&eventDate=$event_date" "-H 'Content-Type: application/json'" "" "200" "Check outdoor event viability for Seattle"
    echo ""
    
    # Step 6: Geocoding Tests
    echo -e "${CYAN}🗺️  Step 6: Geocoding Tests${NC}"
    echo "============================="
    run_test "Geocode New York" "GET" "/api/v1/weather/geocode?location=New York, NY" "-H 'Content-Type: application/json'" "" "200" "Geocode New York location"
    
    run_test "Geocode London" "GET" "/api/v1/weather/geocode?location=London, UK" "-H 'Content-Type: application/json'" "" "200" "Geocode London location"
    
    run_test "Geocode Tokyo" "GET" "/api/v1/weather/geocode?location=Tokyo, Japan" "-H 'Content-Type: application/json'" "" "200" "Geocode Tokyo location"
    
    run_test "Geocode Sydney" "GET" "/api/v1/weather/geocode?location=Sydney, Australia" "-H 'Content-Type: application/json'" "" "200" "Geocode Sydney location"
    
    run_test "Geocode Paris" "GET" "/api/v1/weather/geocode?location=Paris, France" "-H 'Content-Type: application/json'" "" "200" "Geocode Paris location"
    echo ""
    
    # Step 7: Error Handling Tests
    echo -e "${CYAN}⚠️  Step 7: Error Handling Tests${NC}"
    echo "=================================="
    run_test "Invalid Coordinates" "GET" "/api/v1/weather/forecast?lat=999&lon=999" "-H 'Content-Type: application/json'" "" "400" "Test with invalid coordinates"
    
    run_test "Missing Parameters" "GET" "/api/v1/weather/forecast" "-H 'Content-Type: application/json'" "" "400" "Test with missing required parameters"
    
    run_test "Invalid Location" "GET" "/api/v1/weather/forecast/location?location=NonExistentCity12345" "-H 'Content-Type: application/json'" "" "400" "Test with non-existent location"
    
    run_test "Invalid Event Date" "GET" "/api/v1/weather/event-viability?lat=$NEW_YORK_LAT&lon=$NEW_YORK_LON&eventDate=invalid-date" "-H 'Content-Type: application/json'" "" "400" "Test with invalid event date format"
    
    run_test "Past Event Date" "GET" "/api/v1/weather/event-viability?lat=$NEW_YORK_LAT&lon=$NEW_YORK_LON&eventDate=2020-01-01" "-H 'Content-Type: application/json'" "" "400" "Test with past event date"
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
| Health Check | 1 | $([ $PASSED_TESTS -gt 0 ] && echo "1" || echo "0") | $([ $FAILED_TESTS -gt 0 ] && echo "1" || echo "0") | $([ $PASSED_TESTS -gt 0 ] && echo "100%" || echo "0%") |
| Forecast by Coordinates | 4 | 0 | 0 | 0% |
| Forecast by Location | 5 | 0 | 0 | 0% |
| Event Viability | 4 | 0 | 0 | 0% |
| Geocoding | 5 | 0 | 0 | 0% |
| Error Handling | 5 | 0 | 0 | 0% |

### Weather API Information

**Provider:** Open-Meteo API
**Cost:** Completely free
**Coverage:** Global
**API Key:** Not required
**Rate Limits:** Generous, no key required

### Available Endpoints

- \`GET /api/v1/weather/forecast?lat={lat}&lon={lon}\` - Get forecast by coordinates
- \`GET /api/v1/weather/forecast/location?location={name}\` - Get forecast by location name
- \`GET /api/v1/weather/event-viability?lat={lat}&lon={lon}&eventDate={date}\` - Check outdoor event viability
- \`GET /api/v1/weather/geocode?location={name}\` - Geocode location to coordinates

### Recommendations

EOF
    
    if [ $success_rate -ge 90 ]; then
        cat >> "$REPORT_FILE" << EOF
✅ **Excellent!** All weather endpoints are working correctly. The weather module is fully functional.
EOF
    elif [ $success_rate -ge 70 ]; then
        cat >> "$REPORT_FILE" << EOF
⚠️ **Good** - Most weather endpoints are working, but there are some issues that need attention.
EOF
    else
        cat >> "$REPORT_FILE" << EOF
❌ **Needs Attention** - Multiple test failures indicate significant issues with the weather module.
EOF
    fi
    
    cat >> "$REPORT_FILE" << EOF

### Next Steps

1. Review failed tests and fix underlying issues
2. Check server logs for detailed error information
3. Verify Open-Meteo API connectivity
4. Test with different locations and date ranges
5. Consider adding more edge case tests

---

**Report generated by:** Weather Controller Test Script
**Script version:** 1.0
**Weather API:** Open-Meteo (Free)
EOF
    
    echo -e "${GREEN}📄 Detailed report saved to: $REPORT_FILE${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Weather module is working correctly.${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Some tests failed. Please check the report for details.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
