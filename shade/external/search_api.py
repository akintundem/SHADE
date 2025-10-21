"""Mock Search API for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
from datetime import datetime
import random

logger = logging.getLogger(__name__)


class SearchAPIService:
    """Mock Search API for prototyping - returns consistent search results."""
    
    def __init__(self):
        """Initialize mock search API service."""
        self.initialized = False
        self.mock_search_data = self._initialize_mock_search_data()
        self.search_history = []
    
    def _initialize_mock_search_data(self) -> Dict[str, Any]:
        """Initialize with mock search data for consistent responses."""
        return {
            "venues": [
                {
                    "name": "Grand Hotel Ballroom",
                    "location": "New York, NY",
                    "capacity": 300,
                    "price_range": "$5000-$8000",
                    "rating": 4.5,
                    "amenities": ["Catering", "Parking", "AV Equipment", "WiFi"],
                    "contact": "info@grandhotel.com",
                    "website": "https://grandhotel.com"
                },
                {
                    "name": "Riverside Convention Center",
                    "location": "Chicago, IL", 
                    "capacity": 500,
                    "price_range": "$3000-$6000",
                    "rating": 4.2,
                    "amenities": ["Catering", "Parking", "AV Equipment", "WiFi", "Accessibility"],
                    "contact": "events@riversidecc.com",
                    "website": "https://riversidecc.com"
                },
                {
                    "name": "Garden Pavilion",
                    "location": "Los Angeles, CA",
                    "capacity": 150,
                    "price_range": "$2000-$4000",
                    "rating": 4.7,
                    "amenities": ["Outdoor Space", "Catering", "Parking", "Photography Spots"],
                    "contact": "bookings@gardenpavilion.com",
                    "website": "https://gardenpavilion.com"
                }
            ],
            "vendors": [
                {
                    "name": "Elite Catering Co.",
                    "type": "Catering",
                    "location": "New York, NY",
                    "rating": 4.6,
                    "price_range": "$50-$100 per person",
                    "specialties": ["Wedding", "Corporate", "Cocktail Parties"],
                    "contact": "info@elitecatering.com",
                    "website": "https://elitecatering.com"
                },
                {
                    "name": "Perfect Moments Photography",
                    "type": "Photography",
                    "location": "Chicago, IL",
                    "rating": 4.8,
                    "price_range": "$2000-$5000",
                    "specialties": ["Wedding", "Corporate", "Portrait"],
                    "contact": "hello@perfectmoments.com",
                    "website": "https://perfectmoments.com"
                },
                {
                    "name": "Dream Decorations",
                    "type": "Decorations",
                    "location": "Los Angeles, CA",
                    "rating": 4.4,
                    "price_range": "$1000-$3000",
                    "specialties": ["Wedding", "Birthday", "Corporate"],
                    "contact": "info@dreamdecor.com",
                    "website": "https://dreamdecor.com"
                }
            ],
            "events": [
                {
                    "title": "Spring Wedding Trends 2024",
                    "type": "Article",
                    "source": "Event Planning Magazine",
                    "url": "https://eventplanning.com/spring-wedding-trends-2024",
                    "summary": "Latest trends in spring wedding planning including color schemes, decorations, and venue ideas.",
                    "published": "2024-01-15"
                },
                {
                    "title": "Budget-Friendly Event Planning Tips",
                    "type": "Article", 
                    "source": "Event Pro Weekly",
                    "url": "https://eventpro.com/budget-friendly-tips",
                    "summary": "Expert advice on planning memorable events without breaking the bank.",
                    "published": "2024-01-10"
                }
            ]
        }
    
    async def initialize(self):
        """Initialize the search API service."""
        try:
            self.initialized = True
            logger.info("Mock Search API service initialized")
        except Exception as e:
            logger.error(f"Error initializing Search API: {e}")
            raise
    
    async def search_venues(self, query: str, location: str = None, capacity: int = None, price_range: str = None) -> Dict[str, Any]:
        """Search for venues."""
        try:
            venues = self.mock_search_data["venues"].copy()
            
            # Filter by query
            if query:
                venues = [v for v in venues if query.lower() in v["name"].lower() or query.lower() in v["location"].lower()]
            
            # Filter by location
            if location:
                venues = [v for v in venues if location.lower() in v["location"].lower()]
            
            # Filter by capacity
            if capacity:
                venues = [v for v in venues if v["capacity"] >= capacity]
            
            # Filter by price range
            if price_range:
                if "budget" in price_range.lower():
                    venues = [v for v in venues if "$2000" in v["price_range"]]
                elif "premium" in price_range.lower():
                    venues = [v for v in venues if "$5000" in v["price_range"]]
            
            # Add search metadata
            for venue in venues:
                venue["search_score"] = random.uniform(0.7, 1.0)
                venue["last_updated"] = datetime.utcnow().isoformat()
            
            # Sort by search score
            venues.sort(key=lambda x: x["search_score"], reverse=True)
            
            self.search_history.append({
                "query": query,
                "type": "venues",
                "results_count": len(venues),
                "timestamp": datetime.utcnow().isoformat()
            })
            
            logger.info(f"Mock venue search: '{query}' returned {len(venues)} results")
            return {
                "success": True,
                "query": query,
                "results": venues,
                "total_results": len(venues)
            }
            
        except Exception as e:
            logger.error(f"Error searching venues: {e}")
            return {"success": False, "error": str(e)}
    
    async def search_vendors(self, query: str, vendor_type: str = None, location: str = None) -> Dict[str, Any]:
        """Search for vendors."""
        try:
            vendors = self.mock_search_data["vendors"].copy()
            
            # Filter by query
            if query:
                vendors = [v for v in vendors if query.lower() in v["name"].lower() or query.lower() in v["type"].lower()]
            
            # Filter by vendor type
            if vendor_type:
                vendors = [v for v in vendors if vendor_type.lower() in v["type"].lower()]
            
            # Filter by location
            if location:
                vendors = [v for v in vendors if location.lower() in v["location"].lower()]
            
            # Add search metadata
            for vendor in vendors:
                vendor["search_score"] = random.uniform(0.7, 1.0)
                vendor["last_updated"] = datetime.utcnow().isoformat()
            
            # Sort by search score
            vendors.sort(key=lambda x: x["search_score"], reverse=True)
            
            self.search_history.append({
                "query": query,
                "type": "vendors",
                "results_count": len(vendors),
                "timestamp": datetime.utcnow().isoformat()
            })
            
            logger.info(f"Mock vendor search: '{query}' returned {len(vendors)} results")
            return {
                "success": True,
                "query": query,
                "results": vendors,
                "total_results": len(vendors)
            }
            
        except Exception as e:
            logger.error(f"Error searching vendors: {e}")
            return {"success": False, "error": str(e)}
    
    async def search_articles(self, query: str, max_results: int = 5) -> Dict[str, Any]:
        """Search for articles and resources."""
        try:
            articles = self.mock_search_data["events"].copy()
            
            # Filter by query
            if query:
                articles = [a for a in articles if query.lower() in a["title"].lower() or query.lower() in a["summary"].lower()]
            
            # Add search metadata
            for article in articles:
                article["search_score"] = random.uniform(0.6, 1.0)
                article["last_updated"] = datetime.utcnow().isoformat()
            
            # Sort by search score and limit results
            articles.sort(key=lambda x: x["search_score"], reverse=True)
            articles = articles[:max_results]
            
            self.search_history.append({
                "query": query,
                "type": "articles",
                "results_count": len(articles),
                "timestamp": datetime.utcnow().isoformat()
            })
            
            logger.info(f"Mock article search: '{query}' returned {len(articles)} results")
            return {
                "success": True,
                "query": query,
                "results": articles,
                "total_results": len(articles)
            }
            
        except Exception as e:
            logger.error(f"Error searching articles: {e}")
            return {"success": False, "error": str(e)}
    
    async def search_trends(self, topic: str, timeframe: str = "30d") -> Dict[str, Any]:
        """Search for trending topics and insights."""
        try:
            # Mock trending data
            trends = [
                {
                    "topic": "Sustainable Events",
                    "trend_score": 0.85,
                    "mentions": 1250,
                    "growth": "+15%",
                    "related_topics": ["Eco-friendly", "Green Events", "Sustainability"]
                },
                {
                    "topic": "Virtual Event Integration",
                    "trend_score": 0.78,
                    "mentions": 980,
                    "growth": "+22%",
                    "related_topics": ["Hybrid Events", "Technology", "Remote Attendance"]
                },
                {
                    "topic": "Micro Weddings",
                    "trend_score": 0.72,
                    "mentions": 750,
                    "growth": "+8%",
                    "related_topics": ["Intimate Weddings", "Small Events", "Personalized"]
                }
            ]
            
            # Filter by topic if provided
            if topic:
                trends = [t for t in trends if topic.lower() in t["topic"].lower()]
            
            logger.info(f"Mock trend search: '{topic}' returned {len(trends)} trends")
            return {
                "success": True,
                "topic": topic,
                "timeframe": timeframe,
                "trends": trends,
                "total_trends": len(trends)
            }
            
        except Exception as e:
            logger.error(f"Error searching trends: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_search_suggestions(self, query: str) -> List[str]:
        """Get search suggestions based on query."""
        try:
            # Mock suggestions based on query
            suggestions = []
            
            if "venue" in query.lower():
                suggestions = ["Wedding Venues", "Corporate Venues", "Outdoor Venues", "Budget Venues"]
            elif "catering" in query.lower():
                suggestions = ["Wedding Catering", "Corporate Catering", "Cocktail Catering", "Budget Catering"]
            elif "photography" in query.lower():
                suggestions = ["Wedding Photography", "Event Photography", "Portrait Photography", "Budget Photography"]
            else:
                suggestions = ["Event Planning", "Wedding Planning", "Corporate Events", "Party Planning"]
            
            logger.info(f"Mock search suggestions for '{query}': {len(suggestions)} suggestions")
            return suggestions
            
        except Exception as e:
            logger.error(f"Error getting search suggestions: {e}")
            return []
    
    async def get_service_stats(self) -> Dict[str, Any]:
        """Get search API service statistics."""
        try:
            return {
                "total_searches": len(self.search_history),
                "venue_searches": len([s for s in self.search_history if s["type"] == "venues"]),
                "vendor_searches": len([s for s in self.search_history if s["type"] == "vendors"]),
                "article_searches": len([s for s in self.search_history if s["type"] == "articles"]),
                "initialized": self.initialized,
                "mock_data_available": {
                    "venues": len(self.mock_search_data["venues"]),
                    "vendors": len(self.mock_search_data["vendors"]),
                    "articles": len(self.mock_search_data["events"])
                }
            }
            
        except Exception as e:
            logger.error(f"Error getting search service stats: {e}")
            return {"error": str(e)}
