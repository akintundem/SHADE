"""Event planning tools for the Shade agent."""

try:
    from .event_tools import (
        create_event,
        update_event,
        get_event,
        list_events,
        delete_event
    )

    from .venue_tools import (
        search_venues,
        get_venue_details,
        book_venue
    )

    from .vendor_tools import (
        search_vendors,
        get_vendor_quote,
        book_vendor,
        list_vendor_services
    )

    from .budget_tools import (
        create_budget,
        add_budget_item,
        calculate_budget,
        track_payment
    )

    from .attendee_tools import (
        add_attendee,
        send_invitations,
        track_rsvps,
        manage_guest_list
    )

    from .timeline_tools import (
        create_timeline,
        update_timeline,
        get_timeline
    )

    from .risk_tools import (
        assess_risks,
        check_weather
    )

    from .communication_tools import (
        send_notification,
        generate_report
    )
except ImportError as e:
    print(f"Warning: Could not import some tools: {e}")
    # Create empty lists for tools that couldn't be imported
    create_event = update_event = get_event = list_events = delete_event = None
    search_venues = get_venue_details = book_venue = None
    search_vendors = get_vendor_quote = book_vendor = list_vendor_services = None
    create_budget = add_budget_item = calculate_budget = track_payment = None
    add_attendee = send_invitations = track_rsvps = manage_guest_list = None
    create_timeline = update_timeline = get_timeline = None
    assess_risks = check_weather = None
    send_notification = generate_report = None

# All tools list for easy import (filter out None values)
ALL_TOOLS = [
    tool for tool in [
        # Event tools
        create_event,
        update_event,
        get_event,
        list_events,
        delete_event,
        
        # Venue tools
        search_venues,
        get_venue_details,
        book_venue,
        
        # Vendor tools
        search_vendors,
        get_vendor_quote,
        book_vendor,
        list_vendor_services,
        
        # Budget tools
        create_budget,
        add_budget_item,
        calculate_budget,
        track_payment,
        
        # Attendee tools
        add_attendee,
        send_invitations,
        track_rsvps,
        manage_guest_list,
        
        # Timeline tools
        create_timeline,
        update_timeline,
        get_timeline,
        
        # Risk tools
        assess_risks,
        check_weather,
        
        # Communication tools
        send_notification,
        generate_report
    ] if tool is not None
]

__all__ = ["ALL_TOOLS"]
