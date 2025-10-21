"""Mock Google APIs for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
from datetime import datetime, timedelta
import json

logger = logging.getLogger(__name__)


class GoogleAPIService:
    """Mock Google APIs for prototyping - returns consistent data."""
    
    def __init__(self):
        """Initialize mock Google APIs service."""
        self.initialized = False
        self.mock_data = self._initialize_mock_data()
    
    def _initialize_mock_data(self) -> Dict[str, Any]:
        """Initialize with mock data for consistent responses."""
        return {
            "gmail": {
                "sent_emails": [],
                "drafts": [],
                "templates": [
                    {
                        "id": "template_1",
                        "name": "Event Invitation",
                        "subject": "You're Invited to {event_name}",
                        "body": "Dear {guest_name},\n\nYou're cordially invited to {event_name} on {event_date} at {venue_name}.\n\nPlease RSVP by {rsvp_date}.\n\nBest regards,\n{organizer_name}"
                    },
                    {
                        "id": "template_2", 
                        "name": "Event Reminder",
                        "subject": "Reminder: {event_name} Tomorrow",
                        "body": "Dear {guest_name},\n\nThis is a friendly reminder that {event_name} is tomorrow at {event_time}.\n\nLocation: {venue_address}\n\nWe look forward to seeing you!\n\nBest regards,\n{organizer_name}"
                    }
                ]
            },
            "calendar": {
                "events": [
                    {
                        "id": "event_1",
                        "title": "Wedding Planning Meeting",
                        "start": "2024-02-15T10:00:00Z",
                        "end": "2024-02-15T11:00:00Z",
                        "location": "Conference Room A",
                        "attendees": ["john@example.com", "jane@example.com"]
                    },
                    {
                        "id": "event_2",
                        "title": "Venue Site Visit",
                        "start": "2024-02-20T14:00:00Z", 
                        "end": "2024-02-20T16:00:00Z",
                        "location": "Grand Hotel Ballroom",
                        "attendees": ["planner@example.com"]
                    }
                ],
                "availability": {
                    "free_times": [
                        {"start": "2024-02-16T09:00:00Z", "end": "2024-02-16T17:00:00Z"},
                        {"start": "2024-02-17T09:00:00Z", "end": "2024-02-17T17:00:00Z"}
                    ]
                }
            },
            "drive": {
                "files": [
                    {
                        "id": "file_1",
                        "name": "Wedding Budget.xlsx",
                        "type": "spreadsheet",
                        "size": "45KB",
                        "created": "2024-01-15T10:30:00Z"
                    },
                    {
                        "id": "file_2",
                        "name": "Guest List.csv",
                        "type": "csv",
                        "size": "12KB", 
                        "created": "2024-01-20T14:15:00Z"
                    }
                ]
            }
        }
    
    async def initialize(self):
        """Initialize the Google APIs service."""
        try:
            self.initialized = True
            logger.info("Mock Google APIs service initialized")
        except Exception as e:
            logger.error(f"Error initializing Google APIs: {e}")
            raise
    
    # Gmail API Methods
    async def send_email(self, to: str, subject: str, body: str, template_id: Optional[str] = None) -> Dict[str, Any]:
        """Send an email via Gmail API."""
        try:
            email_data = {
                "id": f"email_{len(self.mock_data['gmail']['sent_emails']) + 1}",
                "to": to,
                "subject": subject,
                "body": body,
                "template_id": template_id,
                "sent_at": datetime.utcnow().isoformat(),
                "status": "sent"
            }
            
            self.mock_data["gmail"]["sent_emails"].append(email_data)
            
            logger.info(f"Mock email sent to {to}: {subject}")
            return {
                "success": True,
                "message_id": email_data["id"],
                "status": "sent"
            }
            
        except Exception as e:
            logger.error(f"Error sending email: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_email_templates(self) -> List[Dict[str, Any]]:
        """Get available email templates."""
        try:
            return self.mock_data["gmail"]["templates"]
        except Exception as e:
            logger.error(f"Error getting email templates: {e}")
            return []
    
    async def create_draft(self, to: str, subject: str, body: str) -> Dict[str, Any]:
        """Create an email draft."""
        try:
            draft_data = {
                "id": f"draft_{len(self.mock_data['gmail']['drafts']) + 1}",
                "to": to,
                "subject": subject,
                "body": body,
                "created_at": datetime.utcnow().isoformat()
            }
            
            self.mock_data["gmail"]["drafts"].append(draft_data)
            
            logger.info(f"Mock draft created for {to}: {subject}")
            return {
                "success": True,
                "draft_id": draft_data["id"]
            }
            
        except Exception as e:
            logger.error(f"Error creating draft: {e}")
            return {"success": False, "error": str(e)}
    
    # Calendar API Methods
    async def create_event(self, title: str, start_time: str, end_time: str, location: str = None, attendees: List[str] = None) -> Dict[str, Any]:
        """Create a calendar event."""
        try:
            event_data = {
                "id": f"event_{len(self.mock_data['calendar']['events']) + 1}",
                "title": title,
                "start": start_time,
                "end": end_time,
                "location": location or "TBD",
                "attendees": attendees or [],
                "created_at": datetime.utcnow().isoformat()
            }
            
            self.mock_data["calendar"]["events"].append(event_data)
            
            logger.info(f"Mock calendar event created: {title}")
            return {
                "success": True,
                "event_id": event_data["id"],
                "event": event_data
            }
            
        except Exception as e:
            logger.error(f"Error creating calendar event: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_events(self, start_date: str = None, end_date: str = None) -> List[Dict[str, Any]]:
        """Get calendar events."""
        try:
            events = self.mock_data["calendar"]["events"]
            
            # Filter by date range if provided
            if start_date and end_date:
                filtered_events = []
                for event in events:
                    event_start = event["start"]
                    if start_date <= event_start <= end_date:
                        filtered_events.append(event)
                return filtered_events
            
            return events
            
        except Exception as e:
            logger.error(f"Error getting calendar events: {e}")
            return []
    
    async def check_availability(self, start_time: str, end_time: str) -> Dict[str, Any]:
        """Check availability for a time slot."""
        try:
            # Mock availability check - always return some free times
            free_times = self.mock_data["calendar"]["availability"]["free_times"]
            
            # Simple overlap check for prototyping
            is_available = True
            for event in self.mock_data["calendar"]["events"]:
                if (start_time < event["end"] and end_time > event["start"]):
                    is_available = False
                    break
            
            return {
                "available": is_available,
                "free_times": free_times,
                "conflicts": [] if is_available else ["Existing event conflict"]
            }
            
        except Exception as e:
            logger.error(f"Error checking availability: {e}")
            return {"available": False, "error": str(e)}
    
    # Drive API Methods
    async def create_document(self, name: str, content: str, mime_type: str = "text/plain") -> Dict[str, Any]:
        """Create a document in Google Drive."""
        try:
            document_data = {
                "id": f"file_{len(self.mock_data['drive']['files']) + 1}",
                "name": name,
                "type": mime_type.split("/")[-1],
                "size": f"{len(content)}B",
                "content": content,
                "created": datetime.utcnow().isoformat()
            }
            
            self.mock_data["drive"]["files"].append(document_data)
            
            logger.info(f"Mock document created: {name}")
            return {
                "success": True,
                "file_id": document_data["id"],
                "file": document_data
            }
            
        except Exception as e:
            logger.error(f"Error creating document: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_files(self, query: str = None) -> List[Dict[str, Any]]:
        """Get files from Google Drive."""
        try:
            files = self.mock_data["drive"]["files"]
            
            # Filter by query if provided
            if query:
                filtered_files = []
                for file in files:
                    if query.lower() in file["name"].lower():
                        filtered_files.append(file)
                return filtered_files
            
            return files
            
        except Exception as e:
            logger.error(f"Error getting files: {e}")
            return []
    
    async def share_file(self, file_id: str, email: str, role: str = "reader") -> Dict[str, Any]:
        """Share a file with someone."""
        try:
            # Find the file
            file_found = False
            for file in self.mock_data["drive"]["files"]:
                if file["id"] == file_id:
                    file_found = True
                    break
            
            if not file_found:
                return {"success": False, "error": "File not found"}
            
            logger.info(f"Mock file {file_id} shared with {email} as {role}")
            return {
                "success": True,
                "shared_with": email,
                "role": role
            }
            
        except Exception as e:
            logger.error(f"Error sharing file: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_service_stats(self) -> Dict[str, Any]:
        """Get Google APIs service statistics."""
        try:
            return {
                "gmail": {
                    "sent_emails": len(self.mock_data["gmail"]["sent_emails"]),
                    "drafts": len(self.mock_data["gmail"]["drafts"]),
                    "templates": len(self.mock_data["gmail"]["templates"])
                },
                "calendar": {
                    "events": len(self.mock_data["calendar"]["events"]),
                    "free_times": len(self.mock_data["calendar"]["availability"]["free_times"])
                },
                "drive": {
                    "files": len(self.mock_data["drive"]["files"])
                },
                "initialized": self.initialized
            }
            
        except Exception as e:
            logger.error(f"Error getting service stats: {e}")
            return {"error": str(e)}
