"""Attendee management tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List


@tool
async def add_attendee(
    event_id: str,
    name: str,
    email: str,
    phone: Optional[str] = None,
    plus_one: bool = False,
    dietary_restrictions: Optional[str] = None,
    notes: Optional[str] = None
) -> Dict[str, Any]:
    """Add an attendee to an event.
    
    Args:
        event_id: ID of the event
        name: Attendee's full name
        email: Attendee's email address
        phone: Optional phone number
        plus_one: Whether they can bring a plus one
        dietary_restrictions: Any dietary restrictions
        notes: Additional notes about the attendee
    
    Returns:
        Dict with attendee details
    """
    attendee_id = f"attendee_{event_id}_{name.replace(' ', '_')}"
    
    return {
        "success": True,
        "attendee_id": attendee_id,
        "message": f"Successfully added {name} to the event",
        "attendee": {
            "id": attendee_id,
            "event_id": event_id,
            "name": name,
            "email": email,
            "phone": phone,
            "plus_one": plus_one,
            "dietary_restrictions": dietary_restrictions,
            "notes": notes,
            "rsvp_status": "PENDING",
            "invitation_sent": False
        }
    }


@tool
async def send_invitations(
    event_id: str,
    attendee_ids: Optional[List[str]] = None,
    invitation_type: str = "email",
    custom_message: Optional[str] = None
) -> Dict[str, Any]:
    """Send invitations to attendees.
    
    Args:
        event_id: ID of the event
        attendee_ids: List of attendee IDs (if None, sends to all)
        invitation_type: Type of invitation (email, sms, both)
        custom_message: Custom message to include
    
    Returns:
        Dict with invitation sending results
    """
    # Mock invitation sending
    return {
        "success": True,
        "message": "Invitations sent successfully",
        "invitations": {
            "event_id": event_id,
            "sent_count": 25,
            "failed_count": 0,
            "invitation_type": invitation_type,
            "custom_message": custom_message,
            "sent_to": [
                "john.doe@email.com",
                "jane.smith@email.com",
                "bob.wilson@email.com"
            ]
        }
    }


@tool
async def track_rsvps(event_id: str) -> Dict[str, Any]:
    """Track RSVP responses for an event.
    
    Args:
        event_id: ID of the event
    
    Returns:
        Dict with RSVP tracking information
    """
    # Mock RSVP tracking data
    return {
        "success": True,
        "event_id": event_id,
        "rsvp_summary": {
            "total_invited": 50,
            "responded": 35,
            "pending": 15,
            "accepted": 28,
            "declined": 7,
            "plus_ones": 5
        },
        "responses": [
            {
                "attendee_name": "John Doe",
                "rsvp_status": "ACCEPTED",
                "response_date": "2024-01-10",
                "plus_one": True,
                "dietary_restrictions": "Vegetarian"
            },
            {
                "attendee_name": "Jane Smith",
                "rsvp_status": "ACCEPTED",
                "response_date": "2024-01-11",
                "plus_one": False,
                "dietary_restrictions": None
            },
            {
                "attendee_name": "Bob Wilson",
                "rsvp_status": "DECLINED",
                "response_date": "2024-01-12",
                "plus_one": False,
                "dietary_restrictions": None
            }
        ]
    }


@tool
async def manage_guest_list(
    event_id: str,
    action: str,
    attendee_id: Optional[str] = None,
    filter_status: Optional[str] = None
) -> Dict[str, Any]:
    """Manage the guest list for an event.
    
    Args:
        event_id: ID of the event
        action: Action to perform (list, remove, update_status)
        attendee_id: ID of specific attendee (for remove/update actions)
        filter_status: Filter by RSVP status (PENDING, ACCEPTED, DECLINED)
    
    Returns:
        Dict with guest list management results
    """
    if action == "list":
        # Mock guest list
        guest_list = [
            {
                "id": "attendee_1",
                "name": "John Doe",
                "email": "john.doe@email.com",
                "rsvp_status": "ACCEPTED",
                "plus_one": True,
                "dietary_restrictions": "Vegetarian"
            },
            {
                "id": "attendee_2",
                "name": "Jane Smith",
                "email": "jane.smith@email.com",
                "rsvp_status": "ACCEPTED",
                "plus_one": False,
                "dietary_restrictions": None
            },
            {
                "id": "attendee_3",
                "name": "Bob Wilson",
                "email": "bob.wilson@email.com",
                "rsvp_status": "DECLINED",
                "plus_one": False,
                "dietary_restrictions": None
            }
        ]
        
        # Filter by status if specified
        if filter_status:
            guest_list = [g for g in guest_list if g["rsvp_status"] == filter_status]
        
        return {
            "success": True,
            "action": "list",
            "event_id": event_id,
            "guest_list": guest_list,
            "count": len(guest_list)
        }
    
    elif action == "remove" and attendee_id:
        return {
            "success": True,
            "action": "remove",
            "message": f"Successfully removed attendee {attendee_id}",
            "attendee_id": attendee_id
        }
    
    elif action == "update_status" and attendee_id:
        return {
            "success": True,
            "action": "update_status",
            "message": f"Successfully updated status for attendee {attendee_id}",
            "attendee_id": attendee_id
        }
    
    else:
        return {
            "success": False,
            "error": "Invalid action or missing attendee_id"
        }
