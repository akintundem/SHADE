"""Event management tools for the Shade agent - Integrated with Java Spring API."""

from langchain.tools import tool
from typing import Dict, Any, Optional
import sys
sys.path.append('/Users/mayokun/Desktop/Event Planner Monolith/shade')
from external.java_spring_api import JavaSpringAPIClient

# Global client instance
_java_client = None
# Global event data storage for context passing
_pending_event_data = None

def get_java_client() -> JavaSpringAPIClient:
    """Get or create the Java Spring API client."""
    global _java_client
    if _java_client is None:
        _java_client = JavaSpringAPIClient()
    return _java_client


@tool
async def prepare_event_for_creation(
    name: str,
    event_type: str,
    start_datetime: str,
    description: Optional[str] = None,
    capacity: Optional[int] = None,
    end_datetime: Optional[str] = None,
    theme: Optional[str] = None
) -> Dict[str, Any]:
    """
    Prepare event data for creation. This validates all required fields are present
    before asking for user confirmation.
    
    Args:
        name: Event name (required)
        event_type: CONFERENCE, WEDDING, PARTY, BIRTHDAY, MEETING, WORKSHOP (required)
        start_datetime: ISO format like 2026-11-24T18:00:00 (required)
        description: Event description (optional)
        capacity: Number of guests (optional)
        end_datetime: End time in ISO format (optional)
        theme: Event theme (optional)
    
    Returns:
        Dict with validation status and formatted summary for user confirmation
    """
    # Build event data
    event_data = {
        "name": name,
        "eventType": event_type,
        "startDateTime": start_datetime
    }
    
    if description:
        event_data["description"] = description
    if capacity:
        event_data["capacity"] = capacity
    if end_datetime:
        event_data["endDateTime"] = end_datetime
    if theme:
        event_data["theme"] = theme
    
    # Store event data globally for the create_event_confirmed tool
    global _pending_event_data
    _pending_event_data = event_data
    
    # Format a human-readable summary
    summary = f"""
📅 **Event Summary**

**Name:** {name}
**Type:** {event_type}
**Date & Time:** {start_datetime}
"""
    
    if description:
        summary += f"**Description:** {description}\n"
    if capacity:
        summary += f"**Capacity:** {capacity} guests\n"
    if end_datetime:
        summary += f"**End Time:** {end_datetime}\n"
    if theme:
        summary += f"**Theme:** {theme}\n"
    
    return {
        "success": True,
        "ready_for_creation": True,
        "event_data": event_data,
        "summary": summary,
        "message": "Event is ready for creation! Please confirm to proceed."
    }


@tool
async def create_event_confirmed(
    user_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Create event in Java Spring backend after user confirmation.
    This should ONLY be called after user explicitly approves the creation.
    The event data is retrieved from the shared context.
    
    Args:
        user_id: Optional user ID (defaults to test user for now)
    
    Returns:
        Dict with creation result
    """
    # For testing, use a default user_id if none provided
    if user_id is None:
        user_id = "550e8400-e29b-41d4-a716-446655440000"  # Valid UUID format
    
    # Get the event data from the stored pending event data
    global _pending_event_data
    if _pending_event_data is None:
        return {
            "success": False,
            "error": "No event data found. Please prepare an event first.",
            "message": "Please use prepare_event_for_creation first."
        }
    
    event_data = _pending_event_data
    
    client = get_java_client()
    result = await client.create_event(event_data, user_id)
    
    if result.get("success"):
        event = result.get("event", {})
        # Clear the pending event data after successful creation
        _pending_event_data = None
        return {
            "success": True,
            "event_id": event.get("id"),
            "message": f"🎉 Successfully created event '{event.get('name')}'!",
            "event": event
        }
    else:
        return {
            "success": False,
            "error": result.get("error", "Unknown error"),
            "message": "Failed to create event. Please try again."
        }


@tool
async def get_event_details(event_id: str) -> Dict[str, Any]:
    """
    Get event details from Java Spring backend.
    Use this when user asks about an event they created.
    
    Args:
        event_id: UUID of the event
    
    Returns:
        Dict with event details
    """
    client = get_java_client()
    result = await client.get_event(event_id)
    
    if result.get("success"):
        event = result.get("event", {})
        # Format response in a friendly way
        response = f"""
Found your event! 🎉

**{event.get('name')}**
- Type: {event.get('eventType')}
- Date: {event.get('startDateTime')}
- Status: {event.get('eventStatus')}
"""
        if event.get('capacity'):
            response += f"- Capacity: {event.get('capacity')} guests\n"
        if event.get('description'):
            response += f"- Description: {event.get('description')}\n"
        
        return {
            "success": True,
            "event": event,
            "formatted_response": response
        }
    else:
        return {
            "success": False,
            "error": result.get("error"),
            "message": "Couldn't find that event. Do you have the event ID?"
        }


@tool
async def prepare_event_update(
    event_id: str,
    updates: Dict[str, Any]
) -> Dict[str, Any]:
    """
    Prepare event updates for confirmation before applying.
    
    Args:
        event_id: Event ID to update
        updates: Dictionary of fields to update
    
    Returns:
        Dict with update summary for confirmation
    """
    # Format update summary
    summary = f"""
📝 **Proposed Updates for Event**

**Event ID:** {event_id}

**Changes:**
"""
    for key, value in updates.items():
        summary += f"- {key}: {value}\n"
    
    return {
        "success": True,
        "ready_for_update": True,
        "event_id": event_id,
        "updates": updates,
        "summary": summary,
        "message": "Ready to update! Please confirm to proceed."
    }


@tool
async def update_event_confirmed(
    event_id: str,
    updates: Dict[str, Any]
) -> Dict[str, Any]:
    """
    Update event in Java Spring backend after user confirmation.
    Only call after user explicitly approves the update.
    
    Args:
        event_id: Event ID to update
        updates: Fields to update
    
    Returns:
        Dict with update result
    """
    client = get_java_client()
    result = await client.update_event(event_id, updates)
    
    if result.get("success"):
        return {
            "success": True,
            "message": f"✅ Successfully updated event!",
            "event": result.get("event")
        }
    else:
        return {
            "success": False,
            "error": result.get("error"),
            "message": "Failed to update event. Please try again."
        }


@tool
async def check_java_api_health() -> Dict[str, Any]:
    """
    Check if Java Spring API is reachable.
    Use this to verify backend connectivity.
    
    Returns:
        Dict with health status
    """
    client = get_java_client()
    result = await client.health_check()
    return result
