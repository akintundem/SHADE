"""LangGraph Flow Manager for orchestrating domain agents."""

from typing import Dict, Any, List, Optional, TypedDict
from langgraph.graph import StateGraph, END
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
import asyncio
import logging
from datetime import datetime

from .routing_node import RoutingNode
from .aggregator_node import AggregatorNode
from .shared_context import SharedContextMemory
from .observability import ObservabilityHooks
from .workflow_orchestrator import WorkflowOrchestrator
from .agent_coordinator import AgentCoordinator

logger = logging.getLogger(__name__)


class FlowState(TypedDict):
    """State for the LangGraph flow."""
    messages: List[BaseMessage]
    user_id: str
    chat_id: str
    event_id: Optional[str]
    current_domain: Optional[str]
    domain_responses: Dict[str, Any]
    shared_context: Dict[str, Any]
    routing_decision: Optional[str]
    final_response: Optional[str]
    metadata: Dict[str, Any]
    timestamp: str
    workflow_context: Optional[Dict[str, Any]]
    workflow_execution_id: Optional[str]


class FlowManager:
    """LangGraph Flow Manager for orchestrating domain agents."""
    
    def __init__(self, domain_agents: Dict[str, Any], shared_context: SharedContextMemory):
        """Initialize the flow manager."""
        self.domain_agents = domain_agents
        self.shared_context = shared_context
        self.observability = ObservabilityHooks()
        self.routing_node = RoutingNode(domain_agents, shared_context)
        self.aggregator_node = AggregatorNode(shared_context)
        self.workflow_orchestrator = WorkflowOrchestrator()
        self.agent_coordinator = AgentCoordinator(domain_agents)
        self.graph = None
        self._build_graph()
        
        # Set up coordination between orchestrator and coordinator
        self.workflow_orchestrator.set_agent_coordinator(self.agent_coordinator)
        
    def _build_graph(self):
        """Build the LangGraph workflow."""
        workflow = StateGraph(FlowState)
        
        # Add nodes
        workflow.add_node("routing", self.routing_node.route)
        workflow.add_node("domain_processing", self._domain_processing)
        workflow.add_node("aggregation", self.aggregator_node.aggregate)
        workflow.add_node("final_response", self._final_response)
        
        # Add edges
        workflow.set_entry_point("routing")
        workflow.add_edge("routing", "domain_processing")
        workflow.add_edge("domain_processing", "aggregation")
        workflow.add_edge("aggregation", "final_response")
        workflow.add_edge("final_response", END)
        
        # Compile the graph
        self.graph = workflow.compile()
        
        # Add observability hooks
        self.observability.add_hooks(self.graph)
        
        logger.info("LangGraph flow manager initialized")
    
    async def _domain_processing(self, state: FlowState) -> FlowState:
        """Process with domain agents."""
        try:
            routing_decision = state.get("routing_decision")
            domain_responses = {}
            
            if routing_decision == "single_domain":
                # Process with single domain agent
                domain = state.get("current_domain")
                if domain and domain in self.domain_agents:
                    agent = self.domain_agents[domain]
                    # Pass shared context for conversation continuity
                    response = await agent.process_message(
                        state["messages"][-1].content,
                        state.get("shared_context", {})
                    )
                    if response is not None:
                        domain_responses[domain] = response
                    else:
                        logger.error(f"Agent {domain} returned None response")
                        domain_responses[domain] = {
                            "agent_name": domain,
                            "response": "I'm processing your request.",
                            "tool_calls": [],
                            "tool_results": [],
                            "context": {}
                        }
                    
            elif routing_decision == "multi_domain":
                # Process with multiple domain agents
                domains = state.get("metadata", {}).get("involved_domains", [])
                
                # Create tasks for parallel processing
                tasks = []
                for domain in domains:
                    if domain in self.domain_agents:
                        agent = self.domain_agents[domain]
                        # Pass shared context for conversation continuity
                        task = agent.process_message(
                            state["messages"][-1].content,
                            state.get("shared_context", {})
                        )
                        tasks.append((domain, task))
                
                # Execute in parallel
                if tasks:
                    results = await asyncio.gather(*[task for _, task in tasks], return_exceptions=True)
                    for i, (domain, _) in enumerate(tasks):
                        if not isinstance(results[i], Exception):
                            domain_responses[domain] = results[i]
                        else:
                            logger.error(f"Error processing with {domain} agent: {results[i]}")
                            domain_responses[domain] = {"error": str(results[i])}
            
            # Update shared context with domain responses for conversation continuity
            await self.shared_context.update_context(
                state["user_id"],
                state["chat_id"],
                {"domain_responses": domain_responses}
            )
            
            return {
                **state,
                "domain_responses": domain_responses,
                "shared_context": await self.shared_context.get_context(state["user_id"], state["chat_id"])
            }
            
        except Exception as e:
            logger.error(f"Error in domain processing: {e}")
            return {
                **state,
                "domain_responses": {"error": str(e)},
                "metadata": {**state.get("metadata", {}), "error": str(e)}
            }
    
    async def _final_response(self, state: FlowState) -> FlowState:
        """Generate final response."""
        try:
            final_response = state.get("final_response", "I've processed your request.")
            
            return {
                **state,
                "final_response": final_response,
                "metadata": {
                    **state.get("metadata", {}),
                    "processing_complete": True,
                    "timestamp": datetime.utcnow().isoformat()
                }
            }
            
        except Exception as e:
            logger.error(f"Error generating final response: {e}")
            return {
                **state,
                "final_response": f"I encountered an error: {str(e)}",
                "metadata": {**state.get("metadata", {}), "error": str(e)}
            }
    
    async def process_request(self, message: str, user_id: str, chat_id: str, event_id: Optional[str] = None) -> Dict[str, Any]:
        """Process a request through the LangGraph flow."""
        try:
            print(f"DEBUG: Starting process_request for message: {message}")
            # Check if this is a workflow request
            try:
                workflow_context = await self._check_workflow_request(message, user_id, chat_id)
                print(f"DEBUG: Workflow context: {workflow_context}")
            except Exception as e:
                print(f"DEBUG ERROR in workflow check: {e}")
                import traceback
                print(f"DEBUG TRACEBACK: {traceback.format_exc()}")
                workflow_context = None
            
            # Create initial state
            initial_state: FlowState = {
                "messages": [HumanMessage(content=message)],
                "user_id": user_id,
                "chat_id": chat_id,
                "event_id": event_id,
                "current_domain": None,
                "domain_responses": {},
                "shared_context": await self.shared_context.get_context(user_id, chat_id),
                "routing_decision": None,
                "final_response": None,
                "metadata": {
                    "request_id": f"{user_id}_{chat_id}_{datetime.utcnow().timestamp()}",
                    "start_time": datetime.utcnow().isoformat()
                },
                "timestamp": datetime.utcnow().isoformat(),
                "workflow_context": workflow_context,
                "workflow_execution_id": None
            }
            
            # Execute the graph
            print(f"DEBUG: About to invoke graph")
            result = await self.graph.ainvoke(initial_state)
            print(f"DEBUG: Graph result type: {type(result)}")
            print(f"DEBUG: Graph result: {result}")
            
            # Handle None result
            if result is None:
                logger.error("Graph returned None result")
                return {
                    "reply": "I'm experiencing technical difficulties. Please try again.",
                    "agent_used": "LangGraph Flow (Error)",
                    "data": {"error": "Graph returned None"},
                    "multi_agent": False
                }
            
            # Extract response
            print(f"DEBUG: Extracting response from result")
            final_response = result.get("final_response", "I've processed your request.")
            domain_responses = result.get("domain_responses", {})
            metadata = result.get("metadata", {})
            ui_payload = result.get("ui")
            
            # Determine which agents were used
            agents_used = list(domain_responses.keys())
            
            return {
                "reply": final_response,
                "agent_used": f"LangGraph Flow: {', '.join(agents_used)}" if agents_used else "LangGraph Flow",
                "data": {
                    "domain_responses": domain_responses,
                    "shared_context": result.get("shared_context", {}),
                    "metadata": metadata,
                    "flow_complete": True
                },
                "ui": ui_payload,
                "multi_agent": len(agents_used) > 1
            }
            
        except Exception as e:
            import traceback
            logger.error(f"Error in flow processing: {e}")
            logger.error(f"Traceback: {traceback.format_exc()}")
            print(f"ERROR: {e}")
            print(f"TRACEBACK: {traceback.format_exc()}")
            return {
                "reply": f"I encountered an error while processing your request: {str(e)}",
                "agent_used": "LangGraph Flow (Error)",
                "data": {"error": str(e)},
                "multi_agent": False
            }
    
    async def _check_workflow_request(self, message: str, user_id: str, chat_id: str) -> Optional[Dict[str, Any]]:
        """Check if this is a workflow request and return workflow context."""
        message_lower = message.lower()
        
        # Check for workflow keywords
        workflow_keywords = ["workflow", "plan", "start planning", "begin planning", "create workflow"]
        if any(keyword in message_lower for keyword in workflow_keywords):
            # Determine event type from context
            context = await self.shared_context.get_context(user_id, chat_id)
            event_type = context.get("current_event", {}).get("eventType", "UNKNOWN")
            
            # Get workflow recommendations
            recommendations = await self.workflow_orchestrator.get_workflow_recommendations(event_type, context)
            
            return {
                "is_workflow_request": True,
                "event_type": event_type,
                "recommendations": recommendations,
                "available_templates": await self.workflow_orchestrator.get_available_templates()
            }
        
        return None
    
    async def start_workflow(self, template_id: str, user_id: str, chat_id: str, context: Dict[str, Any]) -> str:
        """Start a workflow execution."""
        try:
            execution_id = await self.workflow_orchestrator.start_workflow(template_id, context, user_id, chat_id)
            logger.info(f"Started workflow {execution_id} for template {template_id}")
            return execution_id
        except Exception as e:
            logger.error(f"Error starting workflow: {e}")
            raise
    
    async def get_workflow_status(self, execution_id: str) -> Dict[str, Any]:
        """Get workflow execution status."""
        try:
            return await self.workflow_orchestrator.get_workflow_status(execution_id)
        except Exception as e:
            logger.error(f"Error getting workflow status: {e}")
            return {"error": str(e)}
    
    async def resume_workflow(self, execution_id: str, user_input: Dict[str, Any]) -> bool:
        """Resume a paused workflow."""
        try:
            await self.workflow_orchestrator.resume_workflow(execution_id, user_input)
            return True
        except Exception as e:
            logger.error(f"Error resuming workflow: {e}")
            return False
    
    async def get_available_workflows(self) -> List[Dict[str, Any]]:
        """Get available workflow templates."""
        try:
            return await self.workflow_orchestrator.get_available_templates()
        except Exception as e:
            logger.error(f"Error getting available workflows: {e}")
            return []
    
    async def get_flow_stats(self) -> Dict[str, Any]:
        """Get flow manager statistics."""
        return {
            "domain_agents": list(self.domain_agents.keys()),
            "shared_context_stats": await self.shared_context.get_stats(),
            "observability_stats": self.observability.get_stats(),
            "graph_compiled": self.graph is not None,
            "workflow_orchestrator_available": self.workflow_orchestrator is not None,
            "agent_coordinator_available": self.agent_coordinator is not None
        }
