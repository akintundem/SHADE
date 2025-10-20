#!/usr/bin/env python3
"""Test script for DTO validation system."""

import asyncio
from validation_service import validation_service
from event_tool import EventTool

async def test_validation():
    """Test the validation system with various data scenarios."""
    
    print("🧪 Testing DTO Validation System\n")
    
    # Test 1: Valid CreateEvent data
    print("Test 1: Valid CreateEvent data")
    valid_data = {
        "name": "Tech Conference 2024",
        "eventType": "CONFERENCE",
        "startDateTime": "2024-12-25T14:00:00",
        "description": "A great tech conference",
        "capacity": 100,
        "isPublic": True
    }
    
    result = validation_service.validate_create_event(valid_data)
    print(f"✅ Valid data: {result.is_valid}")
    if result.errors:
        print(f"Errors: {result.errors}")
    print()
    
    # Test 2: Invalid CreateEvent data (missing required fields)
    print("Test 2: Invalid CreateEvent data (missing required fields)")
    invalid_data = {
        "name": "Tech Conference 2024",
        # Missing eventType and startDateTime
        "description": "A great tech conference"
    }
    
    result = validation_service.validate_create_event(invalid_data)
    print(f"❌ Invalid data: {result.is_valid}")
    print(f"Errors: {result.errors}")
    print(f"Retry message: {result.retry_message}")
    print()
    
    # Test 3: Invalid data types
    print("Test 3: Invalid data types")
    invalid_types = {
        "name": "Tech Conference 2024",
        "eventType": "INVALID_TYPE",
        "startDateTime": "not-a-date",
        "capacity": "not-a-number"
    }
    
    result = validation_service.validate_create_event(invalid_types)
    print(f"❌ Invalid types: {result.is_valid}")
    print(f"Errors: {result.errors}")
    print(f"Retry message: {result.retry_message}")
    print()
    
    # Test 4: Valid UpdateEvent data
    print("Test 4: Valid UpdateEvent data")
    update_data = {
        "description": "Updated description",
        "capacity": 150,
        "isPublic": False
    }
    
    result = validation_service.validate_update_event(update_data)
    print(f"✅ Valid update: {result.is_valid}")
    if result.errors:
        print(f"Errors: {result.errors}")
    print()
    
    # Test 5: Test with EventTool (simulate tool execution)
    print("Test 5: Testing EventTool with validation")
    event_tool = EventTool()
    
    try:
        # This should work
        await event_tool.createEvent(valid_data, "test_chat_id")
        print("✅ EventTool validation passed")
    except Exception as e:
        print(f"❌ EventTool validation failed: {e}")
    
    print("\n🎉 Validation testing complete!")

if __name__ == "__main__":
    asyncio.run(test_validation())
