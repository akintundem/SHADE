"""Communication RAG system for communication planning knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class CommunicationRAGSystem(BaseRAGSystem):
    """RAG system for communication planning knowledge."""
    
    def __init__(self):
        super().__init__("communication_planning")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with communication planning knowledge."""
        self.knowledge_base = [
            {
                "content": "Communication channels: email, SMS, social media, phone calls, and in-person meetings.",
                "metadata": {"type": "channels", "category": "methods"},
                "domain": "communication_planning"
            },
            {
                "content": "Message timing: advance notices, reminders, updates, and follow-ups should be strategically timed.",
                "metadata": {"type": "timing", "category": "strategy"},
                "domain": "communication_planning"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load communication planning knowledge base."""
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant communication planning context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            if not relevant_items:
                return "No specific communication planning context found."
            
            context_parts = [f"- {item['content']}" for item in relevant_items]
            return f"Communication Planning Context:\n" + "\n".join(context_parts)
        except Exception as e:
            return f"Error retrieving communication planning context: {str(e)}"
