"""Simple weather checking tool with mock data."""

import os
import random
from typing import Optional, Dict, Any
from dotenv import load_dotenv

# Load environment variables
load_dotenv()


class WeatherTool:
    """Simple tool for checking weather with mock data."""
    
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("OPENWEATHER_API_KEY")
    
    def can_handle(self, message: str) -> bool:
        """Check if this tool can handle the message."""
        keywords = ["weather", "temperature", "rain", "sunny", "forecast", "outdoor", "climate", "conditions"]
        return any(keyword in message.lower() for keyword in keywords)
    
    def _generate_mock_weather(self) -> Dict[str, Any]:
        """Generate realistic mock weather data."""
        conditions = ["Sunny", "Partly cloudy", "Cloudy", "Light rain", "Heavy rain", "Thunderstorm", "Snow"]
        condition = random.choice(conditions)
        
        # Generate temperature based on condition
        if "rain" in condition.lower() or "thunderstorm" in condition.lower():
            temp = random.randint(15, 25)
        elif "snow" in condition.lower():
            temp = random.randint(-5, 5)
        else:
            temp = random.randint(18, 30)
        
        # Generate humidity based on condition
        if "rain" in condition.lower() or "thunderstorm" in condition.lower():
            humidity = random.randint(70, 95)
        else:
            humidity = random.randint(40, 70)
        
        # Generate wind speed
        wind_speed = random.randint(5, 25)
        
        # Generate recommendation based on weather
        if "sunny" in condition.lower():
            recommendation = "Perfect weather for outdoor events! Don't forget sunscreen."
        elif "rain" in condition.lower() or "thunderstorm" in condition.lower():
            recommendation = "Consider indoor alternatives or have a solid backup plan. Umbrellas recommended."
        elif "snow" in condition.lower():
            recommendation = "Winter weather conditions. Ensure proper heating and safety measures."
        else:
            recommendation = "Good weather for outdoor events. Monitor conditions closely."
        
        return {
            "temperature": f"{temp}°C",
            "condition": condition,
            "humidity": f"{humidity}%",
            "wind_speed": f"{wind_speed} km/h",
            "recommendation": recommendation
        }
    
    def process(self, message: str) -> Dict[str, Any]:
        """Process the message and return weather info with event planning focus."""
        # Always return mock data regardless of API key
        mock_data = self._generate_mock_weather()
        
        # Create event-focused weather response
        condition = mock_data['condition']
        temp = mock_data['temperature']
        recommendation = mock_data['recommendation']
        
        # Event-specific weather analysis
        event_analysis = self._analyze_weather_for_event(mock_data)
        
        if "sunny" in condition.lower():
            message = f"☀️ Perfect event weather! {condition} and {temp} - absolutely ideal for your event! {recommendation}\n\n{event_analysis}"
        elif "rain" in condition.lower():
            message = f"🌧️ Weather check: {condition.lower()} with {temp}. {recommendation}\n\n{event_analysis}"
        elif "snow" in condition.lower():
            message = f"❄️ Winter wonderland! {condition} and {temp}! {recommendation}\n\n{event_analysis}"
        else:
            message = f"🌤️ Weather update: {condition.lower()} with {temp}. {recommendation}\n\n{event_analysis}"
        
        return {
            "success": True,
            "message": message,
            "data": mock_data
        }
    
    def _analyze_weather_for_event(self, weather_data: Dict[str, Any]) -> str:
        """Analyze weather data specifically for event planning."""
        condition = weather_data['condition'].lower()
        temp_str = weather_data['temperature']
        temp = int(temp_str.replace('°C', ''))
        
        # Extract temperature for analysis
        if temp >= 25:
            temp_category = "hot"
        elif temp >= 15:
            temp_category = "warm"
        elif temp >= 5:
            temp_category = "cool"
        else:
            temp_category = "cold"
        
        # Event-specific recommendations
        if "sunny" in condition:
            if temp_category == "hot":
                return "🎯 **Event Planning Tips:**\n• Consider shade or indoor options for comfort\n• Provide plenty of water and cooling stations\n• Schedule breaks in air-conditioned areas\n• Perfect for outdoor photos and activities!"
            elif temp_category == "warm":
                return "🎯 **Event Planning Tips:**\n• Ideal weather for outdoor events!\n• Consider light refreshments and seating\n• Perfect for networking and mingling\n• Great for photo opportunities!"
            else:
                return "🎯 **Event Planning Tips:**\n• Beautiful clear weather for your event\n• Consider heating options for comfort\n• Perfect for outdoor activities\n• Great visibility for all attendees!"
        
        elif "rain" in condition:
            return "🎯 **Event Planning Tips:**\n• **Indoor backup plan essential!**\n• Consider covered outdoor areas\n• Provide umbrellas and rain gear\n• Cozy indoor atmosphere can be magical\n• Perfect for intimate gatherings!"
        
        elif "snow" in condition:
            return "🎯 **Event Planning Tips:**\n• **Indoor venue recommended**\n• Create cozy, warm atmosphere\n• Consider hot beverages and comfort food\n• Perfect for winter-themed events\n• Beautiful backdrop for photos!"
        
        elif "cloudy" in condition:
            return "🎯 **Event Planning Tips:**\n• Excellent weather for events!\n• No harsh sun or rain concerns\n• Perfect for outdoor activities\n• Comfortable temperature for all\n• Great for long-duration events!"
        
        else:
            return "🎯 **Event Planning Tips:**\n• Weather looks good for your event!\n• Monitor conditions as event approaches\n• Have backup plans ready\n• Consider attendee comfort needs\n• Focus on making it memorable!"
