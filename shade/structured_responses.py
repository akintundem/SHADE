"""Structured response classes for the Shade AI chat interface."""

from typing import List, Optional, Dict, Any, Union
from pydantic import BaseModel, Field
from enum import Enum


class EventType(str, Enum):
    """Event type enumeration for chips."""
    WEDDING = "wedding"
    BIRTHDAY = "birthday"
    CORPORATE = "corporate"
    CONFERENCE = "conference"
    PARTY = "party"
    MEETING = "meeting"


class Chip(BaseModel):
    """Individual chip component."""
    id: str
    label: str
    icon: Optional[str] = None
    selected: bool = False
    action: Optional[str] = None


class VenueCard(BaseModel):
    """Structured venue card data."""
    id: str
    name: str
    location: str
    image_url: Optional[str] = None
    rating: Optional[float] = None
    review_count: Optional[int] = None
    guest_capacity: str
    price_range: str
    description: Optional[str] = None
    amenities: List[str] = Field(default_factory=list)
    contact_email: Optional[str] = None
    contact_phone: Optional[str] = None
    website: Optional[str] = None


class EmailTemplate(BaseModel):
    """Structured email template data."""
    id: str
    to_name: str
    to_email: str
    subject: str
    message: str
    template_type: str = "inquiry"
    venue_id: Optional[str] = None


class ActionButton(BaseModel):
    """Action button component."""
    id: str
    label: str
    icon: Optional[str] = None
    action: str
    style: str = "primary"  # primary, secondary, outline
    disabled: bool = False


class StructuredResponse(BaseModel):
    """Main structured response container."""
    response_type: str  # "text", "venue_cards", "email_template", "chips", "mixed"
    text: Optional[str] = None
    venue_cards: List[VenueCard] = Field(default_factory=list)
    email_template: Optional[EmailTemplate] = None
    chips: List[Chip] = Field(default_factory=list)
    action_buttons: List[ActionButton] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class ChatMessage(BaseModel):
    """Enhanced chat message with structured content."""
    id: str
    content: str
    is_user: bool
    timestamp: str
    structured_response: Optional[StructuredResponse] = None
    show_chips: bool = False


class EventTypeChips:
    """Predefined event type chips."""
    
    @staticmethod
    def get_default_chips() -> List[Chip]:
        """Get default event type chips."""
        return [
            Chip(
                id="wedding",
                label="Wedding",
                icon="💒",
                selected=False,
                action="select_event_type"
            ),
            Chip(
                id="birthday",
                label="Birthday",
                icon="🎂",
                selected=False,
                action="select_event_type"
            ),
            Chip(
                id="corporate",
                label="Corporate",
                icon="💼",
                selected=False,
                action="select_event_type"
            ),
            Chip(
                id="conference",
                label="Conference",
                icon="🏢",
                selected=False,
                action="select_event_type"
            ),
            Chip(
                id="party",
                label="Party",
                icon="🎉",
                selected=False,
                action="select_event_type"
            )
        ]


