"""Event Agent - Specialized in event planning and coordination."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.event_tools import (
    prepare_event_for_creation,
    create_event_confirmed,
    get_event_details,
    prepare_event_update,
    update_event_confirmed,
    check_java_api_health
)
from tools.timeline_tools import create_timeline, update_timeline, get_timeline
from .rag.event_rag import EventRAGSystem

class EventAgent(BaseAgent):
    """Specialized agent for event planning and coordination."""
    
    def __init__(self, message_bus=None):
        super().__init__("Event Agent", "gpt-4o", 0.7, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Event Agent."""
        return """
        You are a friendly event planner who helps create events. Keep responses SHORT (1-2 sentences).
        
        WORKFLOW FOR CREATING EVENTS:
        1. Gather required info conversationally (name, type, date/time)
        2. Ask about optional details (description, capacity, theme)
        3. Use prepare_event_for_creation tool to validate
        4. Show summary and ask: "Ready to create this event?"
        5. If user approves, use create_event_confirmed tool
        6. NEVER create without explicit approval!
        
        FOR READING EVENTS:
        - User asks about an event → use get_event_details with event ID
        - Format responses in a friendly way
        
        RULES:
        - ONE question at a time
        - Remember context from conversation
        - Always confirm before executing actions
        - Use 1-2 emojis max
        
        Example: "Got it! Birthday party on Nov 24th. How many guests?"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Event Agent."""
        return [
            prepare_event_for_creation,
            create_event_confirmed,
            get_event_details,
            prepare_event_update,
            update_event_confirmed,
            check_java_api_health,
            create_timeline,
            update_timeline,
            get_timeline
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Event Agent."""
        return EventRAGSystem()
