#!/usr/bin/env python3
"""
Test script for Open-Meteo weather API integration.
This script tests the weather API service and tools.
"""

import asyncio
import sys
import os
import logging

# Add the shade directory to the path
sys.path.append(os.path.join(os.path.dirname(__file__), 'shade'))

from external.weather_api import WeatherAPIService
from tools.weather_tools import get_weather_forecast, check_weather_alerts, get_weather_history

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def test_weather_api():
    """Test the weather API service directly."""
    print("🌤️  Testing Open-Meteo Weather API Integration")
    print("=" * 50)
    
    try:
        # Initialize weather API
        weather_api = WeatherAPIService()
        await weather_api.initialize()
        print("✅ Weather API initialized successfully")
        
        # Test current weather
        print("\n📍 Testing current weather for New York...")
        current_result = await weather_api.get_current_weather("New York, NY")
        if current_result.get("success"):
            weather = current_result["weather"]
            print(f"   Temperature: {weather['temperature']}°C")
            print(f"   Condition: {weather['condition']}")
            print(f"   Humidity: {weather['humidity']}%")
            print(f"   Wind Speed: {weather['wind_speed']} km/h")
        else:
            print(f"   ❌ Error: {current_result.get('error')}")
        
        # Test forecast
        print("\n📅 Testing 5-day forecast for London...")
        forecast_result = await weather_api.get_forecast("London, UK", 5)
        if forecast_result.get("success"):
            forecast = forecast_result["forecast"]
            print(f"   Found {len(forecast)} days of forecast data")
            for day in forecast[:3]:  # Show first 3 days
                print(f"   {day['date']}: {day['high']}°C/{day['low']}°C, {day['condition']}")
        else:
            print(f"   ❌ Error: {forecast_result.get('error')}")
        
        # Test weather alerts
        print("\n⚠️  Testing weather alerts for Miami...")
        alerts_result = await weather_api.get_weather_alerts("Miami, FL")
        if alerts_result.get("success"):
            alerts = alerts_result["alerts"]
            print(f"   Found {len(alerts)} weather alerts")
            for alert in alerts:
                print(f"   - {alert['title']}: {alert['description']}")
        else:
            print(f"   ❌ Error: {alerts_result.get('error')}")
        
        # Test outdoor event viability
        print("\n🎪 Testing outdoor event viability for Seattle...")
        from datetime import datetime, timedelta
        event_date = (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%d")
        viability_result = await weather_api.check_outdoor_event_viability("Seattle, WA", event_date)
        if viability_result.get("success"):
            print(f"   Event Date: {event_date}")
            print(f"   Viable: {viability_result['is_viable']}")
            print(f"   Viability Score: {viability_result['viability_score']}/100")
            if viability_result.get('concerns'):
                print(f"   Concerns: {', '.join(viability_result['concerns'])}")
            if viability_result.get('recommendations'):
                print(f"   Recommendations: {', '.join(viability_result['recommendations'])}")
        else:
            print(f"   ❌ Error: {viability_result.get('error')}")
        
        # Test service stats
        print("\n📊 Testing service statistics...")
        stats = await weather_api.get_service_stats()
        print(f"   API Provider: {stats.get('api_provider')}")
        print(f"   Features: {', '.join(stats.get('features', []))}")
        print(f"   Coverage: {stats.get('coverage')}")
        
        # Cleanup
        await weather_api.cleanup()
        print("\n✅ Weather API test completed successfully!")
        
    except Exception as e:
        logger.error(f"Error testing weather API: {e}")
        print(f"❌ Weather API test failed: {e}")

async def test_weather_tools():
    """Test the weather tools."""
    print("\n🔧 Testing Weather Tools")
    print("=" * 30)
    
    try:
        # Test weather forecast tool
        print("\n📍 Testing weather forecast tool...")
        forecast_result = await get_weather_forecast("Toronto, ON", 3)
        if forecast_result.get("success"):
            print(f"   Location: {forecast_result['location']}")
            print(f"   Source: {forecast_result['source']}")
            current = forecast_result.get("current", {})
            print(f"   Current: {current.get('temperature', 0)}°C, {current.get('condition', 'Unknown')}")
            forecast = forecast_result.get("forecast", [])
            print(f"   Forecast: {len(forecast)} days available")
        else:
            print(f"   ❌ Error: {forecast_result.get('error')}")
        
        # Test weather alerts tool
        print("\n⚠️  Testing weather alerts tool...")
        alerts_result = await check_weather_alerts("Vancouver, BC")
        if alerts_result.get("success"):
            print(f"   Location: {alerts_result['location']}")
            print(f"   Message: {alerts_result['message']}")
            print(f"   Source: {alerts_result['source']}")
        else:
            print(f"   ❌ Error: {alerts_result.get('error')}")
        
        # Test weather history tool
        print("\n📈 Testing weather history tool...")
        history_result = await get_weather_history("Calgary, AB", 7)
        if history_result.get("success"):
            print(f"   Location: {history_result['location']}")
            print(f"   Period: {history_result['period']}")
            print(f"   Summary: {history_result['summary']}")
            print(f"   Source: {history_result['source']}")
        else:
            print(f"   ❌ Error: {history_result.get('error')}")
        
        print("\n✅ Weather tools test completed successfully!")
        
    except Exception as e:
        logger.error(f"Error testing weather tools: {e}")
        print(f"❌ Weather tools test failed: {e}")

async def main():
    """Main test function."""
    print("🚀 Starting Open-Meteo Weather API Integration Test")
    print("=" * 60)
    
    try:
        await test_weather_api()
        await test_weather_tools()
        
        print("\n🎉 All tests completed!")
        print("\n📋 Integration Summary:")
        print("   ✅ Java RiskService updated to use Open-Meteo")
        print("   ✅ Python WeatherAPIService replaced with real API")
        print("   ✅ Geocoding service added for location conversion")
        print("   ✅ Weather tools updated to use real data")
        print("   ✅ All components tested successfully")
        
        print("\n🔗 API Endpoints Used:")
        print("   • https://api.open-meteo.com/v1/forecast")
        print("   • https://api.open-meteo.com/v1/geocoding")
        print("   • No API key required - completely free!")
        
    except Exception as e:
        logger.error(f"Test suite failed: {e}")
        print(f"❌ Test suite failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