class VenueCardBuilder:
    """Builder for creating venue cards."""
    
    @staticmethod
    def create_sample_venues() -> List[VenueCard]:
        """Create sample venue cards for demonstration."""
        return [
            VenueCard(
                id="venue_1",
                name="Grand Ballroom Estate",
                location="Downtown",
                image_url="https://images.unsplash.com/photo-1519167758481-83f1426e4b3e?w=400",
                rating=4.8,
                review_count=203,
                guest_capacity="200-300 guests",
                price_range="$8,000 - $12,000",
                description="Elegant ballroom with crystal chandeliers and marble floors. Perfect for grand celebrations.",
                amenities=["Catering", "Parking", "Audio/Visual", "Bridal Suite"],
                contact_email="events@grandballroom.com",
                contact_phone="(555) 123-4567",
                website="www.grandballroom.com"
            ),
            VenueCard(
                id="venue_2",
                name="Enchanted Garden Estate",
                location="Countryside",
                image_url="https://images.unsplash.com/photo-1519167758481-83f1426e4b3e?w=400",
                rating=4.9,
                review_count=156,
                guest_capacity="150-250 guests",
                price_range="$6,500 - $10,000",
                description="Beautiful outdoor venue with lush gardens and natural beauty.",
                amenities=["Outdoor Ceremony", "Garden", "Parking", "Restrooms"],
                contact_email="info@enchantedgarden.com",
                contact_phone="(555) 234-5678",
                website="www.enchantedgarden.com"
            ),
            VenueCard(
                id="venue_3",
                name="Rustic Barn & Vineyard",
                location="Wine Country",
                image_url="https://images.unsplash.com/photo-1519167758481-83f1426e4b3e?w=400",
                rating=4.7,
                review_count=89,
                guest_capacity="100-200 guests",
                price_range="$5,500 - $9,000",
                description="Charming rustic venue with vineyard views and barn reception space.",
                amenities=["Vineyard Views", "Barn Reception", "Wine Tasting", "Parking"],
                contact_email="events@rusticbarn.com",
                contact_phone="(555) 345-6789",
                website="www.rusticbarn.com"
            )
        ]
    
    @staticmethod
    def create_greek_venues() -> List[VenueCard]:
        """Create Greek wedding venue cards for destination weddings."""
        return [
            VenueCard(
                id="greek_venue_1",
                name="Santorini Cliffside Villa",
                location="Santorini, Greece",
                image_url="https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400",
                rating=4.9,
                review_count=312,
                guest_capacity="50-120 guests",
                price_range="€15,000 - €25,000",
                description="Breathtaking cliffside venue with panoramic Aegean Sea views and iconic white architecture. Perfect for intimate destination weddings.",
                amenities=["Sea Views", "Sunset Ceremony", "Greek Catering", "Photography Spots", "Accommodation"],
                contact_email="weddings@santorinicliff.com",
                contact_phone="+30 2286 123456",
                website="www.santorinicliff.com"
            ),
            VenueCard(
                id="greek_venue_2",
                name="Mykonos Beachfront Estate",
                location="Mykonos, Greece",
                image_url="https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400",
                rating=4.8,
                review_count=267,
                guest_capacity="80-200 guests",
                price_range="€18,000 - €30,000",
                description="Luxurious beachfront estate with private beach access and traditional Cycladic architecture. Ideal for elegant seaside celebrations.",
                amenities=["Private Beach", "Beach Ceremony", "Luxury Accommodation", "Spa Services", "Water Sports"],
                contact_email="events@mykonosbeach.com",
                contact_phone="+30 2289 789012",
                website="www.mykonosbeach.com"
            ),
            VenueCard(
                id="greek_venue_3",
                name="Athens Historic Palace",
                location="Athens, Greece",
                image_url="https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400",
                rating=4.7,
                review_count=189,
                guest_capacity="100-300 guests",
                price_range="€12,000 - €20,000",
                description="Magnificent historic palace in the heart of Athens with ancient Greek architecture and modern amenities. Perfect for grand celebrations.",
                amenities=["Historic Architecture", "City Views", "Museum Access", "Cultural Tours", "Traditional Music"],
                contact_email="weddings@athenspalace.com",
                contact_phone="+30 210 1234567",
                website="www.athenspalace.com"
            ),
            VenueCard(
                id="greek_venue_4",
                name="Crete Mountain Vineyard",
                location="Crete, Greece",
                image_url="https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400",
                rating=4.8,
                review_count=145,
                guest_capacity="60-150 guests",
                price_range="€10,000 - €18,000",
                description="Charming vineyard estate in the Cretan mountains with olive groves and traditional stone buildings. Perfect for rustic-chic celebrations.",
                amenities=["Vineyard Views", "Wine Tasting", "Olive Grove", "Traditional Cooking", "Mountain Views"],
                contact_email="events@cretevineyard.com",
                contact_phone="+30 2810 345678",
                website="www.cretevineyard.com"
            ),
            VenueCard(
                id="greek_venue_5",
                name="Rhodes Medieval Castle",
                location="Rhodes, Greece",
                image_url="https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400",
                rating=4.9,
                review_count=223,
                guest_capacity="40-100 guests",
                price_range="€14,000 - €22,000",
                description="Romantic medieval castle venue with ancient walls and stunning Mediterranean views. Perfect for fairy-tale weddings.",
                amenities=["Medieval Architecture", "Castle Grounds", "Historic Tours", "Sunset Views", "Photography Spots"],
                contact_email="weddings@rhodescastle.com",
                contact_phone="+30 2241 567890",
                website="www.rhodescastle.com"
            )
        ]


