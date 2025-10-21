"""Budget management tools for the Shade agent."""

from langchain.tools import tool
from typing import Dict, Any, Optional, List


@tool
async def create_budget(
    event_id: str,
    total_budget: float,
    currency: str = "USD"
) -> Dict[str, Any]:
    """Create a budget for an event.
    
    Args:
        event_id: ID of the event
        total_budget: Total budget amount
        currency: Currency code (default USD)
    
    Returns:
        Dict with budget creation details
    """
    budget_id = f"budget_{event_id}"
    
    return {
        "success": True,
        "budget_id": budget_id,
        "message": "Budget created successfully",
        "budget": {
            "id": budget_id,
            "event_id": event_id,
            "total_budget": total_budget,
            "currency": currency,
            "allocated": 0.0,
            "remaining": total_budget,
            "line_items": [],
            "status": "ACTIVE"
        }
    }


@tool
async def add_budget_item(
    budget_id: str,
    category: str,
    description: str,
    estimated_cost: float,
    vendor_id: Optional[str] = None,
    is_essential: bool = True
) -> Dict[str, Any]:
    """Add a line item to the budget.
    
    Args:
        budget_id: ID of the budget
        category: Category (venue, catering, photography, etc.)
        description: Description of the item
        estimated_cost: Estimated cost
        vendor_id: Optional vendor ID if already selected
        is_essential: Whether this is an essential item
    
    Returns:
        Dict with budget item details
    """
    item_id = f"item_{budget_id}_{category}"
    
    return {
        "success": True,
        "item_id": item_id,
        "message": "Budget item added successfully",
        "budget_item": {
            "id": item_id,
            "budget_id": budget_id,
            "category": category,
            "description": description,
            "estimated_cost": estimated_cost,
            "actual_cost": None,
            "vendor_id": vendor_id,
            "is_essential": is_essential,
            "status": "PLANNED"
        }
    }


@tool
async def calculate_budget(budget_id: str) -> Dict[str, Any]:
    """Calculate budget totals and remaining amount.
    
    Args:
        budget_id: ID of the budget to calculate
    
    Returns:
        Dict with budget calculations
    """
    # Mock budget calculation
    return {
        "success": True,
        "budget_id": budget_id,
        "calculations": {
            "total_budget": 50000.0,
            "allocated": 35000.0,
            "remaining": 15000.0,
            "percentage_used": 70.0,
            "line_items": [
                {
                    "category": "venue",
                    "allocated": 15000.0,
                    "percentage": 30.0
                },
                {
                    "category": "catering",
                    "allocated": 12000.0,
                    "percentage": 24.0
                },
                {
                    "category": "photography",
                    "allocated": 3000.0,
                    "percentage": 6.0
                },
                {
                    "category": "flowers",
                    "allocated": 2000.0,
                    "percentage": 4.0
                },
                {
                    "category": "music",
                    "allocated": 2000.0,
                    "percentage": 4.0
                },
                {
                    "category": "miscellaneous",
                    "allocated": 1000.0,
                    "percentage": 2.0
                }
            ]
        }
    }


@tool
async def track_payment(
    budget_id: str,
    item_id: str,
    amount: float,
    payment_type: str,
    payment_date: str,
    notes: Optional[str] = None
) -> Dict[str, Any]:
    """Track a payment made for a budget item.
    
    Args:
        budget_id: ID of the budget
        item_id: ID of the budget item
        amount: Payment amount
        payment_type: Type of payment (deposit, final, partial)
        payment_date: Date of payment (YYYY-MM-DD)
        notes: Optional notes about the payment
    
    Returns:
        Dict with payment tracking details
    """
    payment_id = f"payment_{item_id}_{payment_date}"
    
    return {
        "success": True,
        "payment_id": payment_id,
        "message": "Payment tracked successfully",
        "payment": {
            "id": payment_id,
            "budget_id": budget_id,
            "item_id": item_id,
            "amount": amount,
            "payment_type": payment_type,
            "payment_date": payment_date,
            "notes": notes,
            "status": "RECORDED"
        }
    }
