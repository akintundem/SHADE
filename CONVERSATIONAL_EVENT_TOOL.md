# Conversational Event Tool

## Overview

The EventTool has been updated to have a conversational flow that follows the `CreateEventRequest` DTO structure exactly. It guides users through filling out the form step by step, starting with required fields and then optionally collecting additional information.

## 🎯 **How It Works**

### **1. Required Fields First**
The tool ensures these fields are collected before allowing completion:
- `name` - Event name
- `eventType` - Event type (CONFERENCE, MEETING, WORKSHOP, etc.)
- `startDateTime` - Start date and time (ISO format)

### **2. Optional Fields Next**
After required fields are filled, it asks about optional fields one by one:
- `description`, `eventStatus`, `endDateTime`, `capacity`, etc.
- User can say "skip" to move to next field
- User can say "done" to finish with current information

### **3. Special Commands**
- `review` - Show current event summary
- `skip` - Skip current optional field
- `done` - Complete event creation
- `summary` - Show detailed event information

## 💬 **Example Conversation**

```
User: Create an event
AI: I need to know the event name. What would you like to call this event?

User: Tech Summit 2024
AI: I need to know the event type. What type of event is this? (CONFERENCE, MEETING, WORKSHOP, PARTY, etc.)

User: It's a conference
AI: I need to know the start date and time. When does the event start? Please provide date and time

User: June 15, 2024 at 9 AM
AI: Great! Now, would you like to provide the event description? (You can say 'skip' to move to the next field or 'done' to finish)

User: Annual tech conference with 200 attendees
AI: Great! Now, would you like to provide the event status? (You can say 'skip' to move to the next field or 'done' to finish)

User: done
AI: 🎉 Event created successfully!
    📋 Event Summary:
    Required Information:
    • Event name: Tech Summit 2024
    • Event type: CONFERENCE
    • Start date and time: 2024-06-15T09:00:00
    Additional Information:
    • Event description: Annual tech conference with 200 attendees
```

## 🛠️ **Technical Details**

### **EventTool Class Features**
- **Conversation State Tracking**: Tracks where user is in the form
- **AI-Powered Extraction**: Uses OpenAI to extract information from natural language
- **Field Validation**: Ensures required fields are filled before completion
- **Progress Tracking**: Shows what's been filled and what's missing
- **Flexible Input**: Handles various date formats and natural language

### **DTO Alignment**
The tool follows the exact field structure from `CreateEventRequest.java`:
- All 3 required fields are enforced
- All 25+ optional fields are available
- Field names match the Java DTO exactly
- Data types are properly handled (strings, booleans, integers, dates)

## 🚀 **Usage**

### **In the HTML Interface**
1. Go to http://localhost:8000
2. Type "Create an event"
3. Follow the conversational prompts
4. Use commands like "review", "skip", "done"

### **Programmatically**
```python
from shade import EventTool

tool = EventTool()
result = tool.process("Create an event")
print(result["message"])  # Shows first question

result = tool.process("Tech Summit 2024")
print(result["message"])  # Shows next question

# Continue conversation...
result = tool.process("done")
print(result["data"])  # Final event data matching CreateEventRequest DTO
```

## ✨ **Benefits**

1. **Natural Conversation**: Feels like talking to a human assistant
2. **Guided Process**: Never get lost in the form
3. **Flexible**: Can provide information in any order or skip fields
4. **Complete**: Ensures all required information is collected
5. **DTO Compliant**: Output matches Java backend exactly
6. **User Friendly**: Clear prompts and progress indicators

The conversational flow makes event creation feel natural while ensuring the Java DTO is properly filled! 🎉
