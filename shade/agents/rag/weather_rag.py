"""Weather RAG system for weather planning knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class WeatherRAGSystem(BaseRAGSystem):
    """RAG system for weather planning knowledge."""
    
    def __init__(self):
        super().__init__("weather_planning")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with weather planning knowledge."""
        self.knowledge_base = [
            {
                "content": "Weather considerations for events: temperature, precipitation, wind, humidity, and seasonal patterns.",
                "metadata": {"type": "factors", "category": "planning"},
                "domain": "weather_planning"
            },
            {
                "content": "Outdoor event weather contingencies: backup indoor venues, weather monitoring, and emergency procedures.",
                "metadata": {"type": "contingencies", "category": "risk_management"},
                "domain": "weather_planning"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load weather planning knowledge base."""
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant weather planning context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            if not relevant_items:
                return "No specific weather planning context found."
            
            context_parts = [f"- {item['content']}" for item in relevant_items]
            return f"Weather Planning Context:\n" + "\n".join(context_parts)
        except Exception as e:
            return f"Error retrieving weather planning context: {str(e)}"
