#!/bin/bash

# Quick test script for Railway deployment
# Usage: ./test_railway.sh <RAILWAY_URL>

if [ -z "$1" ]; then
    echo "❌ Please provide Railway URL"
    echo "Usage: ./test_railway.sh <RAILWAY_URL>"
    echo "Example: ./test_railway.sh https://yourapp.railway.app"
    exit 1
fi

RAILWAY_URL="$1"
echo "🚂 Testing Railway deployment at: $RAILWAY_URL"
echo ""

# Run auth endpoint tests
echo "🔐 Running Auth Endpoint Tests..."
cd "$(dirname "$0")"
./test_auth_endpoints.sh prod "$RAILWAY_URL"

# Run event endpoint tests  
echo ""
echo "📅 Running Event Endpoint Tests..."
./test_event_endpoints.sh prod "$RAILWAY_URL"

echo ""
echo "✅ All tests completed!"
echo "📄 Check test/reports/ for detailed reports"

