"""Event insights generator for proactive recommendations and analysis."""

from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from dataclasses import dataclass
from enum import Enum
import logging

logger = logging.getLogger(__name__)


class InsightType(Enum):
    """Types of insights."""
    TIMELINE = "timeline"
    BUDGET = "budget"
    VENUE = "venue"
    VENDOR = "vendor"
    RISK = "risk"
    OPPORTUNITY = "opportunity"
    BEST_PRACTICE = "best_practice"


class InsightSeverity(Enum):
    """Insight severity levels."""
    INFO = "info"
    WARNING = "warning"
    URGENT = "urgent"
    CRITICAL = "critical"


@dataclass
class EventInsight:
    """Represents an insight about an event."""
    insight_id: str
    insight_type: InsightType
    severity: InsightSeverity
    title: str
    description: str
    recommendation: str
    action_required: bool
    deadline: Optional[datetime] = None
    related_field: Optional[str] = None


class EventInsightsGenerator:
    """Generates proactive insights and recommendations for events."""
    
    def __init__(self):
        """Initialize the insights generator."""
        self.insight_templates = self._initialize_insight_templates()
    
    def _initialize_insight_templates(self) -> Dict[str, List[Dict[str, Any]]]:
        """Initialize insight templates by event type."""
        return {
            "WEDDING": [
                {
                    "type": InsightType.TIMELINE,
                    "severity": InsightSeverity.WARNING,
                    "condition": lambda event: self._days_until_event(event) < 90,
                    "title": "Short Wedding Planning Timeline",
                    "description": "Wedding planning typically requires 6-12 months",
                    "recommendation": "Consider postponing or simplifying the event",
                    "action_required": True
                },
                {
                    "type": InsightType.VENUE,
                    "severity": InsightSeverity.INFO,
                    "condition": lambda event: not event.get("venueRequirements"),
                    "title": "Venue Requirements Missing",
                    "description": "No venue requirements specified",
                    "recommendation": "Define venue needs to find suitable locations",
                    "action_required": False
                },
                {
                    "type": InsightType.BUDGET,
                    "severity": InsightSeverity.WARNING,
                    "condition": lambda event: not event.get("budget") and event.get("capacity", 0) > 100,
                    "title": "Large Wedding Without Budget",
                    "description": "Large weddings require careful budget planning",
                    "recommendation": "Set a budget to avoid overspending",
                    "action_required": True
                }
            ],
            "CONFERENCE": [
                {
                    "type": InsightType.TIMELINE,
                    "severity": InsightSeverity.WARNING,
                    "condition": lambda event: self._days_until_event(event) < 30,
                    "title": "Short Conference Planning Timeline",
                    "description": "Conferences typically need 3-6 months planning",
                    "recommendation": "Start planning immediately or consider postponing",
                    "action_required": True
                },
                {
                    "type": InsightType.VENUE,
                    "severity": InsightSeverity.INFO,
                    "condition": lambda event: not event.get("technicalRequirements"),
                    "title": "Technical Requirements Missing",
                    "description": "No technical requirements specified",
                    "recommendation": "Define AV, WiFi, and presentation needs",
                    "action_required": False
                },
                {
                    "type": InsightType.VENDOR,
                    "severity": InsightSeverity.INFO,
                    "condition": lambda event: not event.get("cateringRequirements"),
                    "title": "Catering Requirements Missing",
                    "description": "No catering requirements specified",
                    "recommendation": "Consider dietary restrictions and meal preferences",
                    "action_required": False
                }
            ],
            "BIRTHDAY": [
                {
                    "type": InsightType.TIMELINE,
                    "severity": InsightSeverity.INFO,
                    "condition": lambda event: self._days_until_event(event) < 7,
                    "title": "Short Birthday Planning Timeline",
                    "description": "Birthday parties can be planned quickly",
                    "recommendation": "Focus on entertainment and venue booking",
                    "action_required": False
                },
                {
                    "type": InsightType.VENUE,
                    "severity": InsightSeverity.INFO,
                    "condition": lambda event: not event.get("theme"),
                    "title": "Theme Not Specified",
                    "description": "No theme specified for birthday party",
                    "recommendation": "Choose a theme to guide decorations and activities",
                    "action_required": False
                }
            ]
        }
    
    def _days_until_event(self, event: Dict[str, Any]) -> int:
        """Calculate days until event."""
        start_datetime = event.get("startDateTime")
        if not start_datetime:
            return 0
        
        try:
            start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
            return (start_dt - datetime.utcnow()).days
        except ValueError:
            return 0
    
    async def generate_insights(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Generate insights for an event."""
        insights = []
        event_type = event.get("eventType", "UNKNOWN")
        
        # Get templates for this event type
        templates = self.insight_templates.get(event_type, [])
        
        for template in templates:
            try:
                # Check if condition is met
                if template["condition"](event):
                    insight = EventInsight(
                        insight_id=f"{event_type.lower()}_{template['type'].value}_{datetime.utcnow().timestamp()}",
                        insight_type=template["type"],
                        severity=template["severity"],
                        title=template["title"],
                        description=template["description"],
                        recommendation=template["recommendation"],
                        action_required=template["action_required"],
                        related_field=self._get_related_field(template["type"])
                    )
                    insights.append(insight)
            except Exception as e:
                logger.error(f"Error generating insight: {e}")
        
        # Add timeline-specific insights
        insights.extend(await self._generate_timeline_insights(event))
        
        # Add budget-specific insights
        insights.extend(await self._generate_budget_insights(event))
        
        # Add venue-specific insights
        insights.extend(await self._generate_venue_insights(event))
        
        # Add risk insights
        insights.extend(await self._generate_risk_insights(event))
        
        return insights
    
    async def _generate_timeline_insights(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Generate timeline-specific insights."""
        insights = []
        days_until = self._days_until_event(event)
        
        if days_until < 0:
            insights.append(EventInsight(
                insight_id=f"timeline_past_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.TIMELINE,
                severity=InsightSeverity.CRITICAL,
                title="Event Date in the Past",
                description="The event date has already passed",
                recommendation="Update the event date to a future date",
                action_required=True,
                related_field="startDateTime"
            ))
        elif days_until < 7:
            insights.append(EventInsight(
                insight_id=f"timeline_urgent_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.TIMELINE,
                severity=InsightSeverity.URGENT,
                title="Event Starting Soon",
                description=f"Event starts in {days_until} days",
                recommendation="Finalize all arrangements immediately",
                action_required=True,
                deadline=datetime.utcnow() + timedelta(days=days_until)
            ))
        elif days_until < 30:
            insights.append(EventInsight(
                insight_id=f"timeline_warning_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.TIMELINE,
                severity=InsightSeverity.WARNING,
                title="Event Approaching",
                description=f"Event starts in {days_until} days",
                recommendation="Complete remaining planning tasks",
                action_required=False,
                deadline=datetime.utcnow() + timedelta(days=days_until)
            ))
        
        return insights
    
    async def _generate_budget_insights(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Generate budget-specific insights."""
        insights = []
        capacity = event.get("capacity", 0)
        event_type = event.get("eventType", "UNKNOWN")
        
        if capacity > 0 and not event.get("budget"):
            # Estimate budget based on event type and capacity
            estimated_budget = self._estimate_budget(event_type, capacity)
            
            insights.append(EventInsight(
                insight_id=f"budget_estimate_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.BUDGET,
                severity=InsightSeverity.INFO,
                title="Budget Not Set",
                description=f"Estimated budget for {capacity} guests: ${estimated_budget:,}",
                recommendation="Set a realistic budget to guide planning decisions",
                action_required=False,
                related_field="budget"
            ))
        
        return insights
    
    def _estimate_budget(self, event_type: str, capacity: int) -> int:
        """Estimate budget based on event type and capacity."""
        # Base cost per person by event type
        cost_per_person = {
            "WEDDING": 150,
            "CONFERENCE": 75,
            "BIRTHDAY": 50,
            "CORPORATE": 100,
            "PARTY": 40
        }
        
        base_cost = cost_per_person.get(event_type, 60)
        return int(capacity * base_cost)
    
    async def _generate_venue_insights(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Generate venue-specific insights."""
        insights = []
        
        if not event.get("venueRequirements"):
            insights.append(EventInsight(
                insight_id=f"venue_requirements_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.VENUE,
                severity=InsightSeverity.INFO,
                title="Venue Requirements Missing",
                description="No venue requirements specified",
                recommendation="Define venue needs: capacity, location, amenities",
                action_required=False,
                related_field="venueRequirements"
            ))
        
        # Check for outdoor events in winter
        start_datetime = event.get("startDateTime")
        venue_requirements = event.get("venueRequirements", "")
        
        if start_datetime and "outdoor" in venue_requirements.lower():
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                if start_dt.month in [12, 1, 2]:  # Winter months
                    insights.append(EventInsight(
                        insight_id=f"venue_winter_{datetime.utcnow().timestamp()}",
                        insight_type=InsightType.VENUE,
                        severity=InsightSeverity.WARNING,
                        title="Outdoor Event in Winter",
                        description="Outdoor venue in winter season",
                        recommendation="Consider indoor backup or heating options",
                        action_required=False,
                        related_field="venueRequirements"
                    ))
            except ValueError:
                pass
        
        return insights
    
    async def _generate_risk_insights(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Generate risk-specific insights."""
        insights = []
        capacity = event.get("capacity", 0)
        event_type = event.get("eventType", "UNKNOWN")
        
        # Large capacity risk
        if capacity > 500:
            insights.append(EventInsight(
                insight_id=f"risk_capacity_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.RISK,
                severity=InsightSeverity.WARNING,
                title="Large Capacity Event",
                description=f"Event capacity is {capacity} - requires additional planning",
                recommendation="Consider security, logistics, and emergency planning",
                action_required=True,
                related_field="capacity"
            ))
        
        # Short timeline risk
        days_until = self._days_until_event(event)
        if days_until < 14 and event_type in ["WEDDING", "CONFERENCE"]:
            insights.append(EventInsight(
                insight_id=f"risk_timeline_{datetime.utcnow().timestamp()}",
                insight_type=InsightType.RISK,
                severity=InsightSeverity.URGENT,
                title="Short Planning Timeline",
                description=f"Only {days_until} days to plan {event_type}",
                recommendation="Consider postponing or significantly simplifying",
                action_required=True,
                related_field="startDateTime"
            ))
        
        return insights
    
    def _get_related_field(self, insight_type: InsightType) -> Optional[str]:
        """Get the related field for an insight type."""
        field_mapping = {
            InsightType.TIMELINE: "startDateTime",
            InsightType.BUDGET: "budget",
            InsightType.VENUE: "venueRequirements",
            InsightType.VENDOR: "vendorRequirements",
            InsightType.RISK: "riskAssessment"
        }
        return field_mapping.get(insight_type)
    
    async def get_next_steps(self, event: Dict[str, Any]) -> List[str]:
        """Get recommended next steps for an event."""
        next_steps = []
        event_type = event.get("eventType", "UNKNOWN")
        
        # Check what's missing
        if not event.get("capacity"):
            next_steps.append("Set guest capacity")
        
        if not event.get("venueRequirements"):
            next_steps.append("Define venue requirements")
        
        if not event.get("budget") and event_type in ["WEDDING", "CONFERENCE"]:
            next_steps.append("Set budget")
        
        if not event.get("theme") and event_type == "BIRTHDAY":
            next_steps.append("Choose a theme")
        
        if not event.get("technicalRequirements") and event_type == "CONFERENCE":
            next_steps.append("Define technical requirements")
        
        # Add event-type specific steps
        if event_type == "WEDDING":
            next_steps.extend(["Find venue", "Book photographer", "Plan catering"])
        elif event_type == "CONFERENCE":
            next_steps.extend(["Find venue", "Setup AV", "Plan agenda"])
        elif event_type == "BIRTHDAY":
            next_steps.extend(["Choose entertainment", "Plan activities", "Send invitations"])
        
        return next_steps[:5]  # Return top 5 steps
    
    async def get_opportunities(self, event: Dict[str, Any]) -> List[EventInsight]:
        """Get opportunities for improving the event."""
        opportunities = []
        event_type = event.get("eventType", "UNKNOWN")
        
        # Add seasonal opportunities
        start_datetime = event.get("startDateTime")
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                month = start_dt.month
                
                if month in [6, 7, 8]:  # Summer
                    opportunities.append(EventInsight(
                        insight_id=f"opportunity_summer_{datetime.utcnow().timestamp()}",
                        insight_type=InsightType.OPPORTUNITY,
                        severity=InsightSeverity.INFO,
                        title="Summer Event Opportunity",
                        description="Summer events can include outdoor activities",
                        recommendation="Consider outdoor venues and activities",
                        action_required=False
                    ))
                elif month in [12, 1, 2]:  # Winter
                    opportunities.append(EventInsight(
                        insight_id=f"opportunity_winter_{datetime.utcnow().timestamp()}",
                        insight_type=InsightType.OPPORTUNITY,
                        severity=InsightSeverity.INFO,
                        title="Winter Event Opportunity",
                        description="Winter events can have cozy, intimate atmosphere",
                        recommendation="Consider indoor venues with warm ambiance",
                        action_required=False
                    ))
            except ValueError:
                pass
        
        return opportunities
    
    async def get_insights_summary(self, event: Dict[str, Any]) -> Dict[str, Any]:
        """Get a summary of all insights for an event."""
        insights = await self.generate_insights(event)
        opportunities = await self.get_opportunities(event)
        next_steps = await self.get_next_steps(event)
        
        # Categorize insights by severity
        critical_insights = [i for i in insights if i.severity == InsightSeverity.CRITICAL]
        urgent_insights = [i for i in insights if i.severity == InsightSeverity.URGENT]
        warning_insights = [i for i in insights if i.severity == InsightSeverity.WARNING]
        info_insights = [i for i in insights if i.severity == InsightSeverity.INFO]
        
        return {
            "total_insights": len(insights),
            "critical_count": len(critical_insights),
            "urgent_count": len(urgent_insights),
            "warning_count": len(warning_insights),
            "info_count": len(info_insights),
            "opportunities_count": len(opportunities),
            "next_steps": next_steps,
            "critical_insights": [self._insight_to_dict(i) for i in critical_insights],
            "urgent_insights": [self._insight_to_dict(i) for i in urgent_insights],
            "opportunities": [self._insight_to_dict(i) for i in opportunities]
        }
    
    def _insight_to_dict(self, insight: EventInsight) -> Dict[str, Any]:
        """Convert insight to dictionary."""
        return {
            "id": insight.insight_id,
            "type": insight.insight_type.value,
            "severity": insight.severity.value,
            "title": insight.title,
            "description": insight.description,
            "recommendation": insight.recommendation,
            "action_required": insight.action_required,
            "deadline": insight.deadline.isoformat() if insight.deadline else None,
            "related_field": insight.related_field
        }
