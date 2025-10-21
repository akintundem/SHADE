"""Mock Payment API for prototyping."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
from datetime import datetime, timedelta
import random
import uuid

logger = logging.getLogger(__name__)


class PaymentAPIService:
    """Mock Payment API for prototyping - returns consistent payment data."""
    
    def __init__(self):
        """Initialize mock payment API service."""
        self.initialized = False
        self.mock_payment_data = self._initialize_mock_payment_data()
        self.payment_history = []
    
    def _initialize_mock_payment_data(self) -> Dict[str, Any]:
        """Initialize with mock payment data for consistent responses."""
        return {
            "payment_methods": [
                {
                    "id": "pm_1",
                    "type": "card",
                    "last4": "4242",
                    "brand": "Visa",
                    "exp_month": 12,
                    "exp_year": 2025,
                    "is_default": True
                },
                {
                    "id": "pm_2",
                    "type": "card", 
                    "last4": "5555",
                    "brand": "Mastercard",
                    "exp_month": 8,
                    "exp_year": 2026,
                    "is_default": False
                }
            ],
            "invoices": [
                {
                    "id": "inv_1",
                    "amount": 2500.00,
                    "currency": "USD",
                    "status": "paid",
                    "description": "Wedding Venue Deposit",
                    "created": "2024-01-15T10:30:00Z",
                    "paid": "2024-01-15T10:35:00Z"
                },
                {
                    "id": "inv_2",
                    "amount": 1200.00,
                    "currency": "USD", 
                    "status": "pending",
                    "description": "Catering Services",
                    "created": "2024-01-20T14:15:00Z",
                    "due_date": "2024-02-20T23:59:59Z"
                }
            ],
            "transactions": [
                {
                    "id": "txn_1",
                    "amount": 2500.00,
                    "currency": "USD",
                    "status": "succeeded",
                    "description": "Wedding Venue Deposit",
                    "payment_method": "pm_1",
                    "created": "2024-01-15T10:35:00Z"
                },
                {
                    "id": "txn_2",
                    "amount": 500.00,
                    "currency": "USD",
                    "status": "succeeded", 
                    "description": "Photography Retainer",
                    "payment_method": "pm_1",
                    "created": "2024-01-18T16:20:00Z"
                }
            ]
        }
    
    async def initialize(self):
        """Initialize the payment API service."""
        try:
            self.initialized = True
            logger.info("Mock Payment API service initialized")
        except Exception as e:
            logger.error(f"Error initializing Payment API: {e}")
            raise
    
    async def create_payment_intent(self, amount: float, currency: str = "USD", description: str = None) -> Dict[str, Any]:
        """Create a payment intent."""
        try:
            payment_intent = {
                "id": f"pi_{uuid.uuid4().hex[:8]}",
                "amount": amount,
                "currency": currency,
                "description": description or "Event Planning Payment",
                "status": "requires_payment_method",
                "client_secret": f"pi_{uuid.uuid4().hex[:8]}_secret_{uuid.uuid4().hex[:8]}",
                "created": datetime.utcnow().isoformat()
            }
            
            logger.info(f"Mock payment intent created: {amount} {currency}")
            return {
                "success": True,
                "payment_intent": payment_intent
            }
            
        except Exception as e:
            logger.error(f"Error creating payment intent: {e}")
            return {"success": False, "error": str(e)}
    
    async def process_payment(self, payment_intent_id: str, payment_method_id: str) -> Dict[str, Any]:
        """Process a payment."""
        try:
            # Simulate payment processing
            await asyncio.sleep(0.1)  # Simulate processing time
            
            # Mock payment result (90% success rate for prototyping)
            is_successful = random.random() > 0.1
            
            if is_successful:
                transaction = {
                    "id": f"txn_{uuid.uuid4().hex[:8]}",
                    "payment_intent_id": payment_intent_id,
                    "amount": 1000.00,  # Mock amount
                    "currency": "USD",
                    "status": "succeeded",
                    "payment_method": payment_method_id,
                    "created": datetime.utcnow().isoformat()
                }
                
                self.payment_history.append(transaction)
                
                logger.info(f"Mock payment processed successfully: {payment_intent_id}")
                return {
                    "success": True,
                    "transaction": transaction
                }
            else:
                logger.info(f"Mock payment failed: {payment_intent_id}")
                return {
                    "success": False,
                    "error": "Payment failed - insufficient funds"
                }
                
        except Exception as e:
            logger.error(f"Error processing payment: {e}")
            return {"success": False, "error": str(e)}
    
    async def create_invoice(self, customer_email: str, amount: float, currency: str = "USD", description: str = None) -> Dict[str, Any]:
        """Create an invoice."""
        try:
            invoice = {
                "id": f"inv_{uuid.uuid4().hex[:8]}",
                "customer_email": customer_email,
                "amount": amount,
                "currency": currency,
                "description": description or "Event Planning Services",
                "status": "draft",
                "created": datetime.utcnow().isoformat(),
                "due_date": (datetime.utcnow() + timedelta(days=30)).isoformat()
            }
            
            self.mock_payment_data["invoices"].append(invoice)
            
            logger.info(f"Mock invoice created for {customer_email}: {amount} {currency}")
            return {
                "success": True,
                "invoice": invoice
            }
            
        except Exception as e:
            logger.error(f"Error creating invoice: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_payment_methods(self, customer_id: str = None) -> Dict[str, Any]:
        """Get payment methods for a customer."""
        try:
            payment_methods = self.mock_payment_data["payment_methods"].copy()
            
            # Add customer-specific metadata
            for pm in payment_methods:
                pm["customer_id"] = customer_id or "cus_mock"
                pm["created"] = datetime.utcnow().isoformat()
            
            logger.info(f"Mock payment methods retrieved for customer: {customer_id}")
            return {
                "success": True,
                "payment_methods": payment_methods
            }
            
        except Exception as e:
            logger.error(f"Error getting payment methods: {e}")
            return {"success": False, "error": str(e)}
    
    async def add_payment_method(self, customer_id: str, card_token: str) -> Dict[str, Any]:
        """Add a new payment method."""
        try:
            # Mock card details from token
            card_details = {
                "id": f"pm_{uuid.uuid4().hex[:8]}",
                "type": "card",
                "last4": str(random.randint(1000, 9999)),
                "brand": random.choice(["Visa", "Mastercard", "American Express"]),
                "exp_month": random.randint(1, 12),
                "exp_year": random.randint(2025, 2030),
                "is_default": False,
                "customer_id": customer_id,
                "created": datetime.utcnow().isoformat()
            }
            
            self.mock_payment_data["payment_methods"].append(card_details)
            
            logger.info(f"Mock payment method added for customer: {customer_id}")
            return {
                "success": True,
                "payment_method": card_details
            }
            
        except Exception as e:
            logger.error(f"Error adding payment method: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_transactions(self, customer_id: str = None, limit: int = 10) -> Dict[str, Any]:
        """Get transaction history."""
        try:
            transactions = self.mock_payment_data["transactions"].copy()
            
            # Add payment history if available
            transactions.extend(self.payment_history)
            
            # Filter by customer if provided
            if customer_id:
                transactions = [t for t in transactions if t.get("customer_id") == customer_id]
            
            # Sort by creation date (newest first)
            transactions.sort(key=lambda x: x.get("created", ""), reverse=True)
            
            # Limit results
            transactions = transactions[:limit]
            
            logger.info(f"Mock transactions retrieved: {len(transactions)} results")
            return {
                "success": True,
                "transactions": transactions,
                "total_count": len(transactions)
            }
            
        except Exception as e:
            logger.error(f"Error getting transactions: {e}")
            return {"success": False, "error": str(e)}
    
    async def refund_payment(self, transaction_id: str, amount: float = None) -> Dict[str, Any]:
        """Refund a payment."""
        try:
            # Find the transaction
            transaction = None
            for txn in self.mock_payment_data["transactions"] + self.payment_history:
                if txn["id"] == transaction_id:
                    transaction = txn
                    break
            
            if not transaction:
                return {"success": False, "error": "Transaction not found"}
            
            # Create refund
            refund_amount = amount or transaction["amount"]
            refund = {
                "id": f"re_{uuid.uuid4().hex[:8]}",
                "transaction_id": transaction_id,
                "amount": refund_amount,
                "currency": transaction["currency"],
                "status": "succeeded",
                "created": datetime.utcnow().isoformat()
            }
            
            logger.info(f"Mock refund processed: {refund_amount} for transaction {transaction_id}")
            return {
                "success": True,
                "refund": refund
            }
            
        except Exception as e:
            logger.error(f"Error processing refund: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_payment_analytics(self, start_date: str = None, end_date: str = None) -> Dict[str, Any]:
        """Get payment analytics."""
        try:
            # Mock analytics data
            analytics = {
                "total_revenue": 15000.00,
                "total_transactions": 25,
                "success_rate": 0.96,
                "average_transaction": 600.00,
                "refund_rate": 0.02,
                "payment_methods": {
                    "card": 0.85,
                    "bank_transfer": 0.10,
                    "digital_wallet": 0.05
                },
                "currency_breakdown": {
                    "USD": 0.90,
                    "EUR": 0.08,
                    "GBP": 0.02
                }
            }
            
            logger.info("Mock payment analytics retrieved")
            return {
                "success": True,
                "analytics": analytics,
                "period": {
                    "start_date": start_date,
                    "end_date": end_date
                }
            }
            
        except Exception as e:
            logger.error(f"Error getting payment analytics: {e}")
            return {"success": False, "error": str(e)}
    
    async def get_service_stats(self) -> Dict[str, Any]:
        """Get payment API service statistics."""
        try:
            return {
                "payment_methods": len(self.mock_payment_data["payment_methods"]),
                "invoices": len(self.mock_payment_data["invoices"]),
                "transactions": len(self.mock_payment_data["transactions"]),
                "payment_history": len(self.payment_history),
                "initialized": self.initialized,
                "total_processed": len(self.payment_history) + len(self.mock_payment_data["transactions"])
            }
            
        except Exception as e:
            logger.error(f"Error getting payment service stats: {e}")
            return {"error": str(e)}
