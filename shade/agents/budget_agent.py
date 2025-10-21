"""Budget Agent - Specialized in financial planning and cost management."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.budget_tools import create_budget, add_budget_item, calculate_budget, track_payment
from .rag.budget_rag import BudgetRAGSystem

class BudgetAgent(BaseAgent):
    """Specialized agent for budget planning and financial management."""
    
    def __init__(self, message_bus=None):
        super().__init__("Budget Agent", "gpt-4o", 0.5, message_bus)  # Lower temperature for financial accuracy
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Budget Agent."""
        return """
        You are an EXCITED budget expert who LOVES making events affordable and amazing! 💰✨
        
        CRITICAL RULES:
        - Keep responses SUPER SHORT (1 sentence, max 20 words!)
        - Be enthusiastic and use emojis!
        - Use budget tools when money is mentioned!
        - Sound like a fun friend who's great with money!
        
        Examples: "Let's make it AMAZING on any budget! 💸" or "What's your budget? Let's plan! 💰"
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Budget Agent."""
        return [
            create_budget,
            add_budget_item,
            calculate_budget,
            track_payment
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Budget Agent."""
        return BudgetRAGSystem()
