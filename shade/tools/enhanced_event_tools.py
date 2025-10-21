"""Enhanced Event Management Tools - More conversational and intelligent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List
import sys
import json
from datetime import datetime, timedelta
sys.path.append('/Users/mayokun/Desktop/Event Planner Monolith/shade')
from external.java_spring_api import JavaSpringAPIClient

# Global client instance
_java_client = None
# Global event data storage for context passing
_pending_event_data = None
_current_event_id = None

def get_java_client() -> JavaSpringAPIClient:
    """Get or create the Java Spring API client."""
    global _java_client
    if _java_client is None:
        _java_client = JavaSpringAPIClient()
    return _java_client

def format_datetime_for_human(dt_str: str) -> str:
    """Convert ISO datetime to human readable format."""
    try:
        dt = datetime.fromisoformat(dt_str.replace('Z', '+00:00'))
        return dt.strftime("%A, %B %d, %Y at %I:%M %p")
    except:
        return dt_str

def format_datetime_for_api(dt_str: str) -> str:
    """Convert human datetime to ISO format."""
    try:
        # Handle various formats
        if 'T' in dt_str:
            return dt_str
        # Add more parsing logic as needed
        return dt_str
    except:
        return dt_str

@tool
async def start_event_creation(
    name: str,
    event_type: str,
    start_datetime: str,
    description: Optional[str] = None
) -> Dict[str, Any]:
    """
    Start creating a new event with core information (name, type, date).
    This creates the event immediately with basic info, then we can add more details.
    
    Args:
        name: Event name (required)
        event_type: CONFERENCE, WEDDING, PARTY, BIRTHDAY, MEETING, WORKSHOP, etc. (required)
        start_datetime: ISO format like 2026-11-24T18:00:00 (required)
        description: Brief description (optional)
    
    Returns:
        Dict with creation result and next steps
    """
    try:
        # Build core event data
        event_data = {
            "name": name,
            "eventType": event_type,
            "startDateTime": start_datetime,
            "eventStatus": "PLANNING"
        }
        
        if description:
            event_data["description"] = description
        
        # Create the event immediately
        client = get_java_client()
        result = await client.create_event(event_data, "550e8400-e29b-41d4-a716-446655440000")
        
        if result.get("success"):
            event = result.get("event", {})
            global _current_event_id
            _current_event_id = event.get("id")
            
            # Create human-like confirmation
            human_date = format_datetime_for_human(start_datetime)
            confirmation = f"""
📝 **Event Planner's Journal Entry**

✅ **Event Created Successfully!**

**Event:** {name}
**Type:** {event_type.replace('_', ' ').title()}
**Date:** {human_date}
**Status:** Planning Phase

🎯 **Next Steps:**
I've created your event with the essential details. Now let's make it perfect! 

Would you like to add:
• Guest capacity and RSVP details
• Event theme and styling
• Venue requirements
• Technical needs (AV, WiFi, etc.)
• Accessibility considerations
• Emergency plans
• Or any other specific details?

