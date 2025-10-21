"""Agent nodes for the LangGraph workflow."""

from typing import Dict, Any, List
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.prebuilt import ToolNode
from .state import AgentState
from .prompts import SHADE_SYSTEM_PROMPT, SUPERVISOR_PROMPT
from tools import ALL_TOOLS
from memory import ConversationMemory, EntityMemory


class AgentNodes:
    """Agent nodes for the LangGraph workflow."""
    
    def __init__(self):
        self.model = ChatOpenAI(model="gpt-4o", temperature=0.7)
        self.model_with_tools = self.model.bind_tools(ALL_TOOLS)
        self.tool_node = ToolNode(ALL_TOOLS)
        self.conversation_memory = ConversationMemory()
        self.entity_memory = EntityMemory()
    
    async def agent_node(self, state: AgentState) -> Dict[str, Any]:
        """Main agent node that processes messages and decides on actions."""
        try:
            # Increment retry count to prevent infinite loops
            retry_count = state.get("retry_count", 0) + 1
            
            # Get current messages
            messages = state.get("messages", [])
            if not messages:
                return {"messages": [], "retry_count": retry_count}
            
            # Add system message if not present
            if not messages or not isinstance(messages[0], SystemMessage):
                system_msg = SystemMessage(content=SHADE_SYSTEM_PROMPT)
                messages = [system_msg] + list(messages)
            
            # Get context from memory
            chat_id = state.get("current_chat_id")
            if chat_id:
                # Get mentioned entities for context
                mentioned_entities = await self.conversation_memory.get_mentioned_entities(chat_id)
                if mentioned_entities:
                    context_msg = f"Context from previous conversation: {mentioned_entities}"
                    messages.append(SystemMessage(content=context_msg))
            
            # Call the model
            response = await self.model_with_tools.ainvoke(messages)
            
            # Save tool results to memory if any
            if hasattr(response, 'tool_calls') and response.tool_calls:
                for tool_call in response.tool_calls:
                    await self.conversation_memory.save_tool_result(
                        chat_id, 
                        tool_call["name"], 
                        {"tool_call": tool_call}
                    )
            
            return {"messages": [response], "retry_count": retry_count}
            
        except Exception as e:
            error_msg = AIMessage(content=f"I encountered an error: {str(e)}. Let me try again.")
            return {"messages": [error_msg], "retry_count": retry_count}
    
    async def supervisor_node(self, state: AgentState) -> Dict[str, Any]:
        """Supervisor node for multi-step planning."""
        try:
            messages = state.get("messages", [])
            if not messages:
                return {"messages": []}
            
            # Create supervisor prompt
            last_message = messages[-1] if messages else None
            if last_message and hasattr(last_message, 'content'):
                user_request = last_message.content
            else:
                user_request = "Plan an event"
            
            supervisor_prompt = f"""
            {SUPERVISOR_PROMPT}
            
            User Request: {user_request}
            
            Please break this down into a step-by-step plan that the agent can execute.
            Return the plan as a JSON list of steps, each with:
            - step_number: int
            - action: string (what to do)
            - tool: string (which tool to use)
            - parameters: dict (parameters for the tool)
            - description: string (why this step is needed)
            """
            
            # Use a separate model for planning
            planning_model = ChatOpenAI(model="gpt-4o", temperature=0.3)
            response = await planning_model.ainvoke([
                SystemMessage(content=supervisor_prompt),
                HumanMessage(content=user_request)
            ])
            
            # Parse the plan (simplified for now)
            plan = [
                {
                    "step_number": 1,
                    "action": "Create event",
                    "tool": "create_event",
                    "parameters": {"name": "Sample Event", "event_type": "CONFERENCE"},
                    "description": "Create the main event"
                },
                {
                    "step_number": 2,
                    "action": "Search venues",
                    "tool": "search_venues",
                    "parameters": {"location": "Downtown", "capacity": 100},
                    "description": "Find suitable venues"
                }
            ]
            
            # Update state with plan
            state["current_plan"] = plan
            state["plan_step"] = 0
            state["plan_complete"] = False
            
            plan_message = AIMessage(content=f"I've created a plan for your request. Here's what I'll do:\n\n" + 
                                   "\n".join([f"{step['step_number']}. {step['description']}" for step in plan]))
            
            return {
                "messages": [plan_message],
                "current_plan": plan,
                "plan_step": 0,
                "plan_complete": False
            }
            
        except Exception as e:
            error_msg = AIMessage(content=f"I had trouble creating a plan: {str(e)}. Let me try a simpler approach.")
            return {"messages": [error_msg]}
    
    async def approval_node(self, state: AgentState) -> Dict[str, Any]:
        """Approval node for human-in-the-loop decisions."""
        try:
            pending_actions = state.get("pending_actions", [])
            if not pending_actions:
                return {"messages": []}
            
            action = pending_actions[0]
            approval_message = AIMessage(
                content=f"I need your approval for this action: {action.get('description', 'Unknown action')}. "
                       f"Please confirm if you'd like me to proceed."
            )
            
            return {
                "messages": [approval_message],
                "requires_approval": True,
                "approval_pending": action
            }
            
        except Exception as e:
            error_msg = AIMessage(content=f"I had trouble with the approval process: {str(e)}")
            return {"messages": [error_msg]}


def should_continue(state: AgentState) -> str:
    """Determine the next step in the workflow."""
    messages = state.get("messages", [])
    if not messages:
        return "end"
    
    # Check retry count to prevent infinite loops
    retry_count = state.get("retry_count", 0)
    if retry_count >= 5:
        return "end"
    
    last_message = messages[-1]
    
    # Check if we need approval
    if state.get("requires_approval", False):
        return "approval"
    
    # Check if we need supervision (complex planning)
    if hasattr(last_message, 'content') and any(keyword in last_message.content.lower() 
                                               for keyword in ["plan", "organize", "coordinate", "manage"]):
        return "supervisor"
    
    # Check if we have tool calls
    if hasattr(last_message, 'tool_calls') and last_message.tool_calls:
        return "tools"
    
    # Check if we have a plan to execute
    if state.get("current_plan") and not state.get("plan_complete", False):
        return "tools"
    
    # Default to end
    return "end"


def should_get_approval(state: AgentState) -> bool:
    """Check if an action requires approval."""
    pending_actions = state.get("pending_actions", [])
    if not pending_actions:
        return False
    
    action = pending_actions[0]
    action_type = action.get("type", "")
    amount = action.get("amount", 0)
    
    # Require approval for high-value actions
    if action_type == "book_vendor" and amount > 10000:
        return True
    if action_type == "book_venue" and amount > 15000:
        return True
    if action_type == "delete_event":
        return True
    
    return False