class EmailTemplateBuilder:
    """Builder for creating email templates."""
    
    @staticmethod
    def create_venue_inquiry(venue: VenueCard, event_details: Dict[str, Any]) -> EmailTemplate:
        """Create a venue inquiry email template."""
        return EmailTemplate(
            id=f"email_{venue.id}",
            to_name=venue.name,
            to_email=venue.contact_email or "events@venue.com",
            subject=f"Wedding Venue Inquiry - {venue.name}",
            message=f"""Dear {venue.name} Team,

I hope this message finds you well!

I'm currently planning my upcoming wedding and I came across your beautiful venue, {venue.name}. I'm very interested in learning more about availability and would love to schedule a tour.

Event Details:
- Date: {event_details.get('date', 'TBD')}
- Guest Count: {event_details.get('guest_count', 'TBD')}
- Budget: {event_details.get('budget', 'TBD')}

I would appreciate any information you can provide about:
- Availability for my preferred dates
- Pricing and package options
- What's included in your venue rental
- Any special requirements or restrictions

Thank you for your time, and I look forward to hearing from you soon!

Best regards,
{event_details.get('planner_name', 'Event Planner')}""",
            template_type="venue_inquiry",
            venue_id=venue.id
        )


class StructuredResponseBuilder:
    """Builder for creating structured responses."""
    
    @staticmethod
    def create_venue_search_response(venues: List[VenueCard], message: str = None) -> StructuredResponse:
        """Create a structured response for venue search results."""
        return StructuredResponse(
            response_type="venue_cards",
            text=message or f"Here are {len(venues)} amazing venues I found for your event!",
            venue_cards=venues,
            action_buttons=[
                ActionButton(
                    id="search_more",
                    label="Search More",
                    icon="🔍",
                    action="search_venues",
                    style="secondary"
                ),
                ActionButton(
                    id="filter_venues",
                    label="Filter",
                    icon="⚙️",
                    action="filter_venues",
                    style="outline"
                )
            ]
        )
    
    @staticmethod
    def create_email_review_response(email: EmailTemplate) -> StructuredResponse:
        """Create a structured response for email review."""
        return StructuredResponse(
            response_type="email_template",
            text="Here's the email I've prepared for you. Please review before sending:",
            email_template=email,
            action_buttons=[
                ActionButton(
                    id="send_email",
                    label="Send Email",
                    icon="📧",
                    action="send_email",
                    style="primary"
                ),
                ActionButton(
                    id="edit_email",
                    label="Edit",
                    icon="✏️",
                    action="edit_email",
                    style="secondary"
                ),
                ActionButton(
                    id="cancel_email",
                    label="Cancel",
                    icon="❌",
                    action="cancel_email",
                    style="outline"
                )
            ]
        )
    
    @staticmethod
    def create_chips_response(chips: List[Chip], message: str = None) -> StructuredResponse:
        """Create a structured response with chips."""
        return StructuredResponse(
            response_type="chips",
            text=message or "What type of event are you planning?",
            chips=chips
        )
    
    @staticmethod
    def create_mixed_response(
        text: str,
        venue_cards: List[VenueCard] = None,
        email_template: EmailTemplate = None,
        chips: List[Chip] = None,
        action_buttons: List[ActionButton] = None
    ) -> StructuredResponse:
        """Create a mixed structured response."""
        return StructuredResponse(
            response_type="mixed",
            text=text,
            venue_cards=venue_cards or [],
            email_template=email_template,
            chips=chips or [],
            action_buttons=action_buttons or []
        )