Just let me know what you'd like to focus on next! 😊
"""
            
            return {
                "success": True,
                "event_id": event.get("id"),
                "message": confirmation,
                "event": event,
                "next_phase": "enhancement"
            }
        else:
            return {
                "success": False,
                "error": result.get("error", "Unknown error"),
                "message": "I'm sorry, I couldn't create your event. Let me try again with the information you provided."
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "message": "I encountered an issue while creating your event. Let me help you try again."
        }

@tool
async def enhance_event_details(
    capacity: Optional[int] = None,
    end_datetime: Optional[str] = None,
    theme: Optional[str] = None,
    description: Optional[str] = None,
    venue_requirements: Optional[str] = None,
    technical_requirements: Optional[str] = None,
    accessibility_features: Optional[str] = None,
    objectives: Optional[str] = None,
    target_audience: Optional[str] = None,
    hashtag: Optional[str] = None,
    is_public: Optional[bool] = None,
    requires_approval: Optional[bool] = None
) -> Dict[str, Any]:
    """
    Enhance an existing event with additional details.
    This updates the event that was created in the previous step.
    
    Args:
        capacity: Number of guests
        end_datetime: When the event ends
        theme: Event theme/styling
        description: Detailed description
        venue_requirements: What kind of venue is needed
        technical_requirements: AV, WiFi, equipment needs
        accessibility_features: Accessibility considerations
        objectives: Event goals
        target_audience: Who this event is for
        hashtag: Social media hashtag
        is_public: Whether event is public
        requires_approval: Whether RSVPs need approval
    
    Returns:
        Dict with update result
    """
    global _current_event_id
    
    if not _current_event_id:
        return {
            "success": False,
            "error": "No current event to update",
            "message": "I don't have an active event to update. Let's create one first!"
        }
    
    try:
        # Build update data with only provided fields
        update_data = {}
        
        if capacity is not None:
            update_data["capacity"] = capacity
        if end_datetime is not None:
            update_data["endDateTime"] = end_datetime
        if theme is not None:
            update_data["theme"] = theme
        if description is not None:
            update_data["description"] = description
        if venue_requirements is not None:
            update_data["venueRequirements"] = venue_requirements
        if technical_requirements is not None:
            update_data["technicalRequirements"] = technical_requirements
        if accessibility_features is not None:
            update_data["accessibilityFeatures"] = accessibility_features
        if objectives is not None:
            update_data["objectives"] = objectives
        if target_audience is not None:
            update_data["targetAudience"] = target_audience
        if hashtag is not None:
            update_data["hashtag"] = hashtag
        if is_public is not None:
            update_data["isPublic"] = is_public
        if requires_approval is not None:
            update_data["requiresApproval"] = requires_approval
        
        if not update_data:
            return {
                "success": False,
                "error": "No update data provided",
                "message": "I need some details to update your event. What would you like to add or change?"
            }
        
        # Update the event
        client = get_java_client()
        result = await client.update_event(_current_event_id, update_data, "550e8400-e29b-41d4-a716-446655440000")
        
        if result.get("success"):
            event = result.get("event", {})
            
            # Create human-like confirmation
            updates_made = []
            if capacity: updates_made.append(f"Capacity: {capacity} guests")
            if end_datetime: updates_made.append(f"End time: {format_datetime_for_human(end_datetime)}")
            if theme: updates_made.append(f"Theme: {theme}")
            if venue_requirements: updates_made.append(f"Venue needs: {venue_requirements}")
            if technical_requirements: updates_made.append(f"Technical needs: {technical_requirements}")
            
            confirmation = f"""
📝 **Event Planner's Journal Update**

✅ **Event Enhanced Successfully!**

**Updated Details:**
{chr(10).join(f"• {update}" for update in updates_made)}

🎯 **Your event is looking great!** 

Is there anything else you'd like to add or modify? I can help with:
• Emergency and backup plans
• Success metrics and goals
• Branding guidelines
• Post-event tasks
• Or any other specific requirements

