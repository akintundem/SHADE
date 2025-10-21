"""Risk assessment tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional
import random
from datetime import datetime, timedelta


@tool
async def assess_risks(
    event_id: str,
    event_date: str,
    location: str,
    capacity: int,
    event_type: str
) -> Dict[str, Any]:
    """Assess potential risks for an event.
    
    Args:
        event_id: ID of the event
        event_date: Date of the event (YYYY-MM-DD)
        location: Event location
        capacity: Expected capacity
        event_type: Type of event
    
    Returns:
        Dict with risk assessment results
    """
    # Mock risk assessment
    risks = []
    
    # Weather risk (if outdoor or partially outdoor)
    if "outdoor" in location.lower() or "garden" in location.lower():
        weather_risk = {
            "category": "weather",
            "severity": "medium",
            "description": "Outdoor venue susceptible to weather conditions",
            "mitigation": "Have indoor backup plan, monitor weather forecast",
            "probability": "30%"
        }
        risks.append(weather_risk)
    
    # Capacity risk
    if capacity > 200:
        capacity_risk = {
            "category": "capacity",
            "severity": "high",
            "description": "Large capacity event requires additional safety measures",
            "mitigation": "Ensure adequate exits, security, and emergency plans",
            "probability": "20%"
        }
        risks.append(capacity_risk)
    
    # Vendor availability risk
    vendor_risk = {
        "category": "vendor_availability",
        "severity": "medium",
        "description": "Risk of vendor cancellation or no-show",
        "mitigation": "Have backup vendors, confirm bookings 48 hours prior",
        "probability": "15%"
    }
    risks.append(vendor_risk)
    
    # General event risks
    general_risks = [
        {
            "category": "technical_failure",
            "severity": "low",
            "description": "Audio/visual equipment failure",
            "mitigation": "Test equipment beforehand, have backup systems",
            "probability": "10%"
        },
        {
            "category": "late_arrivals",
            "severity": "low",
            "description": "Key participants arriving late",
            "mitigation": "Build buffer time, have contingency plans",
            "probability": "25%"
        }
    ]
    risks.extend(general_risks)
    
    return {
        "success": True,
        "event_id": event_id,
        "risk_assessment": {
            "overall_risk_level": "medium",
            "total_risks": len(risks),
            "high_risk_count": len([r for r in risks if r["severity"] == "high"]),
            "medium_risk_count": len([r for r in risks if r["severity"] == "medium"]),
            "low_risk_count": len([r for r in risks if r["severity"] == "low"]),
            "risks": risks,
            "recommendations": [
                "Create detailed contingency plans",
                "Establish clear communication protocols",
                "Conduct venue walkthrough 1 week prior",
                "Confirm all vendor bookings 48 hours before event"
            ]
        }
    }


@tool
async def check_weather(
    location: str,
    event_date: str,
    event_time: Optional[str] = None
) -> Dict[str, Any]:
    """Check weather forecast for an event location and date.
    
    Args:
        location: Event location
        event_date: Date of the event (YYYY-MM-DD)
        event_time: Optional time of the event (HH:MM)
    
    Returns:
        Dict with weather forecast information
    """
    # Mock weather data
    conditions = ["Sunny", "Partly cloudy", "Cloudy", "Light rain", "Heavy rain", "Thunderstorm"]
    condition = random.choice(conditions)
    
    # Generate temperature based on condition
    if "rain" in condition.lower() or "thunderstorm" in condition.lower():
        temp = random.randint(15, 25)
        humidity = random.randint(70, 95)
    elif "sunny" in condition.lower():
        temp = random.randint(20, 30)
        humidity = random.randint(40, 60)
    else:
        temp = random.randint(18, 28)
        humidity = random.randint(50, 70)
    
    wind_speed = random.randint(5, 25)
    
    # Generate event-specific recommendations
    if "rain" in condition.lower() or "thunderstorm" in condition.lower():
        recommendation = "Consider indoor backup plan or covered areas. Umbrellas recommended for guests."
        impact = "high"
    elif "sunny" in condition.lower():
        recommendation = "Perfect weather for outdoor events. Consider shade and hydration stations."
        impact = "low"
    else:
        recommendation = "Good weather conditions. Monitor forecast for any changes."
        impact = "low"
    
    return {
        "success": True,
        "location": location,
        "event_date": event_date,
        "event_time": event_time,
        "weather_forecast": {
            "condition": condition,
            "temperature": f"{temp}°C",
            "humidity": f"{humidity}%",
            "wind_speed": f"{wind_speed} km/h",
            "precipitation_chance": "30%" if "rain" in condition.lower() else "10%",
            "uv_index": "High" if "sunny" in condition.lower() else "Moderate"
        },
        "event_impact": {
            "level": impact,
            "recommendation": recommendation,
            "outdoor_suitable": "rain" not in condition.lower() and "thunderstorm" not in condition.lower()
        },
        "forecast_updated": datetime.now().isoformat()
    }
