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
    
    # ==================== EVENT MANAGEMENT (SAFE) ====================
    async def get_event_status(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/status") as response:
                if response.status == 200:
                    status = await response.json()
                    return {"success": True, "status": status}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def publish_event(self, event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Publish an event. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID to publish
            user_id: User ID for permission check (required)
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
            
            async with session.post(f"{self.base_url}/events/{event_id}/publish", headers=headers) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def open_registration(self, event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Open registration for an event. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID
            user_id: User ID for permission check (required)
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
            
            async with session.post(f"{self.base_url}/events/{event_id}/open-registration", headers=headers) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def close_registration(self, event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Close registration for an event. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID
            user_id: User ID for permission check (required)
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
            
            async with session.post(f"{self.base_url}/events/{event_id}/close-registration", headers=headers) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def get_event_capacity(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/capacity") as response:
                if response.status == 200:
                    return {"success": True, "capacity": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def update_capacity(self, event_id: str, capacity: int, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Update event capacity. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID
            capacity: New capacity value
            user_id: User ID for permission check (required)
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
            
            async with session.put(
                f"{self.base_url}/events/{event_id}/capacity",
                json={"capacity": capacity},
                headers=headers
            ) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def update_registration_deadline(self, event_id: str, deadline_iso: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.put(
                f"{self.base_url}/events/{event_id}/registration-deadline",
                json={"deadline": deadline_iso}
            ) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def get_visibility(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/visibility") as response:
                if response.status == 200:
                    return {"success": True, "visibility": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def update_visibility(self, event_id: str, is_public: bool, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Update event visibility. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID
            is_public: True for public, False for private
            user_id: User ID for permission check (required)
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
            
            async with session.put(
                f"{self.base_url}/events/{event_id}/visibility",
                json={"isPublic": is_public},
                headers=headers
            ) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def generate_qr_code(self, event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Generate QR code for an event. Requires ownership/permission via X-User-Id header.
        
        Args:
            event_id: Event ID
            user_id: User ID for permission check (required)
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
            
            async with session.post(f"{self.base_url}/events/{event_id}/qr-code/generate", headers=headers) as response:
                if response.status == 200:
                    return {"success": True, "event": await response.json()}
                elif response.status == 403:
                    return {"success": False, "error": "Permission denied", "status_code": 403}
                return {"success": False, "error": await response.text(), "status_code": response.status}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def get_qr_code(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/qr-code") as response:
                if response.status == 200:
                    return {"success": True, "qr": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def validate_event(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/validation") as response:
                if response.status == 200:
                    return {"success": True, "validation": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def event_health(self, event_id: str) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            async with session.get(f"{self.base_url}/events/{event_id}/health") as response:
                if response.status == 200:
                    return {"success": True, "health": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def search_events(self, q: Optional[str] = None, type: Optional[str] = None, status: Optional[str] = None, date_from: Optional[str] = None, date_to: Optional[str] = None) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            params = {}
            if q: params["q"] = q
            if type: params["type"] = type
            if status: params["status"] = status
            if date_from: params["dateFrom"] = date_from
            if date_to: params["dateTo"] = date_to
            async with session.get(f"{self.base_url}/events/search", params=params) as response:
                if response.status == 200:
                    return {"success": True, "events": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def get_my_events(self, user_id: Optional[str] = None) -> Dict[str, Any]:
        try:
            session = await self.get_session()
            headers = {}
            if user_id:
                try:
                    uuid.UUID(user_id)
                    headers["X-User-Id"] = user_id
                except ValueError:
                    headers["X-User-Id"] = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"chatbot-{user_id}"))
            else:
                headers["X-User-Id"] = "550e8400-e29b-41d4-a716-446655440000"
            async with session.get(f"{self.base_url}/events/my-events", headers=headers) as response:
                if response.status == 200:
                    return {"success": True, "summary": await response.json()}
                return {"success": False, "error": await response.text()}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
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

