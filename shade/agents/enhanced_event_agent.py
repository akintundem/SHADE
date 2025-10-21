"""Enhanced Event Agent - More conversational and intelligent event planning."""

from typing import Dict, Any, List
from .base_agent import BaseAgent
from tools.enhanced_event_tools import (
    start_event_creation,
    enhance_event_details,
    get_event_info,
    get_current_event_status,
    check_event_weather,
    search_venues_google
)
from .rag.event_rag import EventRAGSystem

class EnhancedEventAgent(BaseAgent):
    """Enhanced event planning agent with better conversation flow."""
    
    def __init__(self, message_bus=None):
        super().__init__("Enhanced Event Agent", "gpt-4o", 0.7, message_bus)
    
    def get_system_prompt(self, context: Dict[str, Any] = None) -> str:
        """Get the system prompt for the Enhanced Event Agent with dynamic context."""
        base_prompt = """
        You are a professional event planner who creates amazing events through natural conversation! 🎉
        
        **YOUR WORKFLOW:**
        
        1. **CREATE EVENT (Core Info First):**
           - Ask for: name, event type, start date/time
           - Use `start_event_creation` tool to create immediately
           - Give human-like confirmation (like writing in your planner's journal)
           - Then ask what else they'd like to add
        
        2. **ENHANCE EVENT (Add Details):**
           - Use `enhance_event_details` to add more info
           - Ask about: capacity, theme, venue needs, technical requirements, etc.
           - Make it conversational: "What kind of venue are you thinking?"
        
        3. **ANSWER QUESTIONS:**
           - Use `get_event_info` for natural language questions
           - "What date is my event?" → "Your event is scheduled for..."
           - "How many guests?" → "Your event can accommodate..."
           - If info is missing, suggest adding it
        
        4. **CHECK STATUS:**
           - Use `get_current_event_status` to see what's planned
           - Show what's missing and suggest next steps
        
        **CONVERSATION STYLE:**
        - Be warm and professional like a real event planner
        - Use emojis sparingly (1-2 max per message)
        - Write confirmations like journal entries
        - Ask ONE question at a time
        - Remember context from the conversation
        - If they ask about something not set, offer to add it
        
        **EXAMPLES:**
        - "Perfect! I've created your birthday party for October 26th. What theme are you thinking?"
        - "Your event can accommodate 50 guests. Would you like to increase the capacity?"
        - "I don't have a venue set yet. What kind of space are you looking for?"
        
        **RULES:**
        - Always create the event first with core info
        - Then enhance with additional details
        - Be helpful and suggest next steps
        - Keep responses conversational and friendly
        """
        
        # Add dynamic context if available
        if context:
            dynamic_context = self._build_dynamic_context(context)
            if dynamic_context:
                base_prompt += f"\n\n**CURRENT CONTEXT:**\n{dynamic_context}"
        
        return base_prompt
    
    def _build_dynamic_context(self, context: Dict[str, Any]) -> str:
        """Build dynamic context for the system prompt."""
        context_parts = []
        
        # Add event type specific guidance
        event_type = context.get("event_type")
        if event_type:
            event_guidance = self._get_event_type_guidance(event_type)
            if event_guidance:
                context_parts.append(f"**{event_type} PLANNING GUIDANCE:**\n{event_guidance}")
        
        # Add learned patterns
        learned_patterns = context.get("learned_patterns", [])
        if learned_patterns:
            patterns_text = "\n".join([f"- {pattern}" for pattern in learned_patterns[:3]])
            context_parts.append(f"**SUCCESSFUL PATTERNS:**\n{patterns_text}")
        
        # Add validation context
        validation_context = context.get("validation_context")
        if validation_context:
            context_parts.append(f"**VALIDATION NOTES:**\n{validation_context}")
        
        # Add workflow context
        workflow_context = context.get("workflow_context")
        if workflow_context:
            context_parts.append(f"**WORKFLOW STATUS:**\n{workflow_context}")
        
        return "\n\n".join(context_parts)
    
    def _get_event_type_guidance(self, event_type: str) -> str:
        """Get event type specific guidance."""
        guidance = {
            "WEDDING": """
            - Start with guest count and budget
            - Book venue 12-18 months in advance
            - Consider photographer, catering, and flowers
            - Plan for weather backup if outdoor ceremony
            - Send save-the-dates 6-8 months before
            """,
            "CONFERENCE": """
            - Define technical requirements early
            - Book venue 6-12 months in advance
            - Plan for dietary restrictions
            - Test all AV equipment beforehand
            - Consider accessibility needs
            """,
            "BIRTHDAY": """
            - Choose age-appropriate theme
            - Book entertainment 2-4 weeks ahead
            - Plan activities for the age group
            - Consider dietary restrictions
            - Send invitations 2-3 weeks before
            """,
            "CORPORATE": """
            - Define objectives and agenda
            - Book venue 3-6 months in advance
            - Plan for security if needed
            - Consider parking and transportation
            - Have backup technical support
            """
        }
        return guidance.get(event_type, "")
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the Enhanced Event Agent."""
        return [
            start_event_creation,
            enhance_event_details,
            get_event_info,
            get_current_event_status,
            check_event_weather,
            search_venues_google
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the Enhanced Event Agent."""
        return EventRAGSystem()
