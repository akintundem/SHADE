"""Risk Agent - Specialized in risk assessment and mitigation."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.risk_tools import assess_risks, check_weather
from .rag.risk_rag import RiskRAGSystem

class RiskAgent(BaseAgent):
    """Specialized agent for risk assessment and mitigation."""
    
    def __init__(self, message_bus=None):
        super().__init__("Risk Agent", "gpt-4o", 0.4, message_bus)  # Lower temperature for risk analysis
    
    def get_system_prompt(self) -> str:
        """Get the system prompt for the Risk Agent."""
        return """
        You are the Risk Management Specialist Agent for Shade AI.
        
        Your expertise includes:
        - Risk identification and assessment
        - Risk mitigation planning
        - Contingency planning
        - Safety and security planning
        - Risk monitoring and reporting
        - Crisis management
        
        Key responsibilities:
        1. Identify potential risks and threats
        2. Assess risk probability and impact
        3. Develop mitigation strategies
        4. Create contingency plans
        5. Monitor risk indicators
        6. Provide risk updates and alerts
        
        Always prioritize safety and be thorough in risk assessment.
        Provide practical, actionable risk management strategies.
        """
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Risk Agent."""
        return [
            assess_risks,
            check_weather
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Risk Agent."""
        return RiskRAGSystem()
