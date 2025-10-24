"""Weather tools for the Shade agent using Open-Meteo API."""

from langchain.tools import tool
from typing import Dict, Any, Optional
import asyncio
import logging
from .base import java_client

logger = logging.getLogger(__name__)

# Global weather API instance
_weather_api = None

async def _get_weather_api():
    """Get or create weather API instance."""
    global _weather_api
    if _weather_api is None:
        from external.weather_api import WeatherAPIService
        _weather_api = WeatherAPIService()
        await _weather_api.initialize()
    return _weather_api

@tool
async def get_weather_forecast(location: str, days: int = 7) -> Dict[str, Any]:
    """Get weather forecast for a specific location using Open-Meteo API.
    
    Args:
        location: Location to get weather for
        days: Number of days to forecast (default: 7)
    
    Returns:
        Dict with weather forecast data
    """
    try:
        weather_api = await _get_weather_api()
        
        # Get current weather
        current_result = await weather_api.get_current_weather(location)
        if not current_result.get("success"):
            return {"success": False, "error": "Failed to get current weather"}
        
        # Get forecast
        forecast_result = await weather_api.get_forecast(location, days)
        if not forecast_result.get("success"):
            return {"success": False, "error": "Failed to get forecast"}
        
        return {
            "success": True,
            "location": location,
            "current": current_result["weather"],
            "forecast": forecast_result["forecast"],
            "source": "Open-Meteo API"
        }
        
    except Exception as e:
        logger.error(f"Error getting weather forecast: {e}")
        return {"success": False, "error": str(e)}


@tool
async def check_weather_alerts(location: str) -> Dict[str, Any]:
    """Check for weather alerts in a specific location using Open-Meteo API.
    
    Args:
        location: Location to check for alerts
    
    Returns:
        Dict with weather alert information
    """
    try:
        weather_api = await _get_weather_api()
        
        result = await weather_api.get_weather_alerts(location)
        if not result.get("success"):
            return {"success": False, "error": "Failed to get weather alerts"}
        
        alerts = result.get("alerts", [])
        message = f"Found {len(alerts)} weather alerts" if alerts else "No weather alerts for this location"
        
        return {
            "success": True,
            "location": location,
            "alerts": alerts,
            "message": message,
            "source": "Open-Meteo API"
        }
        
    except Exception as e:
        logger.error(f"Error checking weather alerts: {e}")
        return {"success": False, "error": str(e)}


@tool
async def get_weather_history(location: str, days: int = 30) -> Dict[str, Any]:
    """Get historical weather data for a location using Open-Meteo API.
    
    Args:
        location: Location to get history for
        days: Number of days to look back (default: 30)
    
    Returns:
        Dict with historical weather data
    """
    try:
        weather_api = await _get_weather_api()
        
        # Calculate date range
        from datetime import datetime, timedelta
        end_date = datetime.now().strftime("%Y-%m-%d")
        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        
        result = await weather_api.get_historical_weather(location, start_date, end_date)
        if not result.get("success"):
            return {"success": False, "error": "Failed to get historical weather"}
        
        historical_data = result.get("historical", [])
        
        # Calculate summary statistics
        if historical_data:
            temps = [day["high"] for day in historical_data if "high" in day]
            precip = [day.get("precipitation", 0) for day in historical_data]
            
            avg_temp = sum(temps) / len(temps) if temps else 0
            total_precip = sum(precip)
            
            summary = f"Average temperature: {avg_temp:.1f}°C, Total precipitation: {total_precip:.1f}mm"
        else:
            summary = "No historical data available"
        
        return {
            "success": True,
            "location": location,
            "period": f"Last {days} days",
            "historical_data": historical_data,
            "summary": summary,
            "source": "Open-Meteo API"
        }
        
    except Exception as e:
        logger.error(f"Error getting weather history: {e}")
        return {"success": False, "error": str(e)}
