"""Chat service for managing chat sessions and conversation state."""

from typing import Dict, Any, Optional
from datetime import datetime
from mongodb_service import mongodb_service

class ChatService:
    """Service for managing chat sessions and conversation state."""
    
    def __init__(self):
        self.active_chats = {}  # In-memory cache for active chats
    
    async def get_or_create_chat(self, user_id: str, event_id: Optional[str] = None) -> str:
        """Get existing chat or create new one."""
        # Check if user has an active chat
        user_chats = await mongodb_service.get_user_chats(user_id)
        active_chat = None
        
        for chat in user_chats:
            if chat.get("status") == "active":
                active_chat = chat
                break
        
        if active_chat:
            chat_id = active_chat["_id"]
            # Update event_id if provided
            if event_id and not active_chat.get("event_id"):
                await mongodb_service.update_chat_event(chat_id, event_id)
            return chat_id
        else:
            # Create new chat
            return await mongodb_service.create_chat(user_id, event_id)
    
    async def add_message(self, chat_id: str, message: str, is_user: bool, 
                         tool_used: str = None, data: Dict[str, Any] = None) -> bool:
        """Add a message to the chat."""
        message_doc = {
            "message": message,
            "is_user": is_user,
            "tool_used": tool_used,
            "data": data,
            "timestamp": datetime.utcnow()
        }
        return await mongodb_service.add_message(chat_id, message_doc)
    
    async def get_chat_messages(self, chat_id: str) -> list:
        """Get all messages for a chat."""
        chat = await mongodb_service.get_chat(chat_id)
        return chat.get("messages", []) if chat else []
    
    async def save_conversation_state(self, chat_id: str, state: Dict[str, Any]) -> bool:
        """Save conversation state for a chat."""
        return await mongodb_service.save_conversation_state(chat_id, state)
    
    async def get_conversation_state(self, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get conversation state for a chat."""
        return await mongodb_service.get_conversation_state(chat_id)
    
    async def complete_chat(self, chat_id: str) -> bool:
        """Mark chat as completed."""
        try:
            result = await mongodb_service.db.chats.update_one(
                {"_id": ObjectId(chat_id)},
                {
                    "$set": {
                        "status": "completed",
                        "updated_at": datetime.utcnow()
                    }
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error completing chat: {e}")
            return False
    
    async def get_chat_summary(self, chat_id: str) -> Dict[str, Any]:
        """Get chat summary for display."""
        chat = await mongodb_service.get_chat(chat_id)
        if not chat:
            return None
        
        messages = chat.get("messages", [])
        user_messages = [msg for msg in messages if msg.get("is_user")]
        assistant_messages = [msg for msg in messages if not msg.get("is_user")]
        
        return {
            "chat_id": chat_id,
            "user_id": chat.get("user_id"),
            "event_id": chat.get("event_id"),
            "created_at": chat.get("created_at"),
            "updated_at": chat.get("updated_at"),
            "status": chat.get("status"),
            "message_count": len(messages),
            "user_message_count": len(user_messages),
            "assistant_message_count": len(assistant_messages),
            "last_message": messages[-1] if messages else None
        }

# Global chat service instance
chat_service = ChatService()
