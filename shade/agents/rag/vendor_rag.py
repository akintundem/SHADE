"""Vendor RAG system for vendor management knowledge."""

from typing import Dict, Any, List
from .base_rag import BaseRAGSystem


class VendorRAGSystem(BaseRAGSystem):
    """RAG system for vendor management knowledge."""
    
    def __init__(self):
        super().__init__("vendor_management")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with vendor management knowledge."""
        self.knowledge_base = [
            {
                "content": "Vendor categories: catering, entertainment, photography, decorations, transportation, security, and technical services.",
                "metadata": {"type": "categories", "category": "classification"},
                "domain": "vendor_management"
            },
            {
                "content": "Vendor evaluation criteria: experience, reputation, pricing, availability, quality, reliability, and communication.",
                "metadata": {"type": "evaluation", "category": "selection"},
                "domain": "vendor_management"
            }
        ]
    
    async def load_knowledge_base(self):
        """Load vendor management knowledge base."""
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant vendor management context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            if not relevant_items:
                return "No specific vendor management context found."
            
            context_parts = [f"- {item['content']}" for item in relevant_items]
            return f"Vendor Management Context:\n" + "\n".join(context_parts)
        except Exception as e:
            return f"Error retrieving vendor management context: {str(e)}"
