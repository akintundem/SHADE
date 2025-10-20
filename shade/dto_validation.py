"""DTO validation classes that match Java DTOs for event creation and updates."""

from typing import Dict, Any, List, Optional, Union
from datetime import datetime
from enum import Enum
import re

class EventType(Enum):
    CONFERENCE = "CONFERENCE"
    MEETING = "MEETING"
    WORKSHOP = "WORKSHOP"
    PARTY = "PARTY"
    CONCERT = "CONCERT"
    SEMINAR = "SEMINAR"
    EXHIBITION = "EXHIBITION"
    FESTIVAL = "FESTIVAL"
    SPORTS = "SPORTS"
    OTHER = "OTHER"

class EventStatus(Enum):
    PLANNING = "PLANNING"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"
    POSTPONED = "POSTPONED"
    COMPLETED = "COMPLETED"

class ValidationError(Exception):
    """Custom exception for DTO validation errors."""
    def __init__(self, message: str, field: str = None, expected: str = None, actual: Any = None):
        self.message = message
        self.field = field
        self.expected = expected
        self.actual = actual
        super().__init__(self.message)

class DTOValidator:
    """Validator for event DTOs that match Java backend structure."""
    
    @staticmethod
    def validate_create_event_dto(data: Dict[str, Any]) -> Dict[str, Any]:
        """Validate CreateEventRequest DTO structure and data."""
        errors = []
        validated_data = {}
        
        # Required fields validation
        required_fields = {
            'name': str,
            'eventType': str,
            'startDateTime': str
        }
        
        for field, expected_type in required_fields.items():
            if field not in data:
                errors.append(f"Missing required field: {field}")
            elif not isinstance(data[field], expected_type):
                errors.append(f"Field '{field}' must be of type {expected_type.__name__}, got {type(data[field]).__name__}")
            else:
                validated_data[field] = data[field]
        
        if errors:
            raise ValidationError(f"CreateEvent validation failed: {'; '.join(errors)}")
        
        # Validate event name
        if 'name' in validated_data:
            name = validated_data['name'].strip()
            if not name or len(name) < 2:
                errors.append("Event name must be at least 2 characters long")
            elif len(name) > 100:
                errors.append("Event name must be less than 100 characters")
            else:
                validated_data['name'] = name
        
        # Validate event type
        if 'eventType' in validated_data:
            event_type = validated_data['eventType'].upper()
            try:
                EventType(event_type)
                validated_data['eventType'] = event_type
            except ValueError:
                valid_types = [e.value for e in EventType]
                errors.append(f"Invalid eventType '{event_type}'. Must be one of: {', '.join(valid_types)}")
        
        # Validate start date time
        if 'startDateTime' in validated_data:
            if not DTOValidator._is_valid_iso_datetime(validated_data['startDateTime']):
                errors.append("startDateTime must be in ISO format (YYYY-MM-DDTHH:MM:SS)")
        
        # Optional fields validation
        optional_fields = {
            'description': str,
            'endDateTime': str,
            'location': str,
            'capacity': int,
            'isPublic': bool,
            'requiresApproval': bool,
            'qrCodeEnabled': bool,
            'eventStatus': str,
            'theme': str,
            'tags': list,
            'contactEmail': str,
            'contactPhone': str,
            'venue': str,
            'maxCapacity': int,
            'minCapacity': int
        }
        
        for field, expected_type in optional_fields.items():
            if field in data:
                if not isinstance(data[field], expected_type):
                    errors.append(f"Optional field '{field}' must be of type {expected_type.__name__}, got {type(data[field]).__name__}")
                else:
                    validated_data[field] = data[field]
        
        # Validate optional string fields
        string_fields = ['description', 'location', 'theme', 'contactEmail', 'contactPhone', 'venue']
        for field in string_fields:
            if field in validated_data:
                value = validated_data[field].strip()
                if not value:
                    del validated_data[field]  # Remove empty strings
                else:
                    validated_data[field] = value
        
        # Validate optional numeric fields
        if 'capacity' in validated_data:
            if validated_data['capacity'] < 1:
                errors.append("Capacity must be greater than 0")
        
        if 'maxCapacity' in validated_data:
            if validated_data['maxCapacity'] < 1:
                errors.append("Max capacity must be greater than 0")
        
        if 'minCapacity' in validated_data:
            if validated_data['minCapacity'] < 1:
                errors.append("Min capacity must be greater than 0")
        
        # Validate email format
        if 'contactEmail' in validated_data:
            if not DTOValidator._is_valid_email(validated_data['contactEmail']):
                errors.append("Invalid email format for contactEmail")
        
        # Validate phone format
        if 'contactPhone' in validated_data:
            if not DTOValidator._is_valid_phone(validated_data['contactPhone']):
                errors.append("Invalid phone format for contactPhone")
        
        # Validate tags
        if 'tags' in validated_data:
            if not isinstance(validated_data['tags'], list):
                errors.append("Tags must be a list")
            else:
                validated_data['tags'] = [str(tag).strip() for tag in validated_data['tags'] if str(tag).strip()]
        
        if errors:
            raise ValidationError(f"CreateEvent validation failed: {'; '.join(errors)}")
        
        return validated_data
    
    @staticmethod
    def validate_update_event_dto(data: Dict[str, Any]) -> Dict[str, Any]:
        """Validate UpdateEventRequest DTO structure and data."""
        errors = []
        validated_data = {}
        
        # All fields are optional for updates, but we validate the ones that are present
        optional_fields = {
            'name': str,
            'eventType': str,
            'startDateTime': str,
            'endDateTime': str,
            'description': str,
            'location': str,
            'capacity': int,
            'isPublic': bool,
            'requiresApproval': bool,
            'qrCodeEnabled': bool,
            'eventStatus': str,
            'theme': str,
            'tags': list,
            'contactEmail': str,
            'contactPhone': str,
            'venue': str,
            'maxCapacity': int,
            'minCapacity': int
        }
        
        for field, expected_type in optional_fields.items():
            if field in data:
                if not isinstance(data[field], expected_type):
                    errors.append(f"Field '{field}' must be of type {expected_type.__name__}, got {type(data[field]).__name__}")
                else:
                    validated_data[field] = data[field]
        
        if errors:
            raise ValidationError(f"UpdateEvent validation failed: {'; '.join(errors)}")
        
        # Apply same validation rules as create event for fields that are present
        if 'name' in validated_data:
            name = validated_data['name'].strip()
            if not name or len(name) < 2:
                errors.append("Event name must be at least 2 characters long")
            elif len(name) > 100:
                errors.append("Event name must be less than 100 characters")
            else:
                validated_data['name'] = name
        
        if 'eventType' in validated_data:
            event_type = validated_data['eventType'].upper()
            try:
                EventType(event_type)
                validated_data['eventType'] = event_type
            except ValueError:
                valid_types = [e.value for e in EventType]
                errors.append(f"Invalid eventType '{event_type}'. Must be one of: {', '.join(valid_types)}")
        
        if 'startDateTime' in validated_data:
            if not DTOValidator._is_valid_iso_datetime(validated_data['startDateTime']):
                errors.append("startDateTime must be in ISO format (YYYY-MM-DDTHH:MM:SS)")
        
        if 'endDateTime' in validated_data:
            if not DTOValidator._is_valid_iso_datetime(validated_data['endDateTime']):
                errors.append("endDateTime must be in ISO format (YYYY-MM-DDTHH:MM:SS)")
        
        if 'eventStatus' in validated_data:
            status = validated_data['eventStatus'].upper()
            try:
                EventStatus(status)
                validated_data['eventStatus'] = status
            except ValueError:
                valid_statuses = [s.value for s in EventStatus]
                errors.append(f"Invalid eventStatus '{status}'. Must be one of: {', '.join(valid_statuses)}")
        
        # Validate string fields
        string_fields = ['description', 'location', 'theme', 'contactEmail', 'contactPhone', 'venue']
        for field in string_fields:
            if field in validated_data:
                value = validated_data[field].strip()
                if not value:
                    del validated_data[field]  # Remove empty strings
                else:
                    validated_data[field] = value
        
        # Validate numeric fields
        if 'capacity' in validated_data and validated_data['capacity'] < 1:
            errors.append("Capacity must be greater than 0")
        
        if 'maxCapacity' in validated_data and validated_data['maxCapacity'] < 1:
            errors.append("Max capacity must be greater than 0")
        
        if 'minCapacity' in validated_data and validated_data['minCapacity'] < 1:
            errors.append("Min capacity must be greater than 0")
        
        # Validate email and phone
        if 'contactEmail' in validated_data and not DTOValidator._is_valid_email(validated_data['contactEmail']):
            errors.append("Invalid email format for contactEmail")
        
        if 'contactPhone' in validated_data and not DTOValidator._is_valid_phone(validated_data['contactPhone']):
            errors.append("Invalid phone format for contactPhone")
        
        # Validate tags
        if 'tags' in validated_data:
            if not isinstance(validated_data['tags'], list):
                errors.append("Tags must be a list")
            else:
                validated_data['tags'] = [str(tag).strip() for tag in validated_data['tags'] if str(tag).strip()]
        
        if errors:
            raise ValidationError(f"UpdateEvent validation failed: {'; '.join(errors)}")
        
        return validated_data
    
    @staticmethod
    def _is_valid_iso_datetime(date_string: str) -> bool:
        """Validate ISO datetime format."""
        try:
            datetime.fromisoformat(date_string.replace('Z', '+00:00'))
            return True
        except ValueError:
            return False
    
    @staticmethod
    def _is_valid_email(email: str) -> bool:
        """Validate email format."""
        pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
        return re.match(pattern, email) is not None
    
    @staticmethod
    def _is_valid_phone(phone: str) -> bool:
        """Validate phone format."""
        # Remove all non-digit characters
        digits_only = re.sub(r'\D', '', phone)
        # Check if it's a valid length (7-15 digits)
        return 7 <= len(digits_only) <= 15

class ValidationResponse:
    """Response class for validation results."""
    
    def __init__(self, is_valid: bool, data: Dict[str, Any] = None, errors: List[str] = None, 
                 retry_message: str = None):
        self.is_valid = is_valid
        self.data = data or {}
        self.errors = errors or []
        self.retry_message = retry_message
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "is_valid": self.is_valid,
            "data": self.data,
            "errors": self.errors,
            "retry_message": self.retry_message
        }
