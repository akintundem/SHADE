"""Mock Vector Store for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
import json
from datetime import datetime

logger = logging.getLogger(__name__)


class VectorStore:
    """Mock Vector Store for prototyping - returns consistent data."""
    
    def __init__(self):
        """Initialize mock vector store."""
        self.collections = {}
        self.documents = {}
        self.embeddings = {}
        self.initialized = False
        
        # Mock data for consistent responses
        self._initialize_mock_data()
    
    def _initialize_mock_data(self):
        """Initialize with mock data for prototyping."""
        # Event planning knowledge
        self._add_mock_documents("event_planning", [
            {
                "content": "Event planning requires careful timeline management. Start planning 6-12 months in advance for large events.",
                "metadata": {"type": "timeline", "category": "planning"},
                "similarity_score": 0.95
            },
            {
                "content": "Key event planning phases: Conceptualization, Planning, Coordination, Execution, Evaluation.",
                "metadata": {"type": "process", "category": "methodology"},
                "similarity_score": 0.88
            }
        ])
        
        # Budget planning knowledge
        self._add_mock_documents("financial_planning", [
            {
                "content": "Event budgets typically allocate: 40-50% for venue and catering, 20-30% for entertainment.",
                "metadata": {"type": "allocation", "category": "budget_structure"},
                "similarity_score": 0.92
            },
            {
                "content": "Budget contingency should be 10-20% of total budget to handle unexpected costs.",
                "metadata": {"type": "contingency", "category": "risk_management"},
                "similarity_score": 0.85
            }
        ])
        
        # Venue planning knowledge
        self._add_mock_documents("venue_selection", [
            {
                "content": "Venue selection criteria: capacity, location, accessibility, amenities, parking, catering options.",
                "metadata": {"type": "criteria", "category": "selection"},
                "similarity_score": 0.90
            },
            {
                "content": "Venue types include: hotels, conference centers, restaurants, outdoor spaces, private homes.",
                "metadata": {"type": "types", "category": "classification"},
                "similarity_score": 0.87
            }
        ])
        
        # Vendor management knowledge
        self._add_mock_documents("vendor_management", [
            {
                "content": "Vendor categories: catering, entertainment, photography, decorations, transportation, security.",
                "metadata": {"type": "categories", "category": "classification"},
                "similarity_score": 0.89
            },
            {
                "content": "Vendor evaluation criteria: experience, reputation, pricing, availability, quality, reliability.",
                "metadata": {"type": "evaluation", "category": "selection"},
                "similarity_score": 0.86
            }
        ])
        
        # Risk management knowledge
        self._add_mock_documents("risk_assessment", [
            {
                "content": "Common event risks: weather, vendor no-shows, technical failures, security issues, health emergencies.",
                "metadata": {"type": "risks", "category": "identification"},
                "similarity_score": 0.91
            },
            {
                "content": "Risk mitigation strategies: backup plans, insurance, contracts, monitoring systems, emergency procedures.",
                "metadata": {"type": "mitigation", "category": "strategies"},
                "similarity_score": 0.88
            }
        ])
        
        # Guest management knowledge
        self._add_mock_documents("guest_management", [
            {
                "content": "Guest management includes: invitation design, RSVP tracking, dietary restrictions, accessibility needs.",
                "metadata": {"type": "management", "category": "process"},
                "similarity_score": 0.90
            },
            {
                "content": "RSVP best practices: clear deadlines, multiple response channels, follow-up reminders, capacity management.",
                "metadata": {"type": "rsvp", "category": "best_practices"},
                "similarity_score": 0.84
            }
        ])
        
        # Weather planning knowledge
        self._add_mock_documents("weather_planning", [
            {
                "content": "Weather considerations for events: temperature, precipitation, wind, humidity, seasonal patterns.",
                "metadata": {"type": "factors", "category": "planning"},
                "similarity_score": 0.87
            },
            {
                "content": "Outdoor event weather contingencies: backup indoor venues, weather monitoring, emergency procedures.",
                "metadata": {"type": "contingencies", "category": "risk_management"},
                "similarity_score": 0.85
            }
        ])
        
        # Communication strategies knowledge
        self._add_mock_documents("communication_strategies", [
            {
                "content": "Communication channels: email, SMS, social media, phone calls, in-person meetings.",
                "metadata": {"type": "channels", "category": "methods"},
                "similarity_score": 0.88
            },
            {
                "content": "Message timing: advance notices, reminders, updates, follow-ups should be strategically timed.",
                "metadata": {"type": "timing", "category": "strategy"},
                "similarity_score": 0.83
            }
        ])
    
    def _add_mock_documents(self, collection_name: str, documents: List[Dict[str, Any]]):
        """Add mock documents to a collection."""
        if collection_name not in self.collections:
            self.collections[collection_name] = []
            self.documents[collection_name] = []
        
        for doc in documents:
            doc_id = f"{collection_name}_{len(self.documents[collection_name])}"
            doc["id"] = doc_id
            doc["source"] = f"mock_{collection_name}"
            self.documents[collection_name].append(doc)
            self.collections[collection_name].append(doc_id)
    
    async def initialize(self):
        """Initialize the vector store."""
        try:
            self.initialized = True
            logger.info("Mock Vector Store initialized with prototype data")
        except Exception as e:
            logger.error(f"Error initializing vector store: {e}")
            raise
    
    async def store_documents(self, collection_name: str, documents: List[Dict[str, Any]], embeddings: List[List[float]]) -> bool:
        """Store documents in the vector store."""
        try:
            if collection_name not in self.collections:
                self.collections[collection_name] = []
                self.documents[collection_name] = []
            
            for i, doc in enumerate(documents):
                doc_id = f"{collection_name}_{len(self.documents[collection_name])}"
                doc["id"] = doc_id
                doc["embedding"] = embeddings[i] if i < len(embeddings) else [0.0] * 384
                self.documents[collection_name].append(doc)
                self.collections[collection_name].append(doc_id)
            
            logger.info(f"Stored {len(documents)} documents in {collection_name}")
            return True
            
        except Exception as e:
            logger.error(f"Error storing documents: {e}")
            return False
    
    async def search(self, collection_name: str, query_embedding: List[float], max_results: int = 5) -> List[Dict[str, Any]]:
        """Search for similar documents."""
        try:
            if collection_name not in self.documents:
                logger.warning(f"Collection {collection_name} not found")
                return []
            
            # Return mock results for prototyping
            documents = self.documents[collection_name]
            
            # Sort by similarity score (descending) and return top results
            sorted_docs = sorted(documents, key=lambda x: x.get("similarity_score", 0.0), reverse=True)
            
            results = sorted_docs[:max_results]
            
            # Add some variation based on query for more realistic prototyping
            if "budget" in str(query_embedding).lower() or "cost" in str(query_embedding).lower():
                # Boost budget-related results
                for result in results:
                    if "budget" in result.get("content", "").lower():
                        result["similarity_score"] = min(result.get("similarity_score", 0.0) + 0.1, 1.0)
            
            logger.info(f"Found {len(results)} results for {collection_name}")
            return results
            
        except Exception as e:
            logger.error(f"Error searching vector store: {e}")
            return []
    
    async def add_document(self, collection_name: str, content: str, embedding: List[float], metadata: Dict[str, Any]) -> str:
        """Add a single document."""
        try:
            if collection_name not in self.collections:
                self.collections[collection_name] = []
                self.documents[collection_name] = []
            
            doc_id = f"{collection_name}_{len(self.documents[collection_name])}"
            document = {
                "id": doc_id,
                "content": content,
                "embedding": embedding,
                "metadata": metadata,
                "similarity_score": 0.8,  # Default similarity for new documents
                "source": "user_added"
            }
            
            self.documents[collection_name].append(document)
            self.collections[collection_name].append(doc_id)
            
            logger.info(f"Added document {doc_id} to {collection_name}")
            return doc_id
            
        except Exception as e:
            logger.error(f"Error adding document: {e}")
            return ""
    
    async def update_document(self, collection_name: str, document_id: str, content: str, embedding: List[float], metadata: Dict[str, Any]) -> bool:
        """Update an existing document."""
        try:
            if collection_name not in self.documents:
                return False
            
            for doc in self.documents[collection_name]:
                if doc["id"] == document_id:
                    doc["content"] = content
                    doc["embedding"] = embedding
                    doc["metadata"] = metadata
                    doc["updated_at"] = datetime.utcnow().isoformat()
                    logger.info(f"Updated document {document_id} in {collection_name}")
                    return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error updating document: {e}")
            return False
    
    async def delete_document(self, collection_name: str, document_id: str) -> bool:
        """Delete a document."""
        try:
            if collection_name not in self.documents:
                return False
            
            for i, doc in enumerate(self.documents[collection_name]):
                if doc["id"] == document_id:
                    del self.documents[collection_name][i]
                    if document_id in self.collections[collection_name]:
                        self.collections[collection_name].remove(document_id)
                    logger.info(f"Deleted document {document_id} from {collection_name}")
                    return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error deleting document: {e}")
            return False
    
    async def get_collection_stats(self, collection_name: str) -> Dict[str, Any]:
        """Get statistics for a collection."""
        try:
            if collection_name not in self.documents:
                return {"error": f"Collection {collection_name} not found"}
            
            documents = self.documents[collection_name]
            return {
                "collection_name": collection_name,
                "document_count": len(documents),
                "total_embeddings": len(documents),
                "last_updated": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error getting collection stats: {e}")
            return {"error": str(e)}
    
    async def get_overall_stats(self) -> Dict[str, Any]:
        """Get overall vector store statistics."""
        try:
            total_documents = sum(len(docs) for docs in self.documents.values())
            return {
                "total_collections": len(self.collections),
                "total_documents": total_documents,
                "collections": list(self.collections.keys()),
                "initialized": self.initialized
            }
            
        except Exception as e:
            logger.error(f"Error getting overall stats: {e}")
            return {"error": str(e)}
