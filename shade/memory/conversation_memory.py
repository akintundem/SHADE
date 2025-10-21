"""Conversation memory for the Shade agent using MongoDB."""

from typing import Dict, Any, List, Optional
from datetime import datetime
from mongodb_service import mongodb_service


class ConversationMemory:
    """MongoDB-backed conversation memory for the agent."""
    
    def __init__(self):
        self.db = mongodb_service
    
    async def save_conversation_state(self, chat_id: str, state: Dict[str, Any]) -> bool:
        """Save conversation state to MongoDB."""
        try:
            # Remove non-serializable items
            clean_state = self._clean_state(state)
            
            result = await self.db.save_conversation_state(chat_id, clean_state)
            return result
        except Exception as e:
            print(f"Error saving conversation state: {e}")
            return False
    
    async def get_conversation_state(self, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get conversation state from MongoDB."""
        try:
            state = await self.db.get_conversation_state(chat_id)
            return state
        except Exception as e:
            print(f"Error getting conversation state: {e}")
            return None
    
    async def save_tool_result(self, chat_id: str, tool_name: str, result: Dict[str, Any]) -> bool:
        """Save tool execution result for future reference."""
        try:
            tool_result = {
                "tool_name": tool_name,
                "result": result,
                "timestamp": datetime.utcnow()
            }
            
            # Save to tool_history in conversation state
            current_state = await self.get_conversation_state(chat_id) or {}
            tool_history = current_state.get("tool_history", [])
            tool_history.append(tool_result)
            
            current_state["tool_history"] = tool_history
            return await self.save_conversation_state(chat_id, current_state)
        except Exception as e:
            print(f"Error saving tool result: {e}")
            return False
    
    async def get_tool_history(self, chat_id: str, tool_name: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get tool execution history."""
        try:
            state = await self.get_conversation_state(chat_id)
            if not state:
                return []
            
            tool_history = state.get("tool_history", [])
            
            if tool_name:
                return [result for result in tool_history if result.get("tool_name") == tool_name]
            
            return tool_history
        except Exception as e:
            print(f"Error getting tool history: {e}")
            return []
    
    async def save_entity_mention(self, chat_id: str, entity_type: str, entity_value: str) -> bool:
        """Save mentioned entities for context tracking."""
        try:
            current_state = await self.get_conversation_state(chat_id) or {}
            mentioned_entities = current_state.get("mentioned_entities", {})
            
            if entity_type not in mentioned_entities:
                mentioned_entities[entity_type] = []
            
            if entity_value not in mentioned_entities[entity_type]:
                mentioned_entities[entity_type].append(entity_value)
            
            current_state["mentioned_entities"] = mentioned_entities
            return await self.save_conversation_state(chat_id, current_state)
        except Exception as e:
            print(f"Error saving entity mention: {e}")
            return False
    
    async def get_mentioned_entities(self, chat_id: str, entity_type: Optional[str] = None) -> Dict[str, List[str]]:
        """Get mentioned entities."""
        try:
            state = await self.get_conversation_state(chat_id)
            if not state:
                return {}
            
            mentioned_entities = state.get("mentioned_entities", {})
            
            if entity_type:
                return {entity_type: mentioned_entities.get(entity_type, [])}
            
            return mentioned_entities
        except Exception as e:
            print(f"Error getting mentioned entities: {e}")
            return {}
    
    def _clean_state(self, state: Dict[str, Any]) -> Dict[str, Any]:
        """Remove non-serializable items from state."""
        clean_state = {}
        
        for key, value in state.items():
            if key == "messages":
                # Keep only the last 10 messages to avoid large documents
                if isinstance(value, list) and len(value) > 10:
                    clean_state[key] = value[-10:]
                else:
                    clean_state[key] = value
            elif key == "tool_instance":
                # Skip tool instances as they're not serializable
                continue
            elif isinstance(value, (str, int, float, bool, list, dict, type(None))):
                clean_state[key] = value
            else:
                # Convert other types to string
                clean_state[key] = str(value)
        
        return clean_state
