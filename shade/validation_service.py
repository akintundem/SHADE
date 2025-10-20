"""Validation service for integrating DTO validation with event tools."""

from typing import Dict, Any, List
from dto_validation import DTOValidator, ValidationError, ValidationResponse

class ValidationService:
    """Service for validating event data before tool execution."""
    
    def __init__(self):
        self.validator = DTOValidator()
    
    def validate_create_event(self, event_data: Dict[str, Any]) -> ValidationResponse:
        """Validate event data for CreateEvent tool."""
        try:
            validated_data = self.validator.validate_create_event_dto(event_data)
            return ValidationResponse(
                is_valid=True,
                data=validated_data,
                errors=[]
            )
        except ValidationError as e:
            retry_message = self._generate_retry_message("CreateEvent", e)
            return ValidationResponse(
                is_valid=False,
                data=event_data,
                errors=[e.message],
                retry_message=retry_message
            )
        except Exception as e:
            retry_message = self._generate_generic_retry_message("CreateEvent", str(e))
            return ValidationResponse(
                is_valid=False,
                data=event_data,
                errors=[str(e)],
                retry_message=retry_message
            )
    
    def validate_update_event(self, event_data: Dict[str, Any]) -> ValidationResponse:
        """Validate event data for UpdateEvent tool."""
        try:
            validated_data = self.validator.validate_update_event_dto(event_data)
            return ValidationResponse(
                is_valid=True,
                data=validated_data,
                errors=[]
            )
        except ValidationError as e:
            retry_message = self._generate_retry_message("UpdateEvent", e)
            return ValidationResponse(
                is_valid=False,
                data=event_data,
                errors=[e.message],
                retry_message=retry_message
            )
        except Exception as e:
            retry_message = self._generate_generic_retry_message("UpdateEvent", str(e))
            return ValidationResponse(
                is_valid=False,
                data=event_data,
                errors=[str(e)],
                retry_message=retry_message
            )
    
    def _generate_retry_message(self, tool_name: str, error: ValidationError) -> str:
        """Generate a retry message for LangChain to fix the data structure."""
        base_message = f"I need to fix the data structure for {tool_name}. "
        
        if error.field:
            if error.expected and error.actual:
                return f"{base_message}The field '{error.field}' should be {error.expected} but got {type(error.actual).__name__}. Please provide the correct data type."
            else:
                return f"{base_message}There's an issue with the field '{error.field}': {error.message}. Please correct this field."
        else:
            return f"{base_message}{error.message}. Please provide the data in the correct format."
    
    def _generate_generic_retry_message(self, tool_name: str, error_message: str) -> str:
        """Generate a generic retry message for unexpected errors."""
        return f"I encountered an error while preparing data for {tool_name}: {error_message}. Please provide the event data in the correct format and try again."
    
    def get_validation_guidelines(self) -> str:
        """Get validation guidelines for LangChain to follow."""
        return """
        **Event Data Validation Guidelines:**
        
        **Required Fields for CreateEvent:**
        - name: string (2-100 characters)
        - eventType: string (CONFERENCE, MEETING, WORKSHOP, PARTY, CONCERT, SEMINAR, EXHIBITION, FESTIVAL, SPORTS, OTHER)
        - startDateTime: string (ISO format: YYYY-MM-DDTHH:MM:SS)
        
        **Optional Fields:**
        - description: string
        - endDateTime: string (ISO format: YYYY-MM-DDTHH:MM:SS)
        - location: string
        - capacity: integer (must be > 0)
        - isPublic: boolean
        - requiresApproval: boolean
        - qrCodeEnabled: boolean
        - eventStatus: string (PLANNING, CONFIRMED, CANCELLED, POSTPONED, COMPLETED)
        - theme: string
        - tags: array of strings
        - contactEmail: string (valid email format)
        - contactPhone: string (7-15 digits)
        - venue: string
        - maxCapacity: integer (must be > 0)
        - minCapacity: integer (must be > 0)
        
        **Data Type Requirements:**
        - All strings must be non-empty when provided
        - All integers must be positive
        - All booleans must be true/false
        - All arrays must contain valid elements
        - Email must be valid format
        - Phone must be 7-15 digits
        - Dates must be in ISO format
        """
    
    def create_validation_prompt(self, tool_name: str, current_data: Dict[str, Any], errors: List[str]) -> str:
        """Create a prompt for LangChain to fix validation errors."""
        guidelines = self.get_validation_guidelines()
        
        return f"""
        I need to fix the data structure for {tool_name}. Here are the validation errors:
        
        {chr(10).join(f"- {error}" for error in errors)}
        
        Current data provided:
        {current_data}
        
        {guidelines}
        
        Please provide the corrected data in the proper format. Make sure all required fields are present and all data types are correct.
        """

# Global validation service instance
validation_service = ValidationService()
