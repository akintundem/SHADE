"""Agent state definition for LangGraph."""

from typing import TypedDict, List, Dict, Any, Optional, Annotated, Sequence
from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages


class AgentState(TypedDict):
    """State for the Shade event planning agent."""
    
    # Core conversation state
    messages: Annotated[Sequence[BaseMessage], add_messages]
    
    # Current context
    current_event_id: Optional[str]
    current_user_id: Optional[str]
    current_chat_id: Optional[str]
    
    # Planning state
    pending_actions: List[Dict[str, Any]]
    user_preferences: Dict[str, Any]  # Budget range, style, etc.
    requires_approval: bool
    approval_pending: Optional[Dict[str, Any]]
    
    # Tool execution state
    tool_history: List[Dict[str, Any]]
    last_tool_results: List[Dict[str, Any]]
    
    # Memory state
    conversation_summary: Optional[str]
    mentioned_entities: Dict[str, List[str]]  # venues, vendors, dates, etc.
    
    # Multi-step planning
    current_plan: Optional[List[Dict[str, Any]]]
    plan_step: int
    plan_complete: bool
    
    # Error handling
    retry_count: int
    last_error: Optional[str]
