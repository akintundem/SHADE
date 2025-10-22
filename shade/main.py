"""Working main.py for Event Planner AI Tools with full agent functionality."""

import os
import sys
from pathlib import Path
from typing import Optional, Dict, Any, List
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from dotenv import load_dotenv
from structured_responses import (
    StructuredResponse, VenueCard, EmailTemplate, Chip, ActionButton,
    EventTypeChips, VenueCardBuilder, EmailTemplateBuilder, StructuredResponseBuilder
)

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

# Load environment variables
load_dotenv()

# Import the LangGraph Flow Manager
from flow import FlowManager, SharedContextMemory
from agents import MasterOrchestrator
from knowledge import RAGGateway, VectorStore, EmbeddingPipeline, DocumentLoader
from external import GoogleAPIService, WeatherAPIService, SearchAPIService, PaymentAPIService
from data import DataManager
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
    ui: Optional[dict] = None
    structured_response: Optional[StructuredResponse] = None

# LangGraph Flow Manager processor
class ShadeAgentProcessor:
    def __init__(self):
        """Initialize the LangGraph Flow Manager on startup."""
        print("🔄 Initializing LangGraph Flow Manager...")
        try:
            # Initialize data layer
            self.data_manager = DataManager()
            
            # Initialize shared context memory
            self.shared_context = SharedContextMemory()
            
            # Initialize knowledge layer
            self.vector_store = VectorStore()
            self.embedding_pipeline = EmbeddingPipeline()
            self.document_loader = DocumentLoader()
            self.rag_gateway = RAGGateway(self.vector_store, self.embedding_pipeline)
            
            # Initialize external APIs
            self.google_apis = GoogleAPIService()
            self.weather_api = WeatherAPIService()
            self.search_api = SearchAPIService()
            self.payment_api = PaymentAPIService()
            
            # Initialize domain agents
            self.orchestrator = MasterOrchestrator()
            
            # Initialize LangGraph Flow Manager
            self.flow_manager = FlowManager(
                domain_agents=self.orchestrator.agents,
                shared_context=self.shared_context
            )
            
            print("✅ LangGraph Flow Manager initialized successfully")
            print(f"📊 Available agents: {list(self.orchestrator.agents.keys())}")
            print("🧠 Knowledge layer: RAG Gateway with mock data")
            print("🔌 External APIs: Google, Weather, Search, Payment (mocked)")
            print("💾 Data layer: MongoDB, Redis, Async Queue")
            print("🔄 LangGraph flow with routing, domain processing, and aggregation")
        except Exception as e:
            import traceback
            print(f"❌ Error initializing LangGraph Flow Manager: {e}")
            print(f"❌ Full traceback: {traceback.format_exc()}")
            self.flow_manager = None
            self.orchestrator = None
            self.data_manager = None
    
    async def process(self, message: str, user_id: str = "default_user", chat_id: str = "default_chat", event_id: str = None) -> Dict[str, Any]:
        """Process message using the LangGraph Flow Manager."""
        if not self.flow_manager:
            return {
                "reply": "I'm currently experiencing technical difficulties. Please try again later.",
                "tool_used": "error",
                "data": None
            }
        
        try:
            print(f"🔄 LangGraph Flow processing: '{message}'")
            
            # Check for structured response patterns
            structured_response = await self._create_structured_response(message, user_id, chat_id, event_id)
            
            # Process with LangGraph Flow Manager
            result = await self.flow_manager.process_request(message, user_id, chat_id, event_id)
            
            # Save conversation to data layer (disabled for now to focus on tool issues)
            # if self.data_manager:
            #     try:
            #         await self.data_manager.save_conversation(
            #             user_id, chat_id, message, result["reply"], 
            #             {"agent_used": result.get("agent_used", "langgraph_flow")}
            #         )
            #     except Exception as e:
            #         print(f"Error saving conversation: {e}")
            #         # Continue without failing the request
            
            print(f"✅ LangGraph Flow completed processing")
            
            return {
                "reply": "" if structured_response is not None else result["reply"],
                "tool_used": result.get("agent_used", "langgraph_flow"),
                "data": result.get("data", {}),
                "ui": result.get("ui"),
                "structured_response": structured_response
            }
            
        except Exception as e:
            print(f"❌ Error in LangGraph Flow processing: {e}")
            return {
                "reply": f"I encountered an error while processing your request: {str(e)}. Please try again!",
                "tool_used": "error",
                "data": {"error": str(e)}
            }
    
    async def _create_structured_response(self, message: str, user_id: str, chat_id: str, event_id: str = None) -> Optional[StructuredResponse]:
        """Create structured response based on message content."""
        message_lower = message.lower()
        
        # Check for venue search patterns
        if any(keyword in message_lower for keyword in ['venue', 'venues', 'find venue', 'search venue', 'wedding venue']):
            # Check if they specifically want Greek venues
            if any(keyword in message_lower for keyword in ['greece', 'greek', 'santorini', 'mykonos', 'athens', 'crete', 'rhodes']):
                venues = VenueCardBuilder.create_greek_venues()
                return StructuredResponseBuilder.create_venue_search_response(
                    venues, 
                    "I found 5 STUNNING wedding venues in Greece! 🇬🇷💍✨ Perfect for your destination wedding with Toyin! 🎉"
                )
            else:
                venues = VenueCardBuilder.create_sample_venues()
                return StructuredResponseBuilder.create_venue_search_response(
                    venues, 
                    "Here are some amazing venues I found for your event! 🏰✨"
                )
        
        # Check for email inquiry patterns
        elif any(keyword in message_lower for keyword in ['contact venue', 'send email', 'inquiry', 'email venue']):
            venues = VenueCardBuilder.create_sample_venues()
            if venues:
                event_details = {
                    'date': 'June 2024',
                    'guest_count': '200',
                    'budget': '$50,000',
                    'planner_name': 'Sarah Johnson'
                }
                email = EmailTemplateBuilder.create_venue_inquiry(venues[0], event_details)
                return StructuredResponseBuilder.create_email_review_response(email)
        
        # Check for event type selection
        elif any(keyword in message_lower for keyword in ['plan event', 'what event', 'type of event', 'event type']):
            chips = EventTypeChips.get_default_chips()
            return StructuredResponseBuilder.create_chips_response(
                chips,
                "What type of event are you planning? Choose from the options below: 🎉"
            )
        
        # Check for initial greeting
        elif any(keyword in message_lower for keyword in ['hello', 'hi', 'hey', 'start', 'begin']):
            chips = EventTypeChips.get_default_chips()
            return StructuredResponseBuilder.create_mixed_response(
                "Hey there! 👋 I'm Shade, and I'm so excited to help you plan something special! Whether it's a dreamy wedding, an unforgettable birthday bash, or a professional corporate event, I've got you covered from start to finish. 🎉",
                chips=chips
            )
        
        return None

