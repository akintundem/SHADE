"""Client for Java Spring Boot Event Planner API."""

import aiohttp
import logging
import uuid
from typing import Dict, Any, Optional
from datetime import datetime

logger = logging.getLogger(__name__)


class JavaSpringAPIClient:
    """Client to interact with Java Spring Boot Event Planner API on localhost:8080."""
    
    def __init__(self, base_url: str = "http://localhost:8080/api/v1"):
        """Initialize the Java Spring API client."""
        self.base_url = base_url
        self.session = None
        
    async def get_session(self) -> aiohttp.ClientSession:
        """Get or create aiohttp session."""
        if self.session is None or self.session.closed:
            self.session = aiohttp.ClientSession()
        return self.session
    
    async def close(self):
        """Close the aiohttp session."""
        if self.session and not self.session.closed:
            await self.session.close()
    
    # ==================== EVENT CRUD OPERATIONS ====================
    
    async def create_event(self, event_data: Dict[str, Any], user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Create a new event in the Java Spring backend.
        
        Args:
            event_data: Event data matching CreateEventRequest DTO
            user_id: Optional user ID for authorization
            
        Returns:
            Created event response
        """
        try:
            session = await self.get_session()
            headers = {}
            if user_id:
                # Ensure user_id is a valid UUID, generate one if not
                try:
                    # Try to parse as UUID to validate
                    uuid.UUID(user_id)
                    headers["X-User-Id"] = user_id
                except ValueError:
                    # If not a valid UUID, generate a deterministic one for the chatbot
                    chatbot_uuid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"chatbot-{user_id}"))
                    headers["X-User-Id"] = chatbot_uuid
            else:
                # Default chatbot UUID
                headers["X-User-Id"] = "550e8400-e29b-41d4-a716-446655440000"
            
            # Add default values if not provided
            if "eventStatus" not in event_data:
                event_data["eventStatus"] = "PLANNING"
            if "isPublic" not in event_data:
                event_data["isPublic"] = True
            if "requiresApproval" not in event_data:
                event_data["requiresApproval"] = False
            if "qrCodeEnabled" not in event_data:
                event_data["qrCodeEnabled"] = False
            
            async with session.post(
                f"{self.base_url}/events",
                json=event_data,
                headers=headers
            ) as response:
                if response.status == 201:
                    result = await response.json()
                    logger.info(f"✅ Successfully created event: {result.get('id')}")
                    return {
                        "success": True,
                        "event": result,
                        "message": "Event created successfully!"
                    }
                else:
                    error_text = await response.text()
                    logger.error(f"❌ Failed to create event: {response.status} - {error_text}")
                    return {
                        "success": False,
                        "error": f"Failed to create event: {error_text}",
                        "status_code": response.status
                    }
        except Exception as e:
            logger.error(f"❌ Error creating event: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    async def get_event(self, event_id: str) -> Dict[str, Any]:
        """
        Get event by ID from Java Spring backend.
        
        Args:
            event_id: UUID of the event
            
        Returns:
            Event data or error
        """
        try:
            session = await self.get_session()
            
            async with session.get(f"{self.base_url}/events/{event_id}") as response:
                if response.status == 200:
                    result = await response.json()
                    logger.info(f"✅ Successfully retrieved event: {event_id}")
                    return {
                        "success": True,
                        "event": result
                    }
                elif response.status == 404:
                    return {
                        "success": False,
                        "error": "Event not found"
                    }
                else:
                    error_text = await response.text()
                    logger.error(f"❌ Failed to get event: {response.status} - {error_text}")
                    return {
                        "success": False,
                        "error": f"Failed to get event: {error_text}"
                    }
        except Exception as e:
            logger.error(f"❌ Error getting event: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    async def update_event(self, event_id: str, event_data: Dict[str, Any], user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Update an existing event in the Java Spring backend.
        
        Args:
            event_id: UUID of the event to update
            event_data: Update data matching UpdateEventRequest DTO
            user_id: Optional user ID for authorization
            
        Returns:
            Updated event response
        """
        try:
            session = await self.get_session()
            headers = {}
            if user_id:
                # Ensure user_id is a valid UUID, generate one if not
                try:
                    # Try to parse as UUID to validate
                    uuid.UUID(user_id)
                    headers["X-User-Id"] = user_id
                except ValueError:
                    # If not a valid UUID, generate a deterministic one for the chatbot
                    chatbot_uuid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"chatbot-{user_id}"))
                    headers["X-User-Id"] = chatbot_uuid
            else:
                # Default chatbot UUID
                headers["X-User-Id"] = "550e8400-e29b-41d4-a716-446655440000"
            
            async with session.put(
                f"{self.base_url}/events/{event_id}",
                json=event_data,
                headers=headers
            ) as response:
                if response.status == 200:
                    result = await response.json()
                    logger.info(f"✅ Successfully updated event: {event_id}")
                    return {
                        "success": True,
                        "event": result,
                        "message": "Event updated successfully!"
                    }
                else:
                    error_text = await response.text()
                    logger.error(f"❌ Failed to update event: {response.status} - {error_text}")
                    return {
                        "success": False,
                        "error": f"Failed to update event: {error_text}"
                    }
        except Exception as e:
            logger.error(f"❌ Error updating event: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    # Note: Delete is intentionally not implemented per requirements
    # AI should not be able to delete events
    
    async def health_check(self) -> Dict[str, Any]:
        """Check if Java Spring API is reachable."""
        try:
            session = await self.get_session()
            async with session.get(f"http://localhost:8080/actuator/health", timeout=aiohttp.ClientTimeout(total=5)) as response:
                if response.status == 200:
                    return {
                        "success": True,
                        "message": "Java Spring API is healthy"
                    }
                else:
                    return {
                        "success": False,
                        "message": f"Java Spring API returned status {response.status}"
                    }
        except Exception as e:
            logger.error(f"Java Spring API health check failed: {e}")
            return {
                "success": False,
                "message": f"Java Spring API is unreachable: {str(e)}"
            }

    # ==================== ATTENDEE QUERIES ====================
    async def list_attendees_by_event(self, event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        List attendees for a given event.
        """
        try:
            session = await self.get_session()
            headers = {}
            if user_id:
                try:
                    uuid.UUID(user_id)
                    headers["X-User-Id"] = user_id
                except ValueError:
                    chatbot_uuid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"chatbot-{user_id}"))
                    headers["X-User-Id"] = chatbot_uuid
            else:
                headers["X-User-Id"] = "550e8400-e29b-41d4-a716-446655440000"

            async with session.get(
                f"{self.base_url.replace('/api/v1','')}/api/v1/attendees/event/{event_id}",
                headers=headers
            ) as response:
                if response.status == 200:
                    attendees = await response.json()
                    logger.info(f"✅ Retrieved {len(attendees)} attendees for event {event_id}")
                    return {"success": True, "attendees": attendees}
                elif response.status == 404:
                    return {"success": True, "attendees": []}
                else:
                    error_text = await response.text()
                    logger.error(f"❌ Failed to fetch attendees: {response.status} - {error_text}")
                    return {"success": False, "error": f"Failed to fetch attendees: {error_text}"}
        except Exception as e:
            logger.error(f"❌ Error listing attendees: {e}")
            return {"success": False, "error": str(e)}

