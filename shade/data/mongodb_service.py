"""Enhanced MongoDB service for data persistence."""

import os
import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from motor.motor_asyncio import AsyncIOMotorClient
from pymongo.errors import ConnectionFailure, ServerSelectionTimeoutError
from bson import ObjectId
import json

logger = logging.getLogger(__name__)


class MongoDBService:
    """Enhanced MongoDB service with better error handling and features."""
    
    def __init__(self):
        """Initialize MongoDB service."""
        self.client = None
        self.db = None
        self.connected = False
        self.connection_string = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
        self.database_name = os.getenv("MONGODB_DATABASE", "shade_agent")
        self._memory_store = {}
        
    async def connect(self):
        """Connect to MongoDB with enhanced error handling."""
        try:
            self.client = AsyncIOMotorClient(
                self.connection_string,
                serverSelectionTimeoutMS=5000,
                connectTimeoutMS=5000,
                socketTimeoutMS=5000
            )
            self.db = self.client[self.database_name]
            
            # Test connection
            await self.client.admin.command('ping')
            self.connected = True
            
            # Create indexes for better performance
            await self._create_indexes()
            
            logger.info(f"Connected to MongoDB: {self.database_name}")
            return True
            
        except (ConnectionFailure, ServerSelectionTimeoutError) as e:
            logger.warning(f"MongoDB not available: {e}. Running in memory-only mode.")
            self.connected = False
            self._memory_store = {}
            return False
        except Exception as e:
            logger.error(f"Error connecting to MongoDB: {e}")
            self.connected = False
            return False
    
    async def _create_indexes(self):
        """Create database indexes for better performance."""
        try:
            # Conversations collection indexes
            await self.db.conversations.create_index("user_id")
            await self.db.conversations.create_index("chat_id")
            await self.db.conversations.create_index("created_at")
            await self.db.conversations.create_index([("user_id", 1), ("chat_id", 1)])
            
            # Events collection indexes
            await self.db.events.create_index("user_id")
            await self.db.events.create_index("event_id")
            await self.db.events.create_index("created_at")
            await self.db.events.create_index("status")
            
            # Agents collection indexes
            await self.db.agents.create_index("agent_name")
            await self.db.agents.create_index("created_at")
            
            # Knowledge collection indexes
            await self.db.knowledge.create_index("domain")
            await self.db.knowledge.create_index("created_at")
            await self.db.knowledge.create_index([("domain", 1), ("content", "text")])
            
            logger.info("Database indexes created successfully")
            
        except Exception as e:
            logger.error(f"Error creating indexes: {e}")
    
    async def get_collection(self, collection_name: str):
        """Get a collection with connection check."""
        if not self.connected or self.db is None:
            await self.connect()
        
        if self.connected and self.db is not None:
            return self.db[collection_name]
        else:
            raise ConnectionError("Not connected to MongoDB")
    
    # Conversation Management
    async def save_conversation(self, user_id: str, chat_id: str, message: str, response: str, metadata: Dict[str, Any] = None) -> bool:
        """Save a conversation turn."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("conversations")
                document = {
                    "user_id": user_id,
                    "chat_id": chat_id,
                    "message": message,
                    "response": response,
                    "metadata": metadata or {},
                    "created_at": datetime.utcnow(),
                    "timestamp": datetime.utcnow().isoformat()
                }
                await collection.insert_one(document)
                return True
            else:
                # Fallback to memory store
                key = f"{user_id}_{chat_id}"
                if key not in self._memory_store:
                    self._memory_store[key] = []
                self._memory_store[key].append({
                    "message": message,
                    "response": response,
                    "metadata": metadata or {},
                    "created_at": datetime.utcnow().isoformat()
                })
                return True
                
        except Exception as e:
            logger.error(f"Error saving conversation: {e}")
            return False
    
    async def get_conversation_history(self, user_id: str, chat_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        """Get conversation history."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("conversations")
                cursor = collection.find(
                    {"user_id": user_id, "chat_id": chat_id}
                ).sort("created_at", -1).limit(limit)
                
                conversations = []
                async for doc in cursor:
                    doc["_id"] = str(doc["_id"])
                    conversations.append(doc)
                
                return conversations
            else:
                # Fallback to memory store
                key = f"{user_id}_{chat_id}"
                if key in self._memory_store:
                    return self._memory_store[key][-limit:]
                return []
                
        except Exception as e:
            logger.error(f"Error getting conversation history: {e}")
            return []
    
    # Event Management
    async def save_event(self, user_id: str, event_data: Dict[str, Any]) -> str:
        """Save an event."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("events")
                document = {
                    "user_id": user_id,
                    "event_id": event_data.get("event_id", str(ObjectId())),
                    "event_data": event_data,
                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow(),
                    "status": "active"
                }
                result = await collection.insert_one(document)
                return str(result.inserted_id)
            else:
                # Fallback to memory store
                event_id = event_data.get("event_id", str(ObjectId()))
                self._memory_store[f"event_{event_id}"] = {
                    "user_id": user_id,
                    "event_id": event_id,
                    "event_data": event_data,
                    "created_at": datetime.utcnow().isoformat(),
                    "status": "active"
                }
                return event_id
                
        except Exception as e:
            logger.error(f"Error saving event: {e}")
            return ""
    
    async def get_event(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Get an event by ID."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("events")
                document = await collection.find_one({"event_id": event_id})
                if document:
                    document["_id"] = str(document["_id"])
                    return document
                return None
            else:
                # Fallback to memory store
                return self._memory_store.get(f"event_{event_id}")
                
        except Exception as e:
            logger.error(f"Error getting event: {e}")
            return None
    
    async def update_event(self, event_id: str, updates: Dict[str, Any]) -> bool:
        """Update an event."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("events")
                updates["updated_at"] = datetime.utcnow()
                result = await collection.update_one(
                    {"event_id": event_id},
                    {"$set": updates}
                )
                return result.modified_count > 0
            else:
                # Fallback to memory store
                if f"event_{event_id}" in self._memory_store:
                    self._memory_store[f"event_{event_id}"].update(updates)
                    self._memory_store[f"event_{event_id}"]["updated_at"] = datetime.utcnow().isoformat()
                    return True
                return False
                
        except Exception as e:
            logger.error(f"Error updating event: {e}")
            return False
    
    # Agent State Management
    async def save_agent_state(self, agent_name: str, state: Dict[str, Any]) -> bool:
        """Save agent state."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("agents")
                document = {
                    "agent_name": agent_name,
                    "state": state,
                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow()
                }
                await collection.replace_one(
                    {"agent_name": agent_name},
                    document,
                    upsert=True
                )
                return True
            else:
                # Fallback to memory store
                self._memory_store[f"agent_{agent_name}"] = {
                    "agent_name": agent_name,
                    "state": state,
                    "updated_at": datetime.utcnow().isoformat()
                }
                return True
                
        except Exception as e:
            logger.error(f"Error saving agent state: {e}")
            return False
    
    async def get_agent_state(self, agent_name: str) -> Optional[Dict[str, Any]]:
        """Get agent state."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("agents")
                document = await collection.find_one({"agent_name": agent_name})
                if document:
                    return document.get("state")
                return None
            else:
                # Fallback to memory store
                agent_data = self._memory_store.get(f"agent_{agent_name}")
                return agent_data.get("state") if agent_data else None
                
        except Exception as e:
            logger.error(f"Error getting agent state: {e}")
            return None
    
    # Knowledge Management
    async def save_knowledge(self, domain: str, content: str, metadata: Dict[str, Any] = None) -> str:
        """Save knowledge item."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("knowledge")
                document = {
                    "domain": domain,
                    "content": content,
                    "metadata": metadata or {},
                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow()
                }
                result = await collection.insert_one(document)
                return str(result.inserted_id)
            else:
                # Fallback to memory store
                knowledge_id = str(ObjectId())
                self._memory_store[f"knowledge_{knowledge_id}"] = {
                    "domain": domain,
                    "content": content,
                    "metadata": metadata or {},
                    "created_at": datetime.utcnow().isoformat()
                }
                return knowledge_id
                
        except Exception as e:
            logger.error(f"Error saving knowledge: {e}")
            return ""
    
    async def search_knowledge(self, domain: str, query: str, limit: int = 10) -> List[Dict[str, Any]]:
        """Search knowledge by domain and query."""
        try:
            if self.connected and self.db is not None:
                collection = await self.get_collection("knowledge")
                cursor = collection.find({
                    "domain": domain,
                    "$text": {"$search": query}
                }).limit(limit)
                
                results = []
                async for doc in cursor:
                    doc["_id"] = str(doc["_id"])
                    results.append(doc)
                
                return results
            else:
                # Fallback to memory store - simple text search
                results = []
                for key, value in self._memory_store.items():
                    if key.startswith("knowledge_") and value.get("domain") == domain:
                        if query.lower() in value.get("content", "").lower():
                            results.append(value)
                return results[:limit]
                
        except Exception as e:
            logger.error(f"Error searching knowledge: {e}")
            return []
    
    # Analytics and Statistics
    async def get_database_stats(self) -> Dict[str, Any]:
        """Get database statistics."""
        try:
            if self.connected and self.db is not None:
                stats = {
                    "conversations": await self.db.conversations.count_documents({}),
                    "events": await self.db.events.count_documents({}),
                    "agents": await self.db.agents.count_documents({}),
                    "knowledge": await self.db.knowledge.count_documents({}),
                    "connected": True,
                    "database": self.database_name
                }
                return stats
            else:
                return {
                    "conversations": len([k for k in self._memory_store.keys() if "_" in k and not k.startswith("event_") and not k.startswith("agent_") and not k.startswith("knowledge_")]),
                    "events": len([k for k in self._memory_store.keys() if k.startswith("event_")]),
                    "agents": len([k for k in self._memory_store.keys() if k.startswith("agent_")]),
                    "knowledge": len([k for k in self._memory_store.keys() if k.startswith("knowledge_")]),
                    "connected": False,
                    "mode": "memory_only"
                }
                
        except Exception as e:
            logger.error(f"Error getting database stats: {e}")
            return {"error": str(e)}
    
    async def cleanup_old_data(self, days: int = 30) -> int:
        """Clean up old data."""
        try:
            cutoff_date = datetime.utcnow() - timedelta(days=days)
            cleaned_count = 0
            
            if self.connected and self.db is not None:
                # Clean up old conversations
                result = await self.db.conversations.delete_many({
                    "created_at": {"$lt": cutoff_date}
                })
                cleaned_count += result.deleted_count
                
                # Clean up old events
                result = await self.db.events.delete_many({
                    "created_at": {"$lt": cutoff_date},
                    "status": "completed"
                })
                cleaned_count += result.deleted_count
                
            else:
                # Clean up memory store
                keys_to_remove = []
                for key, value in self._memory_store.items():
                    if isinstance(value, dict) and "created_at" in value:
                        created_at = datetime.fromisoformat(value["created_at"].replace("Z", "+00:00"))
                        if created_at < cutoff_date:
                            keys_to_remove.append(key)
                
                for key in keys_to_remove:
                    del self._memory_store[key]
                    cleaned_count += 1
            
            logger.info(f"Cleaned up {cleaned_count} old records")
            return cleaned_count
            
        except Exception as e:
            logger.error(f"Error cleaning up old data: {e}")
            return 0
    
    async def disconnect(self):
        """Disconnect from MongoDB."""
        try:
            if self.client:
                self.client.close()
                self.connected = False
                logger.info("Disconnected from MongoDB")
        except Exception as e:
            logger.error(f"Error disconnecting from MongoDB: {e}")
