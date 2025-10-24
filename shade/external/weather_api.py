"""Open-Meteo Weather API integration for Event Planner."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
import aiohttp
import json
from datetime import datetime, timedelta
import random

logger = logging.getLogger(__name__)


class WeatherAPIService:
    """Open-Meteo Weather API service for real weather data."""
    
    def __init__(self):
        """Initialize Open-Meteo weather API service."""
        self.initialized = False
        self.base_url = "https://api.open-meteo.com/v1"
        self.session = None
    
    async def _geocode_location(self, location: str) -> tuple[float, float]:
        """Convert location string to latitude and longitude using Open-Meteo geocoding."""
        try:
            if not self.session:
                self.session = aiohttp.ClientSession()
            
            geocode_url = f"{self.base_url}/geocoding"
            params = {
                "name": location,
                "count": 1,
                "language": "en",
                "format": "json"
            }
            
            async with self.session.get(geocode_url, params=params) as response:
                if response.status == 200:
                    data = await response.json()
                    if data.get("results"):
                        result = data["results"][0]
                        return result["latitude"], result["longitude"]
                
            # Fallback coordinates for major cities
            fallback_coords = {
                "new york": (40.7128, -74.0060),
                "london": (51.5074, -0.1278),
                "miami": (25.7617, -80.1918),
                "seattle": (47.6062, -122.3321),
                "winnipeg": (49.8951, -97.1384)
            }
            
            location_lower = location.lower()
            for city, coords in fallback_coords.items():
                if city in location_lower:
                    return coords
            
            # Default to New York if no match
            return (40.7128, -74.0060)
            
        except Exception as e:
            logger.error(f"Error geocoding location {location}: {e}")
            return (40.7128, -74.0060)  # Default to New York
    
    async def initialize(self):
        """Initialize the weather API service."""
        try:
            self.session = aiohttp.ClientSession()
            self.initialized = True
            logger.info("Open-Meteo Weather API service initialized")
        except Exception as e:
            logger.error(f"Error initializing Weather API: {e}")
            raise
    
    async def _get_weather_data(self, lat: float, lon: float, days: int = 7) -> Dict[str, Any]:
        """Get weather data from Open-Meteo API."""
        try:
            if not self.session:
                self.session = aiohttp.ClientSession()
            
            url = f"{self.base_url}/forecast"
            params = {
                "latitude": lat,
                "longitude": lon,
                "current": "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m",
                "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max",
                "timezone": "auto"
            }
            
            async with self.session.get(url, params=params) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    logger.error(f"Open-Meteo API error: {response.status}")
                    return None
                    
        except Exception as e:
            logger.error(f"Error fetching weather data: {e}")
            return None
    
    def _parse_weather_code(self, code: int) -> str:
        """Convert Open-Meteo weather code to human-readable condition."""
        weather_codes = {
            0: "Clear sky",
            1: "Mainly clear",
            2: "Partly cloudy", 
            3: "Overcast",
            45: "Fog",
            48: "Depositing rime fog",
            51: "Light drizzle",
            53: "Moderate drizzle",
            55: "Dense drizzle",
            61: "Slight rain",
            63: "Moderate rain",
            65: "Heavy rain",
            71: "Slight snow",
            73: "Moderate snow",
            75: "Heavy snow",
            77: "Snow grains",
            80: "Slight rain showers",
            81: "Moderate rain showers",
            82: "Violent rain showers",
            85: "Slight snow showers",
            86: "Heavy snow showers",
            95: "Thunderstorm",
            96: "Thunderstorm with slight hail",
            99: "Thunderstorm with heavy hail"
        }
        return weather_codes.get(code, "Unknown")
    
    async def get_current_weather(self, location: str = "New York, NY") -> Dict[str, Any]:
        """Get current weather for a location."""
        try:
            # Geocode location to get coordinates
            lat, lon = await self._geocode_location(location)
            
            # Get weather data from Open-Meteo
            weather_data = await self._get_weather_data(lat, lon)
            
            if not weather_data:
                return {"success": False, "error": "Unable to fetch weather data"}
            
            # Parse current weather
            current = weather_data.get("current", {})
            weather_code = current.get("weather_code", 0)
            
            weather_info = {
                "temperature": current.get("temperature_2m", 0),
                "condition": self._parse_weather_code(weather_code),
                "humidity": current.get("relative_humidity_2m", 0),
                "wind_speed": current.get("wind_speed_10m", 0),
                "wind_direction": current.get("wind_direction_10m", 0),
                "precipitation": current.get("precipitation", 0),
                "weather_code": weather_code,
                "location": location,
                "timestamp": datetime.utcnow().isoformat()
            }
            
            logger.info(f"Current weather retrieved for {location}")
            return {
                "success": True,
                "weather": weather_info
            }
            
        except Exception as e:
            logger.error(f"Error getting current weather: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_forecast(self, location: str = "New York, NY", days: int = 5) -> Dict[str, Any]:
        """Get weather forecast for a location."""
        try:
            # Geocode location to get coordinates
            lat, lon = await self._geocode_location(location)
            
            # Get weather data from Open-Meteo
            weather_data = await self._get_weather_data(lat, lon)
            
            if not weather_data:
                return {"success": False, "error": "Unable to fetch weather data"}
            
            # Parse daily forecast
            daily = weather_data.get("daily", {})
            dates = daily.get("time", [])
            max_temps = daily.get("temperature_2m_max", [])
            min_temps = daily.get("temperature_2m_min", [])
            precip_sums = daily.get("precipitation_sum", [])
            precip_probs = daily.get("precipitation_probability_max", [])
            weather_codes = daily.get("weather_code", [])
            wind_speeds = daily.get("wind_speed_10m_max", [])
            
            forecast_data = []
            for i in range(min(days, len(dates))):
                weather_code = weather_codes[i] if i < len(weather_codes) else 0
                forecast_data.append({
                    "date": dates[i],
                    "high": max_temps[i] if i < len(max_temps) else 0,
                    "low": min_temps[i] if i < len(min_temps) else 0,
                    "condition": self._parse_weather_code(weather_code),
                    "precipitation_chance": precip_probs[i] if i < len(precip_probs) else 0,
                    "precipitation_sum": precip_sums[i] if i < len(precip_sums) else 0,
                    "wind_speed": wind_speeds[i] if i < len(wind_speeds) else 0,
                    "weather_code": weather_code
                })
            
            logger.info(f"Forecast retrieved for {location} ({days} days)")
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
            # Geocode location to get coordinates
            lat, lon = await self._geocode_location(location)
            
            # Get weather data to analyze for alerts
            weather_data = await self._get_weather_data(lat, lon)
            
            if not weather_data:
                return {"success": False, "error": "Unable to fetch weather data"}
            
            alerts = []
            current = weather_data.get("current", {})
            daily = weather_data.get("daily", {})
            
            # Check for severe weather conditions
            weather_code = current.get("weather_code", 0)
            wind_speed = current.get("wind_speed_10m", 0)
            precipitation = current.get("precipitation", 0)
            
            # Generate alerts based on weather conditions
            if weather_code >= 95:  # Thunderstorm
                alerts.append({
                    "id": "thunderstorm_alert",
                    "type": "weather_warning",
                    "severity": "high",
                    "title": "Thunderstorm Warning",
                    "description": "Thunderstorms detected in the area. Consider postponing outdoor activities.",
                    "start_time": datetime.utcnow().isoformat(),
                    "end_time": (datetime.utcnow() + timedelta(hours=2)).isoformat()
                })
            
            if wind_speed > 20:
                alerts.append({
                    "id": "wind_alert",
                    "type": "weather_warning", 
                    "severity": "medium",
                    "title": "High Wind Warning",
                    "description": f"Wind speeds of {wind_speed:.1f} km/h may affect outdoor setup.",
                    "start_time": datetime.utcnow().isoformat(),
                    "end_time": (datetime.utcnow() + timedelta(hours=6)).isoformat()
                })
            
            if precipitation > 5:
                alerts.append({
                    "id": "rain_alert",
                    "type": "weather_warning",
                    "severity": "medium", 
                    "title": "Heavy Precipitation",
                    "description": f"Heavy precipitation of {precipitation:.1f}mm expected.",
                    "start_time": datetime.utcnow().isoformat(),
                    "end_time": (datetime.utcnow() + timedelta(hours=4)).isoformat()
                })
            
            logger.info(f"Weather alerts retrieved for {location}")
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
            # Geocode location to get coordinates
            lat, lon = await self._geocode_location(location)
            
            if not self.session:
                self.session = aiohttp.ClientSession()
            
            # Use historical API endpoint
            url = f"{self.base_url}/forecast"
            params = {
                "latitude": lat,
                "longitude": lon,
                "start_date": start_date or (datetime.now() - timedelta(days=7)).strftime("%Y-%m-%d"),
                "end_date": end_date or datetime.now().strftime("%Y-%m-%d"),
                "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code"
            }
            
            async with self.session.get(url, params=params) as response:
                if response.status == 200:
                    data = await response.json()
                    daily = data.get("daily", {})
                    
                    historical_data = []
                    dates = daily.get("time", [])
                    max_temps = daily.get("temperature_2m_max", [])
                    min_temps = daily.get("temperature_2m_min", [])
                    precip_sums = daily.get("precipitation_sum", [])
                    weather_codes = daily.get("weather_code", [])
                    
                    for i in range(len(dates)):
                        weather_code = weather_codes[i] if i < len(weather_codes) else 0
                        historical_data.append({
                            "date": dates[i],
                            "high": max_temps[i] if i < len(max_temps) else 0,
                            "low": min_temps[i] if i < len(min_temps) else 0,
                            "condition": self._parse_weather_code(weather_code),
                            "precipitation": precip_sums[i] if i < len(precip_sums) else 0,
                            "weather_code": weather_code
                        })
                    
                    logger.info(f"Historical weather retrieved for {location}")
                    return {
                        "success": True,
                        "location": location,
                        "historical": historical_data
                    }
                else:
                    return {"success": False, "error": f"API error: {response.status}"}
            
        except Exception as e:
            logger.error(f"Error getting historical weather: {e}")
            return {"success": False, "error": str(e)}
    
    async def check_outdoor_event_viability(self, location: str, event_date: str, event_time: str = "14:00") -> Dict[str, Any]:
        """Check if weather is suitable for an outdoor event."""
        try:
            # Geocode location to get coordinates
            lat, lon = await self._geocode_location(location)
            
            # Get weather data from Open-Meteo
            weather_data = await self._get_weather_data(lat, lon)
            
            if not weather_data:
                return {"success": False, "error": "Unable to fetch weather data"}
            
            # Parse forecast data
            daily = weather_data.get("daily", {})
            dates = daily.get("time", [])
            max_temps = daily.get("temperature_2m_max", [])
            min_temps = daily.get("temperature_2m_min", [])
            precip_probs = daily.get("precipitation_probability_max", [])
            weather_codes = daily.get("weather_code", [])
            wind_speeds = daily.get("wind_speed_10m_max", [])
            
            # Find forecast for the event date
            event_forecast = None
            for i, date in enumerate(dates):
                if date == event_date:
                    weather_code = weather_codes[i] if i < len(weather_codes) else 0
                    event_forecast = {
                        "high": max_temps[i] if i < len(max_temps) else 0,
                        "low": min_temps[i] if i < len(min_temps) else 0,
                        "condition": self._parse_weather_code(weather_code),
                        "precipitation_chance": precip_probs[i] if i < len(precip_probs) else 0,
                        "wind_speed": wind_speeds[i] if i < len(wind_speeds) else 0,
                        "weather_code": weather_code
                    }
                    break
            
            if not event_forecast:
                # Use current weather if event date not found
                current = weather_data.get("current", {})
                event_forecast = {
                    "high": current.get("temperature_2m", 0) + 5,
                    "low": current.get("temperature_2m", 0) - 5,
                    "condition": self._parse_weather_code(current.get("weather_code", 0)),
                    "precipitation_chance": 30,
                    "wind_speed": current.get("wind_speed_10m", 0),
                    "weather_code": current.get("weather_code", 0)
                }
            
            # Determine viability based on weather conditions
            is_viable = True
            concerns = []
            recommendations = []
            
            # Check precipitation risk
            if event_forecast["precipitation_chance"] > 50:
                is_viable = False
                concerns.append("High chance of precipitation")
                recommendations.append("Consider indoor backup venue")
            elif event_forecast["precipitation_chance"] > 30:
                concerns.append("Moderate precipitation risk")
                recommendations.append("Prepare covered areas and umbrellas")
            
            # Check wind risk
            if event_forecast["wind_speed"] > 20:
                concerns.append("High wind speeds")
                recommendations.append("Secure outdoor decorations and equipment")
            elif event_forecast["wind_speed"] > 15:
                concerns.append("Moderate wind conditions")
                recommendations.append("Secure lightweight items")
            
            # Check temperature extremes
            if event_forecast["high"] > 35 or event_forecast["low"] < 5:
                concerns.append("Extreme temperature conditions")
                recommendations.append("Provide climate control or heating/cooling")
            elif event_forecast["high"] > 30 or event_forecast["low"] < 10:
                concerns.append("Challenging temperature conditions")
                recommendations.append("Provide shade and heating/cooling options")
            
            # Check for severe weather codes
            weather_code = event_forecast.get("weather_code", 0)
            if weather_code >= 95:  # Thunderstorm
                is_viable = False
                concerns.append("Severe weather warning")
                recommendations.append("Postpone or move indoors immediately")
            elif weather_code >= 80:  # Rain showers
                concerns.append("Rain showers expected")
                recommendations.append("Prepare covered areas")
            
            # Calculate viability score
            viability_score = 100
            if not is_viable:
                viability_score = 20
            elif len(concerns) >= 3:
                viability_score = 40
            elif len(concerns) >= 2:
                viability_score = 60
            elif len(concerns) >= 1:
                viability_score = 80
            
            logger.info(f"Outdoor event viability checked for {location} on {event_date}")
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
                "api_provider": "Open-Meteo",
                "base_url": self.base_url,
                "initialized": self.initialized,
                "session_active": self.session is not None,
                "features": [
                    "Current weather",
                    "7-day forecast", 
                    "Weather alerts",
                    "Historical data",
                    "Outdoor event viability",
                    "Geocoding"
                ],
                "rate_limits": "No API key required, generous rate limits",
                "coverage": "Global"
            }
            
        except Exception as e:
            logger.error(f"Error getting weather service stats: {e}")
            return {"error": str(e)}
    
    async def cleanup(self):
        """Clean up resources."""
        try:
            if self.session:
                await self.session.close()
                self.session = None
            logger.info("Weather API service cleaned up")
        except Exception as e:
            logger.error(f"Error cleaning up weather service: {e}")
