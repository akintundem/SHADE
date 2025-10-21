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


class FlowManager:
    """LangGraph Flow Manager for orchestrating domain agents."""
    
    def __init__(self, domain_agents: Dict[str, Any], shared_context: SharedContextMemory):
        """Initialize the flow manager."""
        self.domain_agents = domain_agents
        self.shared_context = shared_context
        self.observability = ObservabilityHooks()
        self.routing_node = RoutingNode(domain_agents, shared_context)
        self.aggregator_node = AggregatorNode(shared_context)
        self.graph = None
        self._build_graph()
        
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
                    domain_responses[domain] = response
                    
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
                "timestamp": datetime.utcnow().isoformat()
            }
            
            # Execute the graph
            result = await self.graph.ainvoke(initial_state)
            
            # Extract response
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
            logger.error(f"Error in flow processing: {e}")
            return {
                "reply": f"I encountered an error while processing your request: {str(e)}",
                "agent_used": "LangGraph Flow (Error)",
                "data": {"error": str(e)},
                "multi_agent": False
            }
    
    async def get_flow_stats(self) -> Dict[str, Any]:
        """Get flow manager statistics."""
        return {
            "domain_agents": list(self.domain_agents.keys()),
            "shared_context_stats": await self.shared_context.get_stats(),
            "observability_stats": self.observability.get_stats(),
            "graph_compiled": self.graph is not None
        }
