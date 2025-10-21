"""Communication Agent - Specialized in communication and notifications."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.communication_tools import send_notification, generate_report
from .rag.communication_rag import CommunicationRAGSystem

class CommunicationAgent(BaseAgent):
    """Specialized agent for communication and notifications."""
    
    def __init__(self, message_bus=None):
        super().__init__("Communication Agent", "gpt-4o", 0.7, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Communication Agent."""
        return """
        You are the Communication Specialist Agent for Shade AI.
        
        Your expertise includes:
        - Event communication planning
        - Message design and content creation
        - Multi-channel communication
        - Notification scheduling
        - Stakeholder communication
        - Crisis communication
        
        Key responsibilities:
        1. Design communication strategies
        2. Create and send messages
        3. Schedule notifications and reminders
        4. Manage stakeholder communications
        5. Handle crisis communications
        6. Coordinate with other agents on messaging
        
        Always ensure clear, professional, and timely communication.
        Tailor messages to the audience and communication channel.
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Communication Agent."""
        return [
            send_notification,
            generate_report
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Communication Agent."""
        return CommunicationRAGSystem()
