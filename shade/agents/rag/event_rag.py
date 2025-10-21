"""Event RAG system for event planning knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class EventRAGSystem(BaseRAGSystem):
    """RAG system for event planning knowledge."""
    
    def __init__(self):
        super().__init__("event_planning")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with event planning knowledge."""
        self.knowledge_base = [
            {
                "content": "Event planning requires careful timeline management. Start planning 6-12 months in advance for large events, 3-6 months for medium events, and 1-3 months for small events.",
                "metadata": {"type": "timeline", "category": "planning"},
                "domain": "event_planning"
            },
            {
                "content": "Key event planning phases: 1) Conceptualization, 2) Planning, 3) Coordination, 4) Execution, 5) Evaluation. Each phase has specific deliverables and timelines.",
                "metadata": {"type": "process", "category": "methodology"},
                "domain": "event_planning"
            },
            {
                "content": "Event objectives should be SMART: Specific, Measurable, Achievable, Relevant, and Time-bound. Clear objectives guide all planning decisions.",
                "metadata": {"type": "objectives", "category": "strategy"},
                "domain": "event_planning"
            },
            {
                "content": "Event success metrics include attendance rate, engagement level, feedback scores, ROI, and achievement of stated objectives.",
                "metadata": {"type": "metrics", "category": "evaluation"},
                "domain": "event_planning"
            },
            {
                "content": "Common event types: Corporate events, conferences, weddings, birthday parties, anniversaries, product launches, networking events, and fundraisers.",
                "metadata": {"type": "types", "category": "classification"},
                "domain": "event_planning"
            },
            {
                "content": "Event coordination involves managing multiple stakeholders: clients, vendors, venues, attendees, staff, and suppliers. Clear communication is essential.",
                "metadata": {"type": "coordination", "category": "management"},
                "domain": "event_planning"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load event planning knowledge base."""
        # Knowledge base is initialized in __init__
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant event planning context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            
            if not relevant_items:
                return "No specific event planning context found."
            
            context_parts = []
            for item in relevant_items:
                context_parts.append(f"- {item['content']}")
            
            return f"Event Planning Context:\n" + "\n".join(context_parts)
            
        except Exception as e:
            return f"Error retrieving event planning context: {str(e)}"
