"""Conversational event creation tool that follows CreateEventRequest DTO."""

import json
import os
from typing import Optional, Dict, Any, List
from datetime import datetime
from dotenv import load_dotenv

# Simple in-memory event storage
EVENT_STORAGE = []

# Import LangChain components
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

# Load environment variables
load_dotenv()


class EventTool:
    """Conversational tool for creating events following CreateEventRequest DTO."""
    
    def __init__(self, openai_api_key: Optional[str] = None):
        self.api_key = openai_api_key or os.getenv("OPENAI_API_KEY")
        self._chat = None
        self.event_data = {}
        self.conversation_state = "initial"  # initial, collecting_required, collecting_optional, complete
        self.event_created = False
        self.pending_update = False
        self.pending_data = None
        
        # Conversation guards
        self.banned_topics = [
            "politics", "political", "election", "vote", "government", "president", "minister",
            "war", "conflict", "violence", "hate", "racism", "discrimination",
            "religion", "religious", "church", "mosque", "temple", "god", "jesus", "allah",
            "crypto", "bitcoin", "cryptocurrency", "trading", "investment", "stocks",
            "gambling", "casino", "betting", "lottery"
        ]
        
        self.banned_words = [
            "fuck", "shit", "damn", "hell", "bitch", "ass", "crap", "stupid", "idiot",
            "hate", "kill", "die", "death", "murder", "suicide", "bomb", "terrorist"
        ]
        
        # Required fields from CreateEventRequest DTO
        self.required_fields = {
            "name": "Event name",
            "eventType": "Event type (CONFERENCE, MEETING, WORKSHOP, PARTY, etc.)",
            "startDateTime": "Start date and time (ISO format: YYYY-MM-DDTHH:MM:SS)"
        }
        
        # Optional fields that can be filled
        self.optional_fields = {
            "description": "Event description",
            "eventStatus": "Event status (PLANNING, CONFIRMED, CANCELLED, etc.)",
            "endDateTime": "End date and time",
            "registrationDeadline": "Registration deadline",
            "capacity": "Total capacity (number)",
            "currentAttendeeCount": "Current attendee count",
            "isPublic": "Is the event public? (true/false)",
            "requiresApproval": "Requires approval? (true/false)",
            "qrCodeEnabled": "QR codes enabled? (true/false)",
            "qrCode": "QR code content",
            "coverImageUrl": "Cover image URL",
            "eventWebsiteUrl": "Event website URL",
            "hashtag": "Event hashtag",
            "theme": "Event theme",
            "objectives": "Event objectives",
            "targetAudience": "Target audience",
            "successMetrics": "Success metrics",
            "brandingGuidelines": "Branding guidelines",
            "venueRequirements": "Venue requirements",
            "technicalRequirements": "Technical requirements",
            "accessibilityFeatures": "Accessibility features",
            "emergencyPlan": "Emergency plan",
            "backupPlan": "Backup plan",
            "postEventTasks": "Post-event tasks",
            "metadata": "Additional metadata"
        }
    
    def _get_chat_model(self):
        """Lazy initialization of the chat model."""
        if self._chat is None:
            try:
                if not self.api_key:
                    raise ValueError("OPENAI_API_KEY is not set")
                
                os.environ["OPENAI_API_KEY"] = self.api_key
                self._chat = ChatOpenAI(model="gpt-4o-mini", temperature=0.1)
            except NameError:
                raise ImportError("Install langchain: pip install langchain langchain-openai")
        return self._chat
    
    def can_handle(self, message: str) -> bool:
        """Check if this tool can handle the message with conversation guards."""
        message_lower = message.lower()
        
        # Check for banned content first
        if self._contains_banned_content(message_lower):
            return False
        
        # Check for multiple event creation attempts
        if self.event_created and any(word in message_lower for word in ["create", "new event", "another event", "plan another"]):
            return False
        
        # More specific keywords to avoid conflicts with weather tool
        keywords = ["create event", "create a", "planning an event", "organize event", "event planning"]
        general_keywords = ["conference", "meeting", "workshop", "party", "launch"]
        
        # Read/view keywords
        read_keywords = [
            "show", "list", "view", "see", "display", "get", "find", "search",
            "events", "my events", "all events", "what events", "which events"
        ]
        
        # Check for read/view keywords first
        if any(keyword in message_lower for keyword in read_keywords):
            return True
        
        # Check for specific event creation phrases
        if any(keyword in message_lower for keyword in keywords):
            return True
            
        # Check for general event types, but only if not weather-related
        weather_keywords = ["weather", "temperature", "rain", "sunny", "forecast", "outdoor", "climate", "conditions"]
        if any(weather_keyword in message_lower for weather_keyword in weather_keywords):
            return False
            
        return any(keyword in message_lower for keyword in general_keywords)
    
    def _contains_banned_content(self, message_lower: str) -> bool:
        """Check if message contains banned topics or words."""
        # Check for banned topics
        for topic in self.banned_topics:
            if topic in message_lower:
                return True
        
        # Check for banned words
        for word in self.banned_words:
            if word in message_lower:
                return True
        
        return False
    
    def _handle_banned_content(self, message: str) -> Dict[str, Any]:
        """Handle messages with banned content."""
        return {
            "success": False,
            "message": "Oh darling, I'm here to help you create the most AMAZING event ever! Let's focus on all the wonderful, exciting things we can plan together! What kind of spectacular event are you dreaming of? I'm absolutely buzzing with ideas! ✨🎉",
            "data": None,
            "conversation_state": self.conversation_state
        }
    
    def _handle_multiple_events(self, message: str) -> Dict[str, Any]:
        """Handle attempts to create multiple events."""
        event_name = self.event_data.get('name', 'this event')
        return {
            "success": False,
            "message": f"Oh my goodness, I'm SO excited about '{event_name}' that I want to make it absolutely PERFECT before we even think about anything else! This event is going to be absolutely INCREDIBLE, and I want to give it all my attention! ✨\n\nWhat would you like to add or improve to make this event even more spectacular? I'm here to help you create something absolutely unforgettable! 🎊",
            "data": self.event_data,
            "conversation_state": self.conversation_state
        }
    
    def _extract_event_info(self, message: str) -> Dict[str, Any]:
        """Extract event information from user message using AI."""
        try:
            chat = self._get_chat_model()
            
            # Create a comprehensive prompt for all possible fields
            all_fields = {**self.required_fields, **self.optional_fields}
            field_descriptions = "\n".join([f"- {key}: {desc}" for key, desc in all_fields.items()])
            
            system_prompt = f"""You are an event planning assistant. Extract event information from the user's message and return a JSON object with the following fields. Only include fields that have clear information in the message. Use null for missing information.

Available fields:
{field_descriptions}

Important:
- For eventType, use these exact values: CONFERENCE, MEETING, WORKSHOP, PARTY, SEMINAR, WEBINAR, CONCERT, FESTIVAL, SPORTS, OTHER
- For eventStatus, use these exact values: PLANNING, CONFIRMED, CANCELLED, COMPLETED, POSTPONED
- For dates, use ISO format: YYYY-MM-DDTHH:MM:SS
- For boolean fields, use true/false
- For numbers, use integers
- Return only valid JSON, no other text"""

            response = chat.invoke([
                SystemMessage(content=system_prompt),
                HumanMessage(content=message)
            ])
            
            return json.loads(response.content)
            
        except Exception as e:
            return {"error": str(e)}
    
    def _check_required_fields(self) -> List[str]:
        """Check which required fields are missing."""
        missing = []
        for field in self.required_fields:
            if field not in self.event_data or self.event_data[field] is None:
                missing.append(field)
        return missing
    
    def _get_next_question(self) -> str:
        """Get the next question to ask the user."""
        missing_required = self._check_required_fields()
        
        if missing_required:
            field = missing_required[0]
            return self._get_field_prompt(field)
        
        # All required fields are filled, ask about optional fields
        filled_optional = [k for k, v in self.event_data.items() if k in self.optional_fields and v is not None]
        remaining_optional = [k for k in self.optional_fields if k not in filled_optional]
        
        if remaining_optional:
            field = remaining_optional[0]
            return f"Fantastic! 🎉 Would you like to tell me about the {self.optional_fields[field].lower()}? (Just say 'skip' if you want to move on, or 'done' when you're ready to create this amazing event!)"
        
        return "Perfect! ✨ I have everything I need to create your event. Would you like to review anything or shall we make this event happen?"
    
    def _get_field_prompt(self, field: str) -> str:
        """Get a helpful prompt for a specific field."""
        prompts = {
            "name": "Oh my goodness, I'm absolutely buzzing with excitement! What should we call this incredible event? I want to make sure the name captures the magic we're about to create! ✨",
            "eventType": "I'm getting goosebumps just thinking about this! What kind of spectacular event are we bringing to life? Is it a conference, party, workshop, meeting, or something else entirely? I love the variety! 🎊",
            "startDateTime": "Perfect timing question! When should this magnificent event take place? You can tell me like 'next Friday at 2 PM' or 'December 25th at 6 PM' - I'm here to make it work perfectly! ⏰"
        }
        return prompts.get(field, f"Tell me about the {self.required_fields[field].lower()}")
    
    def _format_event_summary(self) -> str:
        """Format a summary of the collected event data."""
        summary = "📋 **Event Summary:**\n\n"
        
        # Required fields
        summary += "**Required Information:**\n"
        for field in self.required_fields:
            value = self.event_data.get(field, "❌ Missing")
            summary += f"• {self.required_fields[field]}: {value}\n"
        
        # Optional fields that are filled
        filled_optional = {k: v for k, v in self.event_data.items() if k in self.optional_fields and v is not None}
        if filled_optional:
            summary += "\n**Additional Information:**\n"
            for field, value in filled_optional.items():
                summary += f"• {self.optional_fields[field]}: {value}\n"
        
        return summary
    
    def process(self, message: str) -> Dict[str, Any]:
        """Process the conversational event creation and reading with guards."""
        try:
            # Check for banned content
            if self._contains_banned_content(message.lower()):
                return self._handle_banned_content(message)
            
            # Check for multiple event creation attempts
            if self.event_created and any(word in message.lower() for word in ["create", "new event", "another event", "plan another"]):
                return self._handle_multiple_events(message)
            
            # Handle read/view requests first
            if self._is_read_request(message):
                return self._handle_read_request(message)
            
            # Handle special commands
            if message.lower() in ["done", "finish", "complete"]:
                missing_required = self._check_required_fields()
                if missing_required:
                    if len(missing_required) == 1:
                        field = missing_required[0]
                        return {
                            "success": False,
                            "message": f"Oh wait! I still need to know the {self.required_fields[field].lower()}. {self._get_field_prompt(field)}",
                            "data": None,
                            "conversation_state": self.conversation_state
                        }
                    else:
                        return {
                            "success": False,
                            "message": f"Hold on! I still need a few more details to create your event. Let me ask you about them one by one. {self._get_field_prompt(missing_required[0])}",
                            "data": None,
                            "conversation_state": self.conversation_state
                        }
                
                # Automatically create the event
                self.createEvent(self.event_data)
                
                # Set conversation state to collecting additional info
                self.conversation_state = "collecting_additional"
                
                # Return message about event creation and ask for additional info
                event_name = self.event_data.get('name', 'your event')
                event_type = self.event_data.get('eventType', 'event')
                return {
                    "success": True,
                    "message": f"OH MY GOODNESS! 🤩 I just created '{event_name}' and I'm literally jumping with excitement! This {event_type.lower()} is going to be absolutely INCREDIBLE! I can already feel the energy and magic we're about to create together! ✨\n\nNow, darling, let's make this event absolutely PERFECT! I want to add those special touches that will make your guests' jaws drop. What would you like to work on first? I'm thinking description, capacity, venue details, or maybe something else that will make this event unforgettable? 🎊",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state,
                    "show_chips": True
                }
            
            if message.lower() in ["skip", "next"]:
                return {
                    "success": True,
                    "message": self._get_next_question(),
                    "data": self.event_data,
                    "conversation_state": self.conversation_state
                }
            
            # Handle additional information collection
            if self.conversation_state == "collecting_additional":
                return self._handle_additional_info(message)
            
            if message.lower() in ["review", "summary", "show"]:
                return {
                    "success": True,
                    "message": self._format_event_summary() + "\n\n" + self._get_next_question(),
                    "data": self.event_data,
                    "conversation_state": self.conversation_state
                }
            
            # Extract information from the message
            extracted = self._extract_event_info(message)
            
            if "error" in extracted:
                return {
                    "success": False,
                    "message": f"Oops! I had a little trouble understanding that. Could you try saying it differently? 😊",
                    "data": None,
                    "conversation_state": self.conversation_state
                }
            
            # Update event data with extracted information
            for key, value in extracted.items():
                if value is not None:
                    self.event_data[key] = value
            
            # Check if all required fields are present
            missing_required = self._check_required_fields()
            if missing_required:
                self.conversation_state = "collecting_required"
                # Get next question for missing required field
                next_question = self._get_next_question()
                return {
                    "success": True,
                    "message": next_question,
                    "data": self.event_data,
                    "conversation_state": self.conversation_state
                }
            else:
                # All required fields are present - automatically create the event
                self.createEvent(self.event_data)
                
                # Set conversation state to collecting additional info
                self.conversation_state = "collecting_additional"
                
                # Return message about event creation and ask for additional info
                event_name = self.event_data.get('name', 'your event')
                event_type = self.event_data.get('eventType', 'event')
                return {
                    "success": True,
                    "message": f"OH MY GOODNESS! 🤩 I just created '{event_name}' and I'm literally jumping with excitement! This {event_type.lower()} is going to be absolutely INCREDIBLE! I can already feel the energy and magic we're about to create together! ✨\n\nNow, darling, let's make this event absolutely PERFECT! I want to add those special touches that will make your guests' jaws drop. What would you like to work on first? I'm thinking description, capacity, venue details, or maybe something else that will make this event unforgettable? 🎊",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state,
                    "show_chips": True
                }
            
        except Exception as e:
            return {
                "success": False,
                "message": f"Oh no! Something went wrong on my end. Let me try again - could you repeat what you just said? 😅",
                "data": None,
                "conversation_state": self.conversation_state
            }
    
    def createEvent(self, event_data: Dict[str, Any]) -> None:
        """Execute the createEvent tool - prints tool name and data to terminal."""
        print("Tool being called: CreateEvent")
        print(f"This is the data we are passing in: {event_data}")
        print(self._format_event_summary())
        
        # Store the event in memory
        event_id = len(EVENT_STORAGE) + 1
        event_record = {
            "id": event_id,
            "created_at": datetime.now().isoformat(),
            **event_data
        }
        EVENT_STORAGE.append(event_record)
        print(f"Event stored with ID: {event_id}")
        
        # Mark event as created to prevent multiple events
        self.event_created = True
    
    def updateEvent(self, event_data: Dict[str, Any]) -> None:
        """Execute the updateEvent tool - prints tool name and data to terminal."""
        print("Tool being called: UpdateEvent")
        print(f"This is the data we are passing in: {event_data}")
        print(self._format_event_summary())
        
        # Update the most recent event in storage with new information
        if EVENT_STORAGE:
            # Find the most recent event and update it
            latest_event = EVENT_STORAGE[-1]
            latest_event.update(event_data)
            print(f"Updated event ID {latest_event['id']} with new information")
    
    def _handle_additional_info(self, message: str) -> Dict[str, Any]:
        """Handle additional information collection after event creation."""
        # Handle confirmation for updates
        if self.pending_update:
            if message.lower() in ["yes", "yeah", "yep", "sure", "ok", "okay", "definitely", "absolutely", "go ahead", "do it"]:
                # Apply the pending update
                for key, value in self.pending_data.items():
                    if value is not None and key in self.optional_fields:
                        self.event_data[key] = value
                
                # Call updateEvent tool
                self.updateEvent(self.event_data)
                
                # Clear pending update
                self.pending_update = False
                self.pending_data = None
                
                return {
                    "success": True,
                    "message": "DONE! ✨ I've just updated your event with those fantastic details! I'm getting chills thinking about how amazing this is going to be! 🎊\n\nIs there anything else you'd like to add to make this event even more spectacular? I'm here to help you create something absolutely unforgettable!",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state,
                    "show_chips": True
                }
            elif message.lower() in ["no", "nope", "not now", "skip", "cancel"]:
                # Clear pending update without applying
                self.pending_update = False
                self.pending_data = None
                
                return {
                    "success": True,
                    "message": "No problem at all, darling! Let's keep it as is for now. What else would you like to work on? I'm here to help you perfect every detail! ✨",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state,
                    "show_chips": True
                }
        
        # Handle yes/no responses for adding details
        if message.lower() in ["yes", "yeah", "yep", "sure", "ok", "okay", "definitely", "absolutely"]:
            return {
                "success": True,
                "message": "YES! I'm so excited you want to add more details! This is where the magic really happens, darling! ✨\n\nWhat would you like to work on first? I'm thinking:\n• **Event description** - Let's craft something that makes people's hearts skip a beat!\n• **Capacity** - How many amazing people are we expecting?\n• **Venue details** - Where should this spectacular event take place?\n• **Special touches** - QR codes, approval processes, or other magical elements?\n\nWhat speaks to you first? I'm here to make it absolutely perfect! 🎊",
                "data": self.event_data,
                "conversation_state": self.conversation_state,
                "show_chips": True
            }
        
        if message.lower() in ["no", "nope", "not now", "skip", "done", "that's all", "finish"]:
            self.conversation_state = "complete"
            event_name = self.event_data.get('name', 'your event')
            return {
                "success": True,
                "message": f"Absolutely PERFECT! 🎉✨ Your '{event_name}' event is now completely set up and ready to absolutely blow everyone's minds! I'm so proud of what we've created together - this is going to be absolutely INCREDIBLE! \n\nIf you need to make any changes or want to add more magical touches later, just say the word and I'll be right here to help you make it even more spectacular! You've got this! 🎊",
                "data": self.event_data,
                "conversation_state": self.conversation_state
            }
        
        # Extract additional information
        extracted = self._extract_event_info(message)
        
        if "error" in extracted:
            return {
                "success": False,
                "message": "Oops! I had a little trouble understanding that. Could you try saying it differently? 😊",
                "data": self.event_data,
                "conversation_state": self.conversation_state
            }
        
        # Update event data with new information
        updated = False
        for key, value in extracted.items():
            if value is not None and key in self.optional_fields:
                self.event_data[key] = value
                updated = True
        
        if updated:
            # Show what we're about to update before doing it
            updated_fields = []
            for key, value in extracted.items():
                if value is not None and key in self.optional_fields:
                    updated_fields.append(f"• {self.optional_fields[key]}: {value}")
            
            if updated_fields:
                fields_text = "\n".join(updated_fields)
                # Set pending update state
                self.pending_update = True
                self.pending_data = extracted
                return {
                    "success": True,
                    "message": f"Perfect! I love what you're thinking! Let me update your event with:\n\n{fields_text}\n\nShould I go ahead and add these amazing details to your event? I'm so excited about how this is shaping up! ✨",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state,
                    "show_chips": True
                }
        else:
            return {
                "success": True,
                "message": "I didn't catch any specific event details in that. Could you tell me what you'd like to add? For example: 'Add a description about networking' or 'Set capacity to 100 people'?",
                "data": self.event_data,
                "conversation_state": self.conversation_state,
                "show_chips": True
            }
    
    def _is_read_request(self, message: str) -> bool:
        """Check if the message is a read/view request."""
        read_keywords = [
            "show", "list", "view", "see", "display", "get", "find", "search",
            "events", "my events", "all events", "what events", "which events"
        ]
        message_lower = message.lower()
        return any(keyword in message_lower for keyword in read_keywords)
    
    def _handle_read_request(self, message: str) -> Dict[str, Any]:
        """Handle read/view requests for events."""
        if not EVENT_STORAGE:
            return {
                "success": True,
                "message": "📅 You don't have any events yet! Why not create your first amazing event? Just say 'Create an event' and I'll help you plan something spectacular! 🎉",
                "data": None,
                "conversation_state": "initial"
            }
        
        # Format events for display
        events_text = self._format_events_list()
        
        return {
            "success": True,
            "message": f"📅 Here are all your amazing events:\n\n{events_text}\n\nWould you like to create a new event or add details to an existing one?",
            "data": {"events": EVENT_STORAGE},
            "conversation_state": "initial",
            "show_chips": True
        }
    
    def _format_events_list(self) -> str:
        """Format the list of events for display."""
        if not EVENT_STORAGE:
            return "No events found."
        
        events_text = ""
        for i, event in enumerate(EVENT_STORAGE, 1):
            events_text += f"**{i}. {event.get('name', 'Unnamed Event')}**\n"
            events_text += f"   • Type: {event.get('eventType', 'Not specified')}\n"
            events_text += f"   • Date: {event.get('startDateTime', 'Not specified')}\n"
            if event.get('description'):
                events_text += f"   • Description: {event['description']}\n"
            if event.get('capacity'):
                events_text += f"   • Capacity: {event['capacity']}\n"
            if event.get('venue'):
                events_text += f"   • Venue: {event['venue']}\n"
            events_text += f"   • Created: {event.get('created_at', 'Unknown')}\n\n"
        
        return events_text.strip()
    
    def reset(self):
        """Reset the event tool for a new event."""
        self.event_data = {}
        self.conversation_state = "initial"
