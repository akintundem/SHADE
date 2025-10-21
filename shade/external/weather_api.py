"""Mock Weather API for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
from datetime import datetime, timedelta
import random

logger = logging.getLogger(__name__)


class WeatherAPIService:
    """Mock Weather API for prototyping - returns consistent weather data."""
    
    def __init__(self):
        """Initialize mock weather API service."""
        self.initialized = False
        self.mock_weather_data = self._initialize_mock_weather_data()
    
    def _initialize_mock_weather_data(self) -> Dict[str, Any]:
        """Initialize with mock weather data for consistent responses."""
        return {
            "current_weather": {
                "temperature": 22,
                "condition": "Partly Cloudy",
                "humidity": 65,
                "wind_speed": 12,
                "wind_direction": "NW",
                "pressure": 1013,
                "visibility": 10,
                "uv_index": 6
            },
            "forecast": [
                {
                    "date": "2024-02-15",
                    "high": 25,
                    "low": 18,
                    "condition": "Sunny",
                    "precipitation_chance": 10,
                    "wind_speed": 8
                },
                {
                    "date": "2024-02-16", 
                    "high": 23,
                    "low": 16,
                    "condition": "Partly Cloudy",
                    "precipitation_chance": 20,
                    "wind_speed": 10
                },
                {
                    "date": "2024-02-17",
                    "high": 20,
                    "low": 14,
                    "condition": "Rainy",
                    "precipitation_chance": 80,
                    "wind_speed": 15
                },
                {
                    "date": "2024-02-18",
                    "high": 19,
                    "low": 12,
                    "condition": "Overcast",
                    "precipitation_chance": 60,
                    "wind_speed": 12
                },
                {
                    "date": "2024-02-19",
                    "high": 24,
                    "low": 17,
                    "condition": "Sunny",
                    "precipitation_chance": 5,
                    "wind_speed": 6
                }
            ],
            "alerts": [
                {
                    "id": "alert_1",
                    "type": "weather_warning",
                    "severity": "moderate",
                    "title": "Rain Expected",
                    "description": "Heavy rain expected on February 17th with 80% chance of precipitation.",
                    "start_time": "2024-02-17T06:00:00Z",
                    "end_time": "2024-02-17T18:00:00Z"
                }
            ],
            "historical": [
                {
                    "date": "2024-02-10",
                    "high": 26,
                    "low": 19,
                    "condition": "Sunny",
                    "precipitation": 0
                },
                {
                    "date": "2024-02-11",
                    "high": 24,
                    "low": 17,
                    "condition": "Partly Cloudy", 
                    "precipitation": 2
                }
            ]
        }
    
    async def initialize(self):
        """Initialize the weather API service."""
        try:
            self.initialized = True
            logger.info("Mock Weather API service initialized")
        except Exception as e:
            logger.error(f"Error initializing Weather API: {e}")
            raise
    
    async def get_current_weather(self, location: str = "New York, NY") -> Dict[str, Any]:
        """Get current weather for a location."""
        try:
            # Add some variation based on location for more realistic prototyping
            weather_data = self.mock_weather_data["current_weather"].copy()
            
            # Simulate location-based variations
            if "london" in location.lower():
                weather_data["temperature"] = 15
                weather_data["condition"] = "Overcast"
                weather_data["humidity"] = 80
            elif "miami" in location.lower():
                weather_data["temperature"] = 28
                weather_data["condition"] = "Sunny"
                weather_data["humidity"] = 75
            elif "seattle" in location.lower():
                weather_data["temperature"] = 18
                weather_data["condition"] = "Rainy"
                weather_data["humidity"] = 85
            
            weather_data["location"] = location
            weather_data["timestamp"] = datetime.utcnow().isoformat()
            
            logger.info(f"Mock current weather retrieved for {location}")
            return {
                "success": True,
                "weather": weather_data
            }
            
        except Exception as e:
            logger.error(f"Error getting current weather: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_forecast(self, location: str = "New York, NY", days: int = 5) -> Dict[str, Any]:
        """Get weather forecast for a location."""
        try:
            forecast_data = self.mock_weather_data["forecast"][:days].copy()
            
            # Add location-specific adjustments
            for day in forecast_data:
                if "london" in location.lower():
                    day["high"] -= 5
                    day["low"] -= 5
                    day["precipitation_chance"] += 20
                elif "miami" in location.lower():
                    day["high"] += 8
                    day["low"] += 8
                    day["precipitation_chance"] += 10
                elif "seattle" in location.lower():
                    day["high"] -= 3
                    day["low"] -= 3
                    day["precipitation_chance"] += 30
            
            logger.info(f"Mock forecast retrieved for {location} ({days} days)")
            return {
                "success": True,
                "location": location,
                "forecast": forecast_data
            }
            
        except Exception as e:
            logger.error(f"Error getting forecast: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_weather_alerts(self, location: str = "New York, NY") -> Dict[str, Any]:
        """Get weather alerts for a location."""
        try:
            alerts = self.mock_weather_data["alerts"].copy()
            
            # Add location-specific alerts
            if "seattle" in location.lower():
                alerts.append({
                    "id": "alert_2",
                    "type": "weather_warning",
                    "severity": "high",
                    "title": "Heavy Rain Warning",
                    "description": "Heavy rainfall expected with potential flooding.",
                    "start_time": "2024-02-20T00:00:00Z",
                    "end_time": "2024-02-21T00:00:00Z"
                })
            
            logger.info(f"Mock weather alerts retrieved for {location}")
            return {
                "success": True,
                "location": location,
                "alerts": alerts
            }
            
        except Exception as e:
            logger.error(f"Error getting weather alerts: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_historical_weather(self, location: str = "New York, NY", start_date: str = None, end_date: str = None) -> Dict[str, Any]:
        """Get historical weather data."""
        try:
            historical_data = self.mock_weather_data["historical"].copy()
            
            # Filter by date range if provided
            if start_date and end_date:
                filtered_data = []
                for record in historical_data:
                    if start_date <= record["date"] <= end_date:
                        filtered_data.append(record)
                historical_data = filtered_data
            
            logger.info(f"Mock historical weather retrieved for {location}")
            return {
                "success": True,
                "location": location,
                "historical": historical_data
            }
            
        except Exception as e:
            logger.error(f"Error getting historical weather: {e}")
            return {"success": False, "error": str(e)}
    
    async def check_outdoor_event_viability(self, location: str, event_date: str, event_time: str = "14:00") -> Dict[str, Any]:
        """Check if weather is suitable for an outdoor event."""
        try:
            # Find forecast for the event date
            event_forecast = None
            for day in self.mock_weather_data["forecast"]:
                if day["date"] == event_date:
                    event_forecast = day
                    break
            
            if not event_forecast:
                # Generate mock forecast for the date
                event_forecast = {
                    "high": 22,
                    "low": 16,
                    "condition": "Partly Cloudy",
                    "precipitation_chance": 30,
                    "wind_speed": 10
                }
            
            # Determine viability based on weather conditions
            is_viable = True
            concerns = []
            recommendations = []
            
            if event_forecast["precipitation_chance"] > 50:
                is_viable = False
                concerns.append("High chance of precipitation")
                recommendations.append("Consider indoor backup venue")
            
            if event_forecast["wind_speed"] > 20:
                concerns.append("High wind speeds")
                recommendations.append("Secure outdoor decorations and equipment")
            
            if event_forecast["high"] > 35 or event_forecast["low"] < 5:
                concerns.append("Extreme temperature conditions")
                recommendations.append("Provide climate control or heating/cooling")
            
            viability_score = 100
            if not is_viable:
                viability_score = 30
            elif concerns:
                viability_score = 70
            
            logger.info(f"Mock outdoor event viability checked for {location} on {event_date}")
            return {
                "success": True,
                "location": location,
                "event_date": event_date,
                "is_viable": is_viable,
                "viability_score": viability_score,
                "weather_forecast": event_forecast,
                "concerns": concerns,
                "recommendations": recommendations
            }
            
        except Exception as e:
            logger.error(f"Error checking outdoor event viability: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_service_stats(self) -> Dict[str, Any]:
        """Get weather API service statistics."""
        try:
            return {
                "current_weather_calls": 1,
                "forecast_calls": 1,
                "alerts_calls": 1,
                "historical_calls": 1,
                "viability_checks": 1,
                "initialized": self.initialized,
                "mock_data_available": len(self.mock_weather_data["forecast"])
            }
            
        except Exception as e:
            logger.error(f"Error getting weather service stats: {e}")
            return {"error": str(e)}
