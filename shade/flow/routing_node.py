"""Routing node for determining which domain agents to involve."""

from typing import Dict, Any, List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
import logging
import re

logger = logging.getLogger(__name__)


class RoutingNode:
    """Routing node for LangGraph flow."""
    
    def __init__(self, domain_agents: Dict[str, Any], shared_context):
        """Initialize routing node."""
        self.domain_agents = domain_agents
        self.shared_context = shared_context
        self.model = ChatOpenAI(model="gpt-4o", temperature=0.1)
        
        # Domain keywords for routing
        self.domain_keywords = {
            "event": ["event", "planning", "timeline", "schedule", "coordinate", "organize", "ceremony", "reception"],
            "budget": ["budget", "cost", "price", "financial", "expense", "money", "afford", "cheap", "expensive"],
            "venue": ["venue", "location", "place", "space", "room", "hall", "outdoor", "indoor", "capacity"],
            "vendor": ["vendor", "caterer", "photographer", "decorator", "supplier", "service", "catering", "music"],
            "risk": ["risk", "backup", "contingency", "safety", "emergency", "problem", "issue", "concern"],
            "attendee": ["attendee", "guest", "invite", "rsvp", "people", "list", "invitation", "guest list"],
            "weather": ["weather", "outdoor", "rain", "sunny", "forecast", "climate", "temperature", "storm"],
            "outreach": ["email", "invitation", "notification", "announce", "message", "communication", "invite"]
        }
    
    async def route(self, state: Dict[str, Any]) -> Dict[str, Any]:
        """Route the request to appropriate domain agents."""
        try:
            message = state["messages"][-1].content
            user_id = state["user_id"]
            chat_id = state["chat_id"]
            
            # Get shared context
            shared_context = await self.shared_context.get_context(user_id, chat_id)
            
            # Determine routing decision
            routing_decision = await self._determine_routing(message, shared_context)
            
            # Update state with routing decision
            updated_state = {
                **state,
                "routing_decision": routing_decision["decision"],
                "current_domain": routing_decision.get("primary_domain"),
                "metadata": {
                    **state.get("metadata", {}),
                    "involved_domains": routing_decision.get("involved_domains", []),
                    "routing_confidence": routing_decision.get("confidence", 0.0),
                    "routing_reasoning": routing_decision.get("reasoning", "")
                }
            }
            
            logger.info(f"Routing decision: {routing_decision}")
            return updated_state
            
        except Exception as e:
            logger.error(f"Error in routing: {e}")
            return {
                **state,
                "routing_decision": "error",
                "metadata": {
                    **state.get("metadata", {}),
                    "routing_error": str(e)
                }
            }
    
    async def _determine_routing(self, message: str, shared_context: Dict[str, Any]) -> Dict[str, Any]:
        """Determine which domain agents should handle the request."""
        try:
            # Keyword-based routing
            keyword_analysis = self._analyze_keywords(message)
            
            # LLM-based routing for complex cases
            llm_analysis = await self._llm_routing_analysis(message, shared_context)
            
            # Combine analyses - prefer LLM analysis over keyword matching
            involved_domains = llm_analysis["domains"] if llm_analysis["domains"] else keyword_analysis["domains"]
            confidence = llm_analysis["confidence"] if llm_analysis["confidence"] > 0.3 else keyword_analysis["confidence"]
            
            # Simplify routing - prefer single domain
            # For general planning questions, default to event agent only
            if len(involved_domains) == 0 or len(involved_domains) > 2:
                # Default to event domain for general planning or too many domains
                decision = "single_domain"
                primary_domain = "event"
                involved_domains = ["event"]
                confidence = 0.5
            elif len(involved_domains) == 1:
                decision = "single_domain"
                primary_domain = involved_domains[0]
            else:
                # Only use multi-domain if there are exactly 2 highly relevant domains
                if confidence > 0.7:
                    decision = "multi_domain"
                    primary_domain = involved_domains[0]
                    # Sort by confidence if available
                    if "domain_confidence" in llm_analysis:
                        involved_domains.sort(
                            key=lambda d: llm_analysis["domain_confidence"].get(d, 0.5),
                            reverse=True
                        )
                else:
                    # Low confidence - just use primary domain
                    decision = "single_domain"
                    primary_domain = involved_domains[0]
                    involved_domains = [involved_domains[0]]
            
            return {
                "decision": decision,
                "primary_domain": primary_domain,
                "involved_domains": involved_domains,
                "confidence": confidence,
                "reasoning": f"Keywords: {keyword_analysis['reasoning']}, LLM: {llm_analysis['reasoning']}"
            }
            
        except Exception as e:
            logger.error(f"Error in routing determination: {e}")
            return {
                "decision": "single_domain",
                "primary_domain": "event",
                "involved_domains": ["event"],
                "confidence": 0.3,
                "reasoning": f"Error in routing: {str(e)}"
            }
    
    def _analyze_keywords(self, message: str) -> Dict[str, Any]:
        """Analyze message for domain keywords."""
        message_lower = message.lower()
        domain_scores = {}
        
        for domain, keywords in self.domain_keywords.items():
            score = sum(1 for keyword in keywords if keyword in message_lower)
            domain_scores[domain] = score
        
        # Get domains with scores > 0
        involved_domains = [domain for domain, score in domain_scores.items() if score > 0]
        
        # Calculate confidence based on keyword matches
        total_keywords = sum(domain_scores.values())
        confidence = min(total_keywords / 5.0, 1.0) if total_keywords > 0 else 0.0
        
        return {
            "domains": involved_domains,
            "confidence": confidence,
            "reasoning": f"Keyword matches: {dict(domain_scores)}"
        }
    
    async def _llm_routing_analysis(self, message: str, shared_context: Dict[str, Any]) -> Dict[str, Any]:
        """Use LLM to analyze routing requirements."""
        try:
            routing_prompt = f"""
            Analyze this event planning request and determine which specialized agents should handle it.
            
            Available agents: {list(self.domain_agents.keys())}
            
            Request: {message}
            
            Context: {shared_context}
            
            Return a JSON response with:
            - domains: list of agent names that should handle this request
            - confidence: confidence score (0.0 to 1.0)
            - reasoning: explanation of the routing decision
            - domain_confidence: dict mapping each domain to its confidence score
            """
            
            messages = [
                SystemMessage(content=routing_prompt),
                HumanMessage(content="Please analyze this request for routing.")
            ]
            
            response = await self.model.ainvoke(messages)
            content = response.content if hasattr(response, 'content') else str(response)
            
            # Parse JSON response
            import json
            try:
                # Extract JSON from response
                json_match = re.search(r'\{.*\}', content, re.DOTALL)
                if json_match:
                    analysis = json.loads(json_match.group())
                    return {
                        "domains": analysis.get("domains", []),
                        "confidence": analysis.get("confidence", 0.5),
                        "reasoning": analysis.get("reasoning", ""),
                        "domain_confidence": analysis.get("domain_confidence", {})
                    }
            except json.JSONDecodeError:
                pass
            
            # Fallback to keyword analysis
            return {
                "domains": [],
                "confidence": 0.3,
                "reasoning": "LLM analysis failed, using fallback",
                "domain_confidence": {}
            }
            
        except Exception as e:
            logger.error(f"Error in LLM routing analysis: {e}")
            return {
                "domains": [],
                "confidence": 0.3,
                "reasoning": f"LLM analysis error: {str(e)}",
                "domain_confidence": {}
            }
