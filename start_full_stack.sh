#!/bin/bash

# Full Stack Event Planner Startup Script
# This script starts both the Java Spring Boot application and the Python Shade Assistant

echo "🚀 Starting Event Planner Full Stack Application"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if port is in use
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${RED}❌ Port $1 is already in use${NC}"
        return 1
    else
        echo -e "${GREEN}✅ Port $1 is available${NC}"
        return 0
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Attempt $attempt/$max_attempts - waiting for $service_name...${NC}"
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to start within expected time${NC}"
    return 1
}

# Check if required ports are available
echo -e "${BLUE}🔍 Checking port availability...${NC}"
check_port 8080 || exit 1
check_port 8000 || exit 1

# Kill any existing processes on these ports
echo -e "${YELLOW}🧹 Cleaning up any existing processes...${NC}"
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
lsof -ti:8000 | xargs kill -9 2>/dev/null || true

# Start Java Spring Boot Application
echo -e "${BLUE}☕ Starting Java Spring Boot Application...${NC}"
echo "   - Port: 8080"
echo "   - Database: PostgreSQL (localhost:5432)"
echo "   - API Documentation: http://localhost:8080/swagger-ui"

# Start Java app in background
mvn spring-boot:run \
    -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/eventplanner --spring.datasource.username=postgres --spring.datasource.password=postgres --spring.jpa.hibernate.ddl-auto=create-drop" \
    > java_app.log 2>&1 &

JAVA_PID=$!
echo "   Java PID: $JAVA_PID"

# Wait for Java app to be ready
if wait_for_service "http://localhost:8080/actuator/health" "Java Application"; then
    echo -e "${GREEN}✅ Java Application started successfully!${NC}"
    echo "   - Health Check: http://localhost:8080/actuator/health"
    echo "   - API Docs: http://localhost:8080/swagger-ui"
    echo "   - Event API: http://localhost:8080/api/v1/events"
else
    echo -e "${RED}❌ Failed to start Java Application${NC}"
    echo "Check java_app.log for details"
    kill $JAVA_PID 2>/dev/null || true
    exit 1
fi

# Start Python Shade Assistant
echo -e "${BLUE}🐍 Starting Python Shade Assistant...${NC}"
echo "   - Port: 8000"
echo "   - Chat Interface: http://localhost:8000"

# Change to shade directory and start Python app
cd shade

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}⚠️  Virtual environment not found. Creating one...${NC}"
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install dependencies if needed
if [ ! -f "venv/.deps_installed" ]; then
    echo -e "${YELLOW}📦 Installing Python dependencies...${NC}"
    pip install -r requirements.txt
    touch venv/.deps_installed
fi

# Start Python app in background
python main.py > ../python_app.log 2>&1 &

PYTHON_PID=$!
echo "   Python PID: $PYTHON_PID"

# Go back to root directory
cd ..

# Wait for Python app to be ready
if wait_for_service "http://localhost:8000" "Python Shade Assistant"; then
    echo -e "${GREEN}✅ Python Shade Assistant started successfully!${NC}"
    echo "   - Chat Interface: http://localhost:8000"
else
    echo -e "${RED}❌ Failed to start Python Shade Assistant${NC}"
    echo "Check python_app.log for details"
    kill $PYTHON_PID 2>/dev/null || true
    kill $JAVA_PID 2>/dev/null || true
    exit 1
fi

# Display final status
echo ""
echo -e "${GREEN}🎉 Full Stack Application Started Successfully!${NC}"
echo "================================================"
echo -e "${BLUE}📊 Services Status:${NC}"
echo "   ☕ Java Spring Boot: http://localhost:8080"
echo "   🐍 Python Shade Assistant: http://localhost:8000"
echo ""
echo -e "${BLUE}🔗 Quick Links:${NC}"
echo "   • Java Health: http://localhost:8080/actuator/health"
echo "   • Java API Docs: http://localhost:8080/swagger-ui"
echo "   • Event API: http://localhost:8080/api/v1/events"
echo "   • Chat Interface: http://localhost:8000"
echo ""
echo -e "${BLUE}📝 Logs:${NC}"
echo "   • Java Logs: tail -f java_app.log"
echo "   • Python Logs: tail -f python_app.log"
echo ""
echo -e "${YELLOW}🛑 To stop all services: ./stop_full_stack.sh${NC}"
echo ""

# Create a simple test to verify the integration
echo -e "${BLUE}🧪 Testing Java-Python Integration...${NC}"
sleep 3

# Test Java API
echo "Testing Java Event API..."
JAVA_TEST=$(curl -s -X POST "http://localhost:8080/api/v1/events" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -d '{"name": "Integration Test Event", "eventType": "CONFERENCE", "startDateTime": "2025-12-15T09:00:00"}' \
    -w "HTTP_STATUS:%{http_code}")

if echo "$JAVA_TEST" | grep -q "HTTP_STATUS:201"; then
    echo -e "${GREEN}✅ Java API is working correctly${NC}"
else
    echo -e "${RED}❌ Java API test failed${NC}"
    echo "Response: $JAVA_TEST"
fi

# Test Python API
echo "Testing Python Shade Assistant..."
PYTHON_TEST=$(curl -s -X GET "http://localhost:8000/health" -w "HTTP_STATUS:%{http_code}")

if echo "$PYTHON_TEST" | grep -q "HTTP_STATUS:200"; then
    echo -e "${GREEN}✅ Python Shade Assistant is working correctly${NC}"
else
    echo -e "${RED}❌ Python Shade Assistant test failed${NC}"
    echo "Response: $PYTHON_TEST"
fi

echo ""
echo -e "${GREEN}🚀 Ready to use! You can now interact with the chatbot to execute Java commands.${NC}"
echo ""

# Keep the script running and show logs
echo -e "${BLUE}📋 Monitoring logs (Ctrl+C to stop monitoring, services will continue running)...${NC}"
echo ""

# Function to handle cleanup on script exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Stopping services...${NC}"
    kill $JAVA_PID 2>/dev/null || true
    kill $PYTHON_PID 2>/dev/null || true
    echo -e "${GREEN}✅ Services stopped${NC}"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Monitor logs
tail -f java_app.log python_app.log
