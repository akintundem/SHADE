"""Weather Agent - Specialized in weather monitoring and planning."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.weather_tools import get_weather_forecast, check_weather_alerts, get_weather_history
from .rag.weather_rag import WeatherRAGSystem

class WeatherAgent(BaseAgent):
    """Specialized agent for weather monitoring and planning."""
    
    def __init__(self, message_bus=None):
        super().__init__("Weather Agent", "gpt-4o", 0.5, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Weather Agent."""
        return """
        You help with weather info for event planning. Keep responses SHORT (1-2 sentences).
        
        RULES:
        - Use get_weather_forecast tool when you have location + date
        - If missing info, ask briefly: "Where's the event?" or "What date?"
        - Remember conversation context
        - Be practical and helpful
        - Use 1-2 emojis max
        
        Examples:
        - "Where's the event?" 
        - "Looking good! 22°C and sunny ☀️"
        - "Might rain that day - want a backup plan?"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Weather Agent."""
        return [
            get_weather_forecast,
            check_weather_alerts,
            get_weather_history
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Weather Agent."""
        return WeatherRAGSystem()
