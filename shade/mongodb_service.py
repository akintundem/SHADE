"""MongoDB service for the Shade AI Agent."""

import os
from datetime import datetime
from motor.motor_asyncio import AsyncIOMotorClient
from pymongo.errors import ConnectionFailure
import logging

logger = logging.getLogger(__name__)

class MongoDBService:
    """MongoDB service for handling database operations."""
    
    def __init__(self):
        """Initialize MongoDB connection."""
        self.client = None
        self.db = None
        self.connected = False
        self._memory_store = {}  # Fallback in-memory storage
        
    async def connect(self):
        """Connect to MongoDB."""
        try:
            # Get MongoDB URI from environment or use default
            mongodb_uri = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
            database_name = os.getenv("MONGODB_DATABASE", "shade_agent")
            
            self.client = AsyncIOMotorClient(mongodb_uri, serverSelectionTimeoutMS=2000)
            self.db = self.client[database_name]
            
            # Test connection with timeout
            await self.client.admin.command('ping')
            self.connected = True
            logger.info(f"Connected to MongoDB: {database_name}")
            
        except ConnectionFailure as e:
            logger.warning(f"MongoDB not available: {e}. Running in memory-only mode.")
            self.connected = False
            self._memory_store = {}  # Fallback to in-memory storage
        except Exception as e:
            logger.warning(f"MongoDB connection error: {e}. Running in memory-only mode.")
            self.connected = False
            self._memory_store = {}  # Fallback to in-memory storage
    
    async def disconnect(self):
        """Disconnect from MongoDB."""
        if self.client:
            self.client.close()
            self.connected = False
            logger.info("Disconnected from MongoDB")
    
    async def get_collection(self, collection_name: str):
        """Get a collection from the database."""
        if not self.connected:
            await self.connect()
        
        if self.connected and self.db is not None:
            return self.db[collection_name]
        else:
            raise ConnectionError("Not connected to MongoDB")
    
    async def insert_document(self, collection_name: str, document: dict):
        """Insert a document into a collection."""
        try:
            collection = await self.get_collection(collection_name)
            result = await collection.insert_one(document)
            return result.inserted_id
        except Exception as e:
            logger.error(f"Error inserting document: {e}")
            return None
    
    async def find_documents(self, collection_name: str, query: dict = None, limit: int = None):
        """Find documents in a collection."""
        try:
            collection = await self.get_collection(collection_name)
            cursor = collection.find(query or {})
            if limit:
                cursor = cursor.limit(limit)
            return await cursor.to_list(length=limit)
        except Exception as e:
            logger.error(f"Error finding documents: {e}")
            return []
    
    async def update_document(self, collection_name: str, query: dict, update: dict):
        """Update a document in a collection."""
        try:
            collection = await self.get_collection(collection_name)
            result = await collection.update_one(query, update)
            return result.modified_count
        except Exception as e:
            logger.error(f"Error updating document: {e}")
            return 0
    
    async def delete_document(self, collection_name: str, query: dict):
        """Delete a document from a collection."""
        try:
            collection = await self.get_collection(collection_name)
            result = await collection.delete_one(query)
            return result.deleted_count
        except Exception as e:
            logger.error(f"Error deleting document: {e}")
            return 0
    
    async def save_conversation_state(self, chat_id: str, state: dict):
        """Save conversation state for a chat."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("conversations")
                document = {
                    "chat_id": chat_id,
                    "state": state,
                    "updated_at": datetime.utcnow()
                }
                
                # Upsert: update if exists, insert if not
                result = await collection.replace_one(
                    {"chat_id": chat_id},
                    document,
                    upsert=True
                )
                return result.acknowledged
            else:
                # Fallback to in-memory storage
                if not hasattr(self, '_memory_store'):
                    self._memory_store = {}
                self._memory_store[chat_id] = {
                    "state": state,
                    "updated_at": datetime.utcnow()
                }
                return True
        except Exception as e:
            logger.error(f"Error saving conversation state: {e}")
            return False
    
    async def get_conversation_state(self, chat_id: str):
        """Get conversation state for a chat."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("conversations")
                document = await collection.find_one({"chat_id": chat_id})
                
                if document:
                    return document.get("state")
                return None
            else:
                # Fallback to in-memory storage
                if hasattr(self, '_memory_store') and chat_id in self._memory_store:
                    return self._memory_store[chat_id].get("state")
                return None
        except Exception as e:
            logger.error(f"Error getting conversation state: {e}")
            return None

# Global instance
mongodb_service = MongoDBService()
