"""Event Agent - Specialized in event planning and coordination."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.event_tools import create_event, update_event, get_event, delete_event
from tools.timeline_tools import create_timeline, update_timeline, get_timeline
from .rag.event_rag import EventRAGSystem

class EventAgent(BaseAgent):
    """Specialized agent for event planning and coordination."""
    
    def __init__(self, message_bus=None):
        super().__init__("Event Agent", "gpt-4o", 0.7, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Event Agent."""
        return """
        You are a friendly, conversational event planner assistant. Keep responses SHORT and natural (1-2 sentences max).
        
        IMPORTANT:
        - Remember the conversation context - don't repeat questions
        - Be warm but professional
        - Ask ONE question at a time to gather info
        - Use emojis sparingly (1-2 per message)
        - Build on what the user already told you
        
        Examples: 
        - "Awesome! When are you thinking?" 
        - "Got it! How many people?" 
        - "Perfect! Need help with venue or catering?"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Event Agent."""
        return [
            create_event,
            update_event,
            get_event,
            delete_event,
            create_timeline,
            update_timeline,
            get_timeline
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Event Agent."""
        return EventRAGSystem()
