"""Google Maps venue search tool for event planning."""

import json
import os
import requests
from typing import Dict, Any, List, Optional
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

class VenueTool:
    """Tool for searching venues using Google Maps API."""
    
    def __init__(self):
        self.api_key = os.getenv("GOOGLE_MAPS_API_KEY")
        self.base_url = "https://maps.googleapis.com/maps/api/place"
        self.venue_cache = {}  # Simple cache to avoid repeated API calls
        
    def can_handle(self, message: str) -> bool:
        """Check if the message is about venue search."""
        venue_keywords = [
            "venue", "venues", "location", "place", "where", "where to",
            "find venue", "search venue", "venue search", "look for venue",
            "suggest venue", "venue options", "venue recommendations",
            "wedding venue", "conference venue", "party venue", "event venue",
            "hotel", "restaurant", "hall", "ballroom", "conference center",
            "event space", "meeting room", "banquet hall"
        ]
        
        message_lower = message.lower()
        return any(keyword in message_lower for keyword in venue_keywords)
    
    async def process(self, message: str, chat_id: str = None) -> Dict[str, Any]:
        """Process venue search request."""
        try:
            # Extract search parameters from message
            search_params = self._extract_search_params(message)
            
            # Search for venues
            venues = await self._search_venues(search_params)
            
            if not venues:
                return {
                    "success": False,
                    "message": "I couldn't find any venues matching your criteria. Could you try being more specific about the location or event type?",
                    "data": None,
                    "conversation_state": "venue_search"
                }
            
            # Format response with venue suggestions
            formatted_venues = self._format_venues_for_display(venues)
            
            return {
                "success": True,
                "message": f"🎉 I found {len(venues)} amazing venues for your event! Here are my top recommendations:",
                "data": {
                    "venues": formatted_venues,
                    "search_params": search_params,
                    "tool_call_required": True,
                    "tool_name": "VenueSearch"
                },
                "conversation_state": "venue_search",
                "show_chips": True
            }
            
        except Exception as e:
            return {
                "success": False,
                "message": f"Oops! I had trouble searching for venues. Let me try again - could you tell me more about what you're looking for?",
                "data": None,
                "conversation_state": "venue_search"
            }
    
    def _extract_search_params(self, message: str) -> Dict[str, Any]:
        """Extract search parameters from user message."""
        params = {
            "query": "",
            "location": "",
            "radius": 5000,  # 5km default radius
            "event_type": "",
            "price_level": None,
            "rating_min": 4.0
        }
        
        message_lower = message.lower()
        
        # Extract event type
        event_types = {
            "wedding": ["wedding", "marriage", "bridal", "ceremony"],
            "conference": ["conference", "convention", "meeting", "business"],
            "party": ["party", "celebration", "birthday", "anniversary"],
            "corporate": ["corporate", "business", "company", "office"],
            "concert": ["concert", "music", "performance", "show"],
            "workshop": ["workshop", "training", "seminar", "class"]
        }
        
        for event_type, keywords in event_types.items():
            if any(keyword in message_lower for keyword in keywords):
                params["event_type"] = event_type
                break
        
        # Extract location hints
        location_hints = [
            "in", "at", "near", "around", "downtown", "uptown", "city center",
            "historic district", "waterfront", "beach", "park", "hotel"
        ]
        
        words = message.split()
        for i, word in enumerate(words):
            if word.lower() in location_hints and i + 1 < len(words):
                # Try to extract location after the hint
                location_parts = []
                for j in range(i + 1, min(i + 4, len(words))):
                    if words[j].lower() not in ["for", "with", "and", "or"]:
                        location_parts.append(words[j])
                    else:
                        break
                if location_parts:
                    params["location"] = " ".join(location_parts)
                    break
        
        # If no specific location, use a default
        if not params["location"]:
            params["location"] = "New York, NY"  # Default location
        
        # Extract price level hints
        if any(word in message_lower for word in ["cheap", "budget", "affordable", "inexpensive"]):
            params["price_level"] = 1
        elif any(word in message_lower for word in ["expensive", "luxury", "premium", "high-end"]):
            params["price_level"] = 4
        elif any(word in message_lower for word in ["moderate", "mid-range", "reasonable"]):
            params["price_level"] = 2
        
        # Create search query
        query_parts = []
        if params["event_type"]:
            query_parts.append(f"{params['event_type']} venue")
        else:
            query_parts.append("event venue")
        
        if params["location"]:
            query_parts.append(f"in {params['location']}")
        
        params["query"] = " ".join(query_parts)
        
        return params
    
    async def _search_venues(self, params: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Search for venues using Google Maps API."""
        if not self.api_key:
            # Return mock data if no API key
            return self._get_mock_venues(params)
        
        try:
            # Check cache first
            cache_key = f"{params['query']}_{params['location']}_{params['radius']}"
            if cache_key in self.venue_cache:
                return self.venue_cache[cache_key]
            
            # Search for places
            search_url = f"{self.base_url}/textsearch/json"
            search_params = {
                "query": params["query"],
                "location": params["location"],
                "radius": params["radius"],
                "type": "establishment",
                "key": self.api_key
            }
            
            response = requests.get(search_url, params=search_params)
            response.raise_for_status()
            
            data = response.json()
            
            if data["status"] != "OK":
                print(f"Google Maps API error: {data.get('error_message', 'Unknown error')}")
                return self._get_mock_venues(params)
            
            venues = []
            for place in data.get("results", [])[:5]:  # Limit to top 5
                venue = await self._get_venue_details(place["place_id"])
                if venue:
                    venues.append(venue)
            
            # Cache the results
            self.venue_cache[cache_key] = venues
            
            return venues
            
        except Exception as e:
            print(f"Error searching venues: {e}")
            return self._get_mock_venues(params)
    
    async def _get_venue_details(self, place_id: str) -> Optional[Dict[str, Any]]:
        """Get detailed information about a specific venue."""
        try:
            details_url = f"{self.base_url}/details/json"
            details_params = {
                "place_id": place_id,
                "fields": "name,formatted_address,geometry,rating,price_level,photos,formatted_phone_number,website,opening_hours,reviews",
                "key": self.api_key
            }
            
            response = requests.get(details_url, params=details_params)
            response.raise_for_status()
            
            data = response.json()
            
            if data["status"] != "OK":
                return None
            
            result = data["result"]
            
            # Get photo URL if available
            photo_url = None
            if "photos" in result and result["photos"]:
                photo_reference = result["photos"][0]["photo_reference"]
                photo_url = f"https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference={photo_reference}&key={self.api_key}"
            
            return {
                "place_id": place_id,
                "name": result.get("name", "Unknown Venue"),
                "address": result.get("formatted_address", ""),
                "rating": result.get("rating", 0.0),
                "price_level": result.get("price_level", 0),
                "phone": result.get("formatted_phone_number", ""),
                "website": result.get("website", ""),
                "photo_url": photo_url,
                "opening_hours": result.get("opening_hours", {}),
                "reviews": result.get("reviews", [])[:3],  # Top 3 reviews
                "latitude": result.get("geometry", {}).get("location", {}).get("lat"),
                "longitude": result.get("geometry", {}).get("location", {}).get("lng")
            }
            
        except Exception as e:
            print(f"Error getting venue details: {e}")
            return None
    
    def _get_mock_venues(self, params: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Return mock venue data for testing when API key is not available."""
        mock_venues = [
            {
                "place_id": "mock_1",
                "name": "Grand Ballroom Estate",
                "address": "123 Downtown Ave, Downtown",
                "rating": 4.8,
                "price_level": 3,
                "phone": "(555) 123-4567",
                "website": "https://grandballroom.com",
                "photo_url": "https://via.placeholder.com/400x300/4A90E2/FFFFFF?text=Grand+Ballroom",
                "opening_hours": {"open_now": True},
                "reviews": [
                    {
                        "author_name": "Sarah Johnson",
                        "rating": 5,
                        "text": "Absolutely stunning venue! Perfect for our wedding."
                    }
                ],
                "latitude": 40.7128,
                "longitude": -74.0060,
                "capacity": "200-300 guests",
                "price_range": "$8,000 - $12,000"
            },
            {
                "place_id": "mock_2",
                "name": "Luxury Plaza Hotel",
                "address": "456 Historic District, Historic District",
                "rating": 4.9,
                "price_level": 4,
                "phone": "(555) 987-6543",
                "website": "https://luxuryplaza.com",
                "photo_url": "https://via.placeholder.com/400x300/E74C3C/FFFFFF?text=Luxury+Plaza",
                "opening_hours": {"open_now": True},
                "reviews": [
                    {
                        "author_name": "Michael Chen",
                        "rating": 5,
                        "text": "Five-star luxury and world-class service. Perfect for corporate events."
                    }
                ],
                "latitude": 40.7589,
                "longitude": -73.9851,
                "capacity": "250-400 guests",
                "price_range": "$10,000 - $15,000"
            },
            {
                "place_id": "mock_3",
                "name": "Garden Pavilion",
                "address": "789 Park Blvd, Waterfront",
                "rating": 4.6,
                "price_level": 2,
                "phone": "(555) 456-7890",
                "website": "https://gardenpavilion.com",
                "photo_url": "https://via.placeholder.com/400x300/27AE60/FFFFFF?text=Garden+Pavilion",
                "opening_hours": {"open_now": True},
                "reviews": [
                    {
                        "author_name": "Emily Davis",
                        "rating": 4,
                        "text": "Beautiful outdoor space with great views. Perfect for summer events."
                    }
                ],
                "latitude": 40.6892,
                "longitude": -74.0445,
                "capacity": "150-250 guests",
                "price_range": "$5,000 - $8,000"
            }
        ]
        
        # Filter by event type if specified
        if params.get("event_type"):
            event_type = params["event_type"]
            if event_type == "wedding":
                return [v for v in mock_venues if "Ballroom" in v["name"] or "Plaza" in v["name"]]
            elif event_type == "corporate":
                return [v for v in mock_venues if "Plaza" in v["name"] or "Hotel" in v["name"]]
            elif event_type == "party":
                return [v for v in mock_venues if "Garden" in v["name"] or "Pavilion" in v["name"]]
        
        return mock_venues
    
    def _format_venues_for_display(self, venues: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Format venues for display in the UI."""
        formatted = []
        
        for venue in venues:
            # Calculate capacity range based on venue type
            capacity = venue.get("capacity", "100-200 guests")
            
            # Calculate price range based on price level
            price_ranges = {
                1: "$1,000 - $3,000",
                2: "$3,000 - $6,000", 
                3: "$6,000 - $10,000",
                4: "$10,000 - $20,000"
            }
            price_range = venue.get("price_range", price_ranges.get(venue.get("price_level", 2), "$3,000 - $6,000"))
            
            formatted_venue = {
                "id": venue["place_id"],
                "name": venue["name"],
                "address": venue["address"],
                "rating": venue["rating"],
                "price_level": venue["price_level"],
                "phone": venue["phone"],
                "website": venue["website"],
                "photo_url": venue["photo_url"],
                "capacity": capacity,
                "price_range": price_range,
                "latitude": venue.get("latitude"),
                "longitude": venue.get("longitude"),
                "reviews": venue.get("reviews", [])[:2],  # Top 2 reviews for display
                "opening_hours": venue.get("opening_hours", {})
            }
            
            formatted.append(formatted_venue)
        
        return formatted
    
    def get_venue_by_id(self, venue_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific venue by ID."""
        # Search through cache first
        for venues in self.venue_cache.values():
            for venue in venues:
                if venue["place_id"] == venue_id:
                    return venue
        
        return None
