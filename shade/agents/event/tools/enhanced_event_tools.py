"""Enhanced Event Management Tools - More conversational and intelligent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List
import sys
import json
from datetime import datetime, timedelta
sys.path.append('/Users/mayokun/Desktop/Event Planner Monolith/shade')
from external.java_spring_api import JavaSpringAPIClient
from external.weather_api import WeatherAPIService
from external.google_apis import GoogleAPIService
from knowledge.rag_gateway import RAGGateway
from knowledge.vector_store import VectorStore
from knowledge.embedding_pipeline import EmbeddingPipeline
from .validation import EventValidator

# Global client instance
_java_client = None
# Global event data storage for context passing
_pending_event_data = None
_current_event_id = None
_current_user_id = None  # Track current user context

# Lightweight RAG for event facts
_vector_store = VectorStore()
_embedding = EmbeddingPipeline()
_rag = RAGGateway(_vector_store, _embedding)
_rag_initialized = False

async def _ensure_rag_initialized():
    global _rag_initialized
    if not _rag_initialized:
        await _vector_store.initialize()
        await _embedding.initialize()
        await _rag.initialize()
        _rag_initialized = True

async def _write_event_fact(event: Dict[str, Any], summary: str):
    await _ensure_rag_initialized()
    content = f"Event Fact: {summary}\nData: {json.dumps(event, default=str)}"
    metadata = {"event_id": event.get("id"), "type": "event_fact"}
    await _rag.add_document("event", content, metadata)

# External service singletons
_weather_api = WeatherAPIService()
_google_api = GoogleAPIService()
_validator = EventValidator()
_ext_initialized = False

async def _ensure_ext_initialized():
    global _ext_initialized
    if not _ext_initialized:
        await _weather_api.initialize()
        await _google_api.initialize()
        _ext_initialized = True

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
async def check_event_weather(
    event_id: Optional[str] = None,
    location: Optional[str] = None,
    date: Optional[str] = None,
    time: Optional[str] = None
) -> Dict[str, Any]:
    """
    Check weather viability for the event date/time and return a UI insight.
    Provide either an event_id or location+date(+time).
    """
    await _ensure_ext_initialized()
    global _current_event_id
    target_event_id = event_id or _current_event_id

    # Resolve from event if needed
    if target_event_id and (not date or not location):
        client = get_java_client()
        ev = await client.get_event(target_event_id)
        if ev.get("success"):
            e = ev["event"]
            date = date or (e.get("startDateTime", "T").split("T")[0])
            time = time or (e.get("startDateTime", "T").split("T")[1][:5] if "T" in e.get("startDateTime", "") else "14:00")
            location = location or e.get("venueRequirements", "Winnipeg, MB")

    date = date or datetime.utcnow().date().isoformat()
    time = time or "14:00"
    location = location or "Winnipeg, MB"

    wx = await _weather_api.check_outdoor_event_viability(location=location, event_date=date, event_time=time)
    if not wx.get("success"):
        return {"success": False, "message": "Weather service unavailable"}

    fc = wx["weather_forecast"]
    sev = "info"
    if wx["viability_score"] < 40:
        sev = "critical"
    elif wx["viability_score"] < 75:
        sev = "warn"

    reply = f"Weather check for {date} at {time}: {fc['condition']}, precip {fc['precipitation_chance']}%."
    if wx["concerns"]:
        reply += " " + "; ".join(wx["concerns"]) + "."

    return {
        "success": True,
        "message": reply,
        "ui": {
            "insights": [
                {
                    "type": "weather",
                    "severity": sev,
                    "message": reply,
                    "confidence": 0.8
                }
            ],
            "nextStep": {
                "prompt": "Shall I add an indoor backup or adjust time?",
                "suggestedActions": ["Add backup venue", "Adjust time"]
            }
        }
    }

@tool
async def search_venues_google(
    area: str,
    min_capacity: Optional[int] = None
) -> Dict[str, Any]:
    """
    Search venues (mock/Google) and return venue cards shaped for UI.
    """
    await _ensure_ext_initialized()

    # Mocked venue results shaped for UI; integrate real Places later
    venues = [
        {
            "type": "venue",
            "title": "Grand Ballroom Estate",
            "subtitle": f"{area} • 200–300 guests",
            "rating": 4.8,
            "imageUrl": "https://picsum.photos/seed/ballroom/800/400",
            "pill": "$8,000 – $12,000",
            "meta": {"placeId": "mock:grand_ballroom", "driveTimeMin": 15},
            "actions": [
                {"type": "primary", "label": "Select Venue", "action": "select_venue", "payload": {"placeId": "mock:grand_ballroom"}},
                {"type": "secondary", "label": "Send Inquiry", "action": "open_email_draft", "payload": {"placeId": "mock:grand_ballroom"}}
            ]
        },
        {
            "type": "venue",
            "title": "Enchanted Garden Estate",
            "subtitle": f"Countryside • 150–250 guests",
            "rating": 4.9,
            "imageUrl": "https://picsum.photos/seed/garden/800/400",
            "pill": "$6,500 – $10,000",
            "meta": {"placeId": "mock:garden_estate", "driveTimeMin": 22},
            "actions": [
                {"type": "primary", "label": "Select Venue", "action": "select_venue", "payload": {"placeId": "mock:garden_estate"}},
                {"type": "secondary", "label": "Send Inquiry", "action": "open_email_draft", "payload": {"placeId": "mock:garden_estate"}}
            ]
        }
    ]

    if min_capacity:
        venues = [v for v in venues if ("200" in v["subtitle"] or "150" in v["subtitle"]) and min_capacity <= 300]

    return {
        "success": True,
        "message": f"Here are venues around {area} that fit your event.",
        "ui": {"cards": venues, "nextStep": {"prompt": "Select a venue or send an inquiry.", "suggestedActions": ["Select Venue", "Send Inquiry"]}}
    }

# Helper to extract user_id from tool invocation context
def _get_user_id_from_context() -> Optional[str]:
    """Try to extract user_id from current context if available."""
    global _current_user_id
    return _current_user_id

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
        
        # Validate event data before creation
        validation_report = await _validator.validate_event_creation(event_data)
        
        # Check for critical validation issues
        if not validation_report.is_valid:
            critical_issues = validation_report.critical_issues
            if critical_issues:
                return {
                    "success": False,
                    "error": "Validation failed",
                    "message": f"I found some issues that need to be fixed: {'; '.join(critical_issues)}",
                    "validation_report": {
                        "is_valid": False,
                        "critical_issues": critical_issues,
                        "warnings": validation_report.warnings,
                        "suggestions": validation_report.suggestions
                    }
                }
        
        # Create the event immediately
        client = get_java_client()
        result = await client.create_event(event_data, "550e8400-e29b-41d4-a716-446655440000")
        
        if result.get("success"):
            event = result.get("event", {})
            global _current_event_id
            _current_event_id = event.get("id")
            
            # Get predictive warnings and best practices
            predictions = await _validator.predict_issues(event_data)
            best_practices = _validator.get_best_practices(event_type)
            seasonal_recs = _validator.get_seasonal_recommendations(event_data)
            
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
            
            # Add validation warnings if any
            if validation_report.warnings:
                confirmation += f"\n⚠️ **Planning Notes:**\n"
                for warning in validation_report.warnings:
                    confirmation += f"• {warning}\n"
            
            # Add predictive warnings
            if predictions:
                confirmation += f"\n🔮 **Heads Up:**\n"
                for prediction in predictions:
                    confirmation += f"• {prediction}\n"
            
            # Add best practices
            if best_practices:
                confirmation += f"\n💡 **Best Practices for {event_type.replace('_', ' ').title()}:**\n"
                for practice in best_practices[:3]:  # Show top 3
                    confirmation += f"• {practice}\n"
            
            # Store fact in RAG
            await _write_event_fact(event, f"Created event '{name}' on {human_date} ({event_type}).")

            return {
                "success": True,
                "event_id": event.get("id"),
                "message": confirmation,
                "ui": {
                    "chips": [
                        {"label": "Add capacity", "action": "open_capacity"},
                        {"label": "Pick a theme", "action": "open_theme"},
                        {"label": "Choose venue", "action": "open_venue_search"}
                    ],
                    "nextStep": {"prompt": "Want me to check weather for that day?", "suggestedActions": ["Check weather", "Suggest venues"]}
                },
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
        
        # Get current event data for validation
        client = get_java_client()
        current_event_result = await client.get_event(_current_event_id)
        
        if current_event_result.get("success"):
            current_event = current_event_result.get("event", {})
            # Merge current data with updates for validation
            updated_event_data = {**current_event, **update_data}
            
            # Validate the updated event data
            validation_report = await _validator.validate_event_update(updated_event_data, current_event)
            
            # Check for critical validation issues
            if not validation_report.is_valid:
                critical_issues = validation_report.critical_issues
                if critical_issues:
                    return {
                        "success": False,
                        "error": "Validation failed",
                        "message": f"I found some issues with the updates: {'; '.join(critical_issues)}",
                        "validation_report": {
                            "is_valid": False,
                            "critical_issues": critical_issues,
                            "warnings": validation_report.warnings,
                            "suggestions": validation_report.suggestions
                        }
                    }
        
        # Update the event
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
            
            # Store fact in RAG
            await _write_event_fact(event, f"Updated event details: {', '.join(updates_made)}")

            return {
                "success": True,
                "event_id": _current_event_id,
                "message": confirmation,
                "ui": {
                    "insights": [{"type": "update", "severity": "info", "message": ", ".join(updates_made)}],
                    "nextStep": {"prompt": "Anything else to add?", "suggestedActions": ["Set end time", "Invite guests"]}
                },
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
    event_id: Optional[str] = None,
    user_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Get information about an event and answer natural language questions.
    This can answer questions like "What date is my event?" or "How many guests can attend?"
    Only returns data if user has access to the event (owner or event is public).
    
    Args:
        question: Natural language question about the event
        event_id: Specific event ID (optional, uses current event if not provided)
        user_id: User ID for permission check (required for private events)
    
    Returns:
        Dict with answer and event details
    """
    global _current_event_id, _current_user_id
    
    target_event_id = event_id or _current_event_id
    target_user_id = user_id or _current_user_id
    
    if not target_event_id:
        return {
            "success": False,
            "error": "No event specified",
            "message": "I don't have an event to look up. Let's create one first or tell me which event you're asking about!"
        }
    
    try:
        # 1) Try vector facts first
        await _ensure_rag_initialized()
        facts = await _rag.retrieve_context(question, domain="event", max_results=3)
        fact_snippets = [f["content"] for f in facts]

        # 2) Fetch latest DB snapshot
        client = get_java_client()
        result = await client.get_event(target_event_id)
        
        if result.get("success"):
            event = result.get("event", {})
            
            # Check visibility: if private, user must be owner
            if not event.get("isPublic", True) and target_user_id:
                perm_check = await _check_event_permission(target_event_id, target_user_id)
                if not perm_check.get("allowed") or not perm_check.get("is_owner"):
                    return {
                        "success": False,
                        "error": "Access denied",
                        "message": "This is a private event. Only the owner can view event information."
                    }
            elif not event.get("isPublic", True) and not target_user_id:
                return {
                    "success": False,
                    "error": "Access denied",
                    "message": "This is a private event. Please provide your user ID to access it."
                }
            
            # Analyze the question and provide intelligent answers (prefer RAG if directly answers)
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
                
                # Compute discrepancy with attendee list
                attendees_resp = await client.list_attendees_by_event(target_event_id)
                attendee_count = len(attendees_resp.get("attendees", [])) if attendees_resp.get("success") else current_count

                if capacity:
                    note = ""
                    if attendee_count is not None and capacity is not None and attendee_count != capacity:
                        note = f" Also, you set capacity to {capacity} but we currently have {attendee_count} attendee(s) on the list."
                    answer = f"Your event can accommodate **{capacity} guests**. Currently, {attendee_count} people are registered.{note}"
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
                "ui": {
                    "insights": fact_snippets and [{"type": "facts", "severity": "info", "message": fact_snippets[0]}] or []
                },
                "event": event,
                "question": question,
                "supporting_facts": fact_snippets
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

