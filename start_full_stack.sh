#!/bin/bash

# Full Stack Event Planner Startup Script with Docker
# This script starts all services using Docker Compose

echo "🚀 Starting Event Planner Full Stack Application with Docker"
echo "============================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}⚠️  .env file not found. Creating from template...${NC}"
    cp env.template .env
    echo -e "${YELLOW}📝 Please update .env with your actual values before continuing.${NC}"
    echo -e "${YELLOW}   At minimum, set your database passwords and API keys.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker is running${NC}"
echo -e "${GREEN}✅ .env file found${NC}"

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

# Start databases in Docker
echo -e "${BLUE}🐳 Starting databases in Docker...${NC}"
echo "   - PostgreSQL: localhost:5436"
echo "   - Redis: localhost:6379"
echo "   - MongoDB: localhost:27017"
echo "   - Pinecone Local: localhost:5081"

docker-compose up -d

# Wait for databases to be ready
echo -e "${YELLOW}⏳ Waiting for databases to start...${NC}"
sleep 10

# Start Java Spring Boot Application locally
echo -e "${BLUE}☕ Starting Java Spring Boot Application locally...${NC}"
echo "   - Port: 8080"
echo "   - Database: PostgreSQL (localhost:5436)"
echo "   - API Documentation: http://localhost:8080/swagger-ui"

# Start Java app in background
mvn spring-boot:run \
    -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5436/eventplanner --spring.datasource.username=postgres --spring.datasource.password=postgres --spring.jpa.hibernate.ddl-auto=create-drop" \
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

# Start Python Shade Assistant locally
echo -e "${BLUE}🐍 Starting Python Shade Assistant locally...${NC}"
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
echo -e "${GREEN}🎉 Hybrid Setup Started Successfully!${NC}"
echo "================================================"
echo -e "${BLUE}📊 Services Status:${NC}"
echo "   🐳 PostgreSQL (Docker): localhost:5436"
echo "   🐳 Redis (Docker): localhost:6379"
echo "   🐳 MongoDB (Docker): localhost:27017"
echo "   🐳 Pinecone Local (Docker): localhost:5081"
echo "   ☕ Java Spring Boot (Local): http://localhost:8080"
echo "   🐍 Python Shade Assistant (Local): http://localhost:8000"
echo ""
echo -e "${BLUE}🔗 Quick Links:${NC}"
echo "   • Java Health: http://localhost:8080/actuator/health"
echo "   • Java API Docs: http://localhost:8080/swagger-ui"
echo "   • Event API: http://localhost:8080/api/v1/events"
echo "   • Chat Interface: http://localhost:8000"
echo ""
echo -e "${BLUE}📝 Management Commands:${NC}"
echo "   • View Java logs: tail -f java_app.log"
echo "   • View Python logs: tail -f python_app.log"
echo "   • Stop databases: docker-compose down"
echo "   • Stop Java: kill $JAVA_PID"
echo "   • Stop Python: kill $PYTHON_PID"
echo ""
echo -e "${YELLOW}🛑 To stop all services: ./stop_full_stack.sh${NC}"
echo ""

# Function to handle cleanup on script exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Stopping services...${NC}"
    kill $JAVA_PID 2>/dev/null || true
    kill $PYTHON_PID 2>/dev/null || true
    echo -e "${GREEN}✅ Local services stopped${NC}"
    echo -e "${YELLOW}💡 Databases are still running in Docker${NC}"
    echo -e "${YELLOW}💡 To stop databases: docker-compose down${NC}"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Keep the script running and show logs
echo -e "${BLUE}📋 Monitoring logs (Ctrl+C to stop monitoring, services will continue running)...${NC}"
echo ""

# Monitor logs
tail -f java_app.log python_app.log

