#!/bin/bash

# Railway Deployment Script
# This script helps deploy to Railway

echo "🚂 Railway Deployment Script"
echo "============================"
echo ""

# Check if railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI is not installed"
    echo "Install it from: https://docs.railway.app/develop/cli"
    exit 1
fi

# Link to project
echo "📌 Linking to event-planner-monolith project..."
railway link --project event-planner-monolith

echo ""
echo "📋 Available commands:"
echo "1. railway service          # Select a service (run this first)"
echo "2. railway up               # Deploy the current code"
echo "3. railway domain           # Generate/get deployment URL"
echo "4. railway logs             # View deployment logs"
echo ""

# Check if service is linked
echo "🔍 Checking service status..."
railway status

echo ""
echo "💡 To deploy:"
echo "   Step 1: Run 'railway service' and select your service"
echo "   Step 2: Run 'railway up' to deploy"
echo "   Step 3: Run 'railway domain' to get your deployment URL"
echo ""
echo "   OR run: railway deployment up"
echo ""

# Try to deploy if service is already linked
if railway status 2>&1 | grep -q "Service:"; then
    SERVICE=$(railway status 2>&1 | grep "Service:" | awk '{print $2}')
    if [ "$SERVICE" != "None" ]; then
        echo "✅ Service found: $SERVICE"
        echo "🚀 Deploying..."
        railway up
    else
        echo "⚠️  No service linked. Please run 'railway service' first"
    fi
else
    echo "⚠️  Please run 'railway service' to link a service first"
fi

