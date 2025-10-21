"""Venue search and booking tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List
import requests
import os
from dotenv import load_dotenv

load_dotenv()


@tool
async def search_venues(
    location: str,
    capacity: Optional[int] = None,
    event_type: Optional[str] = None,
    price_range: Optional[str] = None,
    radius: int = 5000
) -> Dict[str, Any]:
    """Search for venues using Google Maps API or mock data.
    
    Args:
        location: Location to search (city, address, etc.)
        capacity: Minimum capacity required
        event_type: Type of event (wedding, conference, party, etc.)
        price_range: Price range (budget, moderate, luxury)
        radius: Search radius in meters (default 5000)
    
    Returns:
        Dict with list of venues and their details
    """
    # For now, return mock venue data
    mock_venues = [
        {
            "id": "venue_1",
            "name": "Grand Ballroom Estate",
            "address": "123 Downtown Ave, Downtown",
            "rating": 4.8,
            "price_level": 3,
            "capacity": "200-300 guests",
            "price_range": "$8,000 - $12,000",
            "phone": "(555) 123-4567",
            "website": "https://grandballroom.com",
            "amenities": ["Parking", "Catering", "Audio/Visual", "WiFi"],
            "description": "Elegant ballroom perfect for weddings and corporate events"
        },
        {
            "id": "venue_2", 
            "name": "Luxury Plaza Hotel",
            "address": "456 Historic District, Historic District",
            "rating": 4.9,
            "price_level": 4,
            "capacity": "250-400 guests",
            "price_range": "$10,000 - $15,000",
            "phone": "(555) 987-6543",
            "website": "https://luxuryplaza.com",
            "amenities": ["Parking", "Catering", "Audio/Visual", "WiFi", "Accommodations"],
            "description": "Five-star luxury hotel with world-class service"
        },
        {
            "id": "venue_3",
            "name": "Garden Pavilion",
            "address": "789 Park Blvd, Waterfront",
            "rating": 4.6,
            "price_level": 2,
            "capacity": "150-250 guests",
            "price_range": "$5,000 - $8,000",
            "phone": "(555) 456-7890",
            "website": "https://gardenpavilion.com",
            "amenities": ["Parking", "Outdoor Space", "Catering", "WiFi"],
            "description": "Beautiful outdoor space with garden views"
        }
    ]
    
    # Filter by capacity if specified
    if capacity:
        filtered_venues = []
        for venue in mock_venues:
            # Simple capacity filtering (extract max capacity from range)
            capacity_range = venue["capacity"]
            if "300" in capacity_range and capacity <= 300:
                filtered_venues.append(venue)
            elif "400" in capacity_range and capacity <= 400:
                filtered_venues.append(venue)
            elif "250" in capacity_range and capacity <= 250:
                filtered_venues.append(venue)
        mock_venues = filtered_venues
    
    return {
        "success": True,
        "venues": mock_venues,
        "count": len(mock_venues),
        "search_params": {
            "location": location,
            "capacity": capacity,
            "event_type": event_type,
            "price_range": price_range
        }
    }


@tool
async def get_venue_details(venue_id: str) -> Dict[str, Any]:
    """Get detailed information about a specific venue.
    
    Args:
        venue_id: ID of the venue to get details for
    
    Returns:
        Dict with detailed venue information
    """
    # For now, return mock detailed venue data
    venue_details = {
        "id": venue_id,
        "name": "Grand Ballroom Estate",
        "address": "123 Downtown Ave, Downtown",
        "rating": 4.8,
        "price_level": 3,
        "capacity": "200-300 guests",
        "price_range": "$8,000 - $12,000",
        "phone": "(555) 123-4567",
        "website": "https://grandballroom.com",
        "email": "info@grandballroom.com",
        "amenities": ["Parking", "Catering", "Audio/Visual", "WiFi", "Bridal Suite"],
        "description": "Elegant ballroom perfect for weddings and corporate events",
        "availability": {
            "next_available": "2024-08-15",
            "peak_season": "May-September",
            "minimum_booking": "3 months advance"
        },
        "policies": {
            "cancellation": "30 days notice required",
            "deposit": "50% required to secure booking",
            "insurance": "Event insurance recommended"
        },
        "photos": [
            "https://example.com/venue1.jpg",
            "https://example.com/venue2.jpg"
        ]
    }
    
    return {
        "success": True,
        "venue": venue_details
    }


@tool
async def book_venue(
    venue_id: str,
    event_id: str,
    event_date: str,
    duration_hours: int,
    guest_count: int,
    special_requirements: Optional[str] = None
) -> Dict[str, Any]:
    """Book a venue for an event.
    
    Args:
        venue_id: ID of the venue to book
        event_id: ID of the event
        event_date: Date of the event (YYYY-MM-DD)
        duration_hours: Duration in hours
        guest_count: Expected number of guests
        special_requirements: Any special requirements or requests
    
    Returns:
        Dict with booking confirmation details
    """
    # For now, return mock booking confirmation
    booking_id = f"booking_{venue_id}_{event_id}"
    
    return {
        "success": True,
        "booking_id": booking_id,
        "message": "Venue booking request submitted successfully",
        "booking": {
            "id": booking_id,
            "venue_id": venue_id,
            "event_id": event_id,
            "event_date": event_date,
            "duration_hours": duration_hours,
            "guest_count": guest_count,
            "special_requirements": special_requirements,
            "status": "PENDING_CONFIRMATION",
            "estimated_cost": "$10,000",
            "deposit_required": "$5,000",
            "confirmation_deadline": "2024-01-15"
        }
    }