# Initialize the LangGraph Flow Manager
shade_agent = ShadeAgentProcessor()

INTERNAL_SECRET = os.getenv("INTERNAL_ASSISTANT_SECRET", "dev-internal-secret")


def _is_internal_request(req: Request) -> bool:
    provided = req.headers.get("X-Internal-Service-Auth")
    return provided is not None and provided == INTERNAL_SECRET


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, http_request: Request):
    """Chat endpoint with LangGraph Flow functionality."""
    try:
        # Require internal header for access
        if not _is_internal_request(http_request):
            raise HTTPException(status_code=401, detail="Unauthorized")
        # Process with the LangGraph Flow Manager
        result = await shade_agent.process(
            message=request.message,
            user_id=request.user_id,
            chat_id=request.chat_id or "temp_chat",
            event_id=request.event_id
        )
        
        # Determine UI type for mobile routing
        ui_type = None
        sr = result.get("structured_response")
        if sr is not None:
            try:
                ui_type = sr.response_type  # pydantic object
            except Exception:
                pass
        if ui_type is None:
            ui_type = "chat"

        return ChatResponse(
            reply=result["reply"],
            tool_used=result["tool_used"],
            data=result.get("data"),
            show_chips=bool(result.get("structured_response") and result.get("structured_response").chips),
            chat_id=request.chat_id or "temp_chat",
            user_id=request.user_id,
            event_id=request.event_id,
            ui=result.get("ui"),
            structured_response=result.get("structured_response"),
            uitype=ui_type
        )
    except HTTPException:
        raise
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

@app.get("/health")
async def health():
    """Health check endpoint."""
    return {"status": "healthy", "agent": "Shade AI LangGraph Flow Manager", "version": "3.0.0"}

@app.get("/agents/status")
async def get_agent_status():
    """Get status of all agents."""
    try:
        if shade_agent.orchestrator:
            status = shade_agent.orchestrator.get_agent_status()
            return {"agents": status}
        else:
            return {"error": "Orchestrator not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/flow/status")
async def get_flow_status():
    """Get LangGraph Flow status."""
    try:
        if shade_agent.flow_manager:
            stats = await shade_agent.flow_manager.get_flow_stats()
            return stats
        else:
            return {"error": "Flow Manager not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/agents/communication")
