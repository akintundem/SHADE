"""LangGraph StateGraph definition for the Shade agent."""

from langgraph.graph import StateGraph, END
# from langgraph.checkpoint.mongodb import MongoDBSaver  # Not available yet
from langchain_openai import ChatOpenAI
from .state import AgentState
from .nodes import AgentNodes, should_continue, should_get_approval
from tools import ALL_TOOLS
from mongodb_service import mongodb_service
import os


def create_agent_graph():
    """Create and configure the LangGraph StateGraph."""
    
    # Initialize components
    agent_nodes = AgentNodes()
    model = ChatOpenAI(model="gpt-4o", temperature=0.7)
    
    # For now, use no checkpointer (in-memory only)
    # MongoDB checkpointer will be added when available
    checkpointer = None
    
    # Create the graph
    workflow = StateGraph(AgentState)
    
    # Add nodes
    workflow.add_node("agent", agent_nodes.agent_node)
    workflow.add_node("tools", agent_nodes.tool_node)
    workflow.add_node("supervisor", agent_nodes.supervisor_node)
    workflow.add_node("approval", agent_nodes.approval_node)
    
    # Set entry point
    workflow.set_entry_point("agent")
    
    # Add conditional edges
    workflow.add_conditional_edges(
        "agent",
        should_continue,
        {
            "tools": "tools",
            "supervisor": "supervisor", 
            "approval": "approval",
            "end": END
        }
    )
    
    # Add edges
    workflow.add_edge("tools", "agent")
    workflow.add_edge("supervisor", "agent")
    workflow.add_edge("approval", "agent")
    
    # Compile the graph
    if checkpointer:
        app = workflow.compile(checkpointer=checkpointer)
    else:
        app = workflow.compile()
    
    return app


def create_simple_agent_graph():
    """Create a simplified agent graph without checkpointer for testing."""
    
    # Initialize components
    agent_nodes = AgentNodes()
    
    # Create the graph
    workflow = StateGraph(AgentState)
    
    # Add nodes
    workflow.add_node("agent", agent_nodes.agent_node)
    workflow.add_node("tools", agent_nodes.tool_node)
    
    # Set entry point
    workflow.set_entry_point("agent")
    
    # Add conditional edges
    workflow.add_conditional_edges(
        "agent",
        should_continue,
        {
            "tools": "tools",
            "end": END
        }
    )
    
    # Add edges
    workflow.add_edge("tools", "agent")
    
    # Compile the graph with increased recursion limit
    app = workflow.compile()
    
    # Set recursion limit to prevent infinite loops
    app.config = {"recursion_limit": 50}
    
    return app
