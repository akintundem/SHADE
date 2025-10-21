"""Weather tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional
from .base import java_client


@tool
async def get_weather_forecast(location: str, days: int = 7) -> Dict[str, Any]:
    """Get weather forecast for a specific location.
    
    Args:
        location: Location to get weather for
        days: Number of days to forecast (default: 7)
    
    Returns:
        Dict with weather forecast data
    """
    # Mock weather data for now
    return {
        "success": True,
        "location": location,
        "forecast": {
            "current": {
                "temperature": 22,
                "condition": "Sunny",
                "humidity": 65,
                "wind_speed": 10
            },
            "daily": [
                {
                    "date": "2024-01-15",
                    "high": 25,
                    "low": 18,
                    "condition": "Sunny",
                    "precipitation": 0
                },
                {
                    "date": "2024-01-16", 
                    "high": 23,
                    "low": 16,
                    "condition": "Partly Cloudy",
                    "precipitation": 20
                }
            ]
        }
    }


@tool
async def check_weather_alerts(location: str) -> Dict[str, Any]:
    """Check for weather alerts in a specific location.
    
    Args:
        location: Location to check for alerts
    
    Returns:
        Dict with weather alert information
    """
    # Mock alert data
    return {
        "success": True,
        "location": location,
        "alerts": [],
        "message": "No weather alerts for this location"
    }


@tool
async def get_weather_history(location: str, days: int = 30) -> Dict[str, Any]:
    """Get historical weather data for a location.
    
    Args:
        location: Location to get history for
        days: Number of days to look back (default: 30)
    
    Returns:
        Dict with historical weather data
    """
    # Mock historical data
    return {
        "success": True,
        "location": location,
        "period": f"Last {days} days",
        "average_temperature": 20,
        "average_humidity": 70,
        "total_precipitation": 45,
        "summary": "Mild weather with occasional rain"
    }