Just let me know what's on your mind! 😊
"""
            
            return {
                "success": True,
                "event_id": _current_event_id,
                "message": confirmation,
                "event": event,
                "updates_made": updates_made
            }
        else:
            return {
                "success": False,
                "error": result.get("error", "Unknown error"),
                "message": "I had trouble updating your event. Let me try again with the information you provided."
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "message": "I encountered an issue while updating your event. Let me help you try again."
        }

@tool
async def get_event_info(
    question: str,
    event_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Get information about an event and answer natural language questions.
    This can answer questions like "What date is my event?" or "How many guests can attend?"
    
    Args:
        question: Natural language question about the event
        event_id: Specific event ID (optional, uses current event if not provided)
    
    Returns:
        Dict with answer and event details
    """
    global _current_event_id
    
    target_event_id = event_id or _current_event_id
    
    if not target_event_id:
        return {
            "success": False,
            "error": "No event specified",
            "message": "I don't have an event to look up. Let's create one first or tell me which event you're asking about!"
        }
    
    try:
        client = get_java_client()
        result = await client.get_event(target_event_id, "550e8400-e29b-41d4-a716-446655440000")
        
        if result.get("success"):
            event = result.get("event", {})
            
            # Analyze the question and provide intelligent answers
            question_lower = question.lower()
            
            # Date-related questions
            if any(word in question_lower for word in ['date', 'when', 'time', 'schedule']):
                start_date = event.get('startDateTime')
                end_date = event.get('endDateTime')
                
                if start_date:
                    human_start = format_datetime_for_human(start_date)
                    if end_date:
                        human_end = format_datetime_for_human(end_date)
                        answer = f"Your event is scheduled for **{human_start}** and ends at **{human_end}**."
                    else:
                        answer = f"Your event is scheduled for **{human_start}**. I don't have an end time set yet - would you like to add one?"
                else:
                    answer = "I don't have a date set for your event yet. Would you like to schedule it?"
            
            # Capacity-related questions
            elif any(word in question_lower for word in ['capacity', 'guests', 'people', 'attendees', 'how many']):
                capacity = event.get('capacity')
                current_count = event.get('currentAttendeeCount', 0)
                
                if capacity:
                    answer = f"Your event can accommodate **{capacity} guests**. Currently, {current_count} people are registered."
                else:
                    answer = "I don't have a capacity limit set for your event yet. How many guests are you expecting?"
            
            # Theme-related questions
            elif any(word in question_lower for word in ['theme', 'style', 'decor', 'decoration']):
                theme = event.get('theme')
                if theme:
                    answer = f"Your event theme is **{theme}**. It sounds like it's going to be amazing!"
                else:
                    answer = "I don't have a theme set for your event yet. What kind of theme or style are you thinking about?"
            
            # Venue-related questions
            elif any(word in question_lower for word in ['venue', 'location', 'where', 'place']):
                venue_req = event.get('venueRequirements')
                if venue_req:
                    answer = f"Here are your venue requirements: **{venue_req}**"
                else:
                    answer = "I don't have venue requirements set yet. What kind of venue are you looking for?"
            
            # General event info
            else:
                name = event.get('name', 'Your event')
                event_type = event.get('eventType', 'event')
                status = event.get('eventStatus', 'PLANNING')
                
                answer = f"Here's what I know about **{name}**:\n"
                answer += f"• Type: {event_type.replace('_', ' ').title()}\n"
                answer += f"• Status: {status.replace('_', ' ').title()}\n"
                
                if event.get('startDateTime'):
                    answer += f"• Date: {format_datetime_for_human(event['startDateTime'])}\n"
                if event.get('capacity'):
                    answer += f"• Capacity: {event['capacity']} guests\n"
                if event.get('theme'):
                    answer += f"• Theme: {event['theme']}\n"
            
            return {
                "success": True,
                "answer": answer,
                "event": event,
                "question": question
            }
        else:
            return {
                "success": False,
                "error": result.get("error", "Event not found"),
                "message": "I couldn't find that event. Let me help you create one or check the event ID."
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "message": "I had trouble looking up your event. Let me help you try again."
        }

@tool
async def get_current_event_status() -> Dict[str, Any]:
    """
    Get the status and details of the current event we're working with.
    
    Returns:
        Dict with current event details
    """
    global _current_event_id
    
    if not _current_event_id:
        return {
            "success": False,
            "error": "No current event",
            "message": "We don't have an active event to work with. Let's create one first! 🎉"
        }
    
    try:
        client = get_java_client()
        result = await client.get_event(_current_event_id, "550e8400-e29b-41d4-a716-446655440000")
        
        if result.get("success"):
            event = result.get("event", {})
            
            # Create a comprehensive status report
            status_report = f"""
📝 **Current Event Status**

**Event:** {event.get('name', 'Untitled Event')}
**Type:** {event.get('eventType', 'Unknown').replace('_', ' ').title()}
**Status:** {event.get('eventStatus', 'Unknown').replace('_', ' ').title()}
"""
            
            if event.get('startDateTime'):
                status_report += f"**Start:** {format_datetime_for_human(event['startDateTime'])}\n"
            if event.get('endDateTime'):
                status_report += f"**End:** {format_datetime_for_human(event['endDateTime'])}\n"
            if event.get('capacity'):
                status_report += f"**Capacity:** {event['capacity']} guests\n"
            if event.get('theme'):
                status_report += f"**Theme:** {event['theme']}\n"
            if event.get('description'):
                status_report += f"**Description:** {event['description']}\n"
            
            # Check what's missing
            missing_details = []
            if not event.get('endDateTime'):
                missing_details.append("End time")
            if not event.get('capacity'):
                missing_details.append("Guest capacity")
            if not event.get('theme'):
                missing_details.append("Event theme")
            if not event.get('venueRequirements'):
                missing_details.append("Venue requirements")
            
            if missing_details:
                status_report += f"\n🎯 **Still need to add:** {', '.join(missing_details)}\n"
            else:
                status_report += "\n✅ **Event is fully planned!**\n"
            
            return {
                "success": True,
                "message": status_report,
                "event": event
            }
        else:
            return {
                "success": False,
                "error": result.get("error", "Event not found"),
                "message": "I couldn't find the current event. Let's create a new one!"
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "message": "I had trouble checking the current event. Let me help you try again."
        }
