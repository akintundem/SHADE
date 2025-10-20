"""MongoDB service for chat persistence and event management."""

import os
from datetime import datetime
from typing import Dict, Any, List, Optional
from motor.motor_asyncio import AsyncIOMotorClient
from pymongo import MongoClient
from bson import ObjectId
from dotenv import load_dotenv

load_dotenv()

class MongoDBService:
    """MongoDB service for chat and event persistence."""
    
    def __init__(self):
        self.mongo_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
        self.database_name = os.getenv("MONGODB_DATABASE", "event_planner")
        self.client = None
        self.db = None
        
    async def connect(self):
        """Connect to MongoDB."""
        try:
            self.client = AsyncIOMotorClient(self.mongo_url)
            self.db = self.client[self.database_name]
            # Test connection
            await self.client.admin.command('ping')
            print("✅ Connected to MongoDB successfully!")
            return True
        except Exception as e:
            print(f"❌ Failed to connect to MongoDB: {e}")
            return False
    
    async def disconnect(self):
        """Disconnect from MongoDB."""
        if self.client:
            self.client.close()
    
    # Chat Management
    async def create_chat(self, user_id: str, event_id: Optional[str] = None) -> str:
        """Create a new chat session."""
        chat_doc = {
            "user_id": user_id,
            "event_id": event_id,
            "created_at": datetime.utcnow(),
            "updated_at": datetime.utcnow(),
            "status": "active",
            "messages": []
        }
        result = await self.db.chats.insert_one(chat_doc)
        return str(result.inserted_id)
    
    async def get_chat(self, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get chat by ID."""
        try:
            chat = await self.db.chats.find_one({"_id": ObjectId(chat_id)})
            if chat:
                chat["_id"] = str(chat["_id"])
            return chat
        except Exception as e:
            print(f"Error getting chat: {e}")
            return None
    
    async def get_user_chats(self, user_id: str) -> List[Dict[str, Any]]:
        """Get all chats for a user."""
        try:
            chats = await self.db.chats.find({"user_id": user_id}).to_list(length=None)
            for chat in chats:
                chat["_id"] = str(chat["_id"])
            return chats
        except Exception as e:
            print(f"Error getting user chats: {e}")
            return []
    
    async def get_event_chat(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Get chat for a specific event."""
        try:
            chat = await self.db.chats.find_one({"event_id": event_id})
            if chat:
                chat["_id"] = str(chat["_id"])
            return chat
        except Exception as e:
            print(f"Error getting event chat: {e}")
            return None
    
    async def add_message(self, chat_id: str, message: Dict[str, Any]) -> bool:
        """Add a message to a chat."""
        try:
            message["timestamp"] = datetime.utcnow()
            result = await self.db.chats.update_one(
                {"_id": ObjectId(chat_id)},
                {
                    "$push": {"messages": message},
                    "$set": {"updated_at": datetime.utcnow()}
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error adding message: {e}")
            return False
    
    async def update_chat_event(self, chat_id: str, event_id: str) -> bool:
        """Update chat with event ID."""
        try:
            result = await self.db.chats.update_one(
                {"_id": ObjectId(chat_id)},
                {
                    "$set": {
                        "event_id": event_id,
                        "updated_at": datetime.utcnow()
                    }
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error updating chat event: {e}")
            return False
    
    # Event Management
    async def save_event(self, event_data: Dict[str, Any], chat_id: str) -> str:
        """Save event to MongoDB."""
        try:
            event_doc = {
                "chat_id": chat_id,
                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow(),
                **event_data
            }
            result = await self.db.events.insert_one(event_doc)
            event_id = str(result.inserted_id)
            
            # Update chat with event ID
            await self.update_chat_event(chat_id, event_id)
            
            return event_id
        except Exception as e:
            print(f"Error saving event: {e}")
            return None
    
    async def update_event(self, event_id: str, event_data: Dict[str, Any]) -> bool:
        """Update event in MongoDB."""
        try:
            result = await self.db.events.update_one(
                {"_id": ObjectId(event_id)},
                {
                    "$set": {
                        **event_data,
                        "updated_at": datetime.utcnow()
                    }
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error updating event: {e}")
            return False
    
    async def get_event(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Get event by ID."""
        try:
            event = await self.db.events.find_one({"_id": ObjectId(event_id)})
            if event:
                event["_id"] = str(event["_id"])
            return event
        except Exception as e:
            print(f"Error getting event: {e}")
            return None
    
    async def get_chat_events(self, chat_id: str) -> List[Dict[str, Any]]:
        """Get all events for a chat."""
        try:
            events = await self.db.events.find({"chat_id": chat_id}).to_list(length=None)
            for event in events:
                event["_id"] = str(event["_id"])
            return events
        except Exception as e:
            print(f"Error getting chat events: {e}")
            return []
    
    # Conversation State Management
    async def save_conversation_state(self, chat_id: str, state: Dict[str, Any]) -> bool:
        """Save conversation state for a chat."""
        try:
            result = await self.db.chats.update_one(
                {"_id": ObjectId(chat_id)},
                {
                    "$set": {
                        "conversation_state": state,
                        "updated_at": datetime.utcnow()
                    }
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error saving conversation state: {e}")
            return False
    
    async def get_conversation_state(self, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get conversation state for a chat."""
        try:
            chat = await self.db.chats.find_one(
                {"_id": ObjectId(chat_id)},
                {"conversation_state": 1}
            )
            return chat.get("conversation_state") if chat else None
        except Exception as e:
            print(f"Error getting conversation state: {e}")
            return None

# Global MongoDB service instance
mongodb_service = MongoDBService()
