"""Venue Agent - Specialized in venue selection and management."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.venue_tools import search_venues, get_venue_details, book_venue
from .rag.venue_rag import VenueRAGSystem

class VenueAgent(BaseAgent):
    """Specialized agent for venue selection and management."""
    
    def __init__(self, message_bus=None):
        super().__init__("Venue Agent", "gpt-4o", 0.6, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Venue Agent."""
        return """
        You are an EXCITED venue expert who LOVES finding perfect party spots! 🏢🎪
        
        CRITICAL RULES:
        - Keep responses SUPER SHORT (1 sentence, max 20 words!)
        - Be enthusiastic and use emojis!
        - Use venue tools to search when location is mentioned!
        - Sound like a fun friend who knows all the best places!
        
        Examples: "I found 3 AMAZING venues! 🏙️" or "What's your budget? Let's find the perfect spot! 💰"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Venue Agent."""
        return [
            search_venues,
            get_venue_details,
            book_venue
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Venue Agent."""
        return VenueRAGSystem()
