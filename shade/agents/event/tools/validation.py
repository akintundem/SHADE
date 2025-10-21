"""Comprehensive event validation with predictive warnings and best practices."""

from typing import Dict, Any, List, Optional, Tuple
from datetime import datetime, timedelta
from dataclasses import dataclass
from enum import Enum
import logging

logger = logging.getLogger(__name__)


class ValidationSeverity(Enum):
    """Validation severity levels."""
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"
    CRITICAL = "critical"


@dataclass
class ValidationResult:
    """Result of a validation check."""
    is_valid: bool
    severity: ValidationSeverity
    message: str
    suggestion: Optional[str] = None
    field: Optional[str] = None


@dataclass
class ValidationReport:
    """Complete validation report."""
    is_valid: bool
    results: List[ValidationResult]
    warnings: List[str]
    suggestions: List[str]
    critical_issues: List[str]


class EventValidator:
    """Comprehensive event validation with predictive warnings."""
    
    def __init__(self):
        """Initialize the event validator."""
        self.best_practices = self._initialize_best_practices()
        self.seasonal_considerations = self._initialize_seasonal_considerations()
    
    def _initialize_best_practices(self) -> Dict[str, List[str]]:
        """Initialize best practices by event type."""
        return {
            "WEDDING": [
                "Book venue 12-18 months in advance",
                "Hire photographer 6-12 months ahead",
                "Send save-the-dates 6-8 months before",
                "Plan for 10-20% budget contingency",
                "Consider weather backup for outdoor ceremonies"
            ],
            "CONFERENCE": [
                "Book venue 6-12 months in advance",
                "Test all technical equipment beforehand",
                "Plan for dietary restrictions and allergies",
                "Have backup internet and power options",
                "Consider accessibility requirements"
            ],
            "BIRTHDAY": [
                "Book entertainment 2-4 weeks ahead",
                "Consider age-appropriate activities",
                "Plan for dietary restrictions",
                "Have backup indoor options",
                "Send invitations 2-3 weeks before"
            ],
            "CORPORATE": [
                "Book venue 3-6 months in advance",
                "Test all AV equipment",
                "Plan for security if needed",
                "Consider parking and transportation",
                "Have backup technical support"
            ]
        }
    
    def _initialize_seasonal_considerations(self) -> Dict[str, Dict[str, str]]:
        """Initialize seasonal considerations."""
        return {
            "SPRING": {
                "outdoor_events": "Good weather, but plan for rain",
                "indoor_events": "Consider allergies and pollen",
                "recommendations": "Great for outdoor ceremonies, plan rain backup"
            },
            "SUMMER": {
                "outdoor_events": "Hot weather, need shade and cooling",
                "indoor_events": "Air conditioning essential",
                "recommendations": "Popular season, book early, consider heat"
            },
            "FALL": {
                "outdoor_events": "Pleasant weather, beautiful scenery",
                "indoor_events": "Comfortable temperatures",
                "recommendations": "Peak season for weddings, book 12+ months ahead"
            },
            "WINTER": {
                "outdoor_events": "Cold weather, need heating",
                "indoor_events": "Cozy atmosphere, holiday themes",
                "recommendations": "Indoor events preferred, consider holiday conflicts"
            }
        }
    
    async def validate_event_creation(self, event_data: Dict[str, Any]) -> ValidationReport:
        """Validate event creation data."""
        results = []
        
        # Basic field validation
        results.extend(await self._validate_required_fields(event_data))
        
        # Date/time validation
        results.extend(await self._validate_datetime_consistency(event_data))
        
        # Capacity validation
        results.extend(await self._validate_capacity_requirements(event_data))
        
        # Timeline validation
        results.extend(await self._validate_planning_timeline(event_data))
        
        # Seasonal validation
        results.extend(await self._validate_seasonal_considerations(event_data))
        
        # Event type specific validation
        event_type = event_data.get("eventType", "UNKNOWN")
        results.extend(await self._validate_event_type_specific(event_data, event_type))
        
        return self._compile_validation_report(results)
    
    async def validate_event_update(self, event_data: Dict[str, Any], original_data: Dict[str, Any]) -> ValidationReport:
        """Validate event update data."""
        results = []
        
        # Check for breaking changes
        results.extend(await self._validate_update_compatibility(event_data, original_data))
        
        # Validate new data
        results.extend(await self._validate_required_fields(event_data))
        results.extend(await self._validate_datetime_consistency(event_data))
        results.extend(await self._validate_capacity_requirements(event_data))
        
        return self._compile_validation_report(results)
    
    async def _validate_required_fields(self, event_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate required fields are present."""
        results = []
        required_fields = ["name", "eventType", "startDateTime"]
        
        for field in required_fields:
            if not event_data.get(field):
                results.append(ValidationResult(
                    is_valid=False,
                    severity=ValidationSeverity.ERROR,
                    message=f"Required field '{field}' is missing",
                    suggestion=f"Please provide a value for {field}",
                    field=field
                ))
        
        return results
    
    async def _validate_datetime_consistency(self, event_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate date/time consistency."""
        results = []
        
        start_datetime = event_data.get("startDateTime")
        end_datetime = event_data.get("endDateTime")
        
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                
                # Check if start time is in the past
                if start_dt < datetime.utcnow():
                    results.append(ValidationResult(
                        is_valid=False,
                        severity=ValidationSeverity.WARNING,
                        message="Event start time is in the past",
                        suggestion="Please set a future date and time",
                        field="startDateTime"
                    ))
                
                # Check if end time is after start time
                if end_datetime:
                    try:
                        end_dt = datetime.fromisoformat(end_datetime.replace('Z', '+00:00'))
                        if end_dt <= start_dt:
                            results.append(ValidationResult(
                                is_valid=False,
                                severity=ValidationSeverity.ERROR,
                                message="End time must be after start time",
                                suggestion="Please set an end time that is after the start time",
                                field="endDateTime"
                            ))
                        
                        # Check for reasonable duration
                        duration = end_dt - start_dt
                        if duration.days > 7:
                            results.append(ValidationResult(
                                is_valid=False,
                                severity=ValidationSeverity.WARNING,
                                message="Event duration is very long (over 7 days)",
                                suggestion="Consider breaking this into multiple events",
                                field="endDateTime"
                            ))
                        elif duration.total_seconds() < 3600:  # Less than 1 hour
                            results.append(ValidationResult(
                                is_valid=False,
                                severity=ValidationSeverity.WARNING,
                                message="Event duration is very short (under 1 hour)",
                                suggestion="Consider if this is enough time for your event",
                                field="endDateTime"
                            ))
                    
                    except ValueError:
                        results.append(ValidationResult(
                            is_valid=False,
                            severity=ValidationSeverity.ERROR,
                            message="Invalid end date/time format",
                            suggestion="Please use ISO format (YYYY-MM-DDTHH:MM:SS)",
                            field="endDateTime"
                        ))
                
            except ValueError:
                results.append(ValidationResult(
                    is_valid=False,
                    severity=ValidationSeverity.ERROR,
                    message="Invalid start date/time format",
                    suggestion="Please use ISO format (YYYY-MM-DDTHH:MM:SS)",
                    field="startDateTime"
                ))
        
        return results
    
    async def _validate_capacity_requirements(self, event_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate capacity requirements."""
        results = []
        
        capacity = event_data.get("capacity")
        venue_requirements = event_data.get("venueRequirements", "")
        
        if capacity:
            # Check for reasonable capacity ranges
            if capacity < 1:
                results.append(ValidationResult(
                    is_valid=False,
                    severity=ValidationSeverity.ERROR,
                    message="Capacity must be at least 1",
                    suggestion="Please set a valid capacity",
                    field="capacity"
                ))
            elif capacity > 10000:
                results.append(ValidationResult(
                    is_valid=False,
                    severity=ValidationSeverity.WARNING,
                    message="Very large capacity (over 10,000)",
                    suggestion="Consider if this is manageable and check venue capacity",
                    field="capacity"
                ))
            
            # Check venue requirements alignment
            if venue_requirements and "capacity" in venue_requirements.lower():
                # Extract capacity from venue requirements if possible
                # This is a simplified check - in reality, you'd parse the requirements
                if capacity > 500 and "small" in venue_requirements.lower():
                    results.append(ValidationResult(
                        is_valid=False,
                        severity=ValidationSeverity.WARNING,
                        message="Capacity may not match venue requirements",
                        suggestion="Verify venue can accommodate your capacity",
                        field="capacity"
                    ))
        
        return results
    
    async def _validate_planning_timeline(self, event_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate planning timeline adequacy."""
        results = []
        
        start_datetime = event_data.get("startDateTime")
        event_type = event_data.get("eventType", "UNKNOWN")
        
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                time_until_event = start_dt - datetime.utcnow()
                
                # Minimum planning times by event type
                min_planning_times = {
                    "WEDDING": timedelta(days=90),
                    "CONFERENCE": timedelta(days=30),
                    "CORPORATE": timedelta(days=14),
                    "BIRTHDAY": timedelta(days=7),
                    "PARTY": timedelta(days=7)
                }
                
                min_time = min_planning_times.get(event_type, timedelta(days=7))
                
                if time_until_event < min_time:
                    results.append(ValidationResult(
                        is_valid=False,
                        severity=ValidationSeverity.WARNING,
                        message=f"Limited planning time for {event_type}",
                        suggestion=f"Consider postponing or simplifying the event. {event_type} events typically need {min_time.days} days planning",
                        field="startDateTime"
                    ))
                elif time_until_event < min_time * 2:
                    results.append(ValidationResult(
                        is_valid=True,
                        severity=ValidationSeverity.WARNING,
                        message=f"Short planning timeline for {event_type}",
                        suggestion="Start planning immediately and consider backup options",
                        field="startDateTime"
                    ))
                
            except ValueError:
                pass  # Already handled in datetime validation
        
        return results
    
    async def _validate_seasonal_considerations(self, event_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate seasonal considerations."""
        results = []
        
        start_datetime = event_data.get("startDateTime")
        venue_requirements = event_data.get("venueRequirements", "")
        
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                month = start_dt.month
                
                # Determine season
                if month in [12, 1, 2]:
                    season = "WINTER"
                elif month in [3, 4, 5]:
                    season = "SPRING"
                elif month in [6, 7, 8]:
                    season = "SUMMER"
                else:
                    season = "FALL"
                
                # Check for outdoor events in winter
                if season == "WINTER" and ("outdoor" in venue_requirements.lower() or "garden" in venue_requirements.lower()):
                    results.append(ValidationResult(
                        is_valid=True,
                        severity=ValidationSeverity.WARNING,
                        message="Outdoor event in winter season",
                        suggestion="Consider indoor backup or heating options",
                        field="venueRequirements"
                    ))
                
                # Check for summer outdoor events
                if season == "SUMMER" and "outdoor" in venue_requirements.lower():
                    results.append(ValidationResult(
                        is_valid=True,
                        severity=ValidationSeverity.INFO,
                        message="Outdoor event in summer",
                        suggestion="Ensure shade and cooling options are available",
                        field="venueRequirements"
                    ))
                
            except ValueError:
                pass
        
        return results
    
    async def _validate_event_type_specific(self, event_data: Dict[str, Any], event_type: str) -> List[ValidationResult]:
        """Validate event type specific requirements."""
        results = []
        
        if event_type == "WEDDING":
            # Wedding specific validations
            capacity = event_data.get("capacity")
            if capacity and capacity > 200:
                results.append(ValidationResult(
                    is_valid=True,
                    severity=ValidationSeverity.INFO,
                    message="Large wedding capacity",
                    suggestion="Consider venue size, catering capacity, and guest management",
                    field="capacity"
                ))
        
        elif event_type == "CONFERENCE":
            # Conference specific validations
            technical_requirements = event_data.get("technicalRequirements", "")
            if not technical_requirements:
                results.append(ValidationResult(
                    is_valid=True,
                    severity=ValidationSeverity.WARNING,
                    message="No technical requirements specified for conference",
                    suggestion="Consider AV equipment, WiFi, and presentation needs",
                    field="technicalRequirements"
                ))
        
        elif event_type == "BIRTHDAY":
            # Birthday specific validations
            capacity = event_data.get("capacity")
            if capacity and capacity > 50:
                results.append(ValidationResult(
                    is_valid=True,
                    severity=ValidationSeverity.INFO,
                    message="Large birthday party",
                    suggestion="Consider entertainment, activities, and supervision needs",
                    field="capacity"
                ))
        
        return results
    
    async def _validate_update_compatibility(self, new_data: Dict[str, Any], original_data: Dict[str, Any]) -> List[ValidationResult]:
        """Validate that updates don't break existing plans."""
        results = []
        
        # Check for date changes
        if new_data.get("startDateTime") != original_data.get("startDateTime"):
            results.append(ValidationResult(
                is_valid=True,
                severity=ValidationSeverity.WARNING,
                message="Event date changed",
                suggestion="Verify all bookings and notify attendees",
                field="startDateTime"
            ))
        
        # Check for capacity changes
        if new_data.get("capacity") != original_data.get("capacity"):
            results.append(ValidationResult(
                is_valid=True,
                severity=ValidationSeverity.WARNING,
                message="Event capacity changed",
                suggestion="Update venue and catering arrangements if needed",
                field="capacity"
            ))
        
        return results
    
    def _compile_validation_report(self, results: List[ValidationResult]) -> ValidationReport:
        """Compile validation results into a report."""
        is_valid = all(result.is_valid for result in results)
        
        warnings = [r.message for r in results if r.severity == ValidationSeverity.WARNING]
        suggestions = [r.suggestion for r in results if r.suggestion]
        critical_issues = [r.message for r in results if r.severity in [ValidationSeverity.ERROR, ValidationSeverity.CRITICAL]]
        
        return ValidationReport(
            is_valid=is_valid,
            results=results,
            warnings=warnings,
            suggestions=suggestions,
            critical_issues=critical_issues
        )
    
    async def predict_issues(self, event_data: Dict[str, Any]) -> List[str]:
        """Predict potential issues based on event data."""
        predictions = []
        
        event_type = event_data.get("eventType", "UNKNOWN")
        capacity = event_data.get("capacity", 0)
        start_datetime = event_data.get("startDateTime")
        
        # Capacity-based predictions
        if capacity > 500:
            predictions.append("Large capacity may require additional security and logistics planning")
        
        if capacity > 1000:
            predictions.append("Very large event - consider traffic management and parking")
        
        # Timeline-based predictions
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                time_until_event = start_dt - datetime.utcnow()
                
                if time_until_event.days < 30:
                    predictions.append("Short timeline may limit venue and vendor options")
                
                if time_until_event.days < 14:
                    predictions.append("Very short timeline - consider postponing or simplifying")
                
            except ValueError:
                pass
        
        # Event type predictions
        if event_type == "WEDDING" and capacity > 100:
            predictions.append("Large wedding - book venue and vendors early")
        
        if event_type == "CONFERENCE":
            predictions.append("Conference events require technical setup and testing")
        
        if event_type == "BIRTHDAY" and capacity > 30:
            predictions.append("Large birthday party - consider entertainment and activities")
        
        return predictions
    
    def get_best_practices(self, event_type: str) -> List[str]:
        """Get best practices for event type."""
        return self.best_practices.get(event_type, [])
    
    def get_seasonal_recommendations(self, event_data: Dict[str, Any]) -> List[str]:
        """Get seasonal recommendations."""
        recommendations = []
        
        start_datetime = event_data.get("startDateTime")
        if start_datetime:
            try:
                start_dt = datetime.fromisoformat(start_datetime.replace('Z', '+00:00'))
                month = start_dt.month
                
                if month in [12, 1, 2]:
                    recommendations.append("Winter events: Consider indoor venues and heating")
                elif month in [6, 7, 8]:
                    recommendations.append("Summer events: Ensure cooling and shade options")
                elif month in [3, 4, 5]:
                    recommendations.append("Spring events: Great for outdoor events, plan rain backup")
                else:
                    recommendations.append("Fall events: Peak season, book early")
                
            except ValueError:
                pass
        
        return recommendations
