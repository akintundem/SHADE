"""Timeline management tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List


@tool
async def create_timeline(
    event_id: str,
    event_date: str,
    start_time: str,
    end_time: str,
    timeline_items: Optional[List[Dict[str, Any]]] = None
) -> Dict[str, Any]:
    """Create a timeline for an event.
    
    Args:
        event_id: ID of the event
        event_date: Date of the event (YYYY-MM-DD)
        start_time: Start time (HH:MM)
        end_time: End time (HH:MM)
        timeline_items: Optional list of timeline items
    
    Returns:
        Dict with timeline creation details
    """
    timeline_id = f"timeline_{event_id}"
    
    # Default timeline items if none provided
    if not timeline_items:
        timeline_items = [
            {
                "time": "09:00",
                "activity": "Setup and preparation",
                "duration": "2 hours",
                "responsible": "Event team"
            },
            {
                "time": "11:00",
                "activity": "Guest arrival and registration",
                "duration": "1 hour",
                "responsible": "Registration team"
            },
            {
                "time": "12:00",
                "activity": "Welcome and opening remarks",
                "duration": "30 minutes",
                "responsible": "Host"
            },
            {
                "time": "12:30",
                "activity": "Main event activities",
                "duration": "3 hours",
                "responsible": "Various"
            },
            {
                "time": "15:30",
                "activity": "Closing remarks and wrap-up",
                "duration": "30 minutes",
                "responsible": "Host"
            },
            {
                "time": "16:00",
                "activity": "Cleanup and breakdown",
                "duration": "2 hours",
                "responsible": "Event team"
            }
        ]
    
    return {
        "success": True,
        "timeline_id": timeline_id,
        "message": "Timeline created successfully",
        "timeline": {
            "id": timeline_id,
            "event_id": event_id,
            "event_date": event_date,
            "start_time": start_time,
            "end_time": end_time,
            "timeline_items": timeline_items,
            "status": "DRAFT"
        }
    }


@tool
async def update_timeline(
    timeline_id: str,
    timeline_items: List[Dict[str, Any]],
    notes: Optional[str] = None
) -> Dict[str, Any]:
    """Update an existing timeline.
    
    Args:
        timeline_id: ID of the timeline to update
        timeline_items: Updated list of timeline items
        notes: Optional notes about the changes
    
    Returns:
        Dict with timeline update details
    """
    return {
        "success": True,
        "timeline_id": timeline_id,
        "message": "Timeline updated successfully",
        "timeline": {
            "id": timeline_id,
            "timeline_items": timeline_items,
            "notes": notes,
            "last_updated": "2024-01-15T10:30:00"
        }
    }


@tool
async def get_timeline(timeline_id: str) -> Dict[str, Any]:
    """Get timeline details by ID.
    
    Args:
        timeline_id: ID of the timeline to retrieve
    
    Returns:
        Dict with timeline details
    """
    # Mock timeline data
    return {
        "success": True,
        "timeline": {
            "id": timeline_id,
            "event_id": "evt_123456789",
            "event_date": "2024-06-15",
            "start_time": "09:00",
            "end_time": "18:00",
            "timeline_items": [
                {
                    "time": "09:00",
                    "activity": "Setup and preparation",
                    "duration": "2 hours",
                    "responsible": "Event team",
                    "location": "Main venue",
                    "notes": "Sound check and equipment setup"
                },
                {
                    "time": "11:00",
                    "activity": "Guest arrival and registration",
                    "duration": "1 hour",
                    "responsible": "Registration team",
                    "location": "Entrance",
                    "notes": "Welcome packets and name tags"
                },
                {
                    "time": "12:00",
                    "activity": "Welcome and opening remarks",
                    "duration": "30 minutes",
                    "responsible": "Host",
                    "location": "Main stage",
                    "notes": "Introduction and agenda overview"
                },
                {
                    "time": "12:30",
                    "activity": "Main event activities",
                    "duration": "3 hours",
                    "responsible": "Various",
                    "location": "Multiple locations",
                    "notes": "Keynote, workshops, networking"
                },
                {
                    "time": "15:30",
                    "activity": "Closing remarks and wrap-up",
                    "duration": "30 minutes",
                    "responsible": "Host",
                    "location": "Main stage",
                    "notes": "Thank you and next steps"
                },
                {
                    "time": "16:00",
                    "activity": "Cleanup and breakdown",
                    "duration": "2 hours",
                    "responsible": "Event team",
                    "location": "All areas",
                    "notes": "Equipment return and venue cleanup"
                }
            ],
            "status": "FINALIZED",
            "created_at": "2024-01-10T14:20:00",
            "updated_at": "2024-01-15T10:30:00"
        }
    }
