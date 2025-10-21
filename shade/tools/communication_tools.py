"""Communication tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List


@tool
async def send_notification(
    event_id: str,
    notification_type: str,
    recipients: List[str],
    subject: str,
    message: str,
    priority: str = "normal"
) -> Dict[str, Any]:
    """Send notifications to event stakeholders.
    
    Args:
        event_id: ID of the event
        notification_type: Type of notification (email, sms, push)
        recipients: List of recipient email addresses or phone numbers
        subject: Subject line for the notification
        message: Message content
        priority: Priority level (low, normal, high, urgent)
    
    Returns:
        Dict with notification sending results
    """
    notification_id = f"notif_{event_id}_{notification_type}"
    
    return {
        "success": True,
        "notification_id": notification_id,
        "message": "Notification sent successfully",
        "notification": {
            "id": notification_id,
            "event_id": event_id,
            "type": notification_type,
            "recipients": recipients,
            "subject": subject,
            "message": message,
            "priority": priority,
            "sent_count": len(recipients),
            "failed_count": 0,
            "status": "SENT",
            "sent_at": "2024-01-15T14:30:00"
        }
    }


@tool
async def generate_report(
    event_id: str,
    report_type: str,
    include_sections: Optional[List[str]] = None
) -> Dict[str, Any]:
    """Generate a comprehensive event report.
    
    Args:
        event_id: ID of the event
        report_type: Type of report (summary, detailed, financial, timeline)
        include_sections: Optional list of sections to include
    
    Returns:
        Dict with report generation details
    """
    report_id = f"report_{event_id}_{report_type}"
    
    # Default sections if none specified
    if not include_sections:
        include_sections = ["overview", "budget", "timeline", "attendees", "vendors", "risks"]
    
    # Mock report data
    report_sections = {
        "overview": {
            "event_name": "Tech Conference 2024",
            "event_date": "2024-06-15",
            "location": "Convention Center",
            "capacity": 200,
            "actual_attendees": 185,
            "status": "COMPLETED"
        },
        "budget": {
            "total_budget": 50000.0,
            "total_spent": 47500.0,
            "remaining": 2500.0,
            "top_expenses": [
                {"category": "venue", "amount": 15000.0},
                {"category": "catering", "amount": 12000.0},
                {"category": "speakers", "amount": 8000.0}
            ]
        },
        "timeline": {
            "start_time": "09:00",
            "end_time": "17:00",
            "key_milestones": [
                {"time": "09:00", "activity": "Registration"},
                {"time": "10:00", "activity": "Opening keynote"},
                {"time": "17:00", "activity": "Closing remarks"}
            ]
        },
        "attendees": {
            "total_invited": 200,
            "registered": 185,
            "attended": 180,
            "attendance_rate": "97.3%"
        },
        "vendors": {
            "total_vendors": 5,
            "confirmed": 5,
            "cancelled": 0,
            "vendor_list": ["Catering Co.", "AV Services", "Security", "Photography", "Transportation"]
        },
        "risks": {
            "total_risks_identified": 3,
            "risks_mitigated": 3,
            "incidents": 0,
            "risk_summary": "All identified risks were successfully mitigated"
        }
    }
    
    # Filter sections based on include_sections
    filtered_sections = {k: v for k, v in report_sections.items() if k in include_sections}
    
    return {
        "success": True,
        "report_id": report_id,
        "message": "Report generated successfully",
        "report": {
            "id": report_id,
            "event_id": event_id,
            "type": report_type,
            "sections": filtered_sections,
            "generated_at": "2024-01-15T15:00:00",
            "file_url": f"https://reports.example.com/{report_id}.pdf"
        }
    }
