"""Base RAG system for all agents."""

from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
import logging

logger = logging.getLogger(__name__)


class BaseRAGSystem(ABC):
    """Base RAG system for all agents."""
    
    def __init__(self, domain: str):
        """Initialize the base RAG system."""
        self.domain = domain
        self.knowledge_base = []
        self.vector_store = None
        self.embedding_model = None
    
    @abstractmethod
    async def load_knowledge_base(self):
        """Load the knowledge base for this domain."""
        pass
    
    @abstractmethod
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant context for a query."""
        pass
    
    async def search_knowledge(self, query: str, limit: int = 5) -> List[Dict[str, Any]]:
        """Search the knowledge base for relevant information."""
        try:
            # Simple keyword matching for now
            # In a real implementation, this would use vector embeddings
            query_lower = query.lower()
            relevant_items = []
            
            for item in self.knowledge_base:
                content = item.get("content", "").lower()
                if any(word in content for word in query_lower.split()):
                    relevant_items.append(item)
            
            # Sort by relevance (simple scoring)
            relevant_items.sort(key=lambda x: self._calculate_relevance_score(query_lower, x.get("content", "").lower()))
            
            return relevant_items[:limit]
            
        except Exception as e:
            logger.error(f"Error searching knowledge: {e}")
            return []
    
    def _calculate_relevance_score(self, query: str, content: str) -> float:
        """Calculate relevance score for content."""
        query_words = set(query.split())
        content_words = set(content.split())
        
        if not query_words:
            return 0.0
        
        # Calculate Jaccard similarity
        intersection = len(query_words.intersection(content_words))
        union = len(query_words.union(content_words))
        
        return intersection / union if union > 0 else 0.0
    
    async def add_document(self, content: str, metadata: Dict[str, Any] = None) -> bool:
        """Add a document to the knowledge base."""
        try:
            document = {
                "content": content,
                "metadata": metadata or {},
                "domain": self.domain
            }
            self.knowledge_base.append(document)
            return True
        except Exception as e:
            logger.error(f"Error adding document: {e}")
            return False
    
    async def get_domain_knowledge(self) -> List[Dict[str, Any]]:
        """Get all knowledge for this domain."""
        return self.knowledge_base.copy()
    
    async def clear_knowledge_base(self) -> bool:
        """Clear the knowledge base."""
        try:
            self.knowledge_base.clear()
            return True
        except Exception as e:
            logger.error(f"Error clearing knowledge base: {e}")
            return False
    
    async def get_knowledge_stats(self) -> Dict[str, Any]:
        """Get statistics about the knowledge base."""
        return {
            "domain": self.domain,
            "total_documents": len(self.knowledge_base),
            "metadata_types": list(set(item.get("metadata", {}).get("type", "unknown") for item in self.knowledge_base)),
            "categories": list(set(item.get("metadata", {}).get("category", "unknown") for item in self.knowledge_base))
        }
