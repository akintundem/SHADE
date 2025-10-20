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
        """Process the message and return weather info."""
        # Always return mock data regardless of API key
        mock_data = self._generate_mock_weather()
        
        # Create a personable weather response
        condition = mock_data['condition']
        temp = mock_data['temperature']
        recommendation = mock_data['recommendation']
        
        if "sunny" in condition.lower():
            message = f"☀️ Oh my, it's absolutely gorgeous out there! {condition} and {temp} - perfect weather for your event! {recommendation}"
        elif "rain" in condition.lower():
            message = f"🌧️ Well, we've got some {condition.lower()} with {temp} today. {recommendation} But don't worry, we can make your event amazing indoors too!"
        elif "snow" in condition.lower():
            message = f"❄️ Brrr! It's a winter wonderland out there - {condition} and {temp}! {recommendation} Perfect for a cozy indoor gathering!"
        else:
            message = f"🌤️ The weather is looking {condition.lower()} with {temp} today. {recommendation}"
        
        return {
            "success": True,
            "message": message,
            "data": mock_data
        }
