"""Aggregator node for combining domain agent responses."""

from typing import Dict, Any, List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
import logging

logger = logging.getLogger(__name__)


class AggregatorNode:
    """Aggregator node for combining domain agent responses."""
    
    def __init__(self, shared_context):
        """Initialize aggregator node."""
        self.shared_context = shared_context
        self.model = ChatOpenAI(model="gpt-4o", temperature=0.3)
    
    async def aggregate(self, state: Dict[str, Any]) -> Dict[str, Any]:
        """Aggregate responses from domain agents."""
        try:
            domain_responses = state.get("domain_responses", {})
            shared_context = state.get("shared_context", {})
            user_message = state["messages"][-1].content
            
            if not domain_responses:
                return {
                    **state,
                    "final_response": "I received your request but couldn't process it with any specialized agents.",
                    "metadata": {
                        **state.get("metadata", {}),
                        "aggregation_method": "no_responses"
                    }
                }
            
            # Determine aggregation strategy
            if len(domain_responses) == 1:
                # Single domain response
                domain = list(domain_responses.keys())[0]
                response = domain_responses[domain]
                final_response = self._extract_response_content(response)
                ui_payload = self._extract_ui_payload(response)
                
                return {
                    **state,
                    "final_response": final_response,
                    "ui": ui_payload,
                    "metadata": {
                        **state.get("metadata", {}),
                        "aggregation_method": "single_domain",
                        "primary_domain": domain
                    }
                }
            else:
                # Multi-domain aggregation
                final_response = await self._aggregate_multi_domain(
                    user_message, domain_responses, shared_context
                )
                
                return {
                    **state,
                    "final_response": final_response,
                    "ui": None,
                    "metadata": {
                        **state.get("metadata", {}),
                        "aggregation_method": "multi_domain",
                        "involved_domains": list(domain_responses.keys())
                    }
                }
                
        except Exception as e:
            logger.error(f"Error in aggregation: {e}")
            return {
                **state,
                "final_response": f"I encountered an error while combining responses: {str(e)}",
                "metadata": {
                    **state.get("metadata", {}),
                    "aggregation_error": str(e)
                }
            }
    
    async def _aggregate_multi_domain(self, user_message: str, domain_responses: Dict[str, Any], shared_context: Dict[str, Any]) -> str:
        """Aggregate responses from multiple domain agents."""
        try:
            # Prepare domain responses for synthesis
            response_summary = []
            for domain, response in domain_responses.items():
                content = self._extract_response_content(response)
                response_summary.append(f"{domain.upper()} Agent: {content}")
            
            # Create synthesis prompt
            synthesis_prompt = f"""
            You are synthesizing responses from multiple specialized event planning agents.
            
            Original user request: {user_message}
            
            Agent responses:
            {chr(10).join(response_summary)}
            
            Context: {shared_context}
            
            Please create a comprehensive, coherent response that:
            1. Addresses the user's original request
            2. Synthesizes insights from all relevant agents
            3. Provides actionable next steps
            4. Maintains a professional, helpful tone
            5. Avoids redundancy between agent responses
            
            Focus on the most important information and provide clear guidance.
            """
            
            messages = [
                SystemMessage(content=synthesis_prompt),
                HumanMessage(content="Please synthesize these agent responses into a comprehensive reply.")
            ]
            
            response = await self.model.ainvoke(messages)
            return response.content if hasattr(response, 'content') else str(response)
            
        except Exception as e:
            logger.error(f"Error in multi-domain aggregation: {e}")
            # Fallback to simple concatenation
            fallback_responses = []
            for domain, response in domain_responses.items():
                content = self._extract_response_content(response)
                fallback_responses.append(f"{domain.upper()}: {content}")
            
            return f"I've gathered responses from multiple specialists:\n\n" + "\n\n".join(fallback_responses)
    
    def _extract_response_content(self, response: Any) -> str:
        """Extract content from domain agent response."""
        if isinstance(response, dict):
            if "response" in response:
                return response["response"]
            elif "reply" in response:
                return response["reply"]
            elif "content" in response:
                return response["content"]
            else:
                return str(response)
        elif hasattr(response, 'content'):
            return response.content
        else:
            return str(response)

    def _extract_ui_payload(self, response: Any) -> Any:
        """Extract a structured UI payload from the domain response if present."""
        # Direct UI on the response
        if isinstance(response, dict) and "ui" in response:
            return response["ui"]
        # Search tool results for a ui field
        if isinstance(response, dict) and "tool_results" in response:
            try:
                for tr in reversed(response["tool_results"]):
                    res = tr.get("result") if isinstance(tr, dict) else None
                    if isinstance(res, dict) and "ui" in res:
                        return res["ui"]
            except Exception:
                pass
        return None
    
    async def _prioritize_responses(self, domain_responses: Dict[str, Any]) -> List[tuple]:
        """Prioritize domain responses by importance."""
        priorities = {
            "risk": 1.0,      # Risk information is most critical
            "budget": 0.9,     # Budget constraints are very important
            "venue": 0.8,      # Venue affects many other decisions
            "event": 0.7,      # Core event planning
            "vendor": 0.6,     # Vendor coordination
            "attendee": 0.5,   # Guest management
            "weather": 0.4,    # Weather considerations
            "outreach": 0.3    # Communication
        }
        
        # Sort by priority
        sorted_responses = []
        for domain, response in domain_responses.items():
            priority = priorities.get(domain, 0.5)
            sorted_responses.append((priority, domain, response))
        
        sorted_responses.sort(key=lambda x: x[0], reverse=True)
        return sorted_responses
    
    async def _create_action_plan(self, domain_responses: Dict[str, Any]) -> List[str]:
        """Create an action plan from domain responses."""
        action_plan = []
        
        # Extract action items from each domain
        for domain, response in domain_responses.items():
            if isinstance(response, dict) and "data" in response:
                data = response["data"]
                if isinstance(data, dict):
                    # Look for action items in the data
                    if "actions" in data:
                        for action in data["actions"]:
                            action_plan.append(f"{domain.title()}: {action}")
                    elif "next_steps" in data:
                        for step in data["next_steps"]:
                            action_plan.append(f"{domain.title()}: {step}")
        
        return action_plan
