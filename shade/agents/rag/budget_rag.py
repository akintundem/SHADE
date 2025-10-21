"""Budget RAG system for financial planning knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class BudgetRAGSystem(BaseRAGSystem):
    """RAG system for budget and financial planning knowledge."""
    
    def __init__(self):
        super().__init__("budget_planning")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with budget planning knowledge."""
        self.knowledge_base = [
            {
                "content": "Event budgets typically allocate: 40-50% for venue and catering, 20-30% for entertainment and activities, 10-15% for decorations, 10-15% for miscellaneous expenses.",
                "metadata": {"type": "allocation", "category": "budget_structure"},
                "domain": "budget_planning"
            },
            {
                "content": "Budget contingency should be 10-20% of total budget to handle unexpected costs and last-minute changes.",
                "metadata": {"type": "contingency", "category": "risk_management"},
                "domain": "budget_planning"
            },
            {
                "content": "Cost categories include: venue rental, catering, entertainment, decorations, transportation, staff, marketing, insurance, permits, and miscellaneous expenses.",
                "metadata": {"type": "categories", "category": "expense_types"},
                "domain": "budget_planning"
            },
            {
                "content": "Budget tracking should be done weekly during planning and daily during event execution. Use spreadsheets or budget management software.",
                "metadata": {"type": "tracking", "category": "management"},
                "domain": "budget_planning"
            },
            {
                "content": "Cost-saving strategies: negotiate with vendors, book early for discounts, consider off-peak times, use in-house resources, and leverage partnerships.",
                "metadata": {"type": "savings", "category": "optimization"},
                "domain": "budget_planning"
            },
            {
                "content": "Budget approval process: 1) Initial estimate, 2) Detailed breakdown, 3) Stakeholder review, 4) Approval, 5) Ongoing monitoring and adjustments.",
                "metadata": {"type": "process", "category": "workflow"},
                "domain": "budget_planning"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load budget planning knowledge base."""
        # Knowledge base is initialized in __init__
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant budget planning context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            
            if not relevant_items:
                return "No specific budget planning context found."
            
            context_parts = []
            for item in relevant_items:
                context_parts.append(f"- {item['content']}")
            
            return f"Budget Planning Context:\n" + "\n".join(context_parts)
            
        except Exception as e:
            return f"Error retrieving budget planning context: {str(e)}"
