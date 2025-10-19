# Testing Setup for Event Planner Monolith

## Overview

All testing infrastructure has been successfully migrated from the microservices architecture to the monolithic version.

## What Was Moved

### ✅ Postman Collections
- **Location**: `Postman Collections/`
- **Collections**: All 10 service collections
  - Event_Planner_Auth_Service_Testing.postman_collection.json
  - Event_Planner_Event_Service_Testing.postman_collection.json
  - Event_Planner_Attendee_Service_Testing.postman_collection.json
  - Event_Planner_Budget_Service_Testing.postman_collection.json
  - Event_Planner_Comms_Service_Testing.postman_collection.json
  - Event_Planner_Risk_Service_Testing.postman_collection.json
  - Event_Planner_Timeline_Service_Testing.postman_collection.json
  - Event_Planner_Payments_Service_Testing.postman_collection.json
  - Event_Planner_Vendor_Service_Testing.postman_collection.json
  - Event_Planner_Weather_Service_Testing.postman_collection.json

### ✅ Test Environment Configuration
- **File**: `test-environment.json`
- **Configuration**: Updated for monolith (port 8080)
- **Base URL**: `http://localhost:8080`

### ✅ Test Scripts
- **Main Test Runner**: `test.sh` - Comprehensive Newman test runner
- **CI Test Script**: `ci-test.sh` - For CI/CD pipeline testing
- **Environment Template**: `ci-env.template` - CI environment variables

### ✅ Test Results
- **Directory**: `test-results/`
- **Historical Results**: All previous test runs preserved
- **Report Format**: Detailed markdown reports with statistics

### ✅ Development Scripts
- **Start Script**: `dev-start.sh` - Updated for monolith
- **Stop Script**: `dev-stop.sh` - Stops all services
- **Makefile**: Build and deployment commands

## How to Use

### 1. Start the Monolith
```bash
# Start supporting services
./dev-start.sh

# Start the monolith application
mvn spring-boot:run
```

### 2. Run API Tests
```bash
# Run all Postman collections
./test.sh

# Run specific collection
newman run "Postman Collections/Event_Planner_Auth_Service_Testing.postman_collection.json" -e test-environment.json
```

### 3. View Test Results
```bash
# View latest test results
ls test-results/

# View detailed report
cat test-results/[timestamp]/test_report.md
```

## Key Changes for Monolith

### Port Configuration
- **Before**: Multiple ports (8080, 8082, 8083, 8084, 8086, 8087, 8088, 8089, 8090)
- **After**: Single port (8080)

### API Endpoints
- **Before**: Service-specific endpoints (e.g., `/api/v1/auth/`, `/api/v1/events/`)
- **After**: All endpoints available on single application

### Test Environment
- **Base URL**: `http://localhost:8080`
- **All service endpoints**: Available on the same host and port

## Test Coverage

The monolith maintains the same test coverage as the microservices:

- ✅ Authentication & Authorization
- ✅ Event Management
- ✅ Attendee Management
- ✅ Budget Management
- ✅ Communication Services
- ✅ Risk Management
- ✅ Timeline Management
- ✅ Payment Processing
- ✅ Vendor Management
- ✅ Weather Services

## Benefits of Monolithic Testing

1. **Simplified Setup**: Single application to test
2. **Faster Execution**: No network calls between services
3. **Easier Debugging**: All logs in one place
4. **Consistent Environment**: Same database and configuration
5. **Reduced Complexity**: No service discovery or load balancing issues

## Next Steps

1. **Run Tests**: Execute `./test.sh` to verify all functionality
2. **Review Results**: Check test reports for any failures
3. **Update Collections**: Modify any service-specific tests if needed
4. **CI/CD Integration**: Update your CI pipeline to use the monolith

## Support

For testing issues or questions, refer to:
- `Postman Collections/POSTMAN_TESTING_GUIDE.md`
- `Postman Collections/Event_Planner_Complete_Testing_Guide.md`
- Test results in `test-results/` directory
