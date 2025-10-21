"""Working main.py for Event Planner AI Tools with full agent functionality."""

import os
import sys
from pathlib import Path
from typing import Optional, Dict, Any, List
from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from dotenv import load_dotenv

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

# Load environment variables
load_dotenv()

# Import the real agent system
from agent import create_agent_graph, AgentState
from langchain_core.messages import HumanMessage, AIMessage

app = FastAPI(title="Event Planner AI Assistant", description="AI-powered event planning with full agent functionality")

class ChatRequest(BaseModel):
    message: str
    user_id: str
    chat_id: Optional[str] = None
    event_id: Optional[str] = None

class ChatResponse(BaseModel):
    reply: str
    tool_used: str
    data: Optional[dict] = None
    show_chips: Optional[bool] = False
    chat_id: str
    user_id: str
    event_id: Optional[str] = None

# Real agent processor using LangGraph
class ShadeAgentProcessor:
    def __init__(self):
        """Initialize the real agent with LangGraph on startup."""
        print("🔄 Initializing agent graph...")
        try:
            self.agent_graph = create_agent_graph()
            print("✅ Real agent initialized successfully with LangGraph")
        except Exception as e:
            print(f"❌ Error initializing agent: {e}")
            # Fallback to simple graph if full graph fails
            try:
                from agent.graph import create_simple_agent_graph
                self.agent_graph = create_simple_agent_graph()
                print("✅ Fallback to simple agent graph")
            except Exception as e2:
                print(f"❌ Fallback also failed: {e2}")
                self.agent_graph = None
    
    async def process(self, message: str, user_id: str = "default_user", chat_id: str = "default_chat", event_id: str = None) -> Dict[str, Any]:
        """Process message using the real LangGraph agent."""
        if not self.agent_graph:
            return {
                "reply": "I'm currently experiencing technical difficulties. Please try again later.",
                "tool_used": "error",
                "data": None
            }
        
        try:
            print(f"🤖 Real agent processing: '{message}'")
            
            # Create initial state
            initial_state = {
                "messages": [HumanMessage(content=message)],
                "current_user_id": user_id,
                "current_chat_id": chat_id,
                "current_event_id": event_id,
                "pending_actions": [],
                "user_preferences": {},
                "requires_approval": False,
                "approval_pending": None,
                "tool_history": [],
                "last_tool_results": [],
                "conversation_summary": None,
                "mentioned_entities": {},
                "current_plan": None,
                "plan_step": 0,
                "plan_complete": False,
                "retry_count": 0,
                "last_error": None
            }
            
            # Run the agent graph
            result = await self.agent_graph.ainvoke(initial_state)
            
            # Extract the response
            messages = result.get("messages", [])
            if messages:
                last_message = messages[-1]
                if hasattr(last_message, 'content'):
                    reply = last_message.content
                else:
                    reply = str(last_message)
            else:
                reply = "I processed your request but didn't generate a response."
            
            # Determine tool used from the last message
            tool_used = "agent_reasoning"
            if hasattr(last_message, 'tool_calls') and last_message.tool_calls:
                tool_used = last_message.tool_calls[0].get("name", "unknown_tool")
            
            # Extract any relevant data
            data = {
                "agent_state": result,
                "tool_history": result.get("tool_history", []),
                "current_plan": result.get("current_plan"),
                "requires_approval": result.get("requires_approval", False)
            }
            
            print(f"✅ Agent completed processing")
            
            return {
                "reply": reply,
                "tool_used": tool_used,
                "data": data
            }
            
        except Exception as e:
            print(f"❌ Error in agent processing: {e}")
            return {
                "reply": f"I encountered an error while processing your request: {str(e)}. Please try again!",
                "tool_used": "error",
                "data": {"error": str(e)}
            }

# Initialize the real agent
shade_agent = ShadeAgentProcessor()

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Chat endpoint with real agent functionality."""
    try:
        # Process with the real agent
        result = await shade_agent.process(
            message=request.message,
            user_id=request.user_id,
            chat_id=request.chat_id or "temp_chat",
            event_id=request.event_id
        )
        
        return ChatResponse(
            reply=result["reply"],
            tool_used=result["tool_used"],
            data=result.get("data"),
            show_chips=False,
            chat_id=request.chat_id or "temp_chat",
            user_id=request.user_id,
            event_id=request.event_id
        )
    except Exception as e:
        print(f"❌ Error processing message: {e}")
        return ChatResponse(
            reply=f"Oops! I encountered an error: {str(e)}. Please try again! 😊",
            tool_used="Error",
            data=None,
            show_chips=False,
            chat_id=request.chat_id or "error",
            user_id=request.user_id,
            event_id=request.event_id
        )

@app.post("/reset")
async def reset_conversation():
    """Reset conversation state."""
    return {"message": "Conversation state reset successfully"}

@app.get("/", response_class=HTMLResponse)
async def home():
    """Enhanced HTML interface for the full agent."""
    # Read HTML from separate file
    html_file_path = Path(__file__).parent / "index.html"
    try:
        with open(html_file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        return HTMLResponse(
            content="<h1>Error: index.html not found</h1><p>Please ensure index.html exists in the shade directory.</p>",
            status_code=404
        )

if __name__ == "__main__":
    import uvicorn
    print("🚀 Starting Shade AI Agent...")
    print("📱 Web interface: http://localhost:8000")
    print("🧠 Agent initialized on startup")
    uvicorn.run(app, host="0.0.0.0", port=8000, ws="none")