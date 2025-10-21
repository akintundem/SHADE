#!/bin/bash

# Full Stack Event Planner Stop Script
# This script stops both the Java Spring Boot application and the Python Shade Assistant

echo "🛑 Stopping Event Planner Full Stack Application"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to kill processes on specific ports
kill_port() {
    local port=$1
    local service_name=$2
    
    echo -e "${YELLOW}🔍 Looking for processes on port $port...${NC}"
    
    # Find PIDs using the port
    local pids=$(lsof -ti:$port 2>/dev/null)
    
    if [ -n "$pids" ]; then
        echo -e "${YELLOW}   Found processes: $pids${NC}"
        echo "$pids" | xargs kill -9 2>/dev/null
        echo -e "${GREEN}✅ Stopped $service_name${NC}"
    else
        echo -e "${BLUE}ℹ️  No processes found on port $port${NC}"
    fi
}

# Stop Java Application (port 8080)
echo -e "${BLUE}☕ Stopping Java Spring Boot Application...${NC}"
kill_port 8080 "Java Application"

# Stop Python Shade Assistant (port 8000)
echo -e "${BLUE}🐍 Stopping Python Shade Assistant...${NC}"
kill_port 8000 "Python Shade Assistant"

# Kill any remaining Maven processes
echo -e "${YELLOW}🧹 Cleaning up Maven processes...${NC}"
pkill -f "spring-boot:run" 2>/dev/null || true

# Kill any remaining Python processes
echo -e "${YELLOW}🧹 Cleaning up Python processes...${NC}"
pkill -f "python.*main.py" 2>/dev/null || true

# Wait a moment for processes to fully stop
sleep 2

# Verify ports are free
echo -e "${BLUE}🔍 Verifying ports are free...${NC}"

if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}❌ Port 8080 is still in use${NC}"
else
    echo -e "${GREEN}✅ Port 8080 is free${NC}"
fi

if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}❌ Port 8000 is still in use${NC}"
else
    echo -e "${GREEN}✅ Port 8000 is free${NC}"
fi

echo ""
echo -e "${GREEN}🎉 All services stopped successfully!${NC}"
echo "=============================================="
echo ""
echo -e "${BLUE}📝 Log files are preserved:${NC}"
echo "   • java_app.log"
echo "   • python_app.log"
echo ""
echo -e "${YELLOW}💡 To start again, run: ./start_full_stack.sh${NC}"
