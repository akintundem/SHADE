"""RAG Gateway for unified knowledge retrieval across all agents."""

from typing import Dict, Any, List, Optional, Tuple
import asyncio
import logging
from datetime import datetime
import json

from .vector_store import VectorStore
from .embedding_pipeline import EmbeddingPipeline
from .document_loader import DocumentLoader

logger = logging.getLogger(__name__)


class RAGGateway:
    """Unified RAG Gateway for all domain agents."""
    
    def __init__(self, vector_store: VectorStore, embedding_pipeline: EmbeddingPipeline):
        """Initialize RAG Gateway."""
        self.vector_store = vector_store
        self.embedding_pipeline = embedding_pipeline
        self.document_loader = DocumentLoader()
        self.domain_collections = {
            "event": "event_planning",
            "budget": "financial_planning", 
            "venue": "venue_selection",
            "vendor": "vendor_management",
            "risk": "risk_assessment",
            "attendee": "guest_management",
            "weather": "weather_planning",
            "outreach": "communication_strategies"
        }
        self.cache = {}
        self.cache_ttl = 3600  # 1 hour
    
    async def initialize(self):
        """Initialize the RAG Gateway."""
        try:
            # Initialize vector store
            await self.vector_store.initialize()
            
            # Initialize embedding pipeline
            await self.embedding_pipeline.initialize()
            
            # Load domain-specific knowledge
            await self._load_domain_knowledge()
            
            logger.info("RAG Gateway initialized successfully with mock data")
            
        except Exception as e:
            logger.error(f"Error initializing RAG Gateway: {e}")
            raise
    
    async def _load_domain_knowledge(self):
        """Load domain-specific knowledge into vector store."""
        for domain, collection_name in self.domain_collections.items():
            try:
                # Load documents for this domain
                documents = await self.document_loader.load_domain_documents(domain)
                
                if documents:
                    # Generate embeddings
                    embeddings = await self.embedding_pipeline.embed_documents(documents)
                    
                    # Store in vector database
                    await self.vector_store.store_documents(
                        collection_name=collection_name,
                        documents=documents,
                        embeddings=embeddings
                    )
                    
                    logger.info(f"Loaded {len(documents)} documents for {domain} domain")
                
            except Exception as e:
                logger.error(f"Error loading knowledge for {domain}: {e}")
    
    async def retrieve_context(self, query: str, domain: str, max_results: int = 5) -> List[Dict[str, Any]]:
        """Retrieve relevant context for a domain-specific query."""
        try:
            # Check cache first
            cache_key = f"{domain}_{hash(query)}"
            if cache_key in self.cache:
                cached_result, timestamp = self.cache[cache_key]
                if (datetime.utcnow().timestamp() - timestamp) < self.cache_ttl:
                    return cached_result
            
            # Get collection name for domain
            collection_name = self.domain_collections.get(domain)
            if not collection_name:
                logger.warning(f"No collection found for domain: {domain}")
                return []
            
            # Generate query embedding
            query_embedding = await self.embedding_pipeline.embed_query(query)
            
            # Search vector store
            results = await self.vector_store.search(
                collection_name=collection_name,
                query_embedding=query_embedding,
                max_results=max_results
            )
            
            # Format results
            formatted_results = []
            for result in results:
                formatted_results.append({
                    "content": result.get("content", ""),
                    "metadata": result.get("metadata", {}),
                    "similarity_score": result.get("similarity_score", 0.0),
                    "source": result.get("source", "unknown")
                })
            
            # Cache results
            self.cache[cache_key] = (formatted_results, datetime.utcnow().timestamp())
            
            return formatted_results
            
        except Exception as e:
            logger.error(f"Error retrieving context for {domain}: {e}")
            return []
    
    async def retrieve_multi_domain_context(self, query: str, domains: List[str], max_results_per_domain: int = 3) -> Dict[str, List[Dict[str, Any]]]:
        """Retrieve context from multiple domains."""
        try:
            results = {}
            
            # Create tasks for parallel retrieval
            tasks = []
            for domain in domains:
                task = self.retrieve_context(query, domain, max_results_per_domain)
                tasks.append((domain, task))
            
            # Execute in parallel
            if tasks:
                task_results = await asyncio.gather(*[task for _, task in tasks], return_exceptions=True)
                
                for i, (domain, _) in enumerate(tasks):
                    if not isinstance(task_results[i], Exception):
                        results[domain] = task_results[i]
                    else:
                        logger.error(f"Error retrieving context for {domain}: {task_results[i]}")
                        results[domain] = []
            
            return results
            
        except Exception as e:
            logger.error(f"Error in multi-domain context retrieval: {e}")
            return {domain: [] for domain in domains}
    
    async def add_document(self, domain: str, content: str, metadata: Dict[str, Any] = None) -> bool:
        """Add a new document to the knowledge base."""
        try:
            collection_name = self.domain_collections.get(domain)
            if not collection_name:
                logger.warning(f"No collection found for domain: {domain}")
                return False
            
            # Generate embedding
            embedding = await self.embedding_pipeline.embed_text(content)
            
            # Store in vector database
            document_id = await self.vector_store.add_document(
                collection_name=collection_name,
                content=content,
                embedding=embedding,
                metadata=metadata or {}
            )
            
            # Clear cache for this domain
            self._clear_domain_cache(domain)
            
            logger.info(f"Added document to {domain} domain with ID: {document_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error adding document to {domain}: {e}")
            return False
    
    async def update_document(self, domain: str, document_id: str, content: str, metadata: Dict[str, Any] = None) -> bool:
        """Update an existing document."""
        try:
            collection_name = self.domain_collections.get(domain)
            if not collection_name:
                logger.warning(f"No collection found for domain: {domain}")
                return False
            
            # Generate new embedding
            embedding = await self.embedding_pipeline.embed_text(content)
            
            # Update in vector database
            success = await self.vector_store.update_document(
                collection_name=collection_name,
                document_id=document_id,
                content=content,
                embedding=embedding,
                metadata=metadata or {}
            )
            
            if success:
                # Clear cache for this domain
                self._clear_domain_cache(domain)
                logger.info(f"Updated document {document_id} in {domain} domain")
            
            return success
            
        except Exception as e:
            logger.error(f"Error updating document {document_id} in {domain}: {e}")
            return False
    
    async def delete_document(self, domain: str, document_id: str) -> bool:
        """Delete a document from the knowledge base."""
        try:
            collection_name = self.domain_collections.get(domain)
            if not collection_name:
                logger.warning(f"No collection found for domain: {domain}")
                return False
            
            # Delete from vector database
            success = await self.vector_store.delete_document(
                collection_name=collection_name,
                document_id=document_id
            )
            
            if success:
                # Clear cache for this domain
                self._clear_domain_cache(domain)
                logger.info(f"Deleted document {document_id} from {domain} domain")
            
            return success
            
        except Exception as e:
            logger.error(f"Error deleting document {document_id} from {domain}: {e}")
            return False
    
    def _clear_domain_cache(self, domain: str):
        """Clear cache for a specific domain."""
        keys_to_remove = [key for key in self.cache.keys() if key.startswith(f"{domain}_")]
        for key in keys_to_remove:
            del self.cache[key]
    
    async def get_domain_stats(self, domain: str) -> Dict[str, Any]:
        """Get statistics for a domain's knowledge base."""
        try:
            collection_name = self.domain_collections.get(domain)
            if not collection_name:
                return {"error": f"No collection found for domain: {domain}"}
            
            stats = await self.vector_store.get_collection_stats(collection_name)
            return {
                "domain": domain,
                "collection": collection_name,
                "stats": stats
            }
            
        except Exception as e:
            logger.error(f"Error getting stats for {domain}: {e}")
            return {"error": str(e)}
    
    async def get_gateway_stats(self) -> Dict[str, Any]:
        """Get overall RAG Gateway statistics."""
        try:
            stats = {
                "domains": list(self.domain_collections.keys()),
                "cache_size": len(self.cache),
                "cache_ttl": self.cache_ttl,
                "vector_store_stats": await self.vector_store.get_overall_stats(),
                "embedding_pipeline_stats": await self.embedding_pipeline.get_stats()
            }
            
            return stats
            
        except Exception as e:
            logger.error(f"Error getting gateway stats: {e}")
            return {"error": str(e)}
