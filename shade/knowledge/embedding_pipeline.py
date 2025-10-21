"""Mock Embedding Pipeline for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
import hashlib
import json

logger = logging.getLogger(__name__)


class EmbeddingPipeline:
    """Mock Embedding Pipeline for prototyping - returns consistent embeddings."""
    
    def __init__(self):
        """Initialize mock embedding pipeline."""
        self.embedding_dimension = 384  # Standard embedding dimension
        self.initialized = False
        
        # Cache for consistent embeddings
        self.embedding_cache = {}
    
    async def initialize(self):
        """Initialize the embedding pipeline."""
        try:
            self.initialized = True
            logger.info("Mock Embedding Pipeline initialized")
        except Exception as e:
            logger.error(f"Error initializing embedding pipeline: {e}")
            raise
    
    async def embed_text(self, text: str) -> List[float]:
        """Generate embedding for a single text."""
        try:
            # Check cache first
            cache_key = self._get_cache_key(text)
            if cache_key in self.embedding_cache:
                return self.embedding_cache[cache_key]
            
            # Generate consistent mock embedding based on text content
            embedding = self._generate_mock_embedding(text)
            
            # Cache the embedding
            self.embedding_cache[cache_key] = embedding
            
            logger.debug(f"Generated embedding for text: {text[:50]}...")
            return embedding
            
        except Exception as e:
            logger.error(f"Error generating embedding: {e}")
            # Return zero vector as fallback
            return [0.0] * self.embedding_dimension
    
    async def embed_documents(self, documents: List[Dict[str, Any]]) -> List[List[float]]:
        """Generate embeddings for multiple documents."""
        try:
            embeddings = []
            
            for doc in documents:
                content = doc.get("content", "")
                embedding = await self.embed_text(content)
                embeddings.append(embedding)
            
            logger.info(f"Generated {len(embeddings)} document embeddings")
            return embeddings
            
        except Exception as e:
            logger.error(f"Error generating document embeddings: {e}")
            # Return zero vectors as fallback
            return [[0.0] * self.embedding_dimension for _ in documents]
    
    async def embed_query(self, query: str) -> List[float]:
        """Generate embedding for a search query."""
        try:
            # Use the same method as embed_text but with query-specific logic
            embedding = await self.embed_text(query)
            
            # Boost certain dimensions based on query keywords for more realistic prototyping
            if "budget" in query.lower() or "cost" in query.lower():
                embedding[0] = 0.8  # Boost budget-related dimension
            if "venue" in query.lower() or "location" in query.lower():
                embedding[1] = 0.8  # Boost venue-related dimension
            if "timeline" in query.lower() or "schedule" in query.lower():
                embedding[2] = 0.8  # Boost timeline-related dimension
            
            logger.debug(f"Generated query embedding for: {query[:50]}...")
            return embedding
            
        except Exception as e:
            logger.error(f"Error generating query embedding: {e}")
            return [0.0] * self.embedding_dimension
    
    def _generate_mock_embedding(self, text: str) -> List[float]:
        """Generate a consistent mock embedding based on text content."""
        try:
            # Create a hash of the text for consistency
            text_hash = hashlib.md5(text.encode()).hexdigest()
            
            # Convert hash to consistent float values
            embedding = []
            for i in range(0, len(text_hash), 2):
                hex_pair = text_hash[i:i+2]
                # Convert hex to float between -1 and 1
                value = (int(hex_pair, 16) / 255.0) * 2 - 1
                embedding.append(value)
            
            # Pad or truncate to target dimension
            while len(embedding) < self.embedding_dimension:
                # Use text length and position to generate additional values
                additional_value = (len(text) + len(embedding)) % 100 / 100.0 * 2 - 1
                embedding.append(additional_value)
            
            # Truncate if too long
            embedding = embedding[:self.embedding_dimension]
            
            # Normalize the embedding
            magnitude = sum(x**2 for x in embedding) ** 0.5
            if magnitude > 0:
                embedding = [x / magnitude for x in embedding]
            
            return embedding
            
        except Exception as e:
            logger.error(f"Error generating mock embedding: {e}")
            return [0.0] * self.embedding_dimension
    
    def _get_cache_key(self, text: str) -> str:
        """Generate cache key for text."""
        return hashlib.md5(text.encode()).hexdigest()
    
    async def get_stats(self) -> Dict[str, Any]:
        """Get embedding pipeline statistics."""
        try:
            return {
                "embedding_dimension": self.embedding_dimension,
                "cache_size": len(self.embedding_cache),
                "initialized": self.initialized,
                "total_embeddings_generated": len(self.embedding_cache)
            }
            
        except Exception as e:
            logger.error(f"Error getting embedding stats: {e}")
            return {"error": str(e)}
    
    async def clear_cache(self):
        """Clear the embedding cache."""
        try:
            self.embedding_cache.clear()
            logger.info("Embedding cache cleared")
        except Exception as e:
            logger.error(f"Error clearing cache: {e}")
    
    async def batch_embed(self, texts: List[str], batch_size: int = 10) -> List[List[float]]:
        """Generate embeddings for a batch of texts."""
        try:
            embeddings = []
            
            for i in range(0, len(texts), batch_size):
                batch = texts[i:i + batch_size]
                batch_embeddings = []
                
                for text in batch:
                    embedding = await self.embed_text(text)
                    batch_embeddings.append(embedding)
                
                embeddings.extend(batch_embeddings)
                
                # Small delay to simulate processing time
                await asyncio.sleep(0.01)
            
            logger.info(f"Generated {len(embeddings)} embeddings in batches")
            return embeddings
            
        except Exception as e:
            logger.error(f"Error in batch embedding: {e}")
            return [[0.0] * self.embedding_dimension for _ in texts]
