"""Conversational event creation tool that follows CreateEventRequest DTO."""

import json
import os
from typing import Optional, Dict, Any, List
from datetime import datetime
from dotenv import load_dotenv

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
        """Check if this tool can handle the message."""
        # More specific keywords to avoid conflicts with weather tool
        keywords = ["create event", "create a", "planning an event", "organize event", "event planning"]
        general_keywords = ["conference", "meeting", "workshop", "party", "launch"]
        
        message_lower = message.lower()
        
        # Check for specific event creation phrases first
        if any(keyword in message_lower for keyword in keywords):
            return True
            
        # Check for general event types, but only if not weather-related
        weather_keywords = ["weather", "temperature", "rain", "sunny", "forecast", "outdoor", "climate", "conditions"]
        if any(weather_keyword in message_lower for weather_keyword in weather_keywords):
            return False
            
        return any(keyword in message_lower for keyword in general_keywords)
    
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
            "name": "🎉 Ooh, I'm so excited! What should we call this amazing event?",
            "eventType": "What kind of fabulous event are we planning? Is it a conference, party, workshop, meeting, or something else?",
            "startDateTime": "Perfect! When should this spectacular event take place? You can tell me like 'next Friday at 2 PM' or 'December 25th at 6 PM' - I'm flexible! 😊"
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
        """Process the conversational event creation."""
        try:
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
                
                self.conversation_state = "complete"
                
                # Execute the createEvent tool (prints to terminal only)
                self.createEvent(self.event_data)
                
                # Return a personable completion message (no technical details)
                event_name = self.event_data.get('name', 'your event')
                return {
                    "success": True,
                    "message": f"🎉✨ Amazing! I've just created '{event_name}' for you! Your event is all set up and ready to go. I'm so excited to help make this happen! 🎊",
                    "data": self.event_data,
                    "conversation_state": self.conversation_state
                }
            
            if message.lower() in ["skip", "next"]:
                return {
                    "success": True,
                    "message": self._get_next_question(),
                    "data": self.event_data,
                    "conversation_state": self.conversation_state
                }
            
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
            
            # Update conversation state
            missing_required = self._check_required_fields()
            if missing_required:
                self.conversation_state = "collecting_required"
            else:
                self.conversation_state = "collecting_optional"
            
            # Get next question
            next_question = self._get_next_question()
            
            return {
                "success": True,
                "message": next_question,
                "data": self.event_data,
                "conversation_state": self.conversation_state
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
    
    def reset(self):
        """Reset the event tool for a new event."""
        self.event_data = {}
        self.conversation_state = "initial"