@tool
async def check_event_onboarding_requirements() -> Dict[str, Any]:
    """
    Check required onboarding details for the current event and return missing items.
    Required before sensitive actions (publish, open registration, QR, visibility changes):
    - capacity
    - visibility (public/private)
    - startDateTime (future)
    - eventType
    - name
    - registrationDeadline (recommended if registration will be opened)
    """
    global _current_event_id
    if not _current_event_id:
        return {"success": False, "error": "No current event", "missing": ["event"]}

    client = get_java_client()
    ev = await client.get_event(_current_event_id)
    if not ev.get("success"):
        return {"success": False, "error": ev.get("error", "not found"), "missing": ["event"]}

    e = ev["event"]
    missing = []
    if not e.get("name"): missing.append("name")
    if not e.get("eventType"): missing.append("eventType")
    if not e.get("startDateTime"): missing.append("startDateTime")
    if e.get("startDateTime"):
        try:
            dt = datetime.fromisoformat(e["startDateTime"].replace('Z', '+00:00'))
            if dt <= datetime.utcnow():
                missing.append("startDateTime_future")
        except Exception:
            missing.append("startDateTime_format")
    if e.get("capacity") is None: missing.append("capacity")

    vis = await client.get_visibility(_current_event_id)
    if not vis.get("success"):
        missing.append("visibility")
    else:
        v = vis["visibility"]
        if v.get("isPublic") is None: missing.append("visibility")

    if e.get("eventStatus") == "REGISTRATION_OPEN" and not e.get("registrationDeadline"):
        missing.append("registrationDeadline")

    return {
        "success": True,
        "missing": missing,
        "ready": len([m for m in missing if m not in ["registrationDeadline", "startDateTime_future", "startDateTime_format"]]) == 0
    }

