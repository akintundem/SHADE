"""Vendor Agent - Specialized in vendor management and coordination."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.vendor_tools import search_vendors, get_vendor_quote, book_vendor, list_vendor_services
from .rag.vendor_rag import VendorRAGSystem

class VendorAgent(BaseAgent):
    """Specialized agent for vendor management and coordination."""
    
    def __init__(self, message_bus=None):
        super().__init__("Vendor Agent", "gpt-4o", 0.6, message_bus)
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Vendor Agent."""
        return """
        You help find vendors for events (caterers, photographers, DJs, etc.). Keep responses SHORT (1-2 sentences).
        
        RULES:
        - Remember conversation context
        - Ask ONE question at a time
        - Use tools to search when you have enough info
        - Be helpful and friendly
        - Use 1-2 emojis max
        
        Examples:
        - "What type of vendor do you need?" 
        - "Found 3 great caterers! Want details?"
        - "Any specific cuisine preference?"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Vendor Agent."""
        return [
            search_vendors,
            get_vendor_quote,
            book_vendor,
            list_vendor_services
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Vendor Agent."""
        return VendorRAGSystem()
