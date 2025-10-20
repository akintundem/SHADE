"""Time checking tool for accurate event planning."""

import os
from datetime import datetime, timedelta
from typing import Dict, Any
from dotenv import load_dotenv

load_dotenv()

class TimeTool:
    """Tool for checking current time and time-related calculations."""
    
    def __init__(self):
        self.name = "TimeTool"
    
    def can_handle(self, message: str) -> bool:
        """Check if this tool can handle time-related messages."""
        time_keywords = [
            "time", "date", "when", "now", "current", "today", "tomorrow", 
            "yesterday", "clock", "schedule", "timing", "duration", "hours",
            "minutes", "seconds", "am", "pm", "morning", "afternoon", "evening"
        ]
        message_lower = message.lower()
        return any(keyword in message_lower for keyword in time_keywords)
    
    def process(self, message: str) -> Dict[str, Any]:
        """Process time-related requests."""
        try:
            current_time = datetime.now()
            
            # Extract time-related information from message
            if any(word in message.lower() for word in ["current", "now", "what time", "time now"]):
                return self._get_current_time_info()
            elif any(word in message.lower() for word in ["today", "is today"]):
                return self._get_today_info()
            elif any(word in message.lower() for word in ["tomorrow", "is tomorrow"]):
                return self._get_tomorrow_info()
            elif any(word in message.lower() for word in ["duration", "how long", "length"]):
                return self._get_duration_help()
            else:
                return self._get_general_time_info()
                
        except Exception as e:
            return {
                "success": False,
                "message": f"Oops! I had trouble checking the time. Let me try again! 😊",
                "data": None
            }
    
    def _get_current_time_info(self) -> Dict[str, Any]:
        """Get current time information."""
        now = datetime.now()
        return {
            "success": True,
            "message": f"🕐 Right now it's {now.strftime('%A, %B %d, %Y at %I:%M %p')} - perfect timing to plan your event! ✨",
            "data": {
                "current_time": now.isoformat(),
                "formatted_time": now.strftime('%A, %B %d, %Y at %I:%M %p'),
                "day_of_week": now.strftime('%A'),
                "date": now.strftime('%B %d, %Y'),
                "time": now.strftime('%I:%M %p')
            }
        }
    
    def _get_today_info(self) -> Dict[str, Any]:
        """Get today's information."""
        today = datetime.now()
        return {
            "success": True,
            "message": f"📅 Today is {today.strftime('%A, %B %d, %Y')} - a great day for planning your event! What time were you thinking? 🎉",
            "data": {
                "date": today.strftime('%Y-%m-%d'),
                "formatted_date": today.strftime('%A, %B %d, %Y'),
                "day_of_week": today.strftime('%A'),
                "is_weekend": today.weekday() >= 5
            }
        }
    
    def _get_tomorrow_info(self) -> Dict[str, Any]:
        """Get tomorrow's information."""
        tomorrow = datetime.now() + timedelta(days=1)
        return {
            "success": True,
            "message": f"📅 Tomorrow is {tomorrow.strftime('%A, %B %d, %Y')} - perfect for your event! What time works best? ⏰",
            "data": {
                "date": tomorrow.strftime('%Y-%m-%d'),
                "formatted_date": tomorrow.strftime('%A, %B %d, %Y'),
                "day_of_week": tomorrow.strftime('%A'),
                "is_weekend": tomorrow.weekday() >= 5
            }
        }
    
    def _get_duration_help(self) -> Dict[str, Any]:
        """Get duration planning help."""
        return {
            "success": True,
            "message": "⏱️ Great question! Here are some typical event durations:\n\n• **Workshops**: 2-4 hours\n• **Conferences**: 6-8 hours (full day)\n• **Parties**: 3-5 hours\n• **Meetings**: 1-2 hours\n• **Seminars**: 2-3 hours\n\nWhat type of event are you planning? I can help you pick the perfect duration! 🎯",
            "data": None
        }
    
    def _get_general_time_info(self) -> Dict[str, Any]:
        """Get general time information."""
        now = datetime.now()
        return {
            "success": True,
            "message": f"🕐 Time check! It's currently {now.strftime('%A, %B %d, %Y at %I:%M %p')}. Perfect timing to plan your event! What would you like to know about timing? ✨",
            "data": {
                "current_time": now.isoformat(),
                "formatted_time": now.strftime('%A, %B %d, %Y at %I:%M %p')
            }
        }
    
    def get_current_time(self) -> datetime:
        """Get current time as datetime object."""
        return datetime.now()
    
    def format_time_for_event(self, dt: datetime) -> str:
        """Format datetime for event display."""
        return dt.strftime('%A, %B %d, %Y at %I:%M %p')
    
    def is_weekend(self, dt: datetime = None) -> bool:
        """Check if given date is weekend."""
        if dt is None:
            dt = datetime.now()
        return dt.weekday() >= 5
