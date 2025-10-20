# Testing the Conversational Event Tool

## 🚀 **Web App is Running!**

The chat interface is now available at: **http://localhost:8000**

## 🧪 **How to Test the Event Tool**

### **1. Open the Web Interface**
- Go to http://localhost:8000 in your browser
- You'll see a chat interface with a dark theme

### **2. Start the Conversational Flow**
Type one of these messages to begin:
- `Create an event`
- `I want to create an event`
- `Help me plan an event`

### **3. Follow the Conversation**
The AI will guide you through filling out the CreateEventRequest DTO:

**Step 1: Event Name**
```
You: Create an event
AI: I need to know the event name. What would you like to call this event?
```

**Step 2: Event Type**
```
You: Tech Summit 2024
AI: I need to know the event type. What type of event is this? (CONFERENCE, MEETING, WORKSHOP, PARTY, etc.)
```

**Step 3: Start Date**
```
You: It's a conference
AI: I need to know the start date and time. When does the event start? Please provide date and time
```

**Step 4: Optional Fields**
```
You: June 15, 2024 at 9 AM
AI: Great! Now, would you like to provide the event description? (You can say 'skip' to move to the next field or 'done' to finish)
```

### **4. Use Special Commands**
- `review` - See current event summary
- `skip` - Skip current optional field
- `done` - Complete event creation
- `summary` - Show detailed information

### **5. Complete the Event**
```
You: done
AI: 🎉 Event created successfully!
    📋 Event Summary:
    Required Information:
    • Event name: Tech Summit 2024
    • Event type: CONFERENCE
    • Start date and time: 2024-06-15T09:00:00
    Additional Information:
    • Event description: Annual tech conference
```

## 🎯 **What You're Testing**

1. **Conversational Flow**: Natural back-and-forth conversation
2. **DTO Compliance**: Follows CreateEventRequest structure exactly
3. **Required Fields**: Ensures name, eventType, startDateTime are collected
4. **Optional Fields**: Guides through additional information
5. **User Control**: Can skip fields or finish early
6. **Progress Tracking**: Shows what's been filled and what's missing

## 🔧 **Technical Notes**

- The EventTool uses AI to extract information from natural language
- It maintains conversation state throughout the process
- All field names match the Java CreateEventRequest DTO exactly
- The tool handles various date formats and natural language input
- Weather tool is also available for testing

## 🎉 **Ready to Test!**

Open http://localhost:8000 and start chatting with the AI to create events!
