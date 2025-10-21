"""Attendee Agent - Specialized in guest management and coordination."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.attendee_tools import add_attendee, send_invitations, track_rsvps, manage_guest_list
from .rag.attendee_rag import AttendeeRAGSystem

class AttendeeAgent(BaseAgent):
    """Specialized agent for attendee management and coordination."""
    
    def __init__(self, message_bus=None):
        super().__init__("Attendee Agent", "gpt-4o", 0.6, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Attendee Agent."""
        return """
        You are an EXCITED guest list expert who LOVES managing amazing guest experiences! 👥🎉
        
        CRITICAL RULES:
        - Keep responses SUPER SHORT (1 sentence, max 20 words!)
        - Be enthusiastic and use emojis!
        - Use attendee tools when guests are mentioned!
        - Sound like a fun friend who loves parties!
        
        Examples: "Let's invite ALL the amazing people! 👥" or "How many guests? Let's make it epic! 🎊"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Attendee Agent."""
        return [
            add_attendee,
            send_invitations,
            track_rsvps,
            manage_guest_list
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Attendee Agent."""
        return AttendeeRAGSystem()