async def get_communication_stats():
    """Get communication statistics."""
    try:
        if shade_agent.orchestrator:
            stats = await shade_agent.orchestrator.get_communication_stats()
            return stats
        else:
            return {"error": "Orchestrator not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/knowledge/status")
async def get_knowledge_status():
    """Get knowledge layer status."""
    try:
        if shade_agent.rag_gateway:
            stats = await shade_agent.rag_gateway.get_gateway_stats()
            return stats
        else:
            return {"error": "RAG Gateway not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/external/status")
async def get_external_apis_status():
    """Get external APIs status."""
    try:
        status = {}
        
        if shade_agent.google_apis:
            status["google"] = await shade_agent.google_apis.get_service_stats()
        
        if shade_agent.weather_api:
            status["weather"] = await shade_agent.weather_api.get_service_stats()
            
        if shade_agent.search_api:
            status["search"] = await shade_agent.search_api.get_service_stats()
            
        if shade_agent.payment_api:
            status["payment"] = await shade_agent.payment_api.get_service_stats()
        
        return status
    except Exception as e:
        return {"error": str(e)}

@app.get("/external/weather/current")
async def get_current_weather(location: str = "New York, NY"):
    """Get current weather."""
    try:
        if shade_agent.weather_api:
            result = await shade_agent.weather_api.get_current_weather(location)
            return result
        else:
            return {"error": "Weather API not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/external/weather/forecast")
async def get_weather_forecast(location: str = "New York, NY", days: int = 5):
    """Get weather forecast."""
    try:
        if shade_agent.weather_api:
            result = await shade_agent.weather_api.get_forecast(location, days)
            return result
        else:
            return {"error": "Weather API not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/external/search/venues")
async def search_venues(query: str, location: str = None, capacity: int = None):
    """Search for venues."""
    try:
        if shade_agent.search_api:
            result = await shade_agent.search_api.search_venues(query, location, capacity)
            return result
        else:
            return {"error": "Search API not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/external/search/vendors")
async def search_vendors(query: str, vendor_type: str = None, location: str = None):
    """Search for vendors."""
    try:
        if shade_agent.search_api:
            result = await shade_agent.search_api.search_vendors(query, vendor_type, location)
            return result
        else:
            return {"error": "Search API not available"}
    except Exception as e:
        return {"error": str(e)}

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

@app.get("/data/health")
async def data_health():
    """Data layer health check."""
    try:
        if shade_agent.data_manager:
            health_status = await shade_agent.data_manager.health_check()
            return health_status
        else:
            return {"error": "Data manager not available"}
    except Exception as e:
        return {"error": str(e)}

@app.get("/data/stats")
async def data_stats():
    """Get data layer statistics."""
    try:
        if shade_agent.data_manager:
            stats = await shade_agent.data_manager.get_system_stats()
            return stats
        else:
            return {"error": "Data manager not available"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/data/cleanup")
async def data_cleanup(days: int = 30):
    """Clean up old data."""
    try:
        if shade_agent.data_manager:
            results = await shade_agent.data_manager.cleanup_old_data(days)
            return {"message": f"Cleanup completed", "results": results}
        else:
            return {"error": "Data manager not available"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/data/task")
async def enqueue_task(task_name: str, payload: dict, priority: str = "normal"):
    """Enqueue a background task."""
    try:
        if shade_agent.data_manager:
            from data.async_queue import TaskPriority
            priority_enum = TaskPriority.NORMAL
            if priority.lower() == "high":
                priority_enum = TaskPriority.HIGH
            elif priority.lower() == "urgent":
                priority_enum = TaskPriority.URGENT
            elif priority.lower() == "low":
                priority_enum = TaskPriority.LOW
            
            task_id = await shade_agent.data_manager.enqueue_background_task(task_name, payload, priority_enum)
            return {"task_id": task_id, "status": "enqueued"}
        else:
            return {"error": "Data manager not available"}
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    import uvicorn
    import asyncio
    
    async def startup():
        """Initialize data layer on startup."""
        if shade_agent.data_manager:
            await shade_agent.data_manager.initialize()
            print("💾 Data layer initialized successfully")
    
    # Initialize data layer
    asyncio.run(startup())
    
    print("🚀 Starting Shade AI LangGraph Flow Manager...")
    print("📱 Web interface: http://localhost:8000")
    print("🧠 LangGraph Flow Manager initialized on startup")
    print("💾 Data layer: MongoDB, Redis, Async Queue")
    uvicorn.run(app, host="0.0.0.0", port=8000, ws="none")