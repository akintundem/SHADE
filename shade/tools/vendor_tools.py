"""Vendor management tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List


@tool
async def search_vendors(
    category: str,
    location: Optional[str] = None,
    event_type: Optional[str] = None,
    budget_range: Optional[str] = None
) -> Dict[str, Any]:
    """Search for vendors by category and location.
    
    Args:
        category: Vendor category (catering, photography, flowers, music, etc.)
        location: Location to search in
        event_type: Type of event (wedding, conference, party, etc.)
        budget_range: Budget range (budget, moderate, luxury)
    
    Returns:
        Dict with list of vendors and their details
    """
    # Mock vendor data based on category
    vendor_categories = {
        "catering": [
            {
                "id": "vendor_cat_1",
                "name": "Gourmet Catering Co.",
                "category": "catering",
                "rating": 4.7,
                "price_range": "$50-80 per person",
                "location": "Downtown",
                "phone": "(555) 111-2222",
                "website": "https://gourmetcatering.com",
                "description": "Premium catering with customizable menus",
                "specialties": ["Wedding catering", "Corporate events", "Cocktail parties"]
            },
            {
                "id": "vendor_cat_2",
                "name": "Elegant Eats",
                "category": "catering",
                "rating": 4.5,
                "price_range": "$40-60 per person",
                "location": "Historic District",
                "phone": "(555) 333-4444",
                "website": "https://eleganteats.com",
                "description": "Sophisticated cuisine for special occasions",
                "specialties": ["Fine dining", "Wedding receptions", "Anniversary parties"]
            }
        ],
        "photography": [
            {
                "id": "vendor_photo_1",
                "name": "Memories Photography Studio",
                "category": "photography",
                "rating": 4.9,
                "price_range": "$2,000-4,000",
                "location": "Arts District",
                "phone": "(555) 555-6666",
                "website": "https://memoriesphoto.com",
                "description": "Professional wedding and event photography",
                "specialties": ["Wedding photography", "Event coverage", "Portrait sessions"]
            }
        ],
        "flowers": [
            {
                "id": "vendor_flower_1",
                "name": "Blooms & Beyond",
                "category": "flowers",
                "rating": 4.6,
                "price_range": "$500-2,000",
                "location": "Garden District",
                "phone": "(555) 777-8888",
                "website": "https://bloomsbeyond.com",
                "description": "Fresh floral arrangements for all occasions",
                "specialties": ["Wedding bouquets", "Centerpieces", "Event decor"]
            }
        ],
        "music": [
            {
                "id": "vendor_music_1",
                "name": "Harmony Entertainment",
                "category": "music",
                "rating": 4.8,
                "price_range": "$1,500-3,000",
                "location": "Music Quarter",
                "phone": "(555) 999-0000",
                "website": "https://harmonyent.com",
                "description": "Live music and DJ services for events",
                "specialties": ["Wedding bands", "DJ services", "Corporate entertainment"]
            }
        ]
    }
    
    vendors = vendor_categories.get(category.lower(), [])
    
    return {
        "success": True,
        "vendors": vendors,
        "count": len(vendors),
        "search_params": {
            "category": category,
            "location": location,
            "event_type": event_type,
            "budget_range": budget_range
        }
    }


@tool
async def get_vendor_quote(
    vendor_id: str,
    event_id: str,
    service_details: str,
    event_date: str,
    guest_count: int,
    special_requirements: Optional[str] = None
) -> Dict[str, Any]:
    """Request a quote from a vendor for specific services.
    
    Args:
        vendor_id: ID of the vendor
        event_id: ID of the event
        service_details: Description of services needed
        event_date: Date of the event
        guest_count: Number of guests
        special_requirements: Any special requirements
    
    Returns:
        Dict with quote details
    """
    # Mock quote response
    quote_id = f"quote_{vendor_id}_{event_id}"
    
    return {
        "success": True,
        "quote_id": quote_id,
        "message": "Quote request submitted successfully",
        "quote": {
            "id": quote_id,
            "vendor_id": vendor_id,
            "event_id": event_id,
            "service_details": service_details,
            "event_date": event_date,
            "guest_count": guest_count,
            "special_requirements": special_requirements,
            "estimated_cost": "$2,500",
            "status": "PENDING",
            "response_deadline": "2024-01-20",
            "valid_until": "2024-02-01"
        }
    }


@tool
async def book_vendor(
    vendor_id: str,
    event_id: str,
    quote_id: str,
    final_amount: float,
    deposit_amount: Optional[float] = None
) -> Dict[str, Any]:
    """Book a vendor for an event.
    
    Args:
        vendor_id: ID of the vendor to book
        event_id: ID of the event
        quote_id: ID of the accepted quote
        final_amount: Final agreed amount
        deposit_amount: Deposit amount (if different from standard)
    
    Returns:
        Dict with booking confirmation
    """
    booking_id = f"booking_{vendor_id}_{event_id}"
    
    return {
        "success": True,
        "booking_id": booking_id,
        "message": "Vendor booking confirmed successfully",
        "booking": {
            "id": booking_id,
            "vendor_id": vendor_id,
            "event_id": event_id,
            "quote_id": quote_id,
            "final_amount": final_amount,
            "deposit_amount": deposit_amount or (final_amount * 0.5),
            "status": "CONFIRMED",
            "contract_sent": True,
            "payment_terms": "50% deposit, 50% on event day"
        }
    }


@tool
async def list_vendor_services(vendor_id: str) -> Dict[str, Any]:
    """Get list of services offered by a vendor.
    
    Args:
        vendor_id: ID of the vendor
    
    Returns:
        Dict with vendor services
    """
    # Mock services data
    services = [
        {
            "id": "service_1",
            "name": "Full Wedding Package",
            "description": "Complete wedding catering service",
            "price_range": "$3,000-5,000",
            "duration": "6 hours",
            "includes": ["Setup", "Service", "Cleanup", "Equipment"]
        },
        {
            "id": "service_2",
            "name": "Cocktail Reception",
            "description": "Hors d'oeuvres and cocktail service",
            "price_range": "$1,500-2,500",
            "duration": "3 hours",
            "includes": ["Appetizers", "Bar service", "Staff"]
        }
    ]
    
    return {
        "success": True,
        "vendor_id": vendor_id,
        "services": services,
        "count": len(services)
    }
