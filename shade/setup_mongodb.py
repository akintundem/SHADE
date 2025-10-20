#!/usr/bin/env python3
"""Setup script for MongoDB collections and indexes."""

import asyncio
import os
from motor.motor_asyncio import AsyncIOMotorClient
from dotenv import load_dotenv

load_dotenv()

async def setup_mongodb():
    """Setup MongoDB collections and indexes."""
    mongo_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
    database_name = os.getenv("MONGODB_DATABASE", "event_planner")
    
    client = AsyncIOMotorClient(mongo_url)
    db = client[database_name]
    
    try:
        # Test connection
        await client.admin.command('ping')
        print("✅ Connected to MongoDB successfully!")
        
        # Create collections with validation
        await db.create_collection("chats")
        await db.create_collection("events")
        print("✅ Created collections successfully!")
        
        # Create indexes for better performance
        await db.chats.create_index("user_id")
        await db.chats.create_index("event_id")
        await db.chats.create_index("status")
        await db.chats.create_index("created_at")
        
        await db.events.create_index("chat_id")
        await db.events.create_index("created_at")
        await db.events.create_index("updated_at")
        
        print("✅ Created indexes successfully!")
        
        # Create sample data structure documentation
        sample_chat = {
            "user_id": "sample_user_123",
            "event_id": "sample_event_456",
            "created_at": "2024-01-01T00:00:00Z",
            "updated_at": "2024-01-01T00:00:00Z",
            "status": "active",
            "messages": [
                {
                    "message": "Create an event",
                    "is_user": True,
                    "tool_used": None,
                    "data": None,
                    "timestamp": "2024-01-01T00:00:00Z"
                },
                {
                    "message": "What should we call this event?",
                    "is_user": False,
                    "tool_used": "EventTool",
                    "data": {},
                    "timestamp": "2024-01-01T00:00:01Z"
                }
            ],
            "conversation_state": {
                "active_tool": "EventTool",
                "tool_instance": None
            }
        }
        
        sample_event = {
            "chat_id": "sample_chat_123",
            "created_at": "2024-01-01T00:00:00Z",
            "updated_at": "2024-01-01T00:00:00Z",
            "name": "Sample Event",
            "eventType": "CONFERENCE",
            "startDateTime": "2024-12-25T18:00:00Z",
            "description": "A sample event for testing",
            "capacity": 100,
            "isPublic": True,
            "requiresApproval": False,
            "qrCodeEnabled": True
        }
        
        print("📋 Sample Chat Document Structure:")
        print(f"   - user_id: {sample_chat['user_id']}")
        print(f"   - event_id: {sample_chat['event_id']}")
        print(f"   - status: {sample_chat['status']}")
        print(f"   - messages: {len(sample_chat['messages'])} messages")
        print(f"   - conversation_state: {sample_chat['conversation_state']}")
        
        print("\n📋 Sample Event Document Structure:")
        print(f"   - chat_id: {sample_event['chat_id']}")
        print(f"   - name: {sample_event['name']}")
        print(f"   - eventType: {sample_event['eventType']}")
        print(f"   - startDateTime: {sample_event['startDateTime']}")
        print(f"   - capacity: {sample_event['capacity']}")
        
        print("\n🎉 MongoDB setup completed successfully!")
        print(f"Database: {database_name}")
        print("Collections: chats, events")
        print("Ready for Java integration! 🚀")
        
    except Exception as e:
        print(f"❌ Error setting up MongoDB: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    asyncio.run(setup_mongodb())
