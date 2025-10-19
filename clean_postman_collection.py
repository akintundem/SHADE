#!/usr/bin/env python3
"""
Script to clean the Postman collection by removing microservice-specific tests
"""

import json
import sys

def clean_postman_collection(input_file, output_file):
    """Remove microservice-specific tests from Postman collection"""
    
    # Tests to remove (microservice-specific)
    tests_to_remove = [
        "Direct Auth Service Access (Should Fail)",
        "Auth Service Swagger UI", 
        "Auth Service OpenAPI Docs",
        "Direct Swagger UI Access (Should Fail)",
        "Test X-Internal-Auth Header",
        "Test Wrong X-Internal-Auth Header"
    ]
    
    # Read the collection
    with open(input_file, 'r') as f:
        collection = json.load(f)
    
    # Function to recursively remove tests from items
    def remove_tests_from_items(items):
        if not isinstance(items, list):
            return items
            
        cleaned_items = []
        for item in items:
            if item.get('name') in tests_to_remove:
                print(f"Removing test: {item.get('name')}")
                continue
                
            # Recursively clean nested items
            if 'item' in item:
                item['item'] = remove_tests_from_items(item['item'])
                
            cleaned_items.append(item)
            
        return cleaned_items
    
    # Clean the collection
    if 'item' in collection:
        collection['item'] = remove_tests_from_items(collection['item'])
    
    # Update description
    collection['info']['description'] = """Comprehensive testing collection for the Event Planner Auth Service (Monolithic Version). This collection includes all authentication endpoints, user management, session management, and security testing.

## Setup Instructions:
1. Import this collection into Postman
2. Set the base URL environment variable (see below)
3. Follow the testing sequence in order

## Environment Variables:
- `base_url`: http://localhost:8080 (Monolithic Application URL)
- `auth_token`: Will be set automatically after login
- `refresh_token`: Will be set automatically after login
- `user_id`: Will be set automatically after registration

## Testing Sequence:
1. Start with Health Checks
2. Register a new user
3. Login with credentials
4. Test protected endpoints
5. Test user management
6. Test session management
7. Test security scenarios
8. Test error handling

## Note:
This collection has been cleaned to remove microservice-specific tests that don't apply to the monolithic architecture."""
    
    # Write the cleaned collection
    with open(output_file, 'w') as f:
        json.dump(collection, f, indent=2)
    
    print(f"Cleaned collection saved to: {output_file}")
    print(f"Removed {len(tests_to_remove)} microservice-specific tests")

if __name__ == "__main__":
    input_file = "Postman Collections/Event_Planner_Auth_Service_Testing.postman_collection.json"
    output_file = "Postman Collections/Event_Planner_Auth_Service_Testing_Cleaned.postman_collection.json"
    
    clean_postman_collection(input_file, output_file)
