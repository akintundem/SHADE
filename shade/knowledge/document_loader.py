"""Mock Document Loader for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
import json
from datetime import datetime

logger = logging.getLogger(__name__)


class DocumentLoader:
    """Mock Document Loader for prototyping - returns consistent domain documents."""
    
    def __init__(self):
        """Initialize mock document loader."""
        self.domain_documents = {}
        self._initialize_domain_documents()
    
    def _initialize_domain_documents(self):
        """Initialize with mock domain documents."""
        
        # Event Planning Documents
        self.domain_documents["event"] = [
            {
                "content": "Event planning requires careful timeline management. Start planning 6-12 months in advance for large events, 3-6 months for medium events, and 1-3 months for small events.",
                "metadata": {"type": "timeline", "category": "planning", "domain": "event"},
                "source": "event_planning_guide.pdf"
            },
            {
                "content": "Key event planning phases: 1) Conceptualization, 2) Planning, 3) Coordination, 4) Execution, 5) Evaluation. Each phase has specific deliverables and timelines.",
                "metadata": {"type": "process", "category": "methodology", "domain": "event"},
                "source": "event_methodology.pdf"
            },
            {
                "content": "Event objectives should be SMART: Specific, Measurable, Achievable, Relevant, and Time-bound. Clear objectives guide all planning decisions.",
                "metadata": {"type": "objectives", "category": "strategy", "domain": "event"},
                "source": "event_strategy.pdf"
            },
            {
                "content": "Event success metrics include attendance rate, engagement level, feedback scores, ROI, and achievement of stated objectives.",
                "metadata": {"type": "metrics", "category": "evaluation", "domain": "event"},
                "source": "event_metrics.pdf"
            }
        ]
        
        # Budget Planning Documents
        self.domain_documents["budget"] = [
            {
                "content": "Event budgets typically allocate: 40-50% for venue and catering, 20-30% for entertainment and activities, 10-15% for decorations, 10-15% for miscellaneous expenses.",
                "metadata": {"type": "allocation", "category": "budget_structure", "domain": "budget"},
                "source": "budget_allocation_guide.pdf"
            },
            {
                "content": "Budget contingency should be 10-20% of total budget to handle unexpected costs and last-minute changes.",
                "metadata": {"type": "contingency", "category": "risk_management", "domain": "budget"},
                "source": "budget_contingency.pdf"
            },
            {
                "content": "Cost categories include: venue rental, catering, entertainment, decorations, transportation, staff, marketing, insurance, permits, and miscellaneous expenses.",
                "metadata": {"type": "categories", "category": "expense_types", "domain": "budget"},
                "source": "cost_categories.pdf"
            },
            {
                "content": "Budget tracking should be done weekly during planning and daily during event execution. Use spreadsheets or budget management software.",
                "metadata": {"type": "tracking", "category": "management", "domain": "budget"},
                "source": "budget_tracking.pdf"
            }
        ]
        
        # Venue Selection Documents
        self.domain_documents["venue"] = [
            {
                "content": "Venue selection criteria: capacity, location, accessibility, amenities, parking, catering options, technical capabilities, and cost.",
                "metadata": {"type": "criteria", "category": "selection", "domain": "venue"},
                "source": "venue_selection_guide.pdf"
            },
            {
                "content": "Venue types include: hotels, conference centers, restaurants, outdoor spaces, private homes, museums, theaters, and community centers.",
                "metadata": {"type": "types", "category": "classification", "domain": "venue"},
                "source": "venue_types.pdf"
            },
            {
                "content": "Venue booking timeline: 6-12 months for popular venues, 3-6 months for standard venues, 1-3 months for last-minute bookings.",
                "metadata": {"type": "timeline", "category": "booking", "domain": "venue"},
                "source": "venue_booking.pdf"
            }
        ]
        
        # Vendor Management Documents
        self.domain_documents["vendor"] = [
            {
                "content": "Vendor categories: catering, entertainment, photography, decorations, transportation, security, and technical services.",
                "metadata": {"type": "categories", "category": "classification", "domain": "vendor"},
                "source": "vendor_categories.pdf"
            },
            {
                "content": "Vendor evaluation criteria: experience, reputation, pricing, availability, quality, reliability, and communication.",
                "metadata": {"type": "evaluation", "category": "selection", "domain": "vendor"},
                "source": "vendor_evaluation.pdf"
            },
            {
                "content": "Vendor contract essentials: scope of work, pricing, timeline, cancellation policy, insurance requirements, and payment terms.",
                "metadata": {"type": "contracts", "category": "legal", "domain": "vendor"},
                "source": "vendor_contracts.pdf"
            }
        ]
        
        # Risk Management Documents
        self.domain_documents["risk"] = [
            {
                "content": "Common event risks: weather, vendor no-shows, technical failures, security issues, health emergencies, and capacity overruns.",
                "metadata": {"type": "risks", "category": "identification", "domain": "risk"},
                "source": "risk_identification.pdf"
            },
            {
                "content": "Risk mitigation strategies: backup plans, insurance, contracts, monitoring systems, and emergency procedures.",
                "metadata": {"type": "mitigation", "category": "strategies", "domain": "risk"},
                "source": "risk_mitigation.pdf"
            },
            {
                "content": "Emergency response plan should include: contact information, evacuation procedures, medical protocols, and communication channels.",
                "metadata": {"type": "emergency", "category": "response", "domain": "risk"},
                "source": "emergency_response.pdf"
            }
        ]
        
        # Attendee Management Documents
        self.domain_documents["attendee"] = [
            {
                "content": "Guest management includes: invitation design, RSVP tracking, dietary restrictions, accessibility needs, and communication preferences.",
                "metadata": {"type": "management", "category": "process", "domain": "attendee"},
                "source": "guest_management.pdf"
            },
            {
                "content": "RSVP best practices: clear deadlines, multiple response channels, follow-up reminders, and capacity management.",
                "metadata": {"type": "rsvp", "category": "best_practices", "domain": "attendee"},
                "source": "rsvp_management.pdf"
            },
            {
                "content": "Guest experience planning: welcome procedures, seating arrangements, entertainment, food service, and departure logistics.",
                "metadata": {"type": "experience", "category": "planning", "domain": "attendee"},
                "source": "guest_experience.pdf"
            }
        ]
        
        # Weather Planning Documents
        self.domain_documents["weather"] = [
            {
                "content": "Weather considerations for events: temperature, precipitation, wind, humidity, and seasonal patterns.",
                "metadata": {"type": "factors", "category": "planning", "domain": "weather"},
                "source": "weather_considerations.pdf"
            },
            {
                "content": "Outdoor event weather contingencies: backup indoor venues, weather monitoring, and emergency procedures.",
                "metadata": {"type": "contingencies", "category": "risk_management", "domain": "weather"},
                "source": "weather_contingencies.pdf"
            },
            {
                "content": "Weather monitoring tools: weather apps, professional services, local forecasts, and historical data analysis.",
                "metadata": {"type": "monitoring", "category": "tools", "domain": "weather"},
                "source": "weather_monitoring.pdf"
            }
        ]
        
        # Communication Documents
        self.domain_documents["outreach"] = [
            {
                "content": "Communication channels: email, SMS, social media, phone calls, and in-person meetings.",
                "metadata": {"type": "channels", "category": "methods", "domain": "outreach"},
                "source": "communication_channels.pdf"
            },
            {
                "content": "Message timing: advance notices, reminders, updates, and follow-ups should be strategically timed.",
                "metadata": {"type": "timing", "category": "strategy", "domain": "outreach"},
                "source": "message_timing.pdf"
            },
            {
                "content": "Crisis communication: rapid response protocols, stakeholder notification, media management, and reputation protection.",
                "metadata": {"type": "crisis", "category": "emergency", "domain": "outreach"},
                "source": "crisis_communication.pdf"
            }
        ]
    
    async def load_domain_documents(self, domain: str) -> List[Dict[str, Any]]:
        """Load documents for a specific domain."""
        try:
            if domain not in self.domain_documents:
                logger.warning(f"No documents found for domain: {domain}")
                return []
            
            documents = self.domain_documents[domain].copy()
            
            # Add metadata
            for doc in documents:
                doc["loaded_at"] = datetime.utcnow().isoformat()
                doc["domain"] = domain
            
            logger.info(f"Loaded {len(documents)} documents for {domain} domain")
            return documents
            
        except Exception as e:
            logger.error(f"Error loading documents for {domain}: {e}")
            return []
    
    async def load_all_documents(self) -> Dict[str, List[Dict[str, Any]]]:
        """Load documents for all domains."""
        try:
            all_documents = {}
            
            for domain in self.domain_documents.keys():
                all_documents[domain] = await self.load_domain_documents(domain)
            
            total_documents = sum(len(docs) for docs in all_documents.values())
            logger.info(f"Loaded {total_documents} documents across {len(all_documents)} domains")
            
            return all_documents
            
        except Exception as e:
            logger.error(f"Error loading all documents: {e}")
            return {}
    
    async def search_documents(self, domain: str, query: str, max_results: int = 5) -> List[Dict[str, Any]]:
        """Search documents within a domain."""
        try:
            if domain not in self.domain_documents:
                return []
            
            documents = self.domain_documents[domain]
            query_lower = query.lower()
            
            # Simple keyword matching for prototyping
            matching_docs = []
            for doc in documents:
                content_lower = doc["content"].lower()
                if any(keyword in content_lower for keyword in query_lower.split()):
                    matching_docs.append(doc)
            
            # Sort by relevance (simple keyword count)
            def relevance_score(doc):
                content_lower = doc["content"].lower()
                return sum(1 for keyword in query_lower.split() if keyword in content_lower)
            
            matching_docs.sort(key=relevance_score, reverse=True)
            
            results = matching_docs[:max_results]
            logger.info(f"Found {len(results)} matching documents for '{query}' in {domain}")
            
            return results
            
        except Exception as e:
            logger.error(f"Error searching documents: {e}")
            return []
    
    async def get_domain_stats(self) -> Dict[str, Any]:
        """Get statistics for all domains."""
        try:
            stats = {}
            total_documents = 0
            
            for domain, documents in self.domain_documents.items():
                domain_stats = {
                    "document_count": len(documents),
                    "sources": list(set(doc.get("source", "unknown") for doc in documents)),
                    "categories": list(set(doc.get("metadata", {}).get("category", "unknown") for doc in documents))
                }
                stats[domain] = domain_stats
                total_documents += len(documents)
            
            stats["total_documents"] = total_documents
            stats["total_domains"] = len(self.domain_documents)
            
            return stats
            
        except Exception as e:
            logger.error(f"Error getting domain stats: {e}")
            return {"error": str(e)}
