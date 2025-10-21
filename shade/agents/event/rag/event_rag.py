"""Event RAG system for event planning knowledge."""

from typing import Dict, Any, List, Optional
from datetime import datetime
from ...core.base_rag import BaseRAGSystem
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
from knowledge.interaction_store import InteractionPatternStore, InteractionPattern


class EventRAGSystem(BaseRAGSystem):
    """RAG system for event planning knowledge."""
    
    def __init__(self):
        super().__init__("event_planning")
        self.interaction_store = InteractionPatternStore()
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
    
    async def store_interaction_pattern(self, event_type: str, planning_phase: str, user_action: str, agent_response: str, success_metrics: Dict[str, Any], context: Dict[str, Any]) -> str:
        """Store a successful interaction pattern."""
        try:
            pattern = InteractionPattern(
                event_type=event_type,
                planning_phase=planning_phase,
                user_action=user_action,
                agent_response=agent_response,
                success_metrics=success_metrics,
                context=context,
                timestamp=datetime.utcnow().isoformat(),
                pattern_id=""
            )
            return await self.interaction_store.store_interaction_pattern(pattern)
        except Exception as e:
            return f"Error storing interaction pattern: {str(e)}"
    
    async def get_similar_patterns(self, event_type: str, planning_phase: str, context: Dict[str, Any], limit: int = 5) -> List[InteractionPattern]:
        """Get similar interaction patterns."""
        try:
            return await self.interaction_store.get_similar_patterns(event_type, planning_phase, context, limit)
        except Exception as e:
            return []
    
    async def get_recommended_next_steps(self, event_type: str, current_phase: str, completed_actions: List[str]) -> List[str]:
        """Get recommended next steps based on successful patterns."""
        try:
            return await self.interaction_store.get_recommended_next_steps(event_type, current_phase, completed_actions)
        except Exception as e:
            return []
    
    async def get_best_practices(self, event_type: str, planning_phase: str) -> List[str]:
        """Get best practices for event type and phase."""
        try:
            return await self.interaction_store.get_best_practices(event_type, planning_phase)
        except Exception as e:
            return []
    
    async def learn_from_success(self, event_type: str, completed_sequence: List[str], success_metrics: Dict[str, Any]) -> None:
        """Learn from a successful planning sequence."""
        try:
            await self.interaction_store.learn_from_success(event_type, completed_sequence, success_metrics)
        except Exception as e:
            pass  # Fail silently for learning