async def _check_event_permission(event_id: str, user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Internal helper to check if user has permission to modify an event.
    Returns permission check result.
    """
    global _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for permission check", "allowed": False}
    
    # Get event to check ownership
    client = get_java_client()
    event_result = await client.get_event(event_id)
    
    if not event_result.get("success"):
        return {"success": False, "error": "Event not found", "allowed": False}
    
    event = event_result.get("event", {})
    
    # Check ownership via Java API (it will enforce authorization on the backend)
    # For now, we rely on the Java backend to enforce permissions via headers
    # But we can do a pre-check by verifying the event exists and is accessible
    
    # Attempt to get user's events to verify ownership
    my_events = await client.get_my_events(target_user_id)
    if my_events.get("success"):
        summary = my_events.get("summary", {})
        # Check if event is in user's owned events
        owned_events = summary.get("ownedEvents", [])
        is_owner = any(str(e.get("eventId")) == str(event_id) for e in owned_events)
        
        if is_owner:
            return {"success": True, "allowed": True, "is_owner": True}
    
    # Fallback: rely on backend permission check
    # The Java API will return 403 if user doesn't have permission
    return {"success": True, "allowed": True, "is_owner": None, "note": "Backend will enforce permissions"}

@tool
async def publish_current_event(user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Publish the current event. Requires onboarding requirements to be satisfied.
    Also requires user to have ownership/permission for this event.
    
    Args:
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for publishing events"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to publish this event")}
    
    checklist = await check_event_onboarding_requirements()
    if not checklist.get("success") or not checklist.get("ready"):
        return {"success": False, "error": "Onboarding incomplete", "missing": checklist.get("missing", [])}
    
    client = get_java_client()
    # Note: Backend will enforce ownership via X-User-Id header
    return await client.publish_event(_current_event_id, target_user_id)

@tool
async def open_registration_for_current_event(user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Open registration for the current event. Requires onboarding requirements satisfied
    and preferably a registrationDeadline set. Also requires user to have ownership/permission.
    
    Args:
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for opening registration"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to modify this event")}
    
    checklist = await check_event_onboarding_requirements()
    if not checklist.get("success") or not checklist.get("ready"):
        return {"success": False, "error": "Onboarding incomplete", "missing": checklist.get("missing", [])}
    
    client = get_java_client()
    return await client.open_registration(_current_event_id, target_user_id)

@tool
async def close_registration_for_current_event(user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Close registration for the current event. Requires user to have ownership/permission.
    
    Args:
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for closing registration"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to modify this event")}
    
    client = get_java_client()
    return await client.close_registration(_current_event_id, target_user_id)

@tool
async def update_capacity_for_current_event(capacity: int, user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Update capacity for the current event. Requires user to have ownership/permission.
    
    Args:
        capacity: New capacity value (must be >= 1)
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for updating capacity"}
    
    if capacity < 1:
        return {"success": False, "error": "Capacity must be at least 1"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to modify this event")}
    
    client = get_java_client()
    return await client.update_capacity(_current_event_id, capacity, target_user_id)

@tool
async def set_visibility_for_current_event(is_public: bool, user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Set visibility (public/private) for the current event. Requires user to have ownership/permission.
    
    Args:
        is_public: True for public, False for private
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for changing visibility"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to modify this event")}
    
    client = get_java_client()
    return await client.update_visibility(_current_event_id, is_public, target_user_id)

@tool
async def generate_qr_for_current_event(user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Generate a QR code for the current event. Requires onboarding to be ready and user to have ownership/permission.
    
    Args:
        user_id: User ID of the person requesting the action (required for permission check)
    """
    global _current_event_id, _current_user_id
    target_user_id = user_id or _current_user_id
    
    if not _current_event_id:
        return {"success": False, "error": "No current event"}
    
    if not target_user_id:
        return {"success": False, "error": "User ID required for generating QR code"}
    
    # Check permissions
    perm_check = await _check_event_permission(_current_event_id, target_user_id)
    if not perm_check.get("allowed"):
        return {"success": False, "error": "Permission denied", "details": perm_check.get("error", "You don't have permission to modify this event")}
    
    checklist = await check_event_onboarding_requirements()
    if not checklist.get("success") or not checklist.get("ready"):
        return {"success": False, "error": "Onboarding incomplete", "missing": checklist.get("missing", [])}
    
    client = get_java_client()
    return await client.generate_qr_code(_current_event_id, target_user_id)

@tool
async def get_event_capacity_summary(event_id: Optional[str] = None, user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Get capacity summary (capacity, registered, available spots, registration open) for an event.
    Only returns data if user has access to the event (owner or event is public).
    
    Args:
        event_id: Event ID (uses current event if not provided)
        user_id: User ID for permission check (optional, but recommended)
    """
    global _current_event_id, _current_user_id
    target_event_id = event_id or _current_event_id
    target_user_id = user_id or _current_user_id
    
    if not target_event_id:
        return {"success": False, "error": "No event specified"}
    
    client = get_java_client()
    
    # First check if event exists and user has access
    event_result = await client.get_event(target_event_id)
    if not event_result.get("success"):
        return {"success": False, "error": "Event not found or access denied"}
    
    event = event_result.get("event", {})
    
    # Check visibility: if private, user must be owner
    if not event.get("isPublic", True) and target_user_id:
        perm_check = await _check_event_permission(target_event_id, target_user_id)
        if not perm_check.get("allowed") or not perm_check.get("is_owner"):
            return {"success": False, "error": "Access denied", "details": "This is a private event. Only the owner can view capacity information."}
    
    cap = await client.get_event_capacity(target_event_id)
    return cap

@tool
async def list_my_events(user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    List the current user's events (summary) via Java API.
    """
    client = get_java_client()
    return await client.get_my_events(user_id)

@tool
async def search_public_events(query: Optional[str] = None, type: Optional[str] = None, status: Optional[str] = None, date_from: Optional[str] = None, date_to: Optional[str] = None, user_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Search public events with optional filters. Only returns PUBLIC events.
    Private events are automatically filtered out.
    
    Args:
        query: Search query string
        type: Event type filter (WEDDING, CONFERENCE, etc.)
        status: Event status filter
        date_from: Start date filter (ISO format)
        date_to: End date filter (ISO format)
        user_id: User ID (optional, used to also include user's private events if needed)
    """
    client = get_java_client()
    
    # Use the search endpoint which respects visibility
    # The Java backend getPublicEvents() and searchPublicEvents() only return public events
    result = await client.search_events(q=query, type=type, status=status, date_from=date_from, date_to=date_to)
    
    if result.get("success"):
        events = result.get("events", [])
        # Filter to only public events (backend should do this, but double-check)
        public_events = [e for e in events if e.get("isPublic", False)]
        
        # If user_id provided, also include their private events
        if user_id:
            my_events = await client.get_my_events(user_id)
            if my_events.get("success"):
                summary = my_events.get("summary", {})
                owned = summary.get("ownedEvents", [])
                # Add user's private events to results
                for owned_event in owned:
                    if not owned_event.get("isPublic", False):
                        # Check if it matches filters
                        matches = True
                        if type and owned_event.get("eventType") != type:
                            matches = False
                        if status and owned_event.get("eventStatus") != status:
                            matches = False
                        if query and query.lower() not in owned_event.get("eventName", "").lower():
                            matches = False
                        if matches:
                            public_events.append(owned_event)
        
        result["events"] = public_events
        result["count"] = len(public_events)
    
    return result
