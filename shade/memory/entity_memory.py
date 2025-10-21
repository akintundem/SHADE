"""Entity memory for tracking mentioned entities across conversations."""

from typing import Dict, Any, List, Optional
from datetime import datetime
from mongodb_service import mongodb_service


class EntityMemory:
    """Entity memory for tracking venues, vendors, dates, etc. across conversations."""
    
    def __init__(self):
        self.db = mongodb_service
    
    async def save_entity(self, user_id: str, entity_type: str, entity_data: Dict[str, Any]) -> bool:
        """Save an entity for future reference."""
        try:
            entity_doc = {
                "user_id": user_id,
                "entity_type": entity_type,
                "entity_data": entity_data,
                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow(),
                "usage_count": 1
            }
            
            # Check if entity already exists
            existing = await self.get_entity(user_id, entity_type, entity_data.get("id"))
            if existing:
                # Update existing entity
                entity_doc["usage_count"] = existing.get("usage_count", 0) + 1
                entity_doc["updated_at"] = datetime.utcnow()
                result = await self.db.db.entities.update_one(
                    {"user_id": user_id, "entity_type": entity_type, "entity_data.id": entity_data.get("id")},
                    {"$set": entity_doc}
                )
            else:
                # Create new entity
                result = await self.db.db.entities.insert_one(entity_doc)
            
            return True
        except Exception as e:
            print(f"Error saving entity: {e}")
            return False
    
    async def get_entity(self, user_id: str, entity_type: str, entity_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific entity by ID."""
        try:
            entity = await self.db.db.entities.find_one({
                "user_id": user_id,
                "entity_type": entity_type,
                "entity_data.id": entity_id
            })
            
            if entity:
                entity["_id"] = str(entity["_id"])
            
            return entity
        except Exception as e:
            print(f"Error getting entity: {e}")
            return None
    
    async def get_entities_by_type(self, user_id: str, entity_type: str) -> List[Dict[str, Any]]:
        """Get all entities of a specific type for a user."""
        try:
            entities = await self.db.db.entities.find({
                "user_id": user_id,
                "entity_type": entity_type
            }).sort("usage_count", -1).to_list(length=None)
            
            for entity in entities:
                entity["_id"] = str(entity["_id"])
            
            return entities
        except Exception as e:
            print(f"Error getting entities by type: {e}")
            return []
    
    async def search_entities(self, user_id: str, query: str, entity_type: Optional[str] = None) -> List[Dict[str, Any]]:
        """Search entities by name or description."""
        try:
            search_filter = {
                "user_id": user_id,
                "$or": [
                    {"entity_data.name": {"$regex": query, "$options": "i"}},
                    {"entity_data.description": {"$regex": query, "$options": "i"}}
                ]
            }
            
            if entity_type:
                search_filter["entity_type"] = entity_type
            
            entities = await self.db.db.entities.find(search_filter).sort("usage_count", -1).to_list(length=None)
            
            for entity in entities:
                entity["_id"] = str(entity["_id"])
            
            return entities
        except Exception as e:
            print(f"Error searching entities: {e}")
            return []
    
    async def get_recent_entities(self, user_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        """Get recently used entities."""
        try:
            entities = await self.db.db.entities.find({
                "user_id": user_id
            }).sort("updated_at", -1).limit(limit).to_list(length=None)
            
            for entity in entities:
                entity["_id"] = str(entity["_id"])
            
            return entities
        except Exception as e:
            print(f"Error getting recent entities: {e}")
            return []
    
    async def update_entity_usage(self, user_id: str, entity_type: str, entity_id: str) -> bool:
        """Update entity usage count and timestamp."""
        try:
            result = await self.db.db.entities.update_one(
                {"user_id": user_id, "entity_type": entity_type, "entity_data.id": entity_id},
                {
                    "$inc": {"usage_count": 1},
                    "$set": {"updated_at": datetime.utcnow()}
                }
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error updating entity usage: {e}")
            return False
