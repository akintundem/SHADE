"""Shared context memory for LangGraph flow."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
from datetime import datetime, timedelta
import json

logger = logging.getLogger(__name__)


class SharedContextMemory:
    """Shared context memory for maintaining state across the flow."""
    
    def __init__(self):
        """Initialize shared context memory."""
        self.contexts: Dict[str, Dict[str, Any]] = {}
        self.conversation_histories: Dict[str, List[Dict[str, Any]]] = {}
        self.entity_memories: Dict[str, Dict[str, Any]] = {}
        self._lock = asyncio.Lock()
    
    async def get_context(self, user_id: str, chat_id: str) -> Dict[str, Any]:
        """Get shared context for a user/chat."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.contexts:
                self.contexts[context_key] = {
                    "user_id": user_id,
                    "chat_id": chat_id,
                    "created_at": datetime.utcnow().isoformat(),
                    "last_updated": datetime.utcnow().isoformat(),
                    "entities": {},
                    "preferences": {},
                    "current_event": None,
                    "domain_states": {},
                    "conversation_summary": None
                }
            
            return self.contexts[context_key].copy()
    
    async def update_context(self, user_id: str, chat_id: str, updates: Dict[str, Any]) -> None:
        """Update shared context."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.contexts:
                await self.get_context(user_id, chat_id)
            
            # Update context
            self.contexts[context_key].update(updates)
            self.contexts[context_key]["last_updated"] = datetime.utcnow().isoformat()
            
            # Update domain states if provided
            if "domain_responses" in updates:
                domain_responses = updates["domain_responses"]
                for domain, response in domain_responses.items():
                    if domain not in self.contexts[context_key]["domain_states"]:
                        self.contexts[context_key]["domain_states"][domain] = {}
                    
                    # Only store the response text, not the full response object
                    response_text = response.get("response", "") if isinstance(response, dict) else str(response)
                    self.contexts[context_key]["domain_states"][domain].update({
                        "last_response": response_text,
                        "last_updated": datetime.utcnow().isoformat()
                    })
    
    async def add_entity(self, user_id: str, chat_id: str, entity_type: str, entity_data: Dict[str, Any]) -> None:
        """Add an entity to the context."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.contexts:
                await self.get_context(user_id, chat_id)
            
            if entity_type not in self.contexts[context_key]["entities"]:
                self.contexts[context_key]["entities"][entity_type] = []
            
            entity_data["added_at"] = datetime.utcnow().isoformat()
            self.contexts[context_key]["entities"][entity_type].append(entity_data)
    
    async def get_entities(self, user_id: str, chat_id: str, entity_type: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get entities from context."""
        context = await self.get_context(user_id, chat_id)
        entities = context.get("entities", {})
        
        if entity_type:
            return entities.get(entity_type, [])
        else:
            # Return all entities
            all_entities = []
            for entities_of_type in entities.values():
                all_entities.extend(entities_of_type)
            return all_entities
    
    async def add_conversation_turn(self, user_id: str, chat_id: str, user_message: str, agent_response: str, metadata: Dict[str, Any] = None) -> None:
        """Add a conversation turn to history."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.conversation_histories:
                self.conversation_histories[context_key] = []
            
            turn = {
                "timestamp": datetime.utcnow().isoformat(),
                "user_message": user_message,
                "agent_response": agent_response,
                "metadata": metadata or {}
            }
            
            self.conversation_histories[context_key].append(turn)
            
            # Keep only last 50 turns
            if len(self.conversation_histories[context_key]) > 50:
                self.conversation_histories[context_key] = self.conversation_histories[context_key][-50:]
    
    async def get_conversation_history(self, user_id: str, chat_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        """Get conversation history."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.conversation_histories:
                return []
            
            return self.conversation_histories[context_key][-limit:]
    
    async def update_preferences(self, user_id: str, chat_id: str, preferences: Dict[str, Any]) -> None:
        """Update user preferences."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.contexts:
                await self.get_context(user_id, chat_id)
            
            self.contexts[context_key]["preferences"].update(preferences)
            self.contexts[context_key]["last_updated"] = datetime.utcnow().isoformat()
    
    async def set_current_event(self, user_id: str, chat_id: str, event_data: Dict[str, Any]) -> None:
        """Set the current event being planned."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key not in self.contexts:
                await self.get_context(user_id, chat_id)
            
            self.contexts[context_key]["current_event"] = {
                **event_data,
                "set_at": datetime.utcnow().isoformat()
            }
            self.contexts[context_key]["last_updated"] = datetime.utcnow().isoformat()
    
    async def get_current_event(self, user_id: str, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get the current event being planned."""
        context = await self.get_context(user_id, chat_id)
        return context.get("current_event")
    
    async def clear_context(self, user_id: str, chat_id: str) -> None:
        """Clear context for a user/chat."""
        context_key = f"{user_id}_{chat_id}"
        
        async with self._lock:
            if context_key in self.contexts:
                del self.contexts[context_key]
            if context_key in self.conversation_histories:
                del self.conversation_histories[context_key]
            if context_key in self.entity_memories:
                del self.entity_memories[context_key]
    
    async def get_stats(self) -> Dict[str, Any]:
        """Get memory statistics."""
        async with self._lock:
            return {
                "total_contexts": len(self.contexts),
                "total_conversations": len(self.conversation_histories),
                "total_entities": len(self.entity_memories),
                "memory_usage": {
                    "contexts": sum(len(str(ctx)) for ctx in self.contexts.values()),
                    "conversations": sum(len(conv) for conv in self.conversation_histories.values()),
                    "entities": sum(len(entities) for entities in self.entity_memories.values())
                }
            }
    
    async def cleanup_old_contexts(self, max_age_hours: int = 24) -> int:
        """Clean up old contexts."""
        cutoff_time = datetime.utcnow() - timedelta(hours=max_age_hours)
        cleaned_count = 0
        
        async with self._lock:
            keys_to_remove = []
            
            for context_key, context in self.contexts.items():
                last_updated = datetime.fromisoformat(context.get("last_updated", "1970-01-01T00:00:00"))
                if last_updated < cutoff_time:
                    keys_to_remove.append(context_key)
            
            for key in keys_to_remove:
                if key in self.contexts:
                    del self.contexts[key]
                if key in self.conversation_histories:
                    del self.conversation_histories[key]
                if key in self.entity_memories:
                    del self.entity_memories[key]
                cleaned_count += 1
        
        return cleaned_count
