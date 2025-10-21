"""Venue RAG system for venue planning knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class VenueRAGSystem(BaseRAGSystem):
    """RAG system for venue planning knowledge."""
    
    def __init__(self):
        super().__init__("venue_planning")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with venue planning knowledge."""
        self.knowledge_base = [
            {
                "content": "Venue selection criteria: capacity, location, accessibility, amenities, parking, catering options, technical capabilities, and cost.",
                "metadata": {"type": "criteria", "category": "selection"},
                "domain": "venue_planning"
            },
            {
                "content": "Venue types include: hotels, conference centers, restaurants, outdoor spaces, private homes, museums, theaters, and community centers.",
                "metadata": {"type": "types", "category": "classification"},
                "domain": "venue_planning"
            },
            {
                "content": "Venue booking timeline: 6-12 months for popular venues, 3-6 months for standard venues, 1-3 months for last-minute bookings.",
                "metadata": {"type": "timeline", "category": "booking"},
                "domain": "venue_planning"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load venue planning knowledge base."""
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant venue planning context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            if not relevant_items:
                return "No specific venue planning context found."
            
            context_parts = [f"- {item['content']}" for item in relevant_items]
            return f"Venue Planning Context:\n" + "\n".join(context_parts)
        except Exception as e:
            return f"Error retrieving venue planning context: {str(e)}"
